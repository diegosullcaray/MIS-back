package pe.confianza.mis.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import pe.confianza.mis.usuarios.application.service.UsuarioDirectory.AuthUser;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTOs del módulo `auth` (espejo del contrato de AuthService del frontend). */
public final class AuthDtos {

    private AuthDtos() {}

    /** password = contraseña (dev) o ID token de Google (prod). */
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record MfaChallengeResponse(boolean mfaRequerido, String mfaToken, String email) {}

    public record VerificarOtpRequest(@NotBlank String mfaToken, @NotBlank String otp) {}

    public record LoginResponse(String token, UsuarioActivoDto usuario) {}

    /** Usuario activo devuelto al frontend tras verificar el OTP. */
    public record UsuarioActivoDto(
            UUID id, String nombre, String email, String rol,
            List<String> subsistemas, boolean activo, Instant creadoEn) {

        public static UsuarioActivoDto de(AuthUser u) {
            return new UsuarioActivoDto(u.id(), u.nombre(), u.email(), u.rolSlug(),
                    u.subsistemas(), u.activo(), u.creadoEn());
        }
    }
}
