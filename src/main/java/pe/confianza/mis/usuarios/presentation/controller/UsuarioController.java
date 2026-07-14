package pe.confianza.mis.usuarios.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.confianza.mis.core.web.PageResponse;
import pe.confianza.mis.usuarios.application.service.UsuarioService;
import pe.confianza.mis.usuarios.presentation.dto.UsuarioDtos.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN_SISTEMA')")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<UsuarioDto> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return PageResponse.from(service.listar(q, activo, page, pageSize));
    }

    @GetMapping("/{id}")
    public UsuarioDto obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioDto> crear(@Valid @RequestBody UsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public UsuarioDto actualizar(@PathVariable UUID id, @Valid @RequestBody UsuarioRequest req) {
        return service.actualizar(id, req);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioDto cambiarEstado(@PathVariable UUID id, @RequestBody CambiarEstadoRequest req) {
        return service.cambiarEstado(id, req.activo());
    }
}
