package pe.confianza.mis.auth.infrastructure;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pe.confianza.mis.usuarios.application.service.PasswordAuthenticator;
import pe.confianza.mis.usuarios.application.service.PasswordAuthenticator.AuthFallida;
import pe.confianza.mis.auth.application.service.IdentityVerifier;
import pe.confianza.mis.core.exception.ForbiddenException;
import pe.confianza.mis.core.exception.UnauthorizedException;

import java.util.UUID;

/**
 * Verificador de identidad para DESARROLLO: email + contraseña (BCrypt) con
 * lockout, delegado en el módulo `usuarios`. Activo en todos los perfiles salvo prod.
 */
@Component
@Profile("!prod")
public class PasswordIdentityVerifier implements IdentityVerifier {

    private final PasswordAuthenticator authenticator;

    public PasswordIdentityVerifier(PasswordAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public UUID verificar(String email, String password) {
        try {
            return authenticator.autenticar(email, password);
        } catch (AuthFallida e) {
            if (e.getMotivo() == AuthFallida.Motivo.INACTIVO
                    || e.getMotivo() == AuthFallida.Motivo.BLOQUEADA) {
                throw new ForbiddenException(e.getMessage());
            }
            throw new UnauthorizedException(e.getMessage());
        }
    }
}
