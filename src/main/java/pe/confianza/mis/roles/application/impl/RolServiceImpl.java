package pe.confianza.mis.roles.application.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.confianza.mis.core.exception.ConflictException;
import pe.confianza.mis.core.exception.NotFoundException;
import pe.confianza.mis.roles.application.service.RolLookup;
import pe.confianza.mis.roles.application.service.RolService;
import pe.confianza.mis.roles.domain.entity.Rol;
import pe.confianza.mis.roles.domain.repository.RolRepository;
import pe.confianza.mis.roles.presentation.dto.RolDtos.RolDto;
import pe.confianza.mis.roles.presentation.dto.RolDtos.RolRequest;
import pe.confianza.mis.roles.presentation.mapper.RolMapper;
import pe.confianza.mis.sistemas.application.service.SistemaLookup;
import pe.confianza.mis.usuarios.application.service.UsuarioLookup;

import java.util.*;

@Service
@Transactional
public class RolServiceImpl implements RolService, RolLookup {

    private final RolRepository roles;
    private final UsuarioLookup usuarios;
    private final SistemaLookup sistemaLookup;
    private final RolMapper mapper;

    public RolServiceImpl(RolRepository roles, UsuarioLookup usuarios,
                          SistemaLookup sistemaLookup, RolMapper mapper) {
        this.roles = roles;
        this.usuarios = usuarios;
        this.sistemaLookup = sistemaLookup;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolDto> listar() {
        return roles.findAll().stream().map(this::aDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RolDto obtener(UUID id) {
        return aDto(buscar(id));
    }

    @Override
    public RolDto crear(RolRequest req) {
        if (roles.existsBySlug(req.slug()))
            throw new ConflictException("Ya existe un rol con el slug '" + req.slug() + "'.");
        Rol r = new Rol();
        r.setNombre(req.nombre().trim());
        r.setSlug(req.slug().trim());
        r.setSistemaIds(resolverSistemaIds(req.subsistemas()));
        return aDto(roles.save(r));
    }

    @Override
    public RolDto actualizar(UUID id, RolRequest req) {
        Rol r = buscar(id);           // el slug es inmutable
        r.setNombre(req.nombre().trim());
        r.setSistemaIds(resolverSistemaIds(req.subsistemas()));
        return aDto(r);
    }

    @Override
    public void eliminar(UUID id) {
        Rol r = buscar(id);
        if (usuarios.algunoConRol(id))
            throw new ConflictException(
                    "No se puede eliminar: el rol '" + r.getNombre() + "' tiene usuarios asignados.");
        roles.delete(r);
    }

    // ─── RolLookup (puerto para `sistemas` y `usuarios`) ───────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean existe(UUID rolId) {
        return roles.existsById(rolId);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarRolesConSistema(UUID sistemaId) {
        return roles.contarConSistema(sistemaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RolRef> porId(UUID rolId) {
        return roles.findById(rolId)
                .map(r -> new RolRef(r.getId(), r.getNombre(), r.getSlug(),
                        Set.copyOf(r.getSistemaIds())));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    Rol buscar(UUID id) {
        return roles.findById(id)
                .orElseThrow(() -> new NotFoundException("El rol '" + id + "' no existe."));
    }

    private Set<UUID> resolverSistemaIds(List<String> slugs) {
        return new HashSet<>(sistemaLookup.idsPorSlugs(slugs == null ? List.of() : slugs));
    }

    private RolDto aDto(Rol r) {
        return mapper.toDto(r, subsistemasComoSlugs(r.getSistemaIds()));
    }

    private List<String> subsistemasComoSlugs(Set<UUID> ids) {
        Map<UUID, String> slugs = sistemaLookup.slugsPorIds(ids);
        return ids.stream().map(slugs::get).filter(Objects::nonNull).sorted().toList();
    }
}
