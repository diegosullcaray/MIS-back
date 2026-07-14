package pe.confianza.mis.roles.application.service;

import pe.confianza.mis.roles.presentation.dto.RolDtos.RolDto;
import pe.confianza.mis.roles.presentation.dto.RolDtos.RolRequest;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de Roles expuestos a la capa `presentation` (BE-02). La implementación vive
 * en {@code application/impl}; los módulos externos solo dependen de esta interfaz.
 */
public interface RolService {
    List<RolDto> listar();
    RolDto obtener(UUID id);
    RolDto crear(RolRequest req);
    RolDto actualizar(UUID id, RolRequest req);
    void eliminar(UUID id);
}
