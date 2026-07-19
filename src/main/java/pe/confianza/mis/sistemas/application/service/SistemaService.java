package pe.confianza.mis.sistemas.application.service;

import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.GuardarPermisosRequest;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.PermisoRolSistemaDto;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.SeccionInput;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.SistemaDto;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.SistemaRequest;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.SistemaResumenDto;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de Sistemas (registro de MFEs, estructura y permisos) expuestos a
 * la capa `presentation` (BE-02). La implementación vive en {@code application/impl}.
 */
public interface SistemaService {
    List<SistemaResumenDto> listar();
    SistemaDto obtener(String idOSlug);
    SistemaDto crear(SistemaRequest req);
    SistemaDto actualizar(UUID id, SistemaRequest req);
    void eliminar(UUID id);
    SistemaDto reemplazarEstructura(UUID id, List<SeccionInput> entrada);
    List<PermisoRolSistemaDto> permisosDeSistema(UUID sistemaId);
    List<PermisoRolSistemaDto> permisosDeRol(UUID rolId);

    /**
     * Upsert de los permisos del rol en el sistema, por nivel (v2.2): secciones,
     * subsecciones y módulos, con herencia descendente resuelta por la vista
     * iam.v_permisos_efectivos.
     */
    PermisoRolSistemaDto guardarPermisos(UUID sistemaId, UUID rolId, GuardarPermisosRequest req);
}
