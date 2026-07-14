package pe.confianza.mis.sistemas.presentation.mapper;

import org.mapstruct.Mapper;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.ModuloDto;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.SeccionDto;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.SistemaDto;
import pe.confianza.mis.sistemas.presentation.dto.SistemaDtos.SubseccionDto;
import pe.confianza.mis.sistemas.domain.entity.Modulo;
import pe.confianza.mis.sistemas.domain.entity.Seccion;
import pe.confianza.mis.sistemas.domain.entity.Sistema;
import pe.confianza.mis.sistemas.domain.entity.Subseccion;

/**
 * Mapeo del árbol Sistema → Secciones → Subsecciones → Módulos a DTO en
 * compile-time (BE-03). El resumen (contadores agregados) se compone en el
 * service porque requiere cálculos y un lookup cross-módulo.
 */
@Mapper(componentModel = "spring")
public interface SistemaMapper {

    ModuloDto toDto(Modulo modulo);

    SubseccionDto toDto(Subseccion subseccion);

    SeccionDto toDto(Seccion seccion);

    SistemaDto toDetalle(Sistema sistema);
}
