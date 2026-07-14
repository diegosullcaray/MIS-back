package pe.confianza.mis.core.security;

import java.util.List;
import java.util.UUID;

/** Principal derivado del JWT — inyectable vía SecurityContext. */
public record AuthenticatedUser(
        UUID id,
        String rol,             // slug del rol (ej. 'admin-sistema')
        List<String> subsistemas
) {}
