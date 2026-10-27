package cl.duocuc.induztek.domain.reglas

import cl.duocuc.induztek.domain.model.Momento
import cl.duocuc.induztek.domain.model.Parametro
import cl.duocuc.induztek.domain.model.Prueba
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.util.Formato
import kotlin.math.abs

data class EvaluacionPrueba(
    val resultado: Resultado?,
    val calculo: String?,
    val motivo: String
)

object EvaluadorPruebas {
    const val LECTURA_MINIMA_MOHM = 100.0

    fun indicePolarizacion(lectura1Min: Double, lectura10Min: Double): Double? =
        if (lectura1Min > 0) lectura10Min / lectura1Min else null

    fun errorTtr(teorica: Double, medida: Double): Double? =
        if (teorica > 0) abs(medida - teorica) / teorica * 100.0 else null

    fun porcentajeContactos(valor: Double, maximoPlaca: Double): Double? =
        if (maximoPlaca > 0) valor / maximoPlaca * 100.0 else null

    fun diferenciaTiempo(esperado: Double, medido: Double): Double? =
        if (esperado > 0) abs(medido - esperado) / esperado * 100.0 else null

    fun evaluar(
        tipoPrueba: TipoPrueba,
        tipoEquipo: TipoEquipo,
        valores: Map<Parametro, Double>,
        noOpera: Boolean = false,
        maxContactosPlaca: Double? = null,
        resultadoManual: Resultado? = null
    ): EvaluacionPrueba = when (tipoPrueba) {
        TipoPrueba.AISLACION -> evaluarAislacion(tipoEquipo, valores)
        TipoPrueba.TTR -> evaluarTtr(valores)
        TipoPrueba.DEVANADOS -> EvaluacionPrueba(
            resultado = resultadoManual,
            calculo = null,
            motivo = if (resultadoManual == null) "Sin criterio automático: indique el resultado (RN-08c)"
            else "Resultado indicado manualmente (RN-08c)"
        )
        TipoPrueba.CONTACTOS -> evaluarContactos(valores, maxContactosPlaca)
        TipoPrueba.OPERACION_RELE -> evaluarRele(valores, noOpera)
    }

    private fun evaluarAislacion(tipoEquipo: TipoEquipo, v: Map<Parametro, Double>): EvaluacionPrueba {
        val l1 = v[Parametro.LECTURA_1MIN]
            ?: return EvaluacionPrueba(null, null, "Ingrese la lectura a 1 min")

        if (l1 < LECTURA_MINIMA_MOHM) {
            return EvaluacionPrueba(
                Resultado.RECHAZADO, null,
                "Lectura a 1 min menor a ${Formato.numero(LECTURA_MINIMA_MOHM, 0)} MΩ (RN-06)"
            )
        }
        if (!tipoEquipo.aislacionConIndicePolarizacion) {
            return EvaluacionPrueba(
                Resultado.APROBADO, null,
                "Lectura a 1 min ≥ ${Formato.numero(LECTURA_MINIMA_MOHM, 0)} MΩ"
            )
        }
        val l10 = v[Parametro.LECTURA_10MIN]
            ?: return EvaluacionPrueba(null, null, "Ingrese la lectura a 10 min para calcular el IP")
        val ip = indicePolarizacion(l1, l10)
            ?: return EvaluacionPrueba(null, null, "No se puede calcular el IP")
        val calculo = "IP = ${Formato.numero(ip, 2)}"
        return when {
            ip >= 2.0 -> EvaluacionPrueba(Resultado.APROBADO, calculo, "IP ≥ 2,0")
            ip >= 1.0 -> EvaluacionPrueba(Resultado.OBSERVADO, calculo, "1,0 ≤ IP < 2,0")
            else -> EvaluacionPrueba(Resultado.RECHAZADO, calculo, "IP < 1,0")
        }
    }

    private fun evaluarTtr(v: Map<Parametro, Double>): EvaluacionPrueba {
        val teorica = v[Parametro.RELACION_TEORICA]
        val medida = v[Parametro.RELACION_MEDIDA]
        if (teorica == null || medida == null) {
            return EvaluacionPrueba(null, null, "Ingrese relación teórica y medida")
        }
        val error = errorTtr(teorica, medida)
            ?: return EvaluacionPrueba(null, null, "La relación teórica debe ser mayor a 0")
        val calculo = "Error = ${Formato.numero(error, 2)} %"
        return when {
            error <= 0.5 -> EvaluacionPrueba(Resultado.APROBADO, calculo, "Error ≤ 0,5 %")
            error <= 1.0 -> EvaluacionPrueba(Resultado.OBSERVADO, calculo, "0,5 % < error ≤ 1,0 %")
            else -> EvaluacionPrueba(Resultado.RECHAZADO, calculo, "Error > 1,0 %")
        }
    }

    private fun evaluarContactos(v: Map<Parametro, Double>, maxPlaca: Double?): EvaluacionPrueba {
        val valor = v[Parametro.RESISTENCIA_CONTACTO]
            ?: return EvaluacionPrueba(null, null, "Ingrese la resistencia de contacto")
        if (maxPlaca == null || maxPlaca <= 0) {
            return EvaluacionPrueba(null, null, "El equipo no tiene máximo de contactos en su placa")
        }
        val pct = porcentajeContactos(valor, maxPlaca)!!
        val calculo = "${Formato.numero(pct, 1)} % del máx. (${Formato.numero(maxPlaca, 0)} µΩ)"
        return when {
            pct <= 100.0 -> EvaluacionPrueba(Resultado.APROBADO, calculo, "≤ 100 % del máximo de placa")
            pct <= 120.0 -> EvaluacionPrueba(Resultado.OBSERVADO, calculo, "Entre 100 % y 120 % del máximo")
            else -> EvaluacionPrueba(Resultado.RECHAZADO, calculo, "> 120 % del máximo de placa")
        }
    }

    private fun evaluarRele(v: Map<Parametro, Double>, noOpera: Boolean): EvaluacionPrueba {
        if (noOpera) return EvaluacionPrueba(Resultado.RECHAZADO, null, "El relé no operó (RN-08b)")
        val esperado = v[Parametro.TIEMPO_ESPERADO]
        val medido = v[Parametro.TIEMPO_MEDIDO]
        if (esperado == null || medido == null) {
            return EvaluacionPrueba(null, null, "Ingrese tiempo esperado y tiempo medido")
        }
        val dif = diferenciaTiempo(esperado, medido)
            ?: return EvaluacionPrueba(null, null, "El tiempo esperado debe ser mayor a 0")
        val calculo = "Diferencia = ${Formato.numero(dif, 1)} %"
        return when {
            dif <= 5.0 -> EvaluacionPrueba(Resultado.APROBADO, calculo, "Diferencia ≤ 5 %")
            dif <= 10.0 -> EvaluacionPrueba(Resultado.OBSERVADO, calculo, "5 % < diferencia ≤ 10 %")
            else -> EvaluacionPrueba(Resultado.RECHAZADO, calculo, "Diferencia > 10 %")
        }
    }

    fun peor(a: Resultado?, b: Resultado?): Resultado? = when {
        a == null -> b
        b == null -> a
        else -> if (a.gravedad >= b.gravedad) a else b
    }

    fun pruebasEfectivas(pruebas: List<Prueba>): List<Prueba> =
        pruebas.filter { it.realizada }
            .groupBy { it.tipo to it.elemento }
            .map { (_, grupo) -> grupo.firstOrNull { it.momento == Momento.POSTERIOR } ?: grupo.first() }

    fun resultadoGlobal(pruebas: List<Prueba>): Resultado? =
        pruebasEfectivas(pruebas).map { it.resultado }.fold(null as Resultado?) { acc, r -> peor(acc, r) }
}
