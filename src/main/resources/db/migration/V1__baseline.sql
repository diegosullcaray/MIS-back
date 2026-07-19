-- ============================================================================
-- V1 — Baseline · MIS Host (PostgreSQL 16+)
-- Deriva de .docs/Backend/07_DATABASE_SCHEMA_v2.2.sql, adaptado para JPA:
--   · estado / ip_origen como VARCHAR (mapeo ORM directo, sin enum/inet nativos)
--   · CREATE ... IF NOT EXISTS para reejecución segura
-- v2.1: `orden` obligatorio (>= 1) y ÚNICO por padre (DEFERRABLE) + vista
--       sistemas.v_sidebar (árbol ordenado con ruta canónica).
-- v2.2: permisos por nivel (sección/subsección/módulo) con herencia
--       descendente + vista iam.v_permisos_efectivos.
-- La seguridad (roles, grants, RLS) va en V2; el seed en V3.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid() + crypt() (BCrypt)
CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- búsqueda difusa
CREATE EXTENSION IF NOT EXISTS citext;     -- email case-insensitive

CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS sistemas;
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS auditoria;

-- Dominios reutilizables (formato en una sola definición)
DROP DOMAIN IF EXISTS dom_slug CASCADE;
CREATE DOMAIN dom_slug AS VARCHAR(80)
    CHECK (VALUE ~ '^[a-z0-9]+(-[a-z0-9]+)*$');

DROP DOMAIN IF EXISTS dom_email CASCADE;
CREATE DOMAIN dom_email AS CITEXT
    CHECK (VALUE ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$');

-- ─── IAM: roles y usuarios ────────────────────────────────────────────────────

CREATE TABLE iam.roles (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre         VARCHAR(80) NOT NULL,
    slug           dom_slug    NOT NULL UNIQUE,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE iam.usuarios (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre         VARCHAR(120) NOT NULL,
    email          dom_email    NOT NULL UNIQUE,
    rol_id         UUID         NOT NULL REFERENCES iam.roles (id) ON DELETE RESTRICT,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_usuarios_rol     ON iam.usuarios (rol_id);
CREATE INDEX idx_usuarios_activos ON iam.usuarios (creado_en DESC) WHERE activo;
CREATE INDEX idx_usuarios_busqueda ON iam.usuarios
    USING gin ((nombre || ' ' || email::text) gin_trgm_ops);

CREATE TABLE iam.credenciales (
    usuario_id        UUID PRIMARY KEY REFERENCES iam.usuarios (id) ON DELETE CASCADE,
    password_hash     VARCHAR(100) NOT NULL,
    debe_cambiar      BOOLEAN      NOT NULL DEFAULT FALSE,
    intentos_fallidos SMALLINT     NOT NULL DEFAULT 0,
    bloqueada_hasta   TIMESTAMPTZ,
    rotada_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ─── Sistemas registrados (Remotes) + jerarquía ───────────────────────────────

CREATE TABLE sistemas.sistemas (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre         VARCHAR(80)  NOT NULL,
    slug           dom_slug     NOT NULL UNIQUE,
    descripcion    VARCHAR(300) NOT NULL DEFAULT '',
    icono          VARCHAR(60)  NOT NULL DEFAULT 'pi pi-th-large',
    url            VARCHAR(300) NOT NULL DEFAULT '',
    version        VARCHAR(20)  NOT NULL DEFAULT '1.0.0',
    estado         VARCHAR(15)  NOT NULL DEFAULT 'inactivo'
                   CHECK (estado IN ('activo', 'mantenimiento', 'inactivo')),
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- (v2.1) `orden` es la posición explícita dentro del padre (1, 2, 3…):
-- NOT NULL sin default y ÚNICO por padre. Los UNIQUE son DEFERRABLE INITIALLY
-- DEFERRED para renumerar hermanos en una sola transacción (BE-07). El propio
-- UNIQUE (padre, orden) crea el índice compuesto — sin índices redundantes.

CREATE TABLE sistemas.secciones (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sistema_id UUID        NOT NULL REFERENCES sistemas.sistemas (id) ON DELETE CASCADE,
    nombre     VARCHAR(80) NOT NULL,
    slug       dom_slug    NOT NULL,
    icono      VARCHAR(60) NOT NULL DEFAULT 'pi pi-folder',
    orden      SMALLINT    NOT NULL CHECK (orden >= 1),
    CONSTRAINT uq_secciones_slug  UNIQUE (sistema_id, slug),
    CONSTRAINT uq_secciones_orden UNIQUE (sistema_id, orden)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE sistemas.subsecciones (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seccion_id UUID        NOT NULL REFERENCES sistemas.secciones (id) ON DELETE CASCADE,
    nombre     VARCHAR(80) NOT NULL,
    slug       dom_slug    NOT NULL,
    orden      SMALLINT    NOT NULL CHECK (orden >= 1),
    CONSTRAINT uq_subsecciones_slug  UNIQUE (seccion_id, slug),
    CONSTRAINT uq_subsecciones_orden UNIQUE (seccion_id, orden)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE sistemas.modulos (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subseccion_id UUID        NOT NULL REFERENCES sistemas.subsecciones (id) ON DELETE CASCADE,
    nombre        VARCHAR(80) NOT NULL,
    slug          dom_slug    NOT NULL,
    icono         VARCHAR(60) NOT NULL DEFAULT 'pi pi-file',
    activo        BOOLEAN     NOT NULL DEFAULT TRUE,
    orden         SMALLINT    NOT NULL CHECK (orden >= 1),
    CONSTRAINT uq_modulos_slug  UNIQUE (subseccion_id, slug),
    CONSTRAINT uq_modulos_orden UNIQUE (subseccion_id, orden)
        DEFERRABLE INITIALLY DEFERRED
);

-- ─── Asignaciones y permisos (IAM) ────────────────────────────────────────────

CREATE TABLE iam.rol_sistema (
    rol_id     UUID NOT NULL REFERENCES iam.roles         (id) ON DELETE CASCADE,
    sistema_id UUID NOT NULL REFERENCES sistemas.sistemas (id) ON DELETE RESTRICT,
    PRIMARY KEY (rol_id, sistema_id)
);
CREATE INDEX idx_rol_sistema_sistema ON iam.rol_sistema (sistema_id);

CREATE TABLE iam.usuario_sistema (
    usuario_id UUID NOT NULL REFERENCES iam.usuarios      (id) ON DELETE CASCADE,
    sistema_id UUID NOT NULL REFERENCES sistemas.sistemas (id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, sistema_id)
);
CREATE INDEX idx_usuario_sistema_sist ON iam.usuario_sistema (sistema_id);

-- (v2.2) PERMISOS POR NIVEL: el rol puede recibir permiso en CUALQUIER nivel
-- de la jerarquía — Sistema → Sección → Subsección → Módulo — con HERENCIA
-- descendente. Nivel SISTEMA = iam.rol_sistema (arriba). Una tabla por nivel
-- (FK real a su entidad ⇒ integridad + depuración por CASCADE al reestructurar).
-- La resolución de permisos efectivos la hace iam.v_permisos_efectivos (abajo).

CREATE TABLE iam.permiso_rol_seccion (
    rol_id     UUID NOT NULL REFERENCES iam.roles          (id) ON DELETE CASCADE,
    seccion_id UUID NOT NULL REFERENCES sistemas.secciones (id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, seccion_id)
);
CREATE INDEX idx_permiso_seccion ON iam.permiso_rol_seccion (seccion_id);

CREATE TABLE iam.permiso_rol_subseccion (
    rol_id        UUID NOT NULL REFERENCES iam.roles             (id) ON DELETE CASCADE,
    subseccion_id UUID NOT NULL REFERENCES sistemas.subsecciones (id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, subseccion_id)
);
CREATE INDEX idx_permiso_subseccion ON iam.permiso_rol_subseccion (subseccion_id);

CREATE TABLE iam.permiso_rol_modulo (
    rol_id    UUID NOT NULL REFERENCES iam.roles        (id) ON DELETE CASCADE,
    modulo_id UUID NOT NULL REFERENCES sistemas.modulos (id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, modulo_id)
);
CREATE INDEX idx_permiso_modulo ON iam.permiso_rol_modulo (modulo_id);

-- ─── AUTH: MFA + sesiones ─────────────────────────────────────────────────────

CREATE TABLE auth.otp_desafios (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID         NOT NULL REFERENCES iam.usuarios (id) ON DELETE CASCADE,
    codigo_hash VARCHAR(100) NOT NULL,
    expira_en   TIMESTAMPTZ  NOT NULL,
    intentos    SMALLINT     NOT NULL DEFAULT 0 CHECK (intentos <= 5),
    usado_en    TIMESTAMPTZ,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_otp_usuario_vigente ON auth.otp_desafios (usuario_id, expira_en)
    WHERE usado_en IS NULL;

CREATE TABLE auth.sesiones (
    jti         UUID PRIMARY KEY,
    usuario_id  UUID        NOT NULL REFERENCES iam.usuarios (id) ON DELETE CASCADE,
    emitida_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_en   TIMESTAMPTZ NOT NULL,
    revocada_en TIMESTAMPTZ,
    ip_origen   VARCHAR(45),
    user_agent  VARCHAR(300)
);
CREATE INDEX idx_sesiones_usuario_vigente ON auth.sesiones (usuario_id, expira_en)
    WHERE revocada_en IS NULL;

-- ─── AUDITORÍA: trail de cambios (particionado) + accesos ─────────────────────

CREATE TABLE auditoria.eventos (
    id            BIGINT GENERATED ALWAYS AS IDENTITY,
    ocurrido_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id      UUID,
    trace_id      VARCHAR(64),
    accion        VARCHAR(10) NOT NULL,
    entidad       VARCHAR(60) NOT NULL,
    entidad_id    UUID,
    datos_antes   JSONB,
    datos_despues JSONB,
    PRIMARY KEY (id, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);

CREATE TABLE auditoria.eventos_2026_07 PARTITION OF auditoria.eventos
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE auditoria.eventos_2026_08 PARTITION OF auditoria.eventos
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE auditoria.eventos_default PARTITION OF auditoria.eventos DEFAULT;

CREATE INDEX idx_eventos_tiempo  ON auditoria.eventos USING brin (ocurrido_en);
CREATE INDEX idx_eventos_entidad ON auditoria.eventos (entidad, entidad_id, ocurrido_en DESC);
CREATE INDEX idx_eventos_actor   ON auditoria.eventos (actor_id, ocurrido_en DESC);

CREATE TABLE auditoria.accesos (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ocurrido_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    tipo        VARCHAR(20) NOT NULL,
    usuario_id  UUID,
    email       VARCHAR(160),
    ip_origen   VARCHAR(45),
    user_agent  VARCHAR(300),
    detalle     VARCHAR(300)
);
CREATE INDEX idx_accesos_tiempo  ON auditoria.accesos USING brin (ocurrido_en);
CREATE INDEX idx_accesos_usuario ON auditoria.accesos (usuario_id, ocurrido_en DESC);
CREATE INDEX idx_accesos_tipo    ON auditoria.accesos (tipo, ocurrido_en DESC);

-- ─── Triggers ─────────────────────────────────────────────────────────────────

-- actualizado_en
CREATE OR REPLACE FUNCTION public.fn_touch_actualizado_en() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.actualizado_en := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_touch_roles    BEFORE UPDATE ON iam.roles         FOR EACH ROW EXECUTE FUNCTION public.fn_touch_actualizado_en();
CREATE TRIGGER trg_touch_usuarios BEFORE UPDATE ON iam.usuarios      FOR EACH ROW EXECUTE FUNCTION public.fn_touch_actualizado_en();
CREATE TRIGGER trg_touch_credenc  BEFORE UPDATE ON iam.credenciales  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_actualizado_en();
CREATE TRIGGER trg_touch_sistemas BEFORE UPDATE ON sistemas.sistemas FOR EACH ROW EXECUTE FUNCTION public.fn_touch_actualizado_en();

-- Trail de cambios (SECURITY DEFINER: la app no falsifica la auditoría)
CREATE OR REPLACE FUNCTION auditoria.fn_registrar_cambio() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = auditoria AS $$
DECLARE
    v_actor UUID := NULLIF(current_setting('app.usuario_id', true), '')::uuid;
    v_trace VARCHAR(64) := NULLIF(current_setting('app.trace_id', true), '');
    v_id    UUID;
BEGIN
    v_id := COALESCE((to_jsonb(NEW) ->> 'id')::uuid, (to_jsonb(OLD) ->> 'id')::uuid);
    INSERT INTO auditoria.eventos (actor_id, trace_id, accion, entidad, entidad_id, datos_antes, datos_despues)
    VALUES (
        v_actor, v_trace, TG_OP,
        TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME, v_id,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN to_jsonb(OLD) - 'password_hash' - 'codigo_hash' END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN to_jsonb(NEW) - 'password_hash' - 'codigo_hash' END
    );
    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TRIGGER trg_aud_roles    AFTER INSERT OR UPDATE OR DELETE ON iam.roles              FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_usuarios AFTER INSERT OR UPDATE OR DELETE ON iam.usuarios           FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_credenc  AFTER INSERT OR UPDATE OR DELETE ON iam.credenciales       FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_rolsist  AFTER INSERT OR UPDATE OR DELETE ON iam.rol_sistema        FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_ususist  AFTER INSERT OR UPDATE OR DELETE ON iam.usuario_sistema    FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_perm_sec AFTER INSERT OR UPDATE OR DELETE ON iam.permiso_rol_seccion    FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_perm_sub AFTER INSERT OR UPDATE OR DELETE ON iam.permiso_rol_subseccion FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_permisos AFTER INSERT OR UPDATE OR DELETE ON iam.permiso_rol_modulo FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_sistemas AFTER INSERT OR UPDATE OR DELETE ON sistemas.sistemas      FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();

-- ─── Vistas de apoyo ──────────────────────────────────────────────────────────

CREATE OR REPLACE VIEW sistemas.v_sistemas_resumen AS
WITH conteos AS (
    SELECT sec.sistema_id,
           COUNT(DISTINCT sec.id) AS total_secciones,
           COUNT(m.id)            AS total_modulos
    FROM sistemas.secciones sec
    LEFT JOIN sistemas.subsecciones sub ON sub.seccion_id  = sec.id
    LEFT JOIN sistemas.modulos m        ON m.subseccion_id = sub.id
    GROUP BY sec.sistema_id
),
roles_por_sistema AS (
    SELECT sistema_id, COUNT(*) AS roles_asignados
    FROM iam.rol_sistema GROUP BY sistema_id
)
SELECT s.id, s.nombre, s.slug, s.descripcion, s.icono, s.version, s.estado,
       COALESCE(c.total_secciones, 0) AS total_secciones,
       COALESCE(c.total_modulos,   0) AS total_modulos,
       COALESCE(r.roles_asignados, 0) AS roles_asignados,
       s.actualizado_en
FROM sistemas.sistemas s
LEFT JOIN conteos c           ON c.sistema_id = s.id
LEFT JOIN roles_por_sistema r ON r.sistema_id = s.id;

-- (v2.1) SIDEBAR DE LOS SISTEMAS HIJOS — árbol plano, YA ORDENADO y con la
-- ruta canónica /{sistema}/{seccion}/{subseccion}/{modulo}. Es el contrato
-- que los Remotes consumen para pintar su sidebar; el frontend NO reordena.
CREATE OR REPLACE VIEW sistemas.v_sidebar AS
SELECT
    s.id       AS sistema_id,
    s.slug     AS sistema_slug,
    sec.id     AS seccion_id,
    sec.nombre AS seccion_nombre,
    sec.slug   AS seccion_slug,
    sec.icono  AS seccion_icono,
    sec.orden  AS seccion_orden,
    sub.id     AS subseccion_id,
    sub.nombre AS subseccion_nombre,
    sub.slug   AS subseccion_slug,
    sub.orden  AS subseccion_orden,
    m.id       AS modulo_id,
    m.nombre   AS modulo_nombre,
    m.slug     AS modulo_slug,
    m.icono    AS modulo_icono,
    m.activo   AS modulo_activo,
    m.orden    AS modulo_orden,
    '/' || s.slug || '/' || sec.slug || '/' || sub.slug || '/' || m.slug AS ruta
FROM sistemas.sistemas s
JOIN sistemas.secciones    sec ON sec.sistema_id  = s.id
JOIN sistemas.subsecciones sub ON sub.seccion_id  = sec.id
JOIN sistemas.modulos      m   ON m.subseccion_id = sub.id
ORDER BY s.slug, sec.orden, sub.orden, m.orden;

-- (v2.2) PERMISOS EFECTIVOS — resuelve la herencia de los 4 niveles a su
-- expresión final (rol, módulo): un rol accede a un módulo si el permiso fue
-- concedido en CUALQUIER nivel de su cadena de ancestros. El backend consulta
-- SOLO esta vista (una fuente de verdad).
CREATE OR REPLACE VIEW iam.v_permisos_efectivos AS
WITH arbol AS (
    SELECT m.id   AS modulo_id,
           sub.id AS subseccion_id,
           sec.id AS seccion_id,
           sec.sistema_id
    FROM sistemas.modulos m
    JOIN sistemas.subsecciones sub ON sub.id = m.subseccion_id
    JOIN sistemas.secciones    sec ON sec.id = sub.seccion_id
)
SELECT rs.rol_id, a.modulo_id, 'sistema'::text AS origen
FROM iam.rol_sistema rs JOIN arbol a ON a.sistema_id = rs.sistema_id
UNION
SELECT ps.rol_id, a.modulo_id, 'seccion'
FROM iam.permiso_rol_seccion ps JOIN arbol a ON a.seccion_id = ps.seccion_id
UNION
SELECT pu.rol_id, a.modulo_id, 'subseccion'
FROM iam.permiso_rol_subseccion pu JOIN arbol a ON a.subseccion_id = pu.subseccion_id
UNION
SELECT pm.rol_id, pm.modulo_id, 'modulo'
FROM iam.permiso_rol_modulo pm;

-- Usuario "público" (sin credenciales) — respuesta de GET /usuarios
CREATE OR REPLACE VIEW iam.v_usuarios AS
SELECT u.id, u.nombre, u.email, r.slug AS rol, u.activo, u.creado_en
FROM iam.usuarios u
JOIN iam.roles r ON r.id = u.rol_id;
