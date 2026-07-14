package pe.confianza.mis.auth.application.service;

/** Canal de envío del OTP. Dev: log. Prod: correo corporativo (Gmail/SMTP). */
public interface OtpSender {
    void enviar(String email, String codigo);
}
