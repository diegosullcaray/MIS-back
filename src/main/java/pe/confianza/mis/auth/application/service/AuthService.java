package pe.confianza.mis.auth.application.service;

import pe.confianza.mis.auth.presentation.dto.AuthDtos.LoginRequest;
import pe.confianza.mis.auth.presentation.dto.AuthDtos.LoginResponse;
import pe.confianza.mis.auth.presentation.dto.AuthDtos.MfaChallengeResponse;
import pe.confianza.mis.auth.presentation.dto.AuthDtos.VerificarOtpRequest;

/** Login en 2 pasos con MFA (doc 04 §6). Implementación en {@code application/impl}. */
public interface AuthService {

    /** Paso 1: valida credenciales y genera el desafío MFA (sin sesión). */
    MfaChallengeResponse login(LoginRequest req, String ip, String ua);

    /** Paso 2: valida el OTP, emite el JWT y registra la sesión. */
    LoginResponse verificarOtp(VerificarOtpRequest req, String ip, String ua);
}
