package cl.duocuc.induztek.msusuarios.model;

import java.util.EnumSet;
import java.util.Set;

public enum Rol {
    ADMINISTRADOR("Administrador (Jefe técnico)"),
    INGENIERO("Ingeniero de pruebas"),
    TECNICO("Técnico de terreno"),
    CLIENTE("Usuario cliente");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public Set<Rol> rolesQuePuedeCrear() {
        return switch (this) {
            case ADMINISTRADOR -> EnumSet.allOf(Rol.class);
            case INGENIERO -> EnumSet.of(TECNICO, CLIENTE);
            case TECNICO, CLIENTE -> EnumSet.noneOf(Rol.class);
        };
    }

    public boolean puedeCrear(Rol otro) {
        return rolesQuePuedeCrear().contains(otro);
    }

    public static Rol desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        for (Rol r : values()) {
            if (r.name().equalsIgnoreCase(texto.trim())) {
                return r;
            }
        }
        return null;
    }
}
