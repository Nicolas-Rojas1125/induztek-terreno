package cl.duocuc.induztek.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.induztek.data.repository.PuntoSerie
import cl.duocuc.induztek.domain.util.Formato

@Composable
fun GraficoTendencia(puntos: List<PuntoSerie>, modifier: Modifier = Modifier) {
    if (puntos.isEmpty()) return
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(puntos) {
        progreso.snapTo(0f)
        progreso.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val medidor = rememberTextMeasurer()
    val colorLinea = MaterialTheme.colorScheme.primary
    val colorGuia = MaterialTheme.colorScheme.outline
    val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val estiloEtiqueta = TextStyle(fontSize = 10.sp, color = colorTexto)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val margenIzq = 8.dp.toPx()
        val margenDer = 8.dp.toPx()
        val margenSup = 18.dp.toPx()
        val margenInf = 28.dp.toPx()
        val ancho = size.width - margenIzq - margenDer
        val alto = size.height - margenSup - margenInf

        val valores = puntos.map { it.valor }
        val min = valores.min()
        val max = valores.max()
        val rango = if (max - min < 1e-9) (if (max == 0.0) 1.0 else max * 0.2) else (max - min)
        val minEje = min - rango * 0.15
        val maxEje = max + rango * 0.15

        fun x(i: Int): Float =
            if (puntos.size == 1) margenIzq + ancho / 2 else margenIzq + ancho * i / (puntos.size - 1)

        fun y(v: Double): Float = (margenSup + alto * (1 - (v - minEje) / (maxEje - minEje))).toFloat()

        listOf(0f, 0.5f, 1f).forEach { f ->
            val yy = margenSup + alto * f
            drawLine(
                colorGuia.copy(alpha = 0.5f), Offset(margenIzq, yy), Offset(size.width - margenDer, yy),
                strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }

        if (puntos.size > 1) {
            val camino = Path()
            val limite = progreso.value * (puntos.size - 1)
            puntos.forEachIndexed { i, p ->
                if (i == 0) camino.moveTo(x(0), y(p.valor))
                else if (i <= limite) camino.lineTo(x(i), y(p.valor))
                else if (i - 1 < limite) {
                    val f = limite - (i - 1)
                    val x0 = x(i - 1); val y0 = y(puntos[i - 1].valor)
                    camino.lineTo(x0 + (x(i) - x0) * f, y0 + (y(p.valor) - y0) * f)
                }
            }
            drawPath(camino, colorLinea, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }

        puntos.forEachIndexed { i, p ->
            val visible = puntos.size == 1 || i <= progreso.value * (puntos.size - 1) + 0.01f
            if (!visible) return@forEachIndexed
            val centro = Offset(x(i), y(p.valor))
            drawCircle(coloresResultado(p.resultado).texto, radius = 6.dp.toPx(), center = centro)
            drawCircle(androidx.compose.ui.graphics.Color.White, radius = 2.5.dp.toPx(), center = centro)

            val valorTxt = medidor.measure(Formato.numeroCompacto(p.valor), estiloEtiqueta)
            drawText(
                valorTxt,
                topLeft = Offset(
                    (centro.x - valorTxt.size.width / 2).coerceAtMost(size.width - valorTxt.size.width).coerceAtLeast(0f),
                    (centro.y - valorTxt.size.height - 6.dp.toPx()).coerceAtLeast(0f)
                )
            )
            val fechaTxt = medidor.measure(Formato.fechaDesdeMillis(p.fechaHora).takeLast(7), estiloEtiqueta)
            drawText(
                fechaTxt,
                topLeft = Offset(
                    (centro.x - fechaTxt.size.width / 2).coerceAtMost(size.width - fechaTxt.size.width).coerceAtLeast(0f),
                    size.height - fechaTxt.size.height
                )
            )
        }
    }
}
