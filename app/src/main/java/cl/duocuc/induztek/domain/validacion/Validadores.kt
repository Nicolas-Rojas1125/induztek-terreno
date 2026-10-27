package cl.duocuc.induztek.domain.validacion

import cl.duocuc.induztek.domain.model.Parametro
import cl.duocuc.induztek.domain.util.Formato
import java.time.LocalDate

object Validadores {
    private val regexCorreo = Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")
    private val regexOt = Regex("""^OT-\d{4}-\d{4}$""")
    private val regexTag = Regex("""^[A-Z0-9][A-Z0-9/.\-]{0,29}$""")
    private val regexSerie = Regex("""^[A-Za-z0-9][A-Za-z0-9.\-/ ]{1,39}$""")

    fun obligatorio(valor: String, campo: String): String? =
        if (valor.isBlank()) "$campo es obligatorio" else null

    fun texto(valor: String, campo: String, min: Int = 2, max: Int = 120): String? = when {
        valor.isBlank() -> "$campo es obligatorio"
        valor.trim().length < min -> "$campo debe tener al menos $min caracteres"
        valor.trim().length > max -> "$campo admite máximo $max caracteres"
        else -> null
    }

    fun textoOpcional(valor: String, campo: String, max: Int = 500): String? =
        if (valor.trim().length > max) "$campo admite máximo $max caracteres" else null

    fun nombrePersona(valor: String): String? {
        texto(valor, "El nombre", 3, 120)?.let { return it }
        if (valor.any { it.isDigit() }) return "El nombre no puede contener números"
        if (valor.trim().split(" ").filter { it.isNotBlank() }.size < 2) return "Ingrese nombre y apellido"
        return null
    }

    fun rut(valor: String): String? = when {
        valor.isBlank() -> "El RUT es obligatorio"
        !ValidadorRut.esValido(valor) -> "RUT inválido: revise el dígito verificador (RN-03)"
        else -> null
    }

    fun correo(valor: String): String? = when {
        valor.isBlank() -> "El correo es obligatorio"
        !regexCorreo.matches(valor.trim()) -> "Ingrese un correo válido (ej.: nombre@empresa.cl)"
        else -> null
    }

    fun telefono(valor: String, obligatorio: Boolean = true): String? {
        if (valor.isBlank()) return if (obligatorio) "El teléfono es obligatorio" else null
        val digitos = valor.filter { it.isDigit() }
        val nacional = if (digitos.startsWith("56") && digitos.length == 11) digitos.drop(2) else digitos
        return if (nacional.length == 9) null else "Teléfono inválido (ej.: +56 9 1234 5678)"
    }

    fun contrasena(valor: String): String? = when {
        valor.isBlank() -> "La contraseña es obligatoria"
        valor.length < 8 -> "Mínimo 8 caracteres"
        !valor.any { it.isLetter() } || !valor.any { it.isDigit() } -> "Debe combinar letras y números"
        else -> null
    }

    fun contrasenaLogin(valor: String): String? =
        if (valor.isBlank()) "Ingrese su contraseña" else null

    fun numeroOt(valor: String): String? = when {
        valor.isBlank() -> "El número de OT es obligatorio"
        !regexOt.matches(valor.trim().uppercase()) -> "Formato esperado: OT-2026-1187"
        else -> null
    }

    fun tag(valor: String): String? = when {
        valor.isBlank() -> "El TAG es obligatorio"
        !regexTag.matches(valor.trim().uppercase()) -> "Use letras, números y - / . (ej.: TR-01)"
        else -> null
    }

    fun numeroSerie(valor: String): String? = when {
        valor.isBlank() -> "El número de serie es obligatorio"
        !regexSerie.matches(valor.trim()) -> "Número de serie inválido"
        else -> null
    }

    fun anioFabricacion(valor: String, anioActual: Int = Formato.hoy().year): String? {
        if (valor.isBlank()) return "El año es obligatorio"
        val anio = valor.trim().toIntOrNull() ?: return "Ingrese un año numérico"
        return if (anio < 1950 || anio > anioActual) "El año debe estar entre 1950 y $anioActual" else null
    }

    fun aDecimal(texto: String): Double? {
        val limpio = texto.trim().replace(" ", "")
        if (limpio.isEmpty()) return null
        if (limpio.count { it == ',' } + limpio.count { it == '.' } > 1) return null
        return limpio.replace(',', '.').toDoubleOrNull()
    }

    fun decimalEnRango(
        texto: String, campo: String, min: Double, max: Double, unidad: String = "", obligatorio: Boolean = true
    ): String? {
        if (texto.isBlank()) return if (obligatorio) "$campo es obligatorio" else null
        val n = aDecimal(texto) ?: return "$campo debe ser un número"
        val u = if (unidad.isBlank()) "" else " $unidad"
        return when {
            n < 0 -> "$campo no puede ser negativo"
            n < min || n > max ->
                "$campo debe estar entre ${Formato.numeroCompacto(min)} y ${Formato.numeroCompacto(max)}$u"
            else -> null
        }
    }

    fun medicion(texto: String, parametro: Parametro, factor: Double = 1.0): String? {
        if (texto.isBlank()) return "Ingrese ${parametro.etiqueta.lowercase()}"
        val n = aDecimal(texto) ?: return "Debe ser un número (use coma decimal)"
        if (n < 0) return "Valor negativo: no es posible (HU-03)"
        if (parametro.entero && n % 1.0 != 0.0) return "Debe ser un número entero"
        val base = n * factor
        return if (base < parametro.minimo || base > parametro.maximo) {
            "Fuera de rango: ${Formato.numeroCompacto(parametro.minimo)} a " +
                "${Formato.numeroCompacto(parametro.maximo)} ${parametro.unidad.simbolo}".trimEnd()
        } else null
    }

    fun temperatura(texto: String): String? =
        decimalEnRango(texto, "La temperatura", -30.0, 60.0, "°C", obligatorio = false)

    fun humedad(texto: String): String? =
        decimalEnRango(texto, "La humedad", 0.0, 100.0, "%", obligatorio = false)

    fun fechaVencimiento(calibracion: LocalDate?, vencimiento: LocalDate?): String? = when {
        vencimiento == null -> "Seleccione la fecha de vencimiento"
        calibracion != null && !vencimiento.isAfter(calibracion) ->
            "El vencimiento debe ser posterior a la calibración"
        else -> null
    }

    fun fechaNoFutura(fecha: LocalDate?, campo: String, hoy: LocalDate = Formato.hoy()): String? = when {
        fecha == null -> "Seleccione $campo"
        fecha.isAfter(hoy) -> "$campo no puede ser futura"
        else -> null
    }

    fun seleccion(valor: Any?, campo: String): String? = if (valor == null) "Seleccione $campo" else null

    fun motivo(valor: String, minimo: Int = 10): String? = when {
        valor.isBlank() -> "El motivo es obligatorio"
        valor.trim().length < minimo -> "Describa el motivo (mínimo $minimo caracteres)"
        else -> null
    }
}
