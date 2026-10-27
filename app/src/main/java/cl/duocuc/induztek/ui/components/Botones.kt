package cl.duocuc.induztek.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duocuc.induztek.R
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.InduztekTheme

@Composable
private fun escalaPresion(interaccion: MutableInteractionSource): Float {
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (presionado) 0.96f else 1f, spring(), label = "escalaBoton")
    return escala
}

@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    colorFondo: Color = MaterialTheme.colorScheme.primary,
    colorTexto: Color = MaterialTheme.colorScheme.onPrimary
) {
    val interaccion = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        modifier = modifier
            .height(Dimens.alturaBoton)
            .scale(escalaPresion(interaccion)),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = colorFondo, contentColor = colorTexto),
        interactionSource = interaccion
    ) {
        AnimatedContent(
            targetState = cargando,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "contenidoBoton"
        ) { estaCargando ->
            if (estaCargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = colorTexto
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icono != null) {
                        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    }
                    Text(texto, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    habilitado: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val interaccion = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier
            .height(Dimens.alturaBoton)
            .scale(escalaPresion(interaccion)),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.5.dp, if (habilitado) color else MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        interactionSource = interaccion
    ) {
        if (icono != null) {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun BotonesPreview() {
    InduztekTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonPrincipal("Guardar equipo", {}, Modifier.fillMaxWidth(), icono = R.drawable.ic_check)
            BotonPrincipal("Guardando", {}, Modifier.fillMaxWidth(), cargando = true)
            BotonSecundario("Cancelar", {}, Modifier.fillMaxWidth())
        }
    }
}
