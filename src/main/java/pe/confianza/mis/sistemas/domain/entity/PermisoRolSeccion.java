package pe.confianza.mis.sistemas.domain.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Permiso a nivel de SECCIÓN por rol (tabla iam.permiso_rol_seccion, v2.2):
 * hereda a todas sus subsecciones y módulos. Se modela con UUIDs planos
 * (rol_id, seccion_id) para no acoplar el módulo `sistemas` al dominio de
 * `roles` (BE-01): la existencia del rol se valida vía RolLookup.
 */
@Entity
@Table(name = "permiso_rol_seccion", schema = "iam")
@IdClass(PermisoRolSeccion.Id.class)
public class PermisoRolSeccion {

    @jakarta.persistence.Id
    @Column(name = "rol_id")
    private UUID rolId;

    @jakarta.persistence.Id
    @Column(name = "seccion_id")
    private UUID seccionId;

    protected PermisoRolSeccion() {}

    public PermisoRolSeccion(UUID rolId, UUID seccionId) {
        this.rolId = rolId;
        this.seccionId = seccionId;
    }

    public UUID getRolId() { return rolId; }
    public UUID getSeccionId() { return seccionId; }

    public static class Id implements Serializable {
        private UUID rolId;
        private UUID seccionId;

        public Id() {}
        public Id(UUID rolId, UUID seccionId) { this.rolId = rolId; this.seccionId = seccionId; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Objects.equals(rolId, id.rolId) && Objects.equals(seccionId, id.seccionId);
        }
        @Override public int hashCode() { return Objects.hash(rolId, seccionId); }
    }
}
