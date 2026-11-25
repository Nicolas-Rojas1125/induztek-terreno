package cl.duocuc.induztek.msactivos.model;

public enum TipoEquipo {
    TRANSFORMADOR("Transformador", "PRIMARIO"),
    INTERRUPTOR("Interruptor", "PRIMARIO"),
    CELDA_MT("Celda MT", "PRIMARIO"),
    CABLE("Cable de poder", "PRIMARIO"),
    RELE("Relé de protección", "SECUNDARIO");

    private final String etiqueta;
    private final String clasificacion;

    TipoEquipo(String etiqueta, String clasificacion) {
        this.etiqueta = etiqueta;
        this.clasificacion = clasificacion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getClasificacion() {
        return clasificacion;
    }

    public static TipoEquipo desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        for (TipoEquipo t : values()) {
            if (t.name().equalsIgnoreCase(texto.trim())) {
                return t;
            }
        }
        return null;
    }
}
