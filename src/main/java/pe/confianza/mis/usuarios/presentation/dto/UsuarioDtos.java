package pe.confianza.mis.usuarios.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTOs del módulo `usuarios` (espejo de acceso.model.ts del frontend). */
public final class UsuarioDtos {

    private UsuarioDtos() {}

    /** rol se expone como slug; subsistemas como slugs. */
    public record UsuarioDto(
            UUID id, String nombre, String email, String rol,
            List<String> subsistemas, boolean activo, Instant creadoEn) {}

    public record UsuarioRequest(
            @NotBlank String nombre,
            @NotBlank @Email String email,
            String password,                 // opcional en edición
            @NotNull UUID rolId,
            List<String> subsistemas) {}

    public record CambiarEstadoRequest(boolean activo) {}
}
