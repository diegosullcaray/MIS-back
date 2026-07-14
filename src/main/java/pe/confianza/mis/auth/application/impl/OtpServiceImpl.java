package pe.confianza.mis.auth.application.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.confianza.mis.auth.application.service.OtpSender;
import pe.confianza.mis.auth.application.service.OtpService;
import pe.confianza.mis.auth.domain.entity.OtpDesafio;
import pe.confianza.mis.auth.domain.repository.OtpDesafioRepository;
import pe.confianza.mis.core.exception.UnauthorizedException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/** Genera y valida los desafíos OTP (TTL 3 min, máx. intentos, un solo uso). */
@Service
@Transactional
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpDesafioRepository desafios;
    private final OtpSender sender;
    private final PasswordEncoder encoder;
    private final AuthProperties props;

    public OtpServiceImpl(OtpDesafioRepository desafios, OtpSender sender,
                          PasswordEncoder encoder, AuthProperties props) {
        this.desafios = desafios;
        this.sender = sender;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public UUID crear(UUID usuarioId, String email) {
        String codigo = generarCodigo();
        Instant expira = Instant.now().plus(props.otp().ttl());
        OtpDesafio desafio = desafios.save(new OtpDesafio(usuarioId, encoder.encode(codigo), expira));
        sender.enviar(email, codigo);
        return desafio.getId();
    }

    @Override
    public void verificar(UUID challengeId, UUID usuarioId, String otp) {
        OtpDesafio d = desafios.findById(challengeId)
                .orElseThrow(() -> new UnauthorizedException(
                        "La sesión de verificación expiró. Vuelve a iniciar sesión."));

        if (!d.getUsuarioId().equals(usuarioId) || !d.estaVigente())
            throw new UnauthorizedException("La sesión de verificación expiró. Vuelve a iniciar sesión.");

        if (d.getIntentos() >= props.otp().maxIntentos())
            throw new UnauthorizedException("Demasiados intentos. Vuelve a iniciar sesión.");

        d.registrarIntento();
        if (!encoder.matches(otp, d.getCodigoHash()))
            throw new UnauthorizedException("El código de verificación es incorrecto.");

        d.marcarUsado();
    }

    private String generarCodigo() {
        String fixed = props.otp().fixedCode();
        if (fixed != null && !fixed.isBlank()) {
            log.warn("OTP fijo activo (solo dev). Úsese '{}' para verificar.", fixed);
            return fixed;
        }
        int max = (int) Math.pow(10, props.otp().longitud());
        return String.format("%0" + props.otp().longitud() + "d", RANDOM.nextInt(max));
    }
}
