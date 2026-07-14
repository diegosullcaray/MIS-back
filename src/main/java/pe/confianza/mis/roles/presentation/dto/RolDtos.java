package pe.confianza.mis.roles.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

/** DTOs del módulo `roles` (espejo de acceso.model.ts del frontend). */
public final class RolDtos {

    private RolDtos() {}

    /** subsistemas se expone como slugs. */
    public record RolDto(UUID id, String nombre, String slug, List<String> subsistemas) {}

    public record RolRequest(
            @NotBlank String nombre,
            @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                    message = "El slug debe ser kebab-case en minúsculas.") String slug,
            List<String> subsistemas) {}
}
