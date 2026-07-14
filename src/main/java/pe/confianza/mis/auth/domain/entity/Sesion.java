package pe.confianza.mis.auth.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** JWT emitido (tabla auth.sesiones) → permite revocación inmediata. */
@Entity
@Table(name = "sesiones", schema = "auth")
public class Sesion {

    @Id
    private UUID jti;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "emitida_en", nullable = false)
    private Instant emitidaEn = Instant.now();

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "revocada_en")
    private Instant revocadaEn;

    @Column(name = "ip_origen")
    private String ipOrigen;

    @Column(name = "user_agent")
    private String userAgent;

    protected Sesion() {}

    public Sesion(UUID jti, UUID usuarioId, Instant expiraEn, String ipOrigen, String userAgent) {
        this.jti = jti;
        this.usuarioId = usuarioId;
        this.expiraEn = expiraEn;
        this.ipOrigen = ipOrigen;
        this.userAgent = userAgent;
    }

    public UUID getJti() { return jti; }
    public UUID getUsuarioId() { return usuarioId; }
    public Instant getRevocadaEn() { return revocadaEn; }
    public void revocar() { this.revocadaEn = Instant.now(); }
}
