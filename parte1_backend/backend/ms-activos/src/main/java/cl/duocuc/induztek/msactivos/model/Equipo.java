package cl.duocuc.induztek.msactivos.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "equipo", uniqueConstraints = @UniqueConstraint(columnNames = {"id_instalacion", "tag"}))
public class Equipo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_instalacion", nullable = false)
    private Instalacion instalacion;

    @Column(nullable = false, length = 30)
    private String tag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEquipo tipo;

    @Column(nullable = false, length = 60)
    private String marca;

    @Column(nullable = false, length = 60)
    private String modelo = "";

    @Column(name = "numero_serie", nullable = false, length = 40)
    private String numeroSerie;

    @Column(name = "anio_fabricacion")
    private Integer anioFabricacion;

    @Column(name = "potencia_kva")
    private Double potenciaKva;

    @Column(name = "tension_primaria_kv")
    private Double tensionPrimariaKv;

    @Column(name = "tension_secundaria_kv")
    private Double tensionSecundariaKv;

    @Column(name = "corriente_nominal_a")
    private Double corrienteNominalA;

    @Column(name = "max_contactos_uohm")
    private Double maxContactosUohm;

    @Column(nullable = false, length = 500)
    private String observaciones = "";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "equipo_dato_tecnico", joinColumns = @JoinColumn(name = "id_equipo"))
    @MapKeyColumn(name = "clave", length = 40)
    @Column(name = "valor", length = 80)
    private Map<String, String> datosTecnicos = new LinkedHashMap<>();

    @Column(name = "creado_por")
    private Long creadoPor;

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
    public Instalacion getInstalacion() { return instalacion; }
    public void setInstalacion(Instalacion instalacion) { this.instalacion = instalacion; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public TipoEquipo getTipo() { return tipo; }
    public void setTipo(TipoEquipo tipo) { this.tipo = tipo; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public Integer getAnioFabricacion() { return anioFabricacion; }
    public void setAnioFabricacion(Integer anioFabricacion) { this.anioFabricacion = anioFabricacion; }
    public Double getPotenciaKva() { return potenciaKva; }
    public void setPotenciaKva(Double potenciaKva) { this.potenciaKva = potenciaKva; }
    public Double getTensionPrimariaKv() { return tensionPrimariaKv; }
    public void setTensionPrimariaKv(Double tensionPrimariaKv) { this.tensionPrimariaKv = tensionPrimariaKv; }
    public Double getTensionSecundariaKv() { return tensionSecundariaKv; }
    public void setTensionSecundariaKv(Double tensionSecundariaKv) { this.tensionSecundariaKv = tensionSecundariaKv; }
    public Double getCorrienteNominalA() { return corrienteNominalA; }
    public void setCorrienteNominalA(Double corrienteNominalA) { this.corrienteNominalA = corrienteNominalA; }
    public Double getMaxContactosUohm() { return maxContactosUohm; }
    public void setMaxContactosUohm(Double maxContactosUohm) { this.maxContactosUohm = maxContactosUohm; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Map<String, String> getDatosTecnicos() { return datosTecnicos; }
    public void setDatosTecnicos(Map<String, String> datosTecnicos) { this.datosTecnicos = datosTecnicos; }
    public Long getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Long creadoPor) { this.creadoPor = creadoPor; }
    public Long getCreadoEn() { return creadoEn; }
    public Long getActualizadoEn() { return actualizadoEn; }
}
