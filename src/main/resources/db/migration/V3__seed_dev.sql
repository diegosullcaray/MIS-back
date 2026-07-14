-- ============================================================================
-- V3 — Seed de desarrollo (espejo de la Fake API del Host).
-- Contraseñas hasheadas con BCrypt vía pgcrypto crypt(..., gen_salt('bf',10)),
-- verificables por BCryptPasswordEncoder de Spring.
--   admin@confianza.pe / admin123        · general@confianza.pe / general123
--   supervisor@confianza.pe / supervisor123
-- En producción, sustituir por un seed sin usuarios demo.
-- ============================================================================

-- ─── Roles (PRD §4) ───────────────────────────────────────────────────────────
INSERT INTO iam.roles (nombre, slug) VALUES
    ('Administrador del Sistema', 'admin-sistema'),
    ('Administrador General',     'admin-general'),
    ('Supervisor de Área',        'supervisor-area');

-- ─── Sistemas (Remotes) ───────────────────────────────────────────────────────
INSERT INTO sistemas.sistemas (nombre, slug, descripcion, icono, url, version, estado) VALUES
    ('Contabilidad', 'subsistema-contabilidad', 'Gestión contable, tesorería y reportes financieros.', 'pi pi-chart-bar',  'http://localhost:4201/remoteEntry.json', '1.4.2', 'activo'),
    ('RRHH',         'subsistema-rrhh',         'Personal, asistencia, planillas y beneficios.',        'pi pi-users',      'http://localhost:4202/remoteEntry.json', '2.1.0', 'activo'),
    ('Ventas',       'subsistema-ventas',       'Clientes, cotizaciones, pedidos y facturación.',       'pi pi-chart-line', 'http://localhost:4203/remoteEntry.json', '1.0.5', 'mantenimiento'),
    ('Logística',    'subsistema-logistica',    'Almacenes, inventarios y compras.',                    'pi pi-truck',      'http://localhost:4204/remoteEntry.json', '0.9.0', 'inactivo');

-- Estructura mínima de Contabilidad (para probar estructura/permisos)
WITH s AS (SELECT id FROM sistemas.sistemas WHERE slug = 'subsistema-contabilidad'),
     sec AS (
        INSERT INTO sistemas.secciones (sistema_id, nombre, slug, orden)
        SELECT s.id, 'Contabilidad General', 'contabilidad-general', 0 FROM s
        RETURNING id),
     sub AS (
        INSERT INTO sistemas.subsecciones (seccion_id, nombre, slug, orden)
        SELECT sec.id, 'Libros Contables', 'libros-contables', 0 FROM sec
        RETURNING id)
INSERT INTO sistemas.modulos (subseccion_id, nombre, slug, activo, orden)
SELECT sub.id, x.nombre, x.slug, true, x.orden
FROM sub, (VALUES ('Libro Diario','libro-diario',0),
                  ('Libro Mayor','libro-mayor',1),
                  ('Balance de Comprobación','balance-comprobacion',2)) AS x(nombre, slug, orden);

-- ─── Usuarios + credenciales ──────────────────────────────────────────────────
WITH nuevo AS (
    INSERT INTO iam.usuarios (nombre, email, rol_id, activo)
    SELECT 'Diego Sullcaray', 'admin@confianza.pe', r.id, true
    FROM iam.roles r WHERE r.slug = 'admin-sistema'
    RETURNING id)
INSERT INTO iam.credenciales (usuario_id, password_hash)
SELECT id, crypt('admin123', gen_salt('bf', 10)) FROM nuevo;

WITH nuevo AS (
    INSERT INTO iam.usuarios (nombre, email, rol_id, activo)
    SELECT 'Ana García', 'general@confianza.pe', r.id, true
    FROM iam.roles r WHERE r.slug = 'admin-general'
    RETURNING id)
INSERT INTO iam.credenciales (usuario_id, password_hash)
SELECT id, crypt('general123', gen_salt('bf', 10)) FROM nuevo;

WITH nuevo AS (
    INSERT INTO iam.usuarios (nombre, email, rol_id, activo)
    SELECT 'Carlos Mendoza', 'supervisor@confianza.pe', r.id, true
    FROM iam.roles r WHERE r.slug = 'supervisor-area'
    RETURNING id)
INSERT INTO iam.credenciales (usuario_id, password_hash)
SELECT id, crypt('supervisor123', gen_salt('bf', 10)) FROM nuevo;

-- ─── Asignación de subsistemas por rol (rol_sistema) ──────────────────────────
-- admin-sistema: todos
INSERT INTO iam.rol_sistema (rol_id, sistema_id)
SELECT r.id, s.id FROM iam.roles r CROSS JOIN sistemas.sistemas s
WHERE r.slug = 'admin-sistema';
-- admin-general: contabilidad + rrhh
INSERT INTO iam.rol_sistema (rol_id, sistema_id)
SELECT r.id, s.id FROM iam.roles r, sistemas.sistemas s
WHERE r.slug = 'admin-general' AND s.slug IN ('subsistema-contabilidad', 'subsistema-rrhh');
-- supervisor-area: rrhh
INSERT INTO iam.rol_sistema (rol_id, sistema_id)
SELECT r.id, s.id FROM iam.roles r, sistemas.sistemas s
WHERE r.slug = 'supervisor-area' AND s.slug = 'subsistema-rrhh';
