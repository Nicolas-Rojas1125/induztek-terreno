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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prueba")
public class Prueba {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_protocolo", nullable = false)
    private Protocolo protocolo;

    @Column(name = "tipo_prueba", nullable = false, length = 20)
    private String tipoPrueba;

    @Column(nullable = false, length = 30)
    private String elemento;

    @Column(nullable = false, length = 12)
    private String momento;

    @Column(nullable = false)
    private boolean realizada;

    @Column(name = "motivo_no_realizada", nullable = false, length = 500)
    private String motivoNoRealizada = "";

    @Column(name = "id_instrumento")
    private Long idInstrumento;

    @Column(length = 12)
    private String resultado;

    @Column(name = "no_opera", nullable = false)
    private boolean noOpera;

    @Column(nullable = false, length = 500)
    private String comentario = "";

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "medicion", joinColumns = @JoinColumn(name = "id_prueba"))
    private List<Medicion> mediciones = new ArrayList<>();

    public Long getId() { return id; }
    public Protocolo getProtocolo() { return protocolo; }
    public void setProtocolo(Protocolo protocolo) { this.protocolo = protocolo; }
    public String getTipoPrueba() { return tipoPrueba; }
    public void setTipoPrueba(String tipoPrueba) { this.tipoPrueba = tipoPrueba; }
    public String getElemento() { return elemento; }
    public void setElemento(String elemento) { this.elemento = elemento; }
    public String getMomento() { return momento; }
    public void setMomento(String momento) { this.momento = momento; }
    public boolean isRealizada() { return realizada; }
    public void setRealizada(boolean realizada) { this.realizada = realizada; }
    public String getMotivoNoRealizada() { return motivoNoRealizada; }
    public void setMotivoNoRealizada(String motivoNoRealizada) { this.motivoNoRealizada = motivoNoRealizada; }
    public Long getIdInstrumento() { return idInstrumento; }
    public void setIdInstrumento(Long idInstrumento) { this.idInstrumento = idInstrumento; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public boolean isNoOpera() { return noOpera; }
    public void setNoOpera(boolean noOpera) { this.noOpera = noOpera; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public List<Medicion> getMediciones() { return mediciones; }
}
