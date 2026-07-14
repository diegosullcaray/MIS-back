package pe.confianza.mis.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Emite y valida los JWT de sesión (doc 04 §3.3). Claims: sub, rol, subsistemas, jti. */
@Component
public class JwtProvider {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public record Token(String value, UUID jti, Instant expiresAt) {}

    public Token generar(UUID usuarioId, String rolSlug, List<String> subsistemas) {
        UUID jti = UUID.randomUUID();
        Instant now = Instant.now();
        Instant exp = now.plus(props.expiration());

        String jwt = Jwts.builder()
                .issuer(props.issuer())
                .subject(usuarioId.toString())
                .id(jti.toString())
                .claim("rol", rolSlug)
                .claim("subsistemas", subsistemas)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(exp))
                .signWith(key)
                .compact();

        return new Token(jwt, jti, exp);
    }

    @SuppressWarnings("unchecked")
    public ParsedToken validar(String jwt) {
        Claims c = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(jwt).getPayload();
        return new ParsedToken(
                UUID.fromString(c.getSubject()),
                UUID.fromString(c.getId()),
                c.get("rol", String.class),
                (List<String>) c.getOrDefault("subsistemas", List.of()));
    }

    public record ParsedToken(UUID usuarioId, UUID jti, String rol, List<String> subsistemas) {}
}
