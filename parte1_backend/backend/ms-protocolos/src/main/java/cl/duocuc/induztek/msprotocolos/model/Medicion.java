package cl.duocuc.induztek.msprotocolos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Medicion {
    @Column(nullable = false, length = 30)
    private String parametro;

    @Column(nullable = false)
    private Double valor;

    public Medicion() {
    }

    public Medicion(String parametro, Double valor) {
        this.parametro = parametro;
        this.valor = valor;
    }

    public String getParametro() { return parametro; }
    public Double getValor() { return valor; }
}
