package pe.confianza.mis.sistemas.domain.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "subsecciones", schema = "sistemas")
public class Subseccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id")
    private Seccion seccion;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private short orden = 0;

    @OneToMany(mappedBy = "subseccion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Modulo> modulos = new ArrayList<>();

    public UUID getId() { return id; }
    public Seccion getSeccion() { return seccion; }
    public void setSeccion(Seccion seccion) { this.seccion = seccion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public short getOrden() { return orden; }
    public void setOrden(short orden) { this.orden = orden; }
    public List<Modulo> getModulos() { return modulos; }

    public void addModulo(Modulo m) {
        m.setSubseccion(this);
        modulos.add(m);
    }
}
