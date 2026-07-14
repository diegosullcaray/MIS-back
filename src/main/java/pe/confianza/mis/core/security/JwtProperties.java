package pe.confianza.mis.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuración del JWT (mis.jwt.*). */
@ConfigurationProperties(prefix = "mis.jwt")
public record JwtProperties(
        String secret,
        Duration expiration,
        String issuer
) {
    public JwtProperties {
        if (expiration == null) expiration = Duration.ofHours(8);
        if (issuer == null) issuer = "mis-host";
    }
}
