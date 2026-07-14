package pe.confianza.mis.auth.application.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración de autenticación (mis.auth.*).
 * En dev, `otp.fixed-code` fija el OTP (paridad con el 123456 de la Fake API).
 */
@ConfigurationProperties(prefix = "mis.auth")
public record AuthProperties(Otp otp, Mfa mfa) {

    public AuthProperties {
        if (otp == null) otp = new Otp(null, 0, 0, null);
        if (mfa == null) mfa = new Mfa(null);
    }

    public record Otp(Duration ttl, int maxIntentos, int longitud, String fixedCode) {
        public Otp {
            if (ttl == null) ttl = Duration.ofMinutes(3);
            if (maxIntentos <= 0) maxIntentos = 5;
            if (longitud <= 0) longitud = 6;
        }
    }

    public record Mfa(Duration tokenTtl) {
        public Mfa {
            if (tokenTtl == null) tokenTtl = Duration.ofMinutes(3);
        }
    }
}
