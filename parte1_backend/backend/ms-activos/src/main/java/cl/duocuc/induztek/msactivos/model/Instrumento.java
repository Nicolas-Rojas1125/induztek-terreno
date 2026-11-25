package cl.duocuc.induztek.msactivos.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "instrumento")
public class Instrumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaInstrumento categoria;

    @Column(nullable = false, length = 60)
    private String marca;

    @Column(nullable = false, length = 60)
    private String modelo;

    @Column(name = "numero_serie", nullable = false, unique = true, length = 40)
    private String numeroSerie;

    @Column(name = "ultima_calibracion")
    private LocalDate ultimaCalibracion;

    private LocalDate vencimiento;

    @OneToMany(mappedBy = "instrumento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("fechaCalibracion DESC")
    private List<Calibracion> calibraciones = new ArrayList<>();

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

    public void agregarCalibracion(Calibracion c) {
        c.setInstrumento(this);
        calibraciones.add(c);
        if (ultimaCalibracion == null || !c.getFechaCalibracion().isBefore(ultimaCalibracion)) {
            ultimaCalibracion = c.getFechaCalibracion();
            vencimiento = c.getFechaVencimiento();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public CategoriaInstrumento getCategoria() { return categoria; }
    public void setCategoria(CategoriaInstrumento categoria) { this.categoria = categoria; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public LocalDate getUltimaCalibracion() { return ultimaCalibracion; }
    public LocalDate getVencimiento() { return vencimiento; }
    public List<Calibracion> getCalibraciones() { return calibraciones; }
    public Long getCreadoEn() { return creadoEn; }
    public Long getActualizadoEn() { return actualizadoEn; }
}
