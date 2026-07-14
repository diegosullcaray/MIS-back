package pe.confianza.mis.sistemas.application.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.confianza.mis.roles.application.service.RolLookup;
import pe.confianza.mis.core.exception.ConflictException;
import pe.confianza.mis.core.exception.NotFoundException;
import pe.confianza.mis.core.exception.ValidationException;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.*;
import pe.confianza.mis.sistemas.presentation.mapper.SistemaMapper;
import pe.confianza.mis.sistemas.application.service.SistemaLookup;
import pe.confianza.mis.sistemas.application.service.SistemaService;
import pe.confianza.mis.sistemas.domain.entity.Modulo;
import pe.confianza.mis.sistemas.domain.entity.PermisoRolModulo;
import pe.confianza.mis.sistemas.domain.entity.Seccion;
import pe.confianza.mis.sistemas.domain.entity.Sistema;
import pe.confianza.mis.sistemas.domain.entity.Subseccion;
import pe.confianza.mis.sistemas.domain.repository.ModuloRepository;
import pe.confianza.mis.sistemas.domain.repository.PermisoRolModuloRepository;
import pe.confianza.mis.sistemas.domain.repository.SistemaRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SistemaServiceImpl implements SistemaService, SistemaLookup {

    private final SistemaRepository sistemas;
    private final ModuloRepository modulos;
    private final PermisoRolModuloRepository permisos;
    private final RolLookup rolLookup;
    private final SistemaMapper mapper;

    public SistemaServiceImpl(SistemaRepository sistemas, ModuloRepository modulos,
                              PermisoRolModuloRepository permisos, @Lazy RolLookup rolLookup,
                              SistemaMapper mapper) {
        this.sistemas = sistemas;
        this.modulos = modulos;
        this.permisos = permisos;
        this.rolLookup = rolLookup;
        this.mapper = mapper;
    }

    // ─── Listado / detalle ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SistemaResumenDto> listar() {
        return sistemas.findAll().stream().map(this::aResumen).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SistemaDto obtener(String idOSlug) {
        return mapper.toDetalle(buscar(idOSlug));
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    public SistemaDto crear(SistemaRequest req) {
        if (sistemas.existsBySlug(req.slug()))
            throw new ConflictException("Ya existe un sistema con el slug '" + req.slug() + "'.");
        Sistema s = new Sistema();
        s.setNombre(req.nombre().trim());
        s.setSlug(req.slug().trim());
        aplicarDatos(s, req);
        return mapper.toDetalle(sistemas.save(s));
    }

    @Override
    public SistemaDto actualizar(UUID id, SistemaRequest req) {
        Sistema s = sistemas.findById(id)
                .orElseThrow(() -> new NotFoundException("El sistema '" + id + "' no existe."));
        aplicarDatos(s, req);
        return mapper.toDetalle(s);
    }

    @Override
    public void eliminar(UUID id) {
        Sistema s = sistemas.findById(id)
                .orElseThrow(() -> new NotFoundException("El sistema '" + id + "' no existe."));
        if (contarRolesAsignados(id) > 0)
            throw new ConflictException("No se puede eliminar: el sistema está asignado a uno o más roles.");
        sistemas.delete(s);
    }

    private void aplicarDatos(Sistema s, SistemaRequest req) {
        if (req.descripcion() != null) s.setDescripcion(req.descripcion().trim());
        if (req.icono() != null && !req.icono().isBlank()) s.setIcono(req.icono().trim());
        if (req.url() != null) s.setUrl(req.url().trim());
        if (req.version() != null && !req.version().isBlank()) s.setVersion(req.version().trim());
        if (req.estado() != null) {
            if (!List.of("activo", "mantenimiento", "inactivo").contains(req.estado()))
                throw new ValidationException("Estado inválido: " + req.estado());
            s.setEstado(req.estado());
        }
    }

    // ─── Estructura ─────────────────────────────────────────────────────────

    @Override
    public SistemaDto reemplazarEstructura(UUID id, List<SeccionInput> entrada) {
        Sistema s = sistemas.findById(id)
                .orElseThrow(() -> new NotFoundException("El sistema '" + id + "' no existe."));
        List<Seccion> nuevas = new ArrayList<>();
        short ordSec = 0;
        for (SeccionInput si : entrada) {
            Seccion sec = new Seccion();
            sec.setNombre(si.nombre());
            sec.setSlug(si.slug());
            sec.setOrden(ordSec++);
            short ordSub = 0;
            if (si.subsecciones() != null) for (SubseccionInput subi : si.subsecciones()) {
                Subseccion sub = new Subseccion();
                sub.setNombre(subi.nombre());
                sub.setSlug(subi.slug());
                sub.setOrden(ordSub++);
                short ordMod = 0;
                if (subi.modulos() != null) for (ModuloInput mi : subi.modulos()) {
                    Modulo m = new Modulo();
                    m.setNombre(mi.nombre());
                    m.setSlug(mi.slug());
                    m.setActivo(mi.activo() == null || mi.activo());
                    m.setOrden(ordMod++);
                    sub.addModulo(m);
                }
                sec.addSubseccion(sub);
            }
            nuevas.add(sec);
        }
        s.reemplazarSecciones(nuevas);
        // orphanRemoval + FK ON DELETE CASCADE depuran los permisos huérfanos (BE-07)
        return mapper.toDetalle(sistemas.save(s));
    }

    // ─── Permisos por rol ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PermisoRolSistemaDto> permisosDeSistema(UUID sistemaId) {
        Sistema s = sistemas.findById(sistemaId)
                .orElseThrow(() -> new NotFoundException("El sistema '" + sistemaId + "' no existe."));
        return permisos.findBySistema(s.getId()).stream()
                .collect(Collectors.groupingBy(PermisoRolModulo::getRolId,
                        Collectors.mapping(PermisoRolModulo::getModuloId, Collectors.toList())))
                .entrySet().stream()
                .map(e -> new PermisoRolSistemaDto(e.getKey(), s.getId(), e.getValue()))
                .toList();
    }

    /** GET /roles/{id}/permisos — permisos del rol agrupados por sistema. */
    @Override
    @Transactional(readOnly = true)
    public List<PermisoRolSistemaDto> permisosDeRol(UUID rolId) {
        List<PermisoRolModulo> asignados = permisos.findByRolId(rolId);
        if (asignados.isEmpty()) return List.of();

        Map<UUID, UUID> sistemaPorModulo = new HashMap<>();
        List<UUID> modIds = asignados.stream().map(PermisoRolModulo::getModuloId).toList();
        for (Object[] fila : modulos.sistemaPorModulo(modIds)) {
            sistemaPorModulo.put((UUID) fila[0], (UUID) fila[1]);
        }
        Map<UUID, List<UUID>> porSistema = new LinkedHashMap<>();
        for (PermisoRolModulo p : asignados) {
            UUID sistemaId = sistemaPorModulo.get(p.getModuloId());
            if (sistemaId != null)
                porSistema.computeIfAbsent(sistemaId, k -> new ArrayList<>()).add(p.getModuloId());
        }
        return porSistema.entrySet().stream()
                .map(e -> new PermisoRolSistemaDto(rolId, e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    public PermisoRolSistemaDto guardarPermisos(UUID sistemaId, UUID rolId, List<UUID> moduloIds) {
        Sistema s = sistemas.findById(sistemaId)
                .orElseThrow(() -> new NotFoundException("El sistema '" + sistemaId + "' no existe."));
        if (!rolLookup.existe(rolId))
            throw new NotFoundException("El rol '" + rolId + "' no existe.");

        Set<UUID> validos = new HashSet<>(modulos.idsPorSistema(s.getId()));
        List<UUID> seleccion = moduloIds == null ? List.of()
                : moduloIds.stream().filter(validos::contains).distinct().toList();

        // Reemplaza el set del rol para este sistema
        List<UUID> actuales = permisos.modulosDeRolEnSistema(rolId, s.getId());
        if (!actuales.isEmpty()) permisos.deleteByRolIdAndModuloIdIn(rolId, actuales);
        for (UUID modId : seleccion) permisos.save(new PermisoRolModulo(rolId, modId));

        return new PermisoRolSistemaDto(rolId, s.getId(), seleccion);
    }

    // ─── SistemaLookup (puerto para `usuarios` y `roles`) ─────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> slugsPorIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return sistemas.findAllById(ids).stream()
                .collect(Collectors.toMap(Sistema::getId, Sistema::getSlug));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> idsExistentes(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        return sistemas.findAllById(ids).stream().map(Sistema::getId).collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> idsPorSlugs(Collection<String> slugs) {
        if (slugs == null || slugs.isEmpty()) return List.of();
        List<UUID> out = new ArrayList<>();
        for (String slug : slugs) sistemas.findBySlug(slug).ifPresent(s -> out.add(s.getId()));
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public long contarRolesAsignados(UUID sistemaId) {
        return rolLookup.contarRolesConSistema(sistemaId);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Sistema buscar(String idOSlug) {
        try {
            return sistemas.findById(UUID.fromString(idOSlug))
                    .orElseThrow(() -> new NotFoundException("El sistema '" + idOSlug + "' no existe."));
        } catch (IllegalArgumentException notUuid) {
            return sistemas.findBySlug(idOSlug)
                    .orElseThrow(() -> new NotFoundException("El sistema '" + idOSlug + "' no existe."));
        }
    }

    private SistemaResumenDto aResumen(Sistema s) {
        int totalSec = s.getSecciones().size();
        int totalMod = s.getSecciones().stream()
                .flatMap(sec -> sec.getSubsecciones().stream())
                .mapToInt(sub -> sub.getModulos().size()).sum();
        return new SistemaResumenDto(s.getId(), s.getNombre(), s.getSlug(), s.getDescripcion(),
                s.getIcono(), s.getVersion(), s.getEstado(), totalSec, totalMod,
                contarRolesAsignados(s.getId()), s.getActualizadoEn());
    }
}
