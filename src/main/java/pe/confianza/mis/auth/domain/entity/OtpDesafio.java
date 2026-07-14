package pe.confianza.mis.auth.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Desafío MFA (tabla auth.otp_desafios): hash del OTP, TTL, intentos, un solo uso. */
@Entity
@Table(name = "otp_desafios", schema = "auth")
public class OtpDesafio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private short intentos = 0;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    protected OtpDesafio() {}

    public OtpDesafio(UUID usuarioId, String codigoHash, Instant expiraEn) {
        this.usuarioId = usuarioId;
        this.codigoHash = codigoHash;
        this.expiraEn = expiraEn;
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getCodigoHash() { return codigoHash; }
    public Instant getExpiraEn() { return expiraEn; }
    public short getIntentos() { return intentos; }
    public Instant getUsadoEn() { return usadoEn; }

    public boolean estaVigente() {
        return usadoEn == null && expiraEn.isAfter(Instant.now());
    }

    public void registrarIntento() { this.intentos++; }
    public void marcarUsado() { this.usadoEn = Instant.now(); }
}
