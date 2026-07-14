package pe.confianza.mis.usuarios.application.service;

import java.util.UUID;

/**
 * Puerto de autenticación por contraseña (dev). Encapsula el hash BCrypt y la
 * política de lockout dentro de `usuarios` (donde viven las credenciales).
 */
public interface PasswordAuthenticator {

    /** Devuelve el id del usuario si las credenciales son válidas. */
    UUID autenticar(String email, String rawPassword) throws AuthFallida;

    /** Motivo de fallo, mapeado por el módulo auth a 401/403. */
    class AuthFallida extends RuntimeException {
        public enum Motivo { CREDENCIALES, INACTIVO, BLOQUEADA }
        private final Motivo motivo;
        public AuthFallida(Motivo motivo, String mensaje) { super(mensaje); this.motivo = motivo; }
        public Motivo getMotivo() { return motivo; }
    }
}
