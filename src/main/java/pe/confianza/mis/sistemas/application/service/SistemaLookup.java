package pe.confianza.mis.sistemas.application.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Puerto público del módulo `sistemas` para que `usuarios` y `roles` resuelvan slugs ↔ ids
 * sin acoplarse al dominio (BE-01).
 */
public interface SistemaLookup {
    Map<UUID, String> slugsPorIds(Collection<UUID> ids);
    Set<UUID> idsExistentes(Collection<UUID> ids);
    List<UUID> idsPorSlugs(Collection<String> slugs);
    long contarRolesAsignados(UUID sistemaId);
}
