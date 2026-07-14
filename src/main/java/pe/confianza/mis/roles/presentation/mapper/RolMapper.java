package pe.confianza.mis.roles.presentation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.confianza.mis.roles.presentation.dto.RolDtos.RolDto;
import pe.confianza.mis.roles.domain.entity.Rol;

import java.util.List;

/**
 * Mapeo Entity → DTO en compile-time (BE-03). Los subsistemas (slugs) se resuelven
 * en el service vía SistemaLookup y se inyectan como parámetro.
 */
@Mapper(componentModel = "spring")
public interface RolMapper {

    @Mapping(target = "subsistemas", source = "subsistemas")
    RolDto toDto(Rol rol, List<String> subsistemas);
}
