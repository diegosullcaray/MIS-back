package pe.confianza.mis.auth.application.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.confianza.mis.usuarios.application.service.UsuarioDirectory;
import pe.confianza.mis.usuarios.application.service.UsuarioDirectory.AuthUser;
import pe.confianza.mis.auth.presentation.dto.AuthDtos.*;
import pe.confianza.mis.auth.application.service.AuthService;
import pe.confianza.mis.auth.application.service.IdentityVerifier;
import pe.confianza.mis.auth.application.service.OtpService;
import pe.confianza.mis.auth.domain.entity.Sesion;
import pe.confianza.mis.auth.domain.repository.SesionRepository;
import pe.confianza.mis.core.exception.UnauthorizedException;
import pe.confianza.mis.core.security.JwtProvider;

import java.util.UUID;

/** Orquesta el login en 2 pasos con MFA (doc 04 §6) y la emisión del JWT. */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final IdentityVerifier identityVerifier;
    private final UsuarioDirectory directorio;
    private final OtpService otpService;
    private final MfaTokenProvider mfaTokens;
    private final JwtProvider jwt;
    private final SesionRepository sesiones;
    private final AccessLogger accessLog;

    public AuthServiceImpl(IdentityVerifier identityVerifier, UsuarioDirectory directorio,
                           OtpService otpService, MfaTokenProvider mfaTokens, JwtProvider jwt,
                           SesionRepository sesiones, AccessLogger accessLog) {
        this.identityVerifier = identityVerifier;
        this.directorio = directorio;
        this.otpService = otpService;
        this.mfaTokens = mfaTokens;
        this.jwt = jwt;
        this.sesiones = sesiones;
        this.accessLog = accessLog;
    }

    /** Paso 1: valida credenciales y genera el desafío MFA (sin sesión). */
    @Override
    public MfaChallengeResponse login(LoginRequest req, String ip, String ua) {
        UUID usuarioId;
        try {
            usuarioId = identityVerifier.verificar(req.email(), req.password());
        } catch (RuntimeException e) {
            accessLog.registrar("login_fallido", null, req.email(), ip, ua, e.getMessage());
            throw e;
        }

        AuthUser user = directorio.porId(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado."));

        UUID challengeId = otpService.crear(usuarioId, user.email());
        String mfaToken = mfaTokens.generar(usuarioId, challengeId);
        accessLog.registrar("login_ok", usuarioId, user.email(), ip, ua, "desafío MFA emitido");

        return new MfaChallengeResponse(true, mfaToken, user.email());
    }

    /** Paso 2: valida el OTP, emite el JWT y registra la sesión. */
    @Override
    public LoginResponse verificarOtp(VerificarOtpRequest req, String ip, String ua) {
        MfaTokenProvider.Parsed parsed = mfaTokens.validar(req.mfaToken());

        try {
            otpService.verificar(parsed.challengeId(), parsed.usuarioId(), req.otp());
        } catch (RuntimeException e) {
            accessLog.registrar("otp_fallido", parsed.usuarioId(), null, ip, ua, e.getMessage());
            throw e;
        }

        AuthUser user = directorio.porId(parsed.usuarioId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado."));
        if (!user.activo())
            throw new UnauthorizedException("El usuario está desactivado.");

        JwtProvider.Token token = jwt.generar(user.id(), user.rolSlug(), user.subsistemas());
        sesiones.save(new Sesion(token.jti(), user.id(), token.expiresAt(), ip, ua));
        accessLog.registrar("otp_ok", user.id(), user.email(), ip, ua, "sesión emitida");

        return new LoginResponse(token.value(), UsuarioActivoDto.de(user));
    }
}
