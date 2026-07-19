package pe.confianza.mis.sistemas.domain.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Permiso a nivel de SUBSECCIÓN por rol (tabla iam.permiso_rol_subseccion, v2.2):
 * hereda a todos sus módulos. Se modela con UUIDs planos (rol_id, subseccion_id)
 * para no acoplar el módulo `sistemas` al dominio de `roles` (BE-01).
 */
@Entity
@Table(name = "permiso_rol_subseccion", schema = "iam")
@IdClass(PermisoRolSubseccion.Id.class)
public class PermisoRolSubseccion {

    @jakarta.persistence.Id
    @Column(name = "rol_id")
    private UUID rolId;

    @jakarta.persistence.Id
    @Column(name = "subseccion_id")
    private UUID subseccionId;

    protected PermisoRolSubseccion() {}

    public PermisoRolSubseccion(UUID rolId, UUID subseccionId) {
        this.rolId = rolId;
        this.subseccionId = subseccionId;
    }

    public UUID getRolId() { return rolId; }
    public UUID getSubseccionId() { return subseccionId; }

    public static class Id implements Serializable {
        private UUID rolId;
        private UUID subseccionId;

        public Id() {}
        public Id(UUID rolId, UUID subseccionId) { this.rolId = rolId; this.subseccionId = subseccionId; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Objects.equals(rolId, id.rolId) && Objects.equals(subseccionId, id.subseccionId);
        }
        @Override public int hashCode() { return Objects.hash(rolId, subseccionId); }
    }
}
