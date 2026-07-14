package pe.confianza.mis.usuarios.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de solo lectura para que `auth` obtenga la vista del usuario necesaria
 * para el JWT y la respuesta de sesión, sin acoplarse al dominio de usuarios (BE-01).
 */
public interface UsuarioDirectory {

    Optional<AuthUser> porEmail(String email);
    Optional<AuthUser> porId(UUID id);

    /** Vista del usuario para autenticación: incluye rol y subsistemas efectivos (slugs). */
    record AuthUser(
            UUID id, String nombre, String email, String rolSlug,
            List<String> subsistemas, boolean activo, Instant creadoEn) {}
}
