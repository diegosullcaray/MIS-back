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
import pe.confianza.mis.sistemas.domain.entity.PermisoRolSeccion;
import pe.confianza.mis.sistemas.domain.entity.PermisoRolSubseccion;
import pe.confianza.mis.sistemas.domain.entity.Seccion;
import pe.confianza.mis.sistemas.domain.entity.Sistema;
import pe.confianza.mis.sistemas.domain.entity.Subseccion;
import pe.confianza.mis.sistemas.domain.repository.ModuloRepository;
import pe.confianza.mis.sistemas.domain.repository.PermisoRolModuloRepository;
import pe.confianza.mis.sistemas.domain.repository.PermisoRolSeccionRepository;
import pe.confianza.mis.sistemas.domain.repository.PermisoRolSubseccionRepository;
import pe.confianza.mis.sistemas.domain.repository.SeccionRepository;
import pe.confianza.mis.sistemas.domain.repository.SistemaRepository;
import pe.confianza.mis.sistemas.domain.repository.SubseccionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SistemaServiceImpl implements SistemaService, SistemaLookup {

    private final SistemaRepository sistemas;
    private final SeccionRepository secciones;
    private final SubseccionRepository subsecciones;
    private final ModuloRepository modulos;
    private final PermisoRolSeccionRepository permisosSeccion;
    private final PermisoRolSubseccionRepository permisosSubseccion;
    private final PermisoRolModuloRepository permisos;
    private final RolLookup rolLookup;
    private final SistemaMapper mapper;

    public SistemaServiceImpl(SistemaRepository sistemas, SeccionRepository secciones,
                              SubseccionRepository subsecciones, ModuloRepository modulos,
                              PermisoRolSeccionRepository permisosSeccion,
                              PermisoRolSubseccionRepository permisosSubseccion,
                              PermisoRolModuloRepository permisos, @Lazy RolLookup rolLookup,
                              SistemaMapper mapper) {
        this.sistemas = sistemas;
        this.secciones = secciones;
        this.subsecciones = subsecciones;
        this.modulos = modulos;
        this.permisosSeccion = permisosSeccion;
        this.permisosSubseccion = permisosSubseccion;
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

        // Borra el árbol anterior y sincroniza antes de insertar el nuevo: los
        // UNIQUE de slug no son DEFERRABLE y el reemplazo puede reutilizarlos.
        s.getSecciones().clear();
        sistemas.saveAndFlush(s);

        // `orden` es 1-based y único por padre (v2.1): lo define la posición
        // del elemento en el arreglo recibido.
        List<Seccion> nuevas = new ArrayList<>();
        short ordSec = 1;
        for (SeccionInput si : entrada) {
            Seccion sec = new Seccion();
            sec.setNombre(si.nombre());
            sec.setSlug(si.slug());
            if (si.icono() != null && !si.icono().isBlank()) sec.setIcono(si.icono().trim());
            sec.setOrden(ordSec++);
            short ordSub = 1;
            if (si.subsecciones() != null) for (SubseccionInput subi : si.subsecciones()) {
                Subseccion sub = new Subseccion();
                sub.setNombre(subi.nombre());
                sub.setSlug(subi.slug());
                sub.setOrden(ordSub++);
                short ordMod = 1;
                if (subi.modulos() != null) for (ModuloInput mi : subi.modulos()) {
                    Modulo m = new Modulo();
                    m.setNombre(mi.nombre());
                    m.setSlug(mi.slug());
                    if (mi.icono() != null && !mi.icono().isBlank()) m.setIcono(mi.icono().trim());
                    m.setActivo(mi.activo() == null || mi.activo());
                    m.setOrden(ordMod++);
                    sub.addModulo(m);
                }
                sec.addSubseccion(sub);
            }
            nuevas.add(sec);
        }
        s.reemplazarSecciones(nuevas);
        // orphanRemoval + FK ON DELETE CASCADE depuran los permisos huérfanos
        // de los 3 niveles (sección/subsección/módulo) en la misma transacción (BE-07)
        return mapper.toDetalle(sistemas.save(s));
    }

    // ─── Permisos por rol (v2.2: por nivel, con herencia descendente) ──────────

    @Override
    @Transactional(readOnly = true)
    public List<PermisoRolSistemaDto> permisosDeSistema(UUID sistemaId) {
        Sistema s = sistemas.findById(sistemaId)
                .orElseThrow(() -> new NotFoundException("El sistema '" + sistemaId + "' no existe."));

        Map<UUID, List<UUID>> secPorRol = agrupar(permisosSeccion.findBySistema(s.getId()),
                PermisoRolSeccion::getRolId, PermisoRolSeccion::getSeccionId);
        Map<UUID, List<UUID>> subPorRol = agrupar(permisosSubseccion.findBySistema(s.getId()),
                PermisoRolSubseccion::getRolId, PermisoRolSubseccion::getSubseccionId);
        Map<UUID, List<UUID>> modPorRol = agrupar(permisos.findBySistema(s.getId()),
                PermisoRolModulo::getRolId, PermisoRolModulo::getModuloId);

        Set<UUID> roles = new LinkedHashSet<>();
        roles.addAll(secPorRol.keySet());
        roles.addAll(subPorRol.keySet());
        roles.addAll(modPorRol.keySet());

        return roles.stream()
                .map(rolId -> new PermisoRolSistemaDto(rolId, s.getId(),
                        secPorRol.getOrDefault(rolId, List.of()),
                        subPorRol.getOrDefault(rolId, List.of()),
                        modPorRol.getOrDefault(rolId, List.of())))
                .toList();
    }

    /** GET /roles/{id}/permisos — permisos del rol (3 niveles) agrupados por sistema. */
    @Override
    @Transactional(readOnly = true)
    public List<PermisoRolSistemaDto> permisosDeRol(UUID rolId) {
        List<PermisoRolSeccion> secs = permisosSeccion.findByRolId(rolId);
        List<PermisoRolSubseccion> subs = permisosSubseccion.findByRolId(rolId);
        List<PermisoRolModulo> mods = permisos.findByRolId(rolId);
        if (secs.isEmpty() && subs.isEmpty() && mods.isEmpty()) return List.of();

        Map<UUID, List<UUID>> secPorSistema = porSistema(
                secs.stream().map(PermisoRolSeccion::getSeccionId).toList(),
                secciones::sistemaPorSeccion);
        Map<UUID, List<UUID>> subPorSistema = porSistema(
                subs.stream().map(PermisoRolSubseccion::getSubseccionId).toList(),
                subsecciones::sistemaPorSubseccion);
        Map<UUID, List<UUID>> modPorSistema = porSistema(
                mods.stream().map(PermisoRolModulo::getModuloId).toList(),
                modulos::sistemaPorModulo);

        Set<UUID> sistemasIds = new LinkedHashSet<>();
        sistemasIds.addAll(secPorSistema.keySet());
        sistemasIds.addAll(subPorSistema.keySet());
        sistemasIds.addAll(modPorSistema.keySet());

        return sistemasIds.stream()
                .map(sistemaId -> new PermisoRolSistemaDto(rolId, sistemaId,
                        secPorSistema.getOrDefault(sistemaId, List.of()),
                        subPorSistema.getOrDefault(sistemaId, List.of()),
                        modPorSistema.getOrDefault(sistemaId, List.of())))
                .toList();
    }

    @Override
    public PermisoRolSistemaDto guardarPermisos(UUID sistemaId, UUID rolId, GuardarPermisosRequest req) {
        Sistema s = sistemas.findById(sistemaId)
                .orElseThrow(() -> new NotFoundException("El sistema '" + sistemaId + "' no existe."));
        if (!rolLookup.existe(rolId))
            throw new NotFoundException("El rol '" + rolId + "' no existe.");

        List<UUID> selSec = filtrarValidos(req == null ? null : req.secciones(),
                secciones.idsPorSistema(s.getId()));
        List<UUID> selSub = filtrarValidos(req == null ? null : req.subsecciones(),
                subsecciones.idsPorSistema(s.getId()));
        List<UUID> selMod = filtrarValidos(req == null ? null : req.modulos(),
                modulos.idsPorSistema(s.getId()));

        // Reemplaza el set del rol para este sistema, nivel por nivel
        List<UUID> secActuales = permisosSeccion.seccionesDeRolEnSistema(rolId, s.getId());
        if (!secActuales.isEmpty()) permisosSeccion.deleteByRolIdAndSeccionIdIn(rolId, secActuales);
        for (UUID secId : selSec) permisosSeccion.save(new PermisoRolSeccion(rolId, secId));

        List<UUID> subActuales = permisosSubseccion.subseccionesDeRolEnSistema(rolId, s.getId());
        if (!subActuales.isEmpty()) permisosSubseccion.deleteByRolIdAndSubseccionIdIn(rolId, subActuales);
        for (UUID subId : selSub) permisosSubseccion.save(new PermisoRolSubseccion(rolId, subId));

        List<UUID> modActuales = permisos.modulosDeRolEnSistema(rolId, s.getId());
        if (!modActuales.isEmpty()) permisos.deleteByRolIdAndModuloIdIn(rolId, modActuales);
        for (UUID modId : selMod) permisos.save(new PermisoRolModulo(rolId, modId));

        return new PermisoRolSistemaDto(rolId, s.getId(), selSec, selSub, selMod);
    }

    private static <P> Map<UUID, List<UUID>> agrupar(List<P> permisos,
            java.util.function.Function<P, UUID> clave, java.util.function.Function<P, UUID> valor) {
        return permisos.stream().collect(Collectors.groupingBy(clave, LinkedHashMap::new,
                Collectors.mapping(valor, Collectors.toList())));
    }

    /** Agrupa ids de entidades por su sistema usando la query [entidadId, sistemaId]. */
    private static Map<UUID, List<UUID>> porSistema(List<UUID> ids,
            java.util.function.Function<List<UUID>, List<Object[]>> consulta) {
        if (ids.isEmpty()) return Map.of();
        Map<UUID, List<UUID>> out = new LinkedHashMap<>();
        for (Object[] fila : consulta.apply(ids)) {
            out.computeIfAbsent((UUID) fila[1], k -> new ArrayList<>()).add((UUID) fila[0]);
        }
        return out;
    }

    private static List<UUID> filtrarValidos(List<UUID> seleccion, List<UUID> idsDelSistema) {
        if (seleccion == null || seleccion.isEmpty()) return List.of();
        Set<UUID> validos = new HashSet<>(idsDelSistema);
        return seleccion.stream().filter(validos::contains).distinct().toList();
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
