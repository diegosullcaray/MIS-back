package pe.confianza.mis.core.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Acceso conveniente al usuario autenticado desde los services. */
public final class CurrentUser {

    private CurrentUser() {}

    public static Optional<AuthenticatedUser> get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }
}
