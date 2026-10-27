package cl.duocuc.induztek.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun rememberSacudida(disparador: Int): Float {
    val desplazamiento = remember { Animatable(0f) }
    LaunchedEffect(disparador) {
        if (disparador > 0) {
            for (x in listOf(-22f, 22f, -16f, 16f, -8f, 8f, 0f)) {
                desplazamiento.animateTo(x, tween(durationMillis = 45))
            }
        }
    }
    return desplazamiento.value
}

fun Modifier.sacudir(desplazamiento: Float): Modifier = this.graphicsLayer { translationX = desplazamiento }

@Composable
fun AparicionEscalonada(indice: Int, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val estado = remember { MutableTransitionState(false).apply { targetState = true } }
    val retardo = (indice.coerceAtMost(8)) * 45
    AnimatedVisibility(
        visibleState = estado,
        modifier = modifier,
        enter = fadeIn(tween(320, delayMillis = retardo)) +
            slideInVertically(tween(320, delayMillis = retardo)) { alto -> alto / 3 }
    ) {
        contenido()
    }
}

@Composable
fun rememberPulso(activo: Boolean): Float {
    if (!activo) return 1f
    val transicion = rememberInfiniteTransition(label = "pulso")
    val escala by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "escalaPulso"
    )
    return escala
}
