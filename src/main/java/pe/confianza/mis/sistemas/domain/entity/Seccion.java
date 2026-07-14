package pe.confianza.mis.sistemas.domain.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "secciones", schema = "sistemas")
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sistema_id")
    private Sistema sistema;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private short orden = 0;

    @OneToMany(mappedBy = "seccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Subseccion> subsecciones = new ArrayList<>();

    public UUID getId() { return id; }
    public Sistema getSistema() { return sistema; }
    public void setSistema(Sistema sistema) { this.sistema = sistema; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public short getOrden() { return orden; }
    public void setOrden(short orden) { this.orden = orden; }
    public List<Subseccion> getSubsecciones() { return subsecciones; }

    public void addSubseccion(Subseccion sub) {
        sub.setSeccion(this);
        subsecciones.add(sub);
    }
}
