package pe.confianza.mis.sistemas.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTOs del módulo `sistemas` (espejo de sistema.model.ts del frontend). */
public final class SistemaDtos {

    private SistemaDtos() {}

    public record ModuloDto(UUID id, String nombre, String slug, boolean activo) {}

    public record SubseccionDto(UUID id, String nombre, String slug, List<ModuloDto> modulos) {}

    public record SeccionDto(UUID id, String nombre, String slug, List<SubseccionDto> subsecciones) {}

    public record SistemaDto(
            UUID id, String nombre, String slug, String descripcion, String icono,
            String url, String version, String estado, List<SeccionDto> secciones,
            Instant creadoEn, Instant actualizadoEn) {}

    public record SistemaResumenDto(
            UUID id, String nombre, String slug, String descripcion, String icono,
            String version, String estado, int totalSecciones, int totalModulos,
            long rolesAsignados, Instant actualizadoEn) {}

    public record SistemaRequest(
            @NotBlank String nombre,
            @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                    message = "El slug debe ser kebab-case en minúsculas.") String slug,
            String descripcion, String icono, String url, String version, String estado) {}

    // Estructura (PUT /sistemas/{id}/estructura) — entrada anidada de secciones
    public record ModuloInput(String nombre, String slug, Boolean activo) {}
    public record SubseccionInput(String nombre, String slug, List<ModuloInput> modulos) {}
    public record SeccionInput(String nombre, String slug, List<SubseccionInput> subsecciones) {}

    public record PermisoRolSistemaDto(UUID rolId, UUID sistemaId, List<UUID> modulos) {}
    public record GuardarPermisosRequest(List<UUID> modulos) {}
}
