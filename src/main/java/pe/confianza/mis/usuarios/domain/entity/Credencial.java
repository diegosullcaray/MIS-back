package pe.confianza.mis.usuarios.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Credenciales y política de cuenta (tabla iam.credenciales) — separadas del
 * perfil por seguridad. El hash jamás sale en un DTO.
 */
@Entity
@Table(name = "credenciales", schema = "iam")
public class Credencial {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "debe_cambiar", nullable = false)
    private boolean debeCambiar = false;

    @Column(name = "intentos_fallidos", nullable = false)
    private short intentosFallidos = 0;

    @Column(name = "bloqueada_hasta")
    private Instant bloqueadaHasta;

    @Column(name = "rotada_en", nullable = false)
    private Instant rotadaEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    protected Credencial() {}

    public Credencial(UUID usuarioId, String passwordHash) {
        this.usuarioId = usuarioId;
        this.passwordHash = passwordHash;
    }

    public UUID getUsuarioId() { return usuarioId; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String h) { this.passwordHash = h; this.rotadaEn = Instant.now(); touch(); }
    public boolean isDebeCambiar() { return debeCambiar; }
    public void setDebeCambiar(boolean v) { this.debeCambiar = v; }
    public short getIntentosFallidos() { return intentosFallidos; }
    public Instant getBloqueadaHasta() { return bloqueadaHasta; }

    public boolean estaBloqueada() {
        return bloqueadaHasta != null && bloqueadaHasta.isAfter(Instant.now());
    }

    public void registrarFallo(int maxIntentos, java.time.Duration bloqueo) {
        intentosFallidos++;
        if (intentosFallidos >= maxIntentos) {
            bloqueadaHasta = Instant.now().plus(bloqueo);
            intentosFallidos = 0;
        }
        touch();
    }

    public void registrarExito() {
        intentosFallidos = 0;
        bloqueadaHasta = null;
        touch();
    }

    private void touch() { this.actualizadoEn = Instant.now(); }
}
