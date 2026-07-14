package pe.confianza.mis.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Valida el Bearer JWT en cada petición y coloca el AuthenticatedUser en el
 * SecurityContext. El rol se expone como authority ROLE_<SLUG_MAYÚSCULAS> para
 * usar @PreAuthorize (la jerarquía se define en SecurityConfig).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwt;

    public JwtAuthFilter(JwtProvider jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                JwtProvider.ParsedToken t = jwt.validar(header.substring(7));
                var user = new AuthenticatedUser(t.usuarioId(), t.rol(), t.subsistemas());
                var authority = new SimpleGrantedAuthority(
                        "ROLE_" + t.rol().toUpperCase().replace('-', '_'));
                var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // Token inválido/expirado → queda sin autenticar (401 en el endpoint protegido)
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
