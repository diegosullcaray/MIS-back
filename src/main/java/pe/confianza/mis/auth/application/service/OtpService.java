package pe.confianza.mis.auth.application.service;

import java.util.UUID;

/** Genera y valida los desafíos OTP (TTL 3 min, máx. intentos, un solo uso). */
public interface OtpService {

    /** Crea un desafío y envía el OTP; devuelve el id del desafío. */
    UUID crear(UUID usuarioId, String email);

    /** Valida el OTP contra el desafío; lo marca como usado si es correcto. */
    void verificar(UUID challengeId, UUID usuarioId, String otp);
}
