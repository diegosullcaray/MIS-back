package pe.confianza.mis.sistemas.domain.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Permiso a nivel de módulo por rol (tabla iam.permiso_rol_modulo). Se modela con
 * UUIDs planos (rol_id, modulo_id) para no acoplar el módulo `sistemas` al dominio
 * de `roles` (BE-01): la existencia del rol se valida vía RolLookup.
 */
@Entity
@Table(name = "permiso_rol_modulo", schema = "iam")
@IdClass(PermisoRolModulo.Id.class)
public class PermisoRolModulo {

    @jakarta.persistence.Id
    @Column(name = "rol_id")
    private UUID rolId;

    @jakarta.persistence.Id
    @Column(name = "modulo_id")
    private UUID moduloId;

    protected PermisoRolModulo() {}

    public PermisoRolModulo(UUID rolId, UUID moduloId) {
        this.rolId = rolId;
        this.moduloId = moduloId;
    }

    public UUID getRolId() { return rolId; }
    public UUID getModuloId() { return moduloId; }

    public static class Id implements Serializable {
        private UUID rolId;
        private UUID moduloId;

        public Id() {}
        public Id(UUID rolId, UUID moduloId) { this.rolId = rolId; this.moduloId = moduloId; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Objects.equals(rolId, id.rolId) && Objects.equals(moduloId, id.moduloId);
        }
        @Override public int hashCode() { return Objects.hash(rolId, moduloId); }
    }
}
