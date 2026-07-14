package pe.confianza.mis.roles.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.confianza.mis.roles.application.service.RolService;
import pe.confianza.mis.roles.presentation.dto.RolDtos.*;
import pe.confianza.mis.sistemas.application.service.SistemaService;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.PermisoRolSistemaDto;
import pe.confianza.mis.usuarios.application.service.UsuarioService;
import pe.confianza.mis.usuarios.presentation.dto.UsuarioDtos.UsuarioDto;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
public class RolController {

    private final RolService roles;
    private final UsuarioService usuarios;
    private final SistemaService sistemas;   // application layer (permite BE-01)

    public RolController(RolService roles, UsuarioService usuarios, SistemaService sistemas) {
        this.roles = roles;
        this.usuarios = usuarios;
        this.sistemas = sistemas;
    }

    @GetMapping
    public List<RolDto> listar() {
        return roles.listar();
    }

    @GetMapping("/{id}")
    public RolDto obtener(@PathVariable UUID id) {
        return roles.obtener(id);
    }

    @GetMapping("/{id}/usuarios")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public List<UsuarioDto> usuariosDeRol(@PathVariable UUID id) {
        return usuarios.usuariosDeRol(id);
    }

    @GetMapping("/{id}/permisos")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public List<PermisoRolSistemaDto> permisosDeRol(@PathVariable UUID id) {
        return sistemas.permisosDeRol(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public ResponseEntity<RolDto> crear(@Valid @RequestBody RolRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roles.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public RolDto actualizar(@PathVariable UUID id, @Valid @RequestBody RolRequest req) {
        return roles.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        roles.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
