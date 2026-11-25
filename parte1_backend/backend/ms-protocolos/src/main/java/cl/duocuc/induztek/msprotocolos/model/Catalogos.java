package cl.duocuc.induztek.msprotocolos.model;

import java.util.Set;

public final class Catalogos {
    public static final Set<String> ESTADOS = Set.of("BORRADOR", "REGISTRADO_TERRENO", "OBSERVADO", "FORMALIZADO", "ANULADO");
    public static final Set<String> ESTADOS_VISIBLES_CLIENTE = Set.of("REGISTRADO_TERRENO", "OBSERVADO", "FORMALIZADO");
    public static final Set<String> RESULTADOS = Set.of("APROBADO", "OBSERVADO", "RECHAZADO");
    public static final Set<String> TIPOS_PRUEBA = Set.of("AISLACION", "TTR", "DEVANADOS", "CONTACTOS", "OPERACION_RELE");
    public static final Set<String> MOMENTOS = Set.of("UNICA", "PREVIA", "POSTERIOR");
    public static final Set<String> TIPOS_VISITA = Set.of("SOLO_PRUEBAS", "PRUEBAS_Y_MANT");
    public static final String FORMALIZADO = "FORMALIZADO";
    public static final String BORRADOR = "BORRADOR";

    private Catalogos() {
    }
}
