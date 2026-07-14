package pe.confianza.mis.usuarios.application.service;

import org.springframework.data.domain.Page;
import pe.confianza.mis.usuarios.presentation.dto.UsuarioDtos.UsuarioDto;
import pe.confianza.mis.usuarios.presentation.dto.UsuarioDtos.UsuarioRequest;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de Usuarios expuestos a la capa `presentation` (BE-02). La implementación
 * vive en {@code application/impl}.
 */
public interface UsuarioService {
    Page<UsuarioDto> listar(String q, Boolean activo, int page, int pageSize);
    UsuarioDto obtener(UUID id);
    List<UsuarioDto> usuariosDeRol(UUID rolId);
    UsuarioDto crear(UsuarioRequest req);
    UsuarioDto actualizar(UUID id, UsuarioRequest req);
    UsuarioDto cambiarEstado(UUID id, boolean activo);
}
