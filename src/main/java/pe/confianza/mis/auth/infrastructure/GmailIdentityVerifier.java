package pe.confianza.mis.auth.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pe.confianza.mis.usuarios.application.service.UsuarioDirectory;
import pe.confianza.mis.auth.application.service.IdentityVerifier;
import pe.confianza.mis.core.exception.ForbiddenException;
import pe.confianza.mis.core.exception.UnauthorizedException;

import java.util.UUID;

/**
 * Verificador de identidad para PRODUCCIÓN: cuenta corporativa de Google (Gmail).
 *
 * El frontend obtiene un ID token de Google (OAuth2 / Google Identity Services) y
 * lo envía en el campo `password` del login. Aquí se debe:
 *   1. Validar la firma y el `aud` (client-id) del ID token contra los JWKS de Google.
 *   2. Exigir `email_verified` y que el dominio del email sea el corporativo.
 *   3. Localizar al usuario aprovisionado en IAM y comprobar que está activo.
 *
 * La validación criptográfica del ID token se integra con
 * `google-auth-library-oauth2-http` (o el resource-server OIDC de Spring). Este
 * bean deja el punto de extensión cableado; complétese al habilitar el SSO.
 */
@Component
@Profile("prod")
public class GmailIdentityVerifier implements IdentityVerifier {

    @ConfigurationProperties(prefix = "mis.auth.gmail")
    public record GmailProps(String dominioCorporativo, String clientId) {
        public GmailProps {
            if (dominioCorporativo == null || dominioCorporativo.isBlank())
                dominioCorporativo = "confianza.pe";
        }
    }

    private final UsuarioDirectory directorio;
    private final GmailProps props;

    public GmailIdentityVerifier(UsuarioDirectory directorio, GmailProps props) {
        this.directorio = directorio;
        this.props = props;
    }

    @Override
    public UUID verificar(String email, String googleIdToken) {
        // TODO SSO: validar googleIdToken contra los JWKS de Google (firma, aud, exp,
        //           email_verified) y derivar el email desde el token, no del cliente.
        if (googleIdToken == null || googleIdToken.isBlank())
            throw new UnauthorizedException("Falta el token de Google.");

        String dominio = email == null ? "" : email.substring(email.indexOf('@') + 1);
        if (!props.dominioCorporativo().equalsIgnoreCase(dominio))
            throw new ForbiddenException("Solo se permiten cuentas corporativas @" + props.dominioCorporativo() + ".");

        var user = directorio.porEmail(email)
                .orElseThrow(() -> new ForbiddenException("La cuenta no está aprovisionada en el sistema."));
        if (!user.activo())
            throw new ForbiddenException("El usuario está desactivado.");

        return user.id();
    }
}
