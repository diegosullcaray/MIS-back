package pe.confianza.mis.roles.application.service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Puerto público del módulo `roles` para que `sistemas` y `usuarios` consulten
 * roles sin acoplarse a su dominio (BE-01).
 */
public interface RolLookup {

    boolean existe(UUID rolId);

    long contarRolesConSistema(UUID sistemaId);

    /** Vista mínima del rol para otros módulos (slug + subsistemas habilitados). */
    Optional<RolRef> porId(UUID rolId);

    record RolRef(UUID id, String nombre, String slug, Set<UUID> sistemaIds) {}
}
