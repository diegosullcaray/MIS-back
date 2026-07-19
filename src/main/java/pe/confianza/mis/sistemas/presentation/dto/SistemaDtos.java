package pe.confianza.mis.sistemas.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTOs del módulo `sistemas` (espejo de sistema.model.ts del frontend). */
public final class SistemaDtos {

    private SistemaDtos() {}

    public record ModuloDto(UUID id, String nombre, String slug, String icono, boolean activo) {}

    public record SubseccionDto(UUID id, String nombre, String slug, List<ModuloDto> modulos) {}

    public record SeccionDto(UUID id, String nombre, String slug, String icono, List<SubseccionDto> subsecciones) {}

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

    // Estructura (PUT /sistemas/{id}/estructura) — entrada anidada de secciones.
    // El orden lo define la posición en el arreglo (se persiste 1-based, v2.1);
    // `icono` es opcional (default 'pi pi-folder' / 'pi pi-file').
    public record ModuloInput(String nombre, String slug, String icono, Boolean activo) {}
    public record SubseccionInput(String nombre, String slug, List<ModuloInput> modulos) {}
    public record SeccionInput(String nombre, String slug, String icono, List<SubseccionInput> subsecciones) {}

    // Permisos por nivel (v2.2): el rol puede recibir permiso en sección,
    // subsección o módulo, con herencia descendente. El nivel SISTEMA se
    // administra con el campo `subsistemas` del rol (iam.rol_sistema).
    public record PermisoRolSistemaDto(
            UUID rolId, UUID sistemaId,
            List<UUID> secciones, List<UUID> subsecciones, List<UUID> modulos) {}
    public record GuardarPermisosRequest(
            List<UUID> secciones, List<UUID> subsecciones, List<UUID> modulos) {}
}
