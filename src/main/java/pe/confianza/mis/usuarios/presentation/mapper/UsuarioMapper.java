package pe.confianza.mis.usuarios.presentation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.confianza.mis.usuarios.domain.entity.Usuario;
import pe.confianza.mis.usuarios.presentation.dto.UsuarioDtos.UsuarioDto;

import java.util.List;

/**
 * Mapeo Entity → DTO en compile-time (BE-03). El slug del rol y los subsistemas
 * (slugs) los resuelve el service vía RolLookup/SistemaLookup y se inyectan
 * como parámetros.
 */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "rol", source = "rolSlug")
    @Mapping(target = "subsistemas", source = "subsistemas")
    UsuarioDto toDto(Usuario u, String rolSlug, List<String> subsistemas);
}
