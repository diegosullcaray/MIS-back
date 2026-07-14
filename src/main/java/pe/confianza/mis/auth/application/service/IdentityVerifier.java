package pe.confianza.mis.auth.application.service;

import java.util.UUID;

/**
 * Verifica la identidad en el paso 1 del login y devuelve el id del usuario.
 *
 * - Dev  → {@code PasswordIdentityVerifier}: credential = contraseña.
 * - Prod → {@code GmailIdentityVerifier}: credential = ID token de Google
 *          (cuenta corporativa). Ambos desembocan en el mismo flujo MFA.
 */
public interface IdentityVerifier {
    UUID verificar(String email, String credential);
}
