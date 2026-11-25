package cl.duocuc.induztek.msactivos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "calibracion")
public class Calibracion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_instrumento", nullable = false)
    private Instrumento instrumento;

    @Column(name = "fecha_calibracion", nullable = false)
    private LocalDate fechaCalibracion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "id_usuario_registra")
    private Long idUsuarioRegistra;

    @Column(name = "registrada_por", nullable = false, length = 120)
    private String registradaPor = "";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instrumento getInstrumento() { return instrumento; }
    public void setInstrumento(Instrumento instrumento) { this.instrumento = instrumento; }
    public LocalDate getFechaCalibracion() { return fechaCalibracion; }
    public void setFechaCalibracion(LocalDate fechaCalibracion) { this.fechaCalibracion = fechaCalibracion; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public Long getIdUsuarioRegistra() { return idUsuarioRegistra; }
    public void setIdUsuarioRegistra(Long idUsuarioRegistra) { this.idUsuarioRegistra = idUsuarioRegistra; }
    public String getRegistradaPor() { return registradaPor; }
    public void setRegistradaPor(String registradaPor) { this.registradaPor = registradaPor; }
}
