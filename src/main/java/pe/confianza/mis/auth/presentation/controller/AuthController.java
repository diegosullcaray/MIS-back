package pe.confianza.mis.auth.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import pe.confianza.mis.auth.presentation.dto.AuthDtos.*;
import pe.confianza.mis.auth.application.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** Paso 1: credenciales → desafío MFA (sin sesión). */
    @PostMapping("/login")
    public MfaChallengeResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return auth.login(req, ip(http), http.getHeader("User-Agent"));
    }

    /** Paso 2: OTP → token de sesión + usuario. */
    @PostMapping("/verificar-otp")
    public LoginResponse verificarOtp(@Valid @RequestBody VerificarOtpRequest req, HttpServletRequest http) {
        return auth.verificarOtp(req, ip(http), http.getHeader("User-Agent"));
    }

    private String ip(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : http.getRemoteAddr();
    }
}
