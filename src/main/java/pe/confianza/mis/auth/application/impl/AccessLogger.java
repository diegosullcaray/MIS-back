package pe.confianza.mis.auth.application.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Escribe la bitácora de seguridad en auditoria.accesos (best-effort). */
@Component
public class AccessLogger {

    private static final Logger log = LoggerFactory.getLogger(AccessLogger.class);
    private final JdbcTemplate jdbc;

    public AccessLogger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(String tipo, UUID usuarioId, String email,
                          String ip, String userAgent, String detalle) {
        try {
            jdbc.update("""
                INSERT INTO auditoria.accesos (tipo, usuario_id, email, ip_origen, user_agent, detalle)
                VALUES (?, ?, ?, ?, ?, ?)
                """, tipo, usuarioId, email, ip, userAgent, detalle);
        } catch (Exception e) {
            log.warn("No se pudo registrar el acceso '{}' ({}): {}", tipo, email, e.getMessage());
        }
    }
}
