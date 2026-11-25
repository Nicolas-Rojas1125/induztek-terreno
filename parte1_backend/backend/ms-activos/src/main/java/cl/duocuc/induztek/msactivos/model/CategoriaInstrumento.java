package cl.duocuc.induztek.msactivos.model;

public enum CategoriaInstrumento {
    MEGOHMETRO,
    TTR,
    MICROOHMIMETRO,
    INYECCION_SECUNDARIA;

    public static CategoriaInstrumento desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        for (CategoriaInstrumento c : values()) {
            if (c.name().equalsIgnoreCase(texto.trim())) {
                return c;
            }
        }
        return null;
    }
}
