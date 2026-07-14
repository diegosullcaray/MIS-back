package pe.confianza.mis.sistemas.domain.entity;

import jakarta.persistence.*;
import pe.confianza.mis.core.persistence.AuditableEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sistemas", schema = "sistemas")
public class Sistema extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String descripcion = "";

    @Column(nullable = false)
    private String icono = "pi pi-th-large";

    @Column(nullable = false)
    private String url = "";

    @Column(nullable = false)
    private String version = "1.0.0";

    /** activo | mantenimiento | inactivo */
    @Column(nullable = false)
    private String estado = "inactivo";

    @OneToMany(mappedBy = "sistema", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Seccion> secciones = new ArrayList<>();

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<Seccion> getSecciones() { return secciones; }

    public void reemplazarSecciones(List<Seccion> nuevas) {
        secciones.clear();
        for (Seccion s : nuevas) {
            s.setSistema(this);
            secciones.add(s);
        }
    }
}
