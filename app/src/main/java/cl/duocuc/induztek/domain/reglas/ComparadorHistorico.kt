package cl.duocuc.induztek.domain.reglas

import cl.duocuc.induztek.domain.model.Parametro
import kotlin.math.abs

data class Comparacion(
    val parametro: Parametro,
    val actual: Double,
    val anterior: Double,
    val variacionPct: Double,
    val significativa: Boolean,
    val numeroProtocoloAnterior: String = ""
)

object ComparadorHistorico {
    const val UMBRAL_PORCENTAJE = 25.0

    fun variacion(actual: Double, anterior: Double): Double? =
        if (anterior != 0.0) (actual - anterior) / anterior * 100.0 else null

    fun esSignificativa(variacionPct: Double): Boolean = abs(variacionPct) > UMBRAL_PORCENTAJE

    fun comparar(
        principales: List<Parametro>,
        actuales: Map<Parametro, Double>,
        anteriores: Map<Parametro, Double>,
        numeroProtocoloAnterior: String = ""
    ): List<Comparacion> = principales.mapNotNull { p ->
        val actual = actuales[p] ?: return@mapNotNull null
        val anterior = anteriores[p] ?: return@mapNotNull null
        val v = variacion(actual, anterior) ?: return@mapNotNull null
        Comparacion(p, actual, anterior, v, esSignificativa(v), numeroProtocoloAnterior)
    }
}
