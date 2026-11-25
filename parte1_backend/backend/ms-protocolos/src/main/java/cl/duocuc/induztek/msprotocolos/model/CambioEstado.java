package cl.duocuc.induztek.msprotocolos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CambioEstado {
    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "fecha_hora", nullable = false)
    private Long fechaHora;

    @Column(nullable = false, length = 500)
    private String motivo = "";

    public CambioEstado() {
    }

    public CambioEstado(String estadoAnterior, String estadoNuevo, Long idUsuario, Long fechaHora, String motivo) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.idUsuario = idUsuario;
        this.fechaHora = fechaHora;
        this.motivo = motivo == null ? "" : motivo;
    }

    public String getEstadoAnterior() { return estadoAnterior; }
    public String getEstadoNuevo() { return estadoNuevo; }
    public Long getIdUsuario() { return idUsuario; }
    public Long getFechaHora() { return fechaHora; }
    public String getMotivo() { return motivo; }
}
