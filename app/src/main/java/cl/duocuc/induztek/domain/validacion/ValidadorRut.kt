package cl.duocuc.induztek.domain.validacion

object ValidadorRut {
    fun limpiar(rut: String): String = rut.uppercase().filter { it.isDigit() || it == 'K' }

    fun calcularDv(cuerpo: String): Char {
        var suma = 0
        var multiplicador = 2
        for (digito in cuerpo.reversed()) {
            suma += (digito - '0') * multiplicador
            multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
        }
        return when (val resto = 11 - (suma % 11)) {
            11 -> '0'
            10 -> 'K'
            else -> ('0' + resto)
        }
    }

    fun esValido(rut: String): Boolean {
        val limpio = limpiar(rut)
        if (limpio.length < 8 || limpio.length > 9) return false
        val cuerpo = limpio.dropLast(1)
        if (!cuerpo.all { it.isDigit() }) return false
        return calcularDv(cuerpo) == limpio.last()
    }

    fun formatear(rut: String): String {
        val limpio = limpiar(rut)
        if (limpio.length < 2) return rut.trim()
        val cuerpo = limpio.dropLast(1)
        val dv = limpio.last()
        val conPuntos = cuerpo.reversed().chunked(3).joinToString(".").reversed()
        return "$conPuntos-$dv"
    }
}
