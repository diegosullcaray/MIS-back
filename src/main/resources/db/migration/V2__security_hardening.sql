-- ============================================================================
-- V2 — Endurecimiento de seguridad (roles de BD, grants, RLS).
-- Best-effort: si el usuario de conexión no es superusuario, se omite con NOTICE
-- (útil en entornos gestionados). En docker-compose corre como superusuario.
-- ============================================================================

DO $$
BEGIN
    -- Roles de mínimo privilegio (idempotente)
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mis_app') THEN
        CREATE ROLE mis_app NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mis_auditor') THEN
        CREATE ROLE mis_auditor NOLOGIN;
    END IF;

    -- Nada accesible por defecto
    EXECUTE 'REVOKE ALL ON ALL TABLES IN SCHEMA iam, sistemas, auth, auditoria FROM PUBLIC';

    -- La app opera el negocio
    EXECUTE 'GRANT USAGE ON SCHEMA iam, sistemas, auth TO mis_app';
    EXECUTE 'GRANT USAGE ON SCHEMA auditoria TO mis_app, mis_auditor';
    EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA iam, sistemas, auth TO mis_app';

    -- Auditoría append-only para la app; lectura para el auditor
    EXECUTE 'GRANT SELECT, INSERT ON auditoria.accesos TO mis_app';
    EXECUTE 'GRANT SELECT ON ALL TABLES IN SCHEMA auditoria TO mis_auditor';

    -- RLS: sin políticas de UPDATE/DELETE ⇒ auditoría inmutable
    EXECUTE 'ALTER TABLE auditoria.eventos ENABLE ROW LEVEL SECURITY';
    EXECUTE 'ALTER TABLE auditoria.accesos ENABLE ROW LEVEL SECURITY';
    EXECUTE 'CREATE POLICY pol_eventos_lectura   ON auditoria.eventos FOR SELECT TO mis_auditor USING (true)';
    EXECUTE 'CREATE POLICY pol_accesos_lectura   ON auditoria.accesos FOR SELECT TO mis_auditor, mis_app USING (true)';
    EXECUTE 'CREATE POLICY pol_accesos_insercion ON auditoria.accesos FOR INSERT TO mis_app WITH CHECK (true)';

    RAISE NOTICE 'V2: endurecimiento de seguridad aplicado.';
EXCEPTION
    WHEN insufficient_privilege THEN
        RAISE NOTICE 'V2: privilegios insuficientes para el endurecimiento; se omite (entorno gestionado).';
    WHEN duplicate_object THEN
        RAISE NOTICE 'V2: políticas/roles ya existían; se omite.';
END $$;
