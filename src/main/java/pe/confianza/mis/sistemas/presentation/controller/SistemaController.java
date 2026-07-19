package pe.confianza.mis.sistemas.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.*;
import pe.confianza.mis.sistemas.application.service.SistemaService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sistemas")
public class SistemaController {

    private final SistemaService service;

    public SistemaController(SistemaService service) {
        this.service = service;
    }

    /** GET /api/v1/sistemas → SistemaResumen[] (sidebar + dashboard). */
    @GetMapping
    public List<SistemaResumenDto> listar() {
        return service.listar();
    }

    /** GET /api/v1/sistemas/{idOSlug} → Sistema completo con árbol. */
    @GetMapping("/{idOSlug}")
    public SistemaDto obtener(@PathVariable String idOSlug) {
        return service.obtener(idOSlug);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public ResponseEntity<SistemaDto> crear(@Valid @RequestBody SistemaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public SistemaDto actualizar(@PathVariable UUID id, @Valid @RequestBody SistemaRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** PUT /api/v1/sistemas/{id}/estructura — reemplaza el árbol completo. */
    @PutMapping("/{id}/estructura")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public SistemaDto estructura(@PathVariable UUID id, @RequestBody List<SeccionInput> secciones) {
        return service.reemplazarEstructura(id, secciones);
    }

    @GetMapping("/{id}/permisos")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public List<PermisoRolSistemaDto> permisos(@PathVariable UUID id) {
        return service.permisosDeSistema(id);
    }

    /**
     * PUT /api/v1/sistemas/{id}/permisos/{rolId} — upsert de permisos del rol por
     * nivel (v2.2): body { secciones, subsecciones, modulos } con herencia
     * descendente (conceder un nivel habilita todo lo que cuelga de él).
     */
    @PutMapping("/{id}/permisos/{rolId}")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public PermisoRolSistemaDto guardarPermisos(@PathVariable UUID id, @PathVariable UUID rolId,
                                                @RequestBody GuardarPermisosRequest req) {
        return service.guardarPermisos(id, rolId, req);
    }
}
