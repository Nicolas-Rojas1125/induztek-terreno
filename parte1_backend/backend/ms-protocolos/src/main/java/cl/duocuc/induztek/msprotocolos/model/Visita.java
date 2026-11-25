package cl.duocuc.induztek.msprotocolos.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "visita")
public class Visita {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "numero_ot", nullable = false, length = 20)
    private String numeroOt;

    @Column(name = "id_instalacion", nullable = false)
    private Long idInstalacion;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "tipo_visita", nullable = false, length = 20)
    private String tipoVisita;

    @Column(name = "id_ingeniero_cargo", nullable = false)
    private Long idIngenieroCargo;

    @Column(name = "fecha_hora_cierre")
    private Long fechaHoraCierre;

    @Column(name = "latitud_cierre")
    private Double latitudCierre;

    @Column(name = "longitud_cierre")
    private Double longitudCierre;

    @Column(name = "id_usuario_cierra")
    private Long idUsuarioCierra;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "visita_participante", joinColumns = @JoinColumn(name = "id_visita"))
    @Column(name = "id_usuario")
    private Set<Long> participantes = new LinkedHashSet<>();

    @Column(name = "receptor_nombre", length = 120)
    private String receptorNombre;

    @Column(name = "receptor_rut", length = 12)
    private String receptorRut;

    @Column(name = "receptor_cargo", length = 80)
    private String receptorCargo;

    @Column(name = "fecha_hora_recepcion")
    private Long fechaHoraRecepcion;

    @Column(length = 8)
    private String origen;

    @Column(name = "actualizado_en", nullable = false)
    private Long actualizadoEn;

    public Long getId() { return id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getNumeroOt() { return numeroOt; }
    public void setNumeroOt(String numeroOt) { this.numeroOt = numeroOt; }
    public Long getIdInstalacion() { return idInstalacion; }
    public void setIdInstalacion(Long idInstalacion) { this.idInstalacion = idInstalacion; }
    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getTipoVisita() { return tipoVisita; }
    public void setTipoVisita(String tipoVisita) { this.tipoVisita = tipoVisita; }
    public Long getIdIngenieroCargo() { return idIngenieroCargo; }
    public void setIdIngenieroCargo(Long idIngenieroCargo) { this.idIngenieroCargo = idIngenieroCargo; }
    public Long getFechaHoraCierre() { return fechaHoraCierre; }
    public void setFechaHoraCierre(Long fechaHoraCierre) { this.fechaHoraCierre = fechaHoraCierre; }
    public Double getLatitudCierre() { return latitudCierre; }
    public void setLatitudCierre(Double latitudCierre) { this.latitudCierre = latitudCierre; }
    public Double getLongitudCierre() { return longitudCierre; }
    public void setLongitudCierre(Double longitudCierre) { this.longitudCierre = longitudCierre; }
    public Long getIdUsuarioCierra() { return idUsuarioCierra; }
    public void setIdUsuarioCierra(Long idUsuarioCierra) { this.idUsuarioCierra = idUsuarioCierra; }
    public Set<Long> getParticipantes() { return participantes; }
    public String getReceptorNombre() { return receptorNombre; }
    public void setReceptorNombre(String receptorNombre) { this.receptorNombre = receptorNombre; }
    public String getReceptorRut() { return receptorRut; }
    public void setReceptorRut(String receptorRut) { this.receptorRut = receptorRut; }
    public String getReceptorCargo() { return receptorCargo; }
    public void setReceptorCargo(String receptorCargo) { this.receptorCargo = receptorCargo; }
    public Long getFechaHoraRecepcion() { return fechaHoraRecepcion; }
    public void setFechaHoraRecepcion(Long fechaHoraRecepcion) { this.fechaHoraRecepcion = fechaHoraRecepcion; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public Long getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Long actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
