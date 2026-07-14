package pe.confianza.mis.auth.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.confianza.mis.auth.application.service.OtpSender;

/**
 * Emisor de OTP por LOG (desarrollo). En prod se sustituye por un sender de correo
 * corporativo (Gmail API / SMTP) — mismo puerto {@link OtpSender}.
 */
@Component
public class LogOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LogOtpSender.class);

    @Override
    public void enviar(String email, String codigo) {
        log.info("═══ OTP para {} → {} (solo dev; en prod se envía por correo) ═══", email, codigo);
    }
}
