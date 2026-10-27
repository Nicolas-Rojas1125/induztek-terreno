package cl.duocuc.induztek.domain.model

enum class Clasificacion(val etiqueta: String) {
    PRIMARIO("Primario"),
    SECUNDARIO("Secundario")
}

enum class Unidad(val simbolo: String) {
    KV("kV"),
    MEGAOHM("MΩ"),
    MILIOHM("mΩ"),
    MICROOHM("µΩ"),
    AMPERE("A"),
    MILISEGUNDO("ms"),
    ADIMENSIONAL("")
}

enum class Parametro(
    val etiqueta: String,
    val unidad: Unidad,
    val minimo: Double,
    val maximo: Double,
    val entero: Boolean = false,
    val admiteGigaohm: Boolean = false
) {
    TENSION_PRUEBA("Tensión de prueba", Unidad.KV, 0.1, 15.0),
    LECTURA_1MIN("Lectura a 1 min", Unidad.MEGAOHM, 0.01, 10_000_000.0, admiteGigaohm = true),
    LECTURA_10MIN("Lectura a 10 min", Unidad.MEGAOHM, 0.01, 10_000_000.0, admiteGigaohm = true),
    POSICION_TAP("Posición del tap", Unidad.ADIMENSIONAL, 1.0, 33.0, entero = true),
    RELACION_TEORICA("Relación teórica", Unidad.ADIMENSIONAL, 0.01, 1_000.0),
    RELACION_MEDIDA("Relación medida", Unidad.ADIMENSIONAL, 0.01, 1_000.0),
    RESISTENCIA("Resistencia", Unidad.MILIOHM, 0.001, 1_000_000.0),
    RESISTENCIA_CONTACTO("Resistencia de contacto", Unidad.MICROOHM, 0.1, 100_000.0),
    CORRIENTE_AJUSTE("Corriente de ajuste", Unidad.AMPERE, 0.01, 100.0),
    CORRIENTE_OPERACION("Corriente de operación", Unidad.AMPERE, 0.01, 200.0),
    TIEMPO_ESPERADO("Tiempo esperado", Unidad.MILISEGUNDO, 1.0, 60_000.0),
    TIEMPO_MEDIDO("Tiempo medido", Unidad.MILISEGUNDO, 1.0, 60_000.0);

    val etiquetaConUnidad: String
        get() = if (unidad.simbolo.isEmpty()) etiqueta else "$etiqueta (${unidad.simbolo})"
}

enum class TipoPrueba(
    val etiqueta: String,
    val descripcionCorta: String,
    val resultadoManual: Boolean = false
) {
    AISLACION("Resistencia de aislación", "Megóhmetro · IP = 10 min ÷ 1 min"),
    TTR("Relación de transformación (TTR)", "Error % = |medida − teórica| ÷ teórica"),
    DEVANADOS("Resistencia de devanados", "Valor por fase · resultado manual", resultadoManual = true),
    CONTACTOS("Resistencia de contactos", "Valor por polo vs. máximo de placa"),
    OPERACION_RELE("Prueba de operación de relé", "Inyección secundaria · tiempo de operación");

    fun parametros(tipoEquipo: TipoEquipo): List<Parametro> = when (this) {
        AISLACION -> if (tipoEquipo.aislacionConIndicePolarizacion)
            listOf(Parametro.TENSION_PRUEBA, Parametro.LECTURA_1MIN, Parametro.LECTURA_10MIN)
        else
            listOf(Parametro.TENSION_PRUEBA, Parametro.LECTURA_1MIN)
        TTR -> listOf(Parametro.POSICION_TAP, Parametro.RELACION_TEORICA, Parametro.RELACION_MEDIDA)
        DEVANADOS -> listOf(Parametro.RESISTENCIA)
        CONTACTOS -> listOf(Parametro.RESISTENCIA_CONTACTO)
        OPERACION_RELE -> listOf(
            Parametro.CORRIENTE_AJUSTE, Parametro.CORRIENTE_OPERACION,
            Parametro.TIEMPO_ESPERADO, Parametro.TIEMPO_MEDIDO
        )
    }

    fun elementos(tipoEquipo: TipoEquipo): List<String> = when (this) {
        AISLACION -> when (tipoEquipo) {
            TipoEquipo.TRANSFORMADOR -> listOf("AT-BT", "AT-T", "BT-T")
            TipoEquipo.CABLE -> listOf("Fase R", "Fase S", "Fase T")
            else -> listOf("Polo 1", "Polo 2", "Polo 3")
        }
        TTR -> listOf("H1-X1", "H2-X2", "H3-X3")
        DEVANADOS -> listOf("AT U/H1", "AT V/H2", "AT W/H3", "BT U/X1", "BT V/X2", "BT W/X3")
        CONTACTOS -> listOf("Polo 1", "Polo 2", "Polo 3")
        OPERACION_RELE -> listOf("Fase R", "Fase S", "Fase T", "Neutro")
    }

    val parametrosPrincipales: List<Parametro>
        get() = when (this) {
            AISLACION -> listOf(Parametro.LECTURA_1MIN, Parametro.LECTURA_10MIN)
            TTR -> listOf(Parametro.RELACION_MEDIDA)
            DEVANADOS -> listOf(Parametro.RESISTENCIA)
            CONTACTOS -> listOf(Parametro.RESISTENCIA_CONTACTO)
            OPERACION_RELE -> listOf(Parametro.TIEMPO_MEDIDO)
        }
}

enum class TipoEquipo(
    val etiqueta: String,
    val clasificacion: Clasificacion,
    val pruebas: List<TipoPrueba>,
    val aislacionConIndicePolarizacion: Boolean,
    val usaPotencia: Boolean,
    val usaTensionSecundaria: Boolean,
    val usaCorriente: Boolean,
    val usaMaxContactos: Boolean
) {
    TRANSFORMADOR(
        "Transformador", Clasificacion.PRIMARIO,
        listOf(TipoPrueba.AISLACION, TipoPrueba.TTR, TipoPrueba.DEVANADOS),
        aislacionConIndicePolarizacion = true,
        usaPotencia = true, usaTensionSecundaria = true, usaCorriente = false, usaMaxContactos = false
    ),
    INTERRUPTOR(
        "Interruptor", Clasificacion.PRIMARIO,
        listOf(TipoPrueba.AISLACION, TipoPrueba.CONTACTOS),
        aislacionConIndicePolarizacion = false,
        usaPotencia = false, usaTensionSecundaria = false, usaCorriente = true, usaMaxContactos = true
    ),
    CELDA_MT(
        "Celda MT", Clasificacion.PRIMARIO,
        listOf(TipoPrueba.AISLACION, TipoPrueba.CONTACTOS),
        aislacionConIndicePolarizacion = false,
        usaPotencia = false, usaTensionSecundaria = false, usaCorriente = true, usaMaxContactos = true
    ),
    CABLE(
        "Cable de poder", Clasificacion.PRIMARIO,
        listOf(TipoPrueba.AISLACION),
        aislacionConIndicePolarizacion = true,
        usaPotencia = false, usaTensionSecundaria = false, usaCorriente = false, usaMaxContactos = false
    ),
    RELE(
        "Relé de protección", Clasificacion.SECUNDARIO,
        listOf(TipoPrueba.OPERACION_RELE),
        aislacionConIndicePolarizacion = false,
        usaPotencia = false, usaTensionSecundaria = false, usaCorriente = true, usaMaxContactos = false
    );

    companion object {
        fun desdeCodigo(codigo: String): TipoEquipo = entries.firstOrNull { it.name == codigo } ?: TRANSFORMADOR
    }
}

enum class CategoriaInstrumento(val etiqueta: String, val pruebas: Set<TipoPrueba>) {
    MEGOHMETRO("Megóhmetro", setOf(TipoPrueba.AISLACION)),
    TTR("Medidor de relación (TTR)", setOf(TipoPrueba.TTR)),
    MICROOHMIMETRO("Micro-ohmímetro", setOf(TipoPrueba.CONTACTOS, TipoPrueba.DEVANADOS)),
    INYECCION_SECUNDARIA("Equipo de inyección secundaria", setOf(TipoPrueba.OPERACION_RELE));

    companion object {
        fun desdeCodigo(codigo: String): CategoriaInstrumento =
            entries.firstOrNull { it.name == codigo } ?: MEGOHMETRO
    }
}

enum class Resultado(val etiqueta: String, val gravedad: Int) {
    APROBADO("Aprobado", 0),
    OBSERVADO("Observado", 1),
    RECHAZADO("Rechazado", 2);

    companion object {
        fun desdeCodigo(codigo: String?): Resultado? = entries.firstOrNull { it.name == codigo }
    }
}

enum class EstadoProtocolo(val etiqueta: String) {
    BORRADOR("Borrador"),
    REGISTRADO_TERRENO("Registrado en terreno"),
    OBSERVADO("Observado"),
    FORMALIZADO("Formalizado"),
    ANULADO("Anulado");

    companion object {
        fun desdeCodigo(codigo: String): EstadoProtocolo = entries.firstOrNull { it.name == codigo } ?: BORRADOR
    }
}

enum class Momento(val etiqueta: String) {
    UNICA("Única"),
    PREVIA("Previa al mantenimiento"),
    POSTERIOR("Posterior al mantenimiento");

    companion object {
        fun desdeCodigo(codigo: String): Momento = entries.firstOrNull { it.name == codigo } ?: UNICA
    }
}

enum class TipoVisita(val etiqueta: String) {
    SOLO_PRUEBAS("Solo pruebas"),
    PRUEBAS_Y_MANT("Pruebas y mantenimiento");

    companion object {
        fun desdeCodigo(codigo: String): TipoVisita = entries.firstOrNull { it.name == codigo } ?: SOLO_PRUEBAS
    }
}
