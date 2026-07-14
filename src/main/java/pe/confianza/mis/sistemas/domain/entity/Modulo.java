package pe.confianza.mis.sistemas.domain.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "modulos", schema = "sistemas")
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subseccion_id")
    private Subseccion subseccion;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private short orden = 0;

    public UUID getId() { return id; }
    public Subseccion getSubseccion() { return subseccion; }
    public void setSubseccion(Subseccion subseccion) { this.subseccion = subseccion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public short getOrden() { return orden; }
    public void setOrden(short orden) { this.orden = orden; }
}
