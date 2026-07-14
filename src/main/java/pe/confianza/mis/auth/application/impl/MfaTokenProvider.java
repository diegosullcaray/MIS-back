package pe.confianza.mis.auth.application.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import pe.confianza.mis.core.security.JwtProperties;
import pe.confianza.mis.core.exception.UnauthorizedException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Token opaco de desafío MFA (paso 1 → paso 2). Firmado, de un solo uso lógico,
 * con TTL corto. Lleva `typ=mfa` para que nunca pueda usarse como token de sesión.
 */
@Component
public class MfaTokenProvider {

    private final SecretKey key;
    private final AuthProperties authProps;

    public MfaTokenProvider(JwtProperties jwtProps, AuthProperties authProps) {
        this.key = Keys.hmacShaKeyFor(jwtProps.secret().getBytes(StandardCharsets.UTF_8));
        this.authProps = authProps;
    }

    public String generar(UUID usuarioId, UUID challengeId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim("cid", challengeId.toString())
                .claim("typ", "mfa")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(authProps.mfa().tokenTtl())))
                .signWith(key)
                .compact();
    }

    public Parsed validar(String mfaToken) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(mfaToken).getPayload();
            if (!"mfa".equals(c.get("typ", String.class)))
                throw new UnauthorizedException("Token de verificación inválido.");
            return new Parsed(UUID.fromString(c.getSubject()),
                    UUID.fromString(c.get("cid", String.class)));
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("La sesión de verificación expiró. Vuelve a iniciar sesión.");
        }
    }

    public record Parsed(UUID usuarioId, UUID challengeId) {}
}
