-- ============================================================================
-- 07 — DATABASE SCHEMA · MIS Host (PostgreSQL 16+)
-- Proyecto : MIS - Management Information System (Financiera Confianza)
-- Versión  : 2.0.0 (2026-07-12)
-- Uso      : Migración baseline de Flyway (db/migration/V1__baseline.sql)
--            Contrato documentado en 04_BACKEND_SCHEMA.md §5
--
-- Principios de diseño (v2.0):
--   · ESQUEMAS por bounded context (iam / sistemas / auth / auditoria) —
--     espejo del monolito modular del doc 04 §2; cada esquema es la costura
--     natural si un módulo se extrae a microservicio.
--   · SEGURIDAD avanzada: credenciales separadas del perfil, lockout de
--     cuenta, roles de BD con mínimo privilegio, auditoría append-only con
--     Row Level Security, y contexto de actor por transacción.
--   · AUDITORÍA de logs: trail de cambios (JSONB antes/después) particionado
--     por mes + bitácora de accesos (login/OTP/denegaciones).
--   · SIN redundancias: dominios reutilizables (slug, email), un solo trigger
--     de `actualizado_en`, extensiones declaradas antes de usarse.
--
-- Contexto de actor (lo setea Spring en cada transacción, p. ej. con un
-- interceptor de Hibernate):
--     SET LOCAL app.usuario_id = '<uuid del actor>';
--     SET LOCAL app.trace_id   = '<correlación de la petición>';
-- ============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 0. Extensiones, esquemas, dominios y roles de base de datos
-- ─────────────────────────────────────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- búsqueda difusa (GET /usuarios?q=)
CREATE EXTENSION IF NOT EXISTS citext;     -- email case-insensitive sin lower() manual

CREATE SCHEMA iam;         -- usuarios, roles, asignaciones y permisos (módulo `accesos`)
CREATE SCHEMA sistemas;    -- remotes registrados y su jerarquía (módulo `sistemas`)
CREATE SCHEMA auth;        -- desafíos MFA y sesiones emitidas (módulo `auth`)
CREATE SCHEMA auditoria;   -- trail de cambios y bitácora de accesos (append-only)

-- Dominios reutilizables (una sola definición del formato — cero redundancia)
CREATE DOMAIN dom_slug AS VARCHAR(80)
    CHECK (VALUE ~ '^[a-z0-9]+(-[a-z0-9]+)*$');

CREATE DOMAIN dom_email AS CITEXT
    CHECK (VALUE ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$');

-- Roles de BD — mínimo privilegio (la app NUNCA se conecta como owner):
--   mis_owner    → dueño de esquemas; solo lo usa Flyway (DDL).
--   mis_app      → DML del backend Spring; en auditoría SOLO inserta.
--   mis_auditor  → solo lectura de auditoría (cumplimiento / seguridad).
CREATE ROLE mis_owner   NOLOGIN;
CREATE ROLE mis_app     NOLOGIN;
CREATE ROLE mis_auditor NOLOGIN;
-- Usuarios de conexión reales:  CREATE USER mis_backend LOGIN PASSWORD '...' IN ROLE mis_app;

-- Tipos enumerados (más eficientes y auto-documentados que CHECK de strings)
CREATE TYPE sistemas.estado_sistema AS ENUM ('activo', 'mantenimiento', 'inactivo');
CREATE TYPE auditoria.accion_evento AS ENUM ('INSERT', 'UPDATE', 'DELETE');
CREATE TYPE auditoria.tipo_acceso   AS ENUM
    ('login_ok', 'login_fallido', 'otp_ok', 'otp_fallido', 'otp_expirado',
     'sesion_revocada', 'acceso_denegado', 'logout');

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. IAM — Roles y Usuarios
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE iam.roles (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre         VARCHAR(80)  NOT NULL,
    slug           dom_slug     NOT NULL UNIQUE,   -- ej. 'admin-sistema' (inmutable)
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Perfil del usuario (datos NO sensibles — las credenciales viven aparte)
CREATE TABLE iam.usuarios (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre         VARCHAR(120) NOT NULL,
    email          dom_email    NOT NULL UNIQUE,   -- citext: único sin importar mayúsculas
    rol_id         UUID         NOT NULL REFERENCES iam.roles (id) ON DELETE RESTRICT,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_usuarios_rol ON iam.usuarios (rol_id);
-- Listados casi siempre filtran activos primero (índice parcial, más pequeño)
CREATE INDEX idx_usuarios_activos ON iam.usuarios (creado_en DESC) WHERE activo;
-- Búsqueda difusa del listado (GET /usuarios?q=) sobre nombre + email
CREATE INDEX idx_usuarios_busqueda ON iam.usuarios
    USING gin ((nombre || ' ' || email::text) gin_trgm_ops);

-- Credenciales y política de cuenta — separadas del perfil (patrón de seguridad:
-- el hash jamás viaja en consultas de listado y su acceso es auditable aparte)
CREATE TABLE iam.credenciales (
    usuario_id        UUID PRIMARY KEY REFERENCES iam.usuarios (id) ON DELETE CASCADE,
    password_hash     VARCHAR(100) NOT NULL,        -- BCrypt/Argon2id (nunca en DTOs)
    debe_cambiar      BOOLEAN      NOT NULL DEFAULT FALSE,
    intentos_fallidos SMALLINT     NOT NULL DEFAULT 0,
    bloqueada_hasta   TIMESTAMPTZ,                  -- lockout tras N intentos fallidos
    rotada_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Sistemas registrados (Remotes) — jerarquía Sistema → Sección → Subsección → Módulo
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE sistemas.sistemas (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre         VARCHAR(80)  NOT NULL,
    slug           dom_slug     NOT NULL UNIQUE,   -- = nombre del Remote en federation.manifest.json
    descripcion    VARCHAR(300) NOT NULL DEFAULT '',
    icono          VARCHAR(60)  NOT NULL DEFAULT 'pi pi-th-large',
    url            VARCHAR(300) NOT NULL DEFAULT '',   -- remoteEntry.json del MFE
    version        VARCHAR(20)  NOT NULL DEFAULT '1.0.0',
    estado         sistemas.estado_sistema NOT NULL DEFAULT 'inactivo',
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE sistemas.secciones (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sistema_id UUID        NOT NULL REFERENCES sistemas.sistemas (id) ON DELETE CASCADE,
    nombre     VARCHAR(80) NOT NULL,
    slug       dom_slug    NOT NULL,
    orden      SMALLINT    NOT NULL DEFAULT 0,

    CONSTRAINT uq_secciones_slug UNIQUE (sistema_id, slug)
);

CREATE TABLE sistemas.subsecciones (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seccion_id UUID        NOT NULL REFERENCES sistemas.secciones (id) ON DELETE CASCADE,
    nombre     VARCHAR(80) NOT NULL,
    slug       dom_slug    NOT NULL,
    orden      SMALLINT    NOT NULL DEFAULT 0,

    CONSTRAINT uq_subsecciones_slug UNIQUE (seccion_id, slug)
);

CREATE TABLE sistemas.modulos (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subseccion_id UUID        NOT NULL REFERENCES sistemas.subsecciones (id) ON DELETE CASCADE,
    nombre        VARCHAR(80) NOT NULL,
    slug          dom_slug    NOT NULL,
    activo        BOOLEAN     NOT NULL DEFAULT TRUE,
    orden         SMALLINT    NOT NULL DEFAULT 0,

    CONSTRAINT uq_modulos_slug UNIQUE (subseccion_id, slug)
);

CREATE INDEX idx_secciones_sistema    ON sistemas.secciones    (sistema_id, orden);
CREATE INDEX idx_subsecciones_seccion ON sistemas.subsecciones (seccion_id, orden);
CREATE INDEX idx_modulos_subseccion   ON sistemas.modulos      (subseccion_id, orden);

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Asignaciones y permisos (pertenecen a IAM; referencian a `sistemas` —
--    esta FK cruzada es la costura a cortar si IAM se extrae a microservicio)
-- ─────────────────────────────────────────────────────────────────────────────

-- Subsistemas habilitados por ROL (campo `subsistemas` del modelo Rol)
CREATE TABLE iam.rol_sistema (
    rol_id     UUID NOT NULL REFERENCES iam.roles         (id) ON DELETE CASCADE,
    sistema_id UUID NOT NULL REFERENCES sistemas.sistemas (id) ON DELETE RESTRICT,
    -- RESTRICT: soporta el "409 si el sistema está asignado" de DELETE /sistemas/{id}

    PRIMARY KEY (rol_id, sistema_id)
);

-- Subsistemas habilitados por USUARIO (override individual del rol)
CREATE TABLE iam.usuario_sistema (
    usuario_id UUID NOT NULL REFERENCES iam.usuarios       (id) ON DELETE CASCADE,
    sistema_id UUID NOT NULL REFERENCES sistemas.sistemas  (id) ON DELETE CASCADE,

    PRIMARY KEY (usuario_id, sistema_id)
);

-- Permisos a nivel de MÓDULO por rol (PermisoRolSistema = agrupación por sistema)
CREATE TABLE iam.permiso_rol_modulo (
    rol_id    UUID NOT NULL REFERENCES iam.roles        (id) ON DELETE CASCADE,
    modulo_id UUID NOT NULL REFERENCES sistemas.modulos (id) ON DELETE CASCADE,
    -- CASCADE en modulos: al reemplazar la estructura (PUT /sistemas/{id}/estructura)
    -- los permisos huérfanos se depuran solos.

    PRIMARY KEY (rol_id, modulo_id)
);

CREATE INDEX idx_rol_sistema_sistema  ON iam.rol_sistema        (sistema_id);
CREATE INDEX idx_usuario_sistema_sist ON iam.usuario_sistema    (sistema_id);
CREATE INDEX idx_permiso_modulo       ON iam.permiso_rol_modulo (modulo_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. AUTH — Desafíos MFA y sesiones emitidas
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE auth.otp_desafios (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID         NOT NULL REFERENCES iam.usuarios (id) ON DELETE CASCADE,
    codigo_hash VARCHAR(100) NOT NULL,             -- hash del OTP (nunca en claro)
    expira_en   TIMESTAMPTZ  NOT NULL,             -- creado_en + 3 minutos
    intentos    SMALLINT     NOT NULL DEFAULT 0 CHECK (intentos <= 5),
    usado_en    TIMESTAMPTZ,                       -- NULL = pendiente (un solo uso)
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_otp_usuario_vigente ON auth.otp_desafios (usuario_id, expira_en)
    WHERE usado_en IS NULL;

-- Registro de sesiones JWT emitidas → permite REVOCACIÓN inmediata (logout,
-- cuenta comprometida) sin esperar la expiración del token.
CREATE TABLE auth.sesiones (
    jti         UUID PRIMARY KEY,                  -- claim `jti` del JWT
    usuario_id  UUID        NOT NULL REFERENCES iam.usuarios (id) ON DELETE CASCADE,
    emitida_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_en   TIMESTAMPTZ NOT NULL,
    revocada_en TIMESTAMPTZ,                       -- NULL = vigente
    ip_origen   INET,
    user_agent  VARCHAR(300)
);

CREATE INDEX idx_sesiones_usuario_vigente ON auth.sesiones (usuario_id, expira_en)
    WHERE revocada_en IS NULL;

-- Limpieza programada (pg_cron o @Scheduled de Spring):
--   DELETE FROM auth.otp_desafios WHERE expira_en < now() - interval '1 day';
--   DELETE FROM auth.sesiones     WHERE expira_en < now() - interval '7 days';

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. AUDITORÍA — trail de cambios (particionado) + bitácora de accesos
--    Append-only: ni la app puede alterar lo ya escrito (RLS + grants, §7)
-- ─────────────────────────────────────────────────────────────────────────────

-- 5.1 Trail de cambios de datos: quién cambió qué, antes y después.
--     Particionado por mes → retención barata (DROP PARTITION) y consultas
--     acotadas por rango de fechas.
CREATE TABLE auditoria.eventos (
    id           BIGINT GENERATED ALWAYS AS IDENTITY,
    ocurrido_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id     UUID,                             -- NULL = proceso de sistema
    trace_id     VARCHAR(64),                      -- correlación con logs de la app
    accion       auditoria.accion_evento NOT NULL,
    entidad      VARCHAR(60) NOT NULL,             -- 'iam.usuarios', 'sistemas.sistemas', …
    entidad_id   UUID,
    datos_antes  JSONB,                            -- NULL en INSERT
    datos_despues JSONB,                           -- NULL en DELETE

    PRIMARY KEY (id, ocurrido_en)
) PARTITION BY RANGE (ocurrido_en);

-- Particiones iniciales + default de seguridad (crear las siguientes con
-- pg_partman o un job mensual)
CREATE TABLE auditoria.eventos_2026_07 PARTITION OF auditoria.eventos
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE auditoria.eventos_2026_08 PARTITION OF auditoria.eventos
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE auditoria.eventos_default PARTITION OF auditoria.eventos DEFAULT;

-- BRIN: índice mínimo ideal para series temporales append-only
CREATE INDEX idx_eventos_tiempo  ON auditoria.eventos USING brin (ocurrido_en);
CREATE INDEX idx_eventos_entidad ON auditoria.eventos (entidad, entidad_id, ocurrido_en DESC);
CREATE INDEX idx_eventos_actor   ON auditoria.eventos (actor_id, ocurrido_en DESC);

-- 5.2 Bitácora de accesos y seguridad (logins, OTP, denegaciones, revocaciones)
CREATE TABLE auditoria.accesos (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ocurrido_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    tipo        auditoria.tipo_acceso NOT NULL,
    usuario_id  UUID,                              -- NULL si el email no existe
    email       VARCHAR(160),                      -- lo que se intentó (para forense)
    ip_origen   INET,
    user_agent  VARCHAR(300),
    detalle     VARCHAR(300)                       -- ej. 'intento 3/5', 'rol sin permiso a /admin/accesos'
);

CREATE INDEX idx_accesos_tiempo  ON auditoria.accesos USING brin (ocurrido_en);
CREATE INDEX idx_accesos_usuario ON auditoria.accesos (usuario_id, ocurrido_en DESC);
CREATE INDEX idx_accesos_tipo    ON auditoria.accesos (tipo, ocurrido_en DESC);

-- 5.3 Trigger genérico de auditoría de cambios.
--     SECURITY DEFINER: escribe en auditoría aunque mis_app no tenga permisos
--     directos de INSERT (la auditoría no se puede falsificar desde la app).
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
        v_actor,
        v_trace,
        TG_OP::auditoria.accion_evento,
        TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME,
        v_id,
        CASE WHEN TG_OP IN ('UPDATE', 'DELETE')
             THEN to_jsonb(OLD) - 'password_hash' - 'codigo_hash' END,  -- jamás auditar secretos
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE')
             THEN to_jsonb(NEW) - 'password_hash' - 'codigo_hash' END
    );
    RETURN COALESCE(NEW, OLD);
END;
$$;

-- Tablas de negocio auditadas (las de auth se cubren con auditoria.accesos)
CREATE TRIGGER trg_aud_roles      AFTER INSERT OR UPDATE OR DELETE ON iam.roles              FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_usuarios   AFTER INSERT OR UPDATE OR DELETE ON iam.usuarios           FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_credenc    AFTER INSERT OR UPDATE OR DELETE ON iam.credenciales       FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_rol_sist   AFTER INSERT OR UPDATE OR DELETE ON iam.rol_sistema        FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_usu_sist   AFTER INSERT OR UPDATE OR DELETE ON iam.usuario_sistema    FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_permisos   AFTER INSERT OR UPDATE OR DELETE ON iam.permiso_rol_modulo FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();
CREATE TRIGGER trg_aud_sistemas   AFTER INSERT OR UPDATE OR DELETE ON sistemas.sistemas      FOR EACH ROW EXECUTE FUNCTION auditoria.fn_registrar_cambio();

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. Trigger único de `actualizado_en` (cero duplicación de funciones)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION public.fn_touch_actualizado_en() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.actualizado_en := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_touch_roles       BEFORE UPDATE ON iam.roles         FOR EACH ROW EXECUTE FUNCTION public.fn_touch_actualizado_en();
CREATE TRIGGER trg_touch_usuarios    BEFORE UPDATE ON iam.usuarios      FOR EACH ROW EXECUTE FUNCTION public.fn_touch_actualizado_en();
CREATE TRIGGER trg_touch_credenc     BEFORE UPDATE ON iam.credenciales  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_actualizado_en();
CREATE TRIGGER trg_touch_sistemas    BEFORE UPDATE ON sistemas.sistemas FOR EACH ROW EXECUTE FUNCTION public.fn_touch_actualizado_en();

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. Seguridad: grants de mínimo privilegio + Row Level Security
-- ─────────────────────────────────────────────────────────────────────────────

-- Nada es accesible por defecto
REVOKE ALL ON ALL TABLES IN SCHEMA iam, sistemas, auth, auditoria FROM PUBLIC;

GRANT USAGE ON SCHEMA iam, sistemas, auth TO mis_app;
GRANT USAGE ON SCHEMA auditoria           TO mis_app, mis_auditor;

-- La app opera el negocio…
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA iam, sistemas, auth TO mis_app;
-- …pero la auditoría es APPEND-ONLY incluso para la app (escribe el trigger
-- SECURITY DEFINER; a la app solo se le permite leer accesos para consultas):
GRANT SELECT, INSERT ON auditoria.accesos TO mis_app;
GRANT SELECT ON ALL TABLES IN SCHEMA auditoria TO mis_auditor;

-- RLS en auditoría: sin política de UPDATE/DELETE ⇒ inmutable para todos
-- los roles no-owner, incluso si un grant futuro se equivoca.
ALTER TABLE auditoria.eventos ENABLE ROW LEVEL SECURITY;
ALTER TABLE auditoria.accesos ENABLE ROW LEVEL SECURITY;

CREATE POLICY pol_eventos_lectura  ON auditoria.eventos FOR SELECT TO mis_auditor USING (true);
CREATE POLICY pol_accesos_lectura  ON auditoria.accesos FOR SELECT TO mis_auditor, mis_app USING (true);
CREATE POLICY pol_accesos_insercion ON auditoria.accesos FOR INSERT TO mis_app WITH CHECK (true);

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. Seed mínimo (los 3 roles del PRD §4) — en Flyway real: V2__seed.sql
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO iam.roles (nombre, slug) VALUES
    ('Administrador del Sistema', 'admin-sistema'),
    ('Administrador General',     'admin-general'),
    ('Supervisor de Área',        'supervisor-area');

-- Admin inicial (el hash BCrypt/Argon2id se genera en el despliegue, NUNCA en claro):
-- WITH nuevo AS (
--     INSERT INTO iam.usuarios (nombre, email, rol_id)
--     SELECT 'Administrador MIS', 'admin@confianza.pe', id
--       FROM iam.roles WHERE slug = 'admin-sistema'
--     RETURNING id
-- )
-- INSERT INTO iam.credenciales (usuario_id, password_hash, debe_cambiar)
-- SELECT id, '<hash>', TRUE FROM nuevo;

-- ─────────────────────────────────────────────────────────────────────────────
-- 9. Vistas de apoyo
-- ─────────────────────────────────────────────────────────────────────────────

-- GET /api/v1/sistemas (SistemaResumen) — un solo escaneo, sin subconsultas
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
    FROM iam.rol_sistema
    GROUP BY sistema_id
)
SELECT s.id, s.nombre, s.slug, s.descripcion, s.icono, s.version, s.estado,
       COALESCE(c.total_secciones, 0) AS total_secciones,
       COALESCE(c.total_modulos,   0) AS total_modulos,
       COALESCE(r.roles_asignados, 0) AS roles_asignados,
       s.actualizado_en
FROM sistemas.sistemas s
LEFT JOIN conteos c            ON c.sistema_id = s.id
LEFT JOIN roles_por_sistema r  ON r.sistema_id = s.id;

-- Usuario "público" (sin credenciales) — respuesta de GET /usuarios
CREATE OR REPLACE VIEW iam.v_usuarios AS
SELECT u.id, u.nombre, u.email, r.slug AS rol, u.activo, u.creado_en
FROM iam.usuarios u
JOIN iam.roles r ON r.id = u.rol_id;

GRANT SELECT ON sistemas.v_sistemas_resumen, iam.v_usuarios TO mis_app;
