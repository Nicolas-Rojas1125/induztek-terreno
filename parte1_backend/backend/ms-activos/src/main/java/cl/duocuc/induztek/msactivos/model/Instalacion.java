package cl.duocuc.induztek.msactivos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "instalacion", uniqueConstraints = @UniqueConstraint(columnNames = {"id_cliente", "nombre"}))
public class Instalacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 200)
    private String ubicacion;

    @Column(name = "id_comuna", nullable = false)
    private Long idComuna;

    @Column(name = "nombre_comuna", nullable = false, length = 80)
    private String nombreComuna = "";

    @Column(name = "nombre_region", nullable = false, length = 80)
    private String nombreRegion = "";

    @Column(name = "observaciones_acceso", nullable = false, length = 500)
    private String observacionesAcceso = "";

    private Double latitud;

    private Double longitud;

    @Column(name = "creado_en", nullable = false)
    private Long creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Long actualizadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = System.currentTimeMillis();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public Long getIdComuna() { return idComuna; }
    public void setIdComuna(Long idComuna) { this.idComuna = idComuna; }
    public String getNombreComuna() { return nombreComuna; }
    public void setNombreComuna(String nombreComuna) { this.nombreComuna = nombreComuna; }
    public String getNombreRegion() { return nombreRegion; }
    public void setNombreRegion(String nombreRegion) { this.nombreRegion = nombreRegion; }
    public String getObservacionesAcceso() { return observacionesAcceso; }
    public void setObservacionesAcceso(String observacionesAcceso) { this.observacionesAcceso = observacionesAcceso; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public Long getCreadoEn() { return creadoEn; }
    public Long getActualizadoEn() { return actualizadoEn; }
}
