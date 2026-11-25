package cl.duocuc.induztek.msprotocolos.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "protocolo")
public class Protocolo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(nullable = false, unique = true, length = 24)
    private String numero;

    @Column(name = "visita_uuid", nullable = false, length = 36)
    private String visitaUuid;

    @Column(name = "id_equipo", nullable = false)
    private Long idEquipo;

    @Column(name = "id_instalacion", nullable = false)
    private Long idInstalacion;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @Column(name = "id_usuario_registra", nullable = false)
    private Long idUsuarioRegistra;

    @Column(name = "fecha_hora_prueba", nullable = false)
    private Long fechaHoraPrueba;

    @Column(name = "temperatura_c")
    private Double temperaturaC;

    @Column(name = "humedad_pct")
    private Double humedadPct;

    @Column(name = "descripcion_mantenimiento", nullable = false, length = 1000)
    private String descripcionMantenimiento = "";

    @Column(nullable = false, length = 1000)
    private String observaciones = "";

    @Column(name = "comentario_variacion", nullable = false, length = 1000)
    private String comentarioVariacion = "";

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "resultado_global", length = 12)
    private String resultadoGlobal;

    @Column(name = "reemplaza_uuid", length = 36)
    private String reemplazaUuid;

    @OneToMany(mappedBy = "protocolo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Prueba> pruebas = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "historial_estado", joinColumns = @JoinColumn(name = "id_protocolo"))
    @OrderBy("fechaHora ASC")
    private List<CambioEstado> historial = new ArrayList<>();

    @Column(name = "actualizado_en", nullable = false)
    private Long actualizadoEn;

    public void reemplazarPruebas(List<Prueba> nuevas) {
        pruebas.clear();
        for (Prueba p : nuevas) {
            p.setProtocolo(this);
            pruebas.add(p);
        }
    }

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getVisitaUuid() { return visitaUuid; }
    public void setVisitaUuid(String visitaUuid) { this.visitaUuid = visitaUuid; }
    public Long getIdEquipo() { return idEquipo; }
    public void setIdEquipo(Long idEquipo) { this.idEquipo = idEquipo; }
    public Long getIdInstalacion() { return idInstalacion; }
    public void setIdInstalacion(Long idInstalacion) { this.idInstalacion = idInstalacion; }
    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }
    public Long getIdUsuarioRegistra() { return idUsuarioRegistra; }
    public void setIdUsuarioRegistra(Long idUsuarioRegistra) { this.idUsuarioRegistra = idUsuarioRegistra; }
    public Long getFechaHoraPrueba() { return fechaHoraPrueba; }
    public void setFechaHoraPrueba(Long fechaHoraPrueba) { this.fechaHoraPrueba = fechaHoraPrueba; }
    public Double getTemperaturaC() { return temperaturaC; }
    public void setTemperaturaC(Double temperaturaC) { this.temperaturaC = temperaturaC; }
    public Double getHumedadPct() { return humedadPct; }
    public void setHumedadPct(Double humedadPct) { this.humedadPct = humedadPct; }
    public String getDescripcionMantenimiento() { return descripcionMantenimiento; }
    public void setDescripcionMantenimiento(String descripcionMantenimiento) { this.descripcionMantenimiento = descripcionMantenimiento; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getComentarioVariacion() { return comentarioVariacion; }
    public void setComentarioVariacion(String comentarioVariacion) { this.comentarioVariacion = comentarioVariacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getResultadoGlobal() { return resultadoGlobal; }
    public void setResultadoGlobal(String resultadoGlobal) { this.resultadoGlobal = resultadoGlobal; }
    public String getReemplazaUuid() { return reemplazaUuid; }
    public void setReemplazaUuid(String reemplazaUuid) { this.reemplazaUuid = reemplazaUuid; }
    public List<Prueba> getPruebas() { return pruebas; }
    public List<CambioEstado> getHistorial() { return historial; }
    public Long getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Long actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
