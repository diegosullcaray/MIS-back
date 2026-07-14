package pe.confianza.mis.usuarios.domain.entity;

import jakarta.persistence.*;
import pe.confianza.mis.core.persistence.AuditableEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "usuarios", schema = "iam")
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * FK a iam.roles como UUID plano: el rol vive en el módulo `roles` y las
     * entidades no cruzan la frontera del módulo (BE-01); los datos del rol
     * se resuelven vía RolLookup.
     */
    @Column(name = "rol_id", nullable = false)
    private UUID rolId;

    @Column(nullable = false)
    private boolean activo = true;

    /** Override de subsistemas por usuario (tabla iam.usuario_sistema). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_sistema", schema = "iam",
            joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "sistema_id")
    private Set<UUID> sistemaIds = new HashSet<>();

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UUID getRolId() { return rolId; }
    public void setRolId(UUID rolId) { this.rolId = rolId; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Set<UUID> getSistemaIds() { return sistemaIds; }
    public void setSistemaIds(Set<UUID> ids) { this.sistemaIds = ids; }
}
