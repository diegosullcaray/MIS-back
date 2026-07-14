package pe.confianza.mis.roles.domain.entity;

import jakarta.persistence.*;
import pe.confianza.mis.core.persistence.AuditableEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles", schema = "iam")
public class Rol extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true, updatable = false)
    private String slug;

    /** Subsistemas habilitados por el rol (tabla iam.rol_sistema, por sistema_id). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_sistema", schema = "iam",
            joinColumns = @JoinColumn(name = "rol_id"))
    @Column(name = "sistema_id")
    private Set<UUID> sistemaIds = new HashSet<>();

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public Set<UUID> getSistemaIds() { return sistemaIds; }
    public void setSistemaIds(Set<UUID> ids) { this.sistemaIds = ids; }
}
