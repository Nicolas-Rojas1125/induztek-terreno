package cl.duocuc.induztek.domain.model

import cl.duocuc.induztek.domain.util.Formato

enum class TipoCampoTecnico { NUMERO, ENTERO, TEXTO, OPCION }

data class CampoTecnico(
    val clave: String,
    val etiqueta: String,
    val tipo: TipoCampoTecnico,
    val unidad: String = "",
    val opciones: List<String> = emptyList(),
    val minimo: Double? = null,
    val maximo: Double? = null,
    val obligatorio: Boolean = false,
    val ayuda: String? = null,
    val patron: Regex? = null
) {
    val etiquetaConUnidad: String get() = if (unidad.isBlank()) etiqueta else "$etiqueta ($unidad)"
}

object FichaTecnica {
    private val grupoConexion = CampoTecnico(
        "grupoConexion", "Grupo de conexión", TipoCampoTecnico.OPCION,
        opciones = listOf("Dyn1", "Dyn5", "Dyn11", "YNd1", "YNd11", "Yyn0", "Dd0", "Otro"), obligatorio = true
    )
    private val frecuencia = CampoTecnico("frecuenciaHz", "Frecuencia", TipoCampoTecnico.OPCION, "Hz", listOf("50", "60"), obligatorio = true)
    private val impedancia = CampoTecnico("impedanciaPct", "Impedancia de cortocircuito", TipoCampoTecnico.NUMERO, "%", minimo = 0.5, maximo = 20.0)
    private val refrigeracion = CampoTecnico(
        "refrigeracion", "Refrigeración", TipoCampoTecnico.OPCION,
        opciones = listOf("ONAN", "ONAF", "OFAF", "ODAF", "AN", "AF")
    )
    private val medioAislanteTrafo = CampoTecnico(
        "medioAislante", "Medio aislante", TipoCampoTecnico.OPCION,
        opciones = listOf("Aceite mineral", "Aceite vegetal", "Seco (resina)"), obligatorio = true
    )
    private val volumenAceite = CampoTecnico("volumenAceiteL", "Volumen de aceite", TipoCampoTecnico.NUMERO, "L", minimo = 0.0, maximo = 100_000.0)
    private val masaTotal = CampoTecnico("masaTotalKg", "Masa total", TipoCampoTecnico.NUMERO, "kg", minimo = 1.0, maximo = 500_000.0)
    private val numeroTaps = CampoTecnico("numeroTaps", "Número de posiciones del tap", TipoCampoTecnico.ENTERO, minimo = 1.0, maximo = 33.0)
    private val tapNominal = CampoTecnico("tapNominal", "Posición nominal del tap", TipoCampoTecnico.ENTERO, minimo = 1.0, maximo = 33.0)

    private val poderCorte = CampoTecnico("poderCorteKa", "Poder de corte", TipoCampoTecnico.NUMERO, "kA", minimo = 1.0, maximo = 100.0, obligatorio = true)
    private val medioExtincion = CampoTecnico(
        "medioExtincion", "Medio de extinción", TipoCampoTecnico.OPCION,
        opciones = listOf("SF6", "Vacío", "Aceite", "Aire"), obligatorio = true
    )
    private val mecanismo = CampoTecnico(
        "mecanismo", "Mecanismo de operación", TipoCampoTecnico.OPCION,
        opciones = listOf("Resorte", "Hidráulico", "Neumático", "Magnético")
    )
    private val tensionControl = CampoTecnico("tensionControlV", "Tensión de control", TipoCampoTecnico.NUMERO, "Vcc", minimo = 12.0, maximo = 250.0)
    private val numeroPolos = CampoTecnico("numeroPolos", "Número de polos", TipoCampoTecnico.ENTERO, minimo = 1.0, maximo = 4.0)

    private val medioAislanteCelda = CampoTecnico(
        "medioAislante", "Medio aislante", TipoCampoTecnico.OPCION,
        opciones = listOf("Aire", "SF6", "Sólido"), obligatorio = true
    )
    private val tipoCelda = CampoTecnico(
        "tipoCelda", "Función de la celda", TipoCampoTecnico.OPCION,
        opciones = listOf("Llegada", "Salida", "Medida", "Acoplamiento", "Protección de transformador")
    )
    private val corrienteBarras = CampoTecnico("corrienteBarrasA", "Corriente de barras", TipoCampoTecnico.NUMERO, "A", minimo = 100.0, maximo = 6_300.0)

    private val seccion = CampoTecnico("seccionMm2", "Sección del conductor", TipoCampoTecnico.NUMERO, "mm²", minimo = 1.5, maximo = 1_000.0, obligatorio = true)
    private val material = CampoTecnico("materialConductor", "Material del conductor", TipoCampoTecnico.OPCION, opciones = listOf("Cobre", "Aluminio"), obligatorio = true)
    private val aislacionCable = CampoTecnico(
        "aislacion", "Tipo de aislación", TipoCampoTecnico.OPCION,
        opciones = listOf("XLPE", "EPR", "PVC", "Papel impregnado"), obligatorio = true
    )
    private val longitud = CampoTecnico("longitudM", "Longitud", TipoCampoTecnico.NUMERO, "m", minimo = 1.0, maximo = 50_000.0)
    private val conductores = CampoTecnico("numeroConductores", "Número de conductores", TipoCampoTecnico.ENTERO, minimo = 1.0, maximo = 4.0)
    private val instalacionCable = CampoTecnico(
        "tipoInstalacion", "Tipo de instalación", TipoCampoTecnico.OPCION,
        opciones = listOf("Ducto", "Bandeja", "Enterrado directo", "Aéreo")
    )

    private val funcionesAnsi = CampoTecnico(
        "funcionesAnsi", "Funciones ANSI", TipoCampoTecnico.TEXTO, obligatorio = true,
        ayuda = "Ej.: 50/51, 50N/51N", patron = Regex("^[0-9A-Za-z/ ,.\\-]{2,40}$")
    )
    private val corrienteSecundaria = CampoTecnico(
        "corrienteNominalSecundariaA", "Corriente nominal secundaria (In)", TipoCampoTecnico.OPCION, "A",
        listOf("1", "5"), obligatorio = true
    )
    private val relacionTc = CampoTecnico(
        "relacionTc", "Relación del TC", TipoCampoTecnico.TEXTO,
        ayuda = "Ej.: 400/5", patron = Regex("^\\d{1,5}/\\d{1,2}$")
    )
    private val curva = CampoTecnico(
        "curva", "Curva de operación", TipoCampoTecnico.OPCION,
        opciones = listOf("IEC normal inversa", "IEC muy inversa", "IEC extremadamente inversa", "IEEE moderadamente inversa", "Tiempo definido")
    )
    private val tensionAuxiliar = CampoTecnico("tensionAuxiliarV", "Tensión auxiliar", TipoCampoTecnico.NUMERO, "Vcc", minimo = 24.0, maximo = 250.0)
    private val firmware = CampoTecnico("firmware", "Versión de firmware", TipoCampoTecnico.TEXTO, patron = Regex("^[0-9A-Za-z.\\-_ ]{1,20}$"))

    fun campos(tipo: TipoEquipo): List<CampoTecnico> = when (tipo) {
        TipoEquipo.TRANSFORMADOR -> listOf(
            grupoConexion, frecuencia, impedancia, refrigeracion, medioAislanteTrafo,
            volumenAceite, masaTotal, numeroTaps, tapNominal
        )
        TipoEquipo.INTERRUPTOR -> listOf(poderCorte, medioExtincion, mecanismo, tensionControl, numeroPolos)
        TipoEquipo.CELDA_MT -> listOf(poderCorte, medioAislanteCelda, tipoCelda, corrienteBarras)
        TipoEquipo.CABLE -> listOf(seccion, material, aislacionCable, longitud, conductores, instalacionCable)
        TipoEquipo.RELE -> listOf(funcionesAnsi, corrienteSecundaria, relacionTc, curva, tensionAuxiliar, firmware)
    }

    fun campo(tipo: TipoEquipo, clave: String): CampoTecnico? = campos(tipo).firstOrNull { it.clave == clave }

    fun validar(campo: CampoTecnico, valor: String): String? {
        val texto = valor.trim()
        if (texto.isEmpty()) return if (campo.obligatorio) "${campo.etiqueta} es obligatorio" else null
        return when (campo.tipo) {
            TipoCampoTecnico.OPCION ->
                if (texto in campo.opciones) null else "Seleccione una opción válida"
            TipoCampoTecnico.TEXTO -> when {
                texto.length > 40 -> "Máximo 40 caracteres"
                campo.patron != null && !campo.patron.matches(texto) ->
                    "Formato inválido" + (campo.ayuda?.let { " ($it)" } ?: "")
                else -> null
            }
            TipoCampoTecnico.NUMERO, TipoCampoTecnico.ENTERO -> {
                val n = numero(texto) ?: return "Debe ser un número"
                when {
                    campo.tipo == TipoCampoTecnico.ENTERO && n % 1.0 != 0.0 -> "Debe ser un número entero"
                    campo.minimo != null && n < campo.minimo -> "Mínimo ${Formato.numeroCompacto(campo.minimo)} ${campo.unidad}".trimEnd()
                    campo.maximo != null && n > campo.maximo -> "Máximo ${Formato.numeroCompacto(campo.maximo)} ${campo.unidad}".trimEnd()
                    else -> null
                }
            }
        }
    }

    fun validarTodo(tipo: TipoEquipo, datos: Map<String, String>): Map<String, String> =
        campos(tipo).mapNotNull { c -> validar(c, datos[c.clave].orEmpty())?.let { c.clave to it } }.toMap()

    fun normalizar(tipo: TipoEquipo, datos: Map<String, String>): Map<String, String> =
        campos(tipo).mapNotNull { c ->
            val v = datos[c.clave]?.trim().orEmpty()
            if (v.isEmpty()) null
            else c.clave to when (c.tipo) {
                TipoCampoTecnico.NUMERO, TipoCampoTecnico.ENTERO -> numero(v)?.let { Formato.numeroEditable(it).replace(',', '.') } ?: v
                else -> v
            }
        }.toMap()

    fun textoVisible(campo: CampoTecnico, valor: String): String = when (campo.tipo) {
        TipoCampoTecnico.NUMERO, TipoCampoTecnico.ENTERO ->
            (numero(valor)?.let { Formato.numeroCompacto(it) } ?: valor) + if (campo.unidad.isBlank()) "" else " ${campo.unidad}"
        else -> valor
    }

    fun completitud(tipo: TipoEquipo, datos: Map<String, String>): Int {
        val total = campos(tipo).size
        if (total == 0) return 100
        val llenos = campos(tipo).count { !datos[it.clave].isNullOrBlank() }
        return llenos * 100 / total
    }

    private fun numero(texto: String): Double? {
        val limpio = texto.trim().replace(" ", "")
        if (limpio.count { it == ',' } + limpio.count { it == '.' } > 1) return null
        return limpio.replace(',', '.').toDoubleOrNull()
    }
}
