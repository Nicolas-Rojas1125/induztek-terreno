package cl.duocuc.induztek.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.ui.theme.AmbarObservado
import cl.duocuc.induztek.ui.theme.AmbarObservadoFondo
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.GrisNeutro
import cl.duocuc.induztek.ui.theme.GrisNeutroFondo
import cl.duocuc.induztek.ui.theme.InduztekTheme
import cl.duocuc.induztek.ui.theme.MoradoFormalizado
import cl.duocuc.induztek.ui.theme.MoradoFormalizadoFondo
import cl.duocuc.induztek.ui.theme.RojoRechazado
import cl.duocuc.induztek.ui.theme.RojoRechazadoFondo
import cl.duocuc.induztek.ui.theme.VerdeAprobado
import cl.duocuc.induztek.ui.theme.VerdeAprobadoFondo

data class ColoresChip(val texto: Color, val fondo: Color, @DrawableRes val icono: Int)

fun coloresResultado(r: Resultado?): ColoresChip = when (r) {
    Resultado.APROBADO -> ColoresChip(VerdeAprobado, VerdeAprobadoFondo, R.drawable.ic_check)
    Resultado.OBSERVADO -> ColoresChip(AmbarObservado, AmbarObservadoFondo, R.drawable.ic_advertencia)
    Resultado.RECHAZADO -> ColoresChip(RojoRechazado, RojoRechazadoFondo, R.drawable.ic_error)
    null -> ColoresChip(GrisNeutro, GrisNeutroFondo, R.drawable.ic_info)
}

fun coloresEstado(e: EstadoProtocolo): ColoresChip = when (e) {
    EstadoProtocolo.BORRADOR -> ColoresChip(GrisNeutro, GrisNeutroFondo, R.drawable.ic_editar)
    EstadoProtocolo.REGISTRADO_TERRENO -> ColoresChip(Color(0xFF0D3B66), Color(0xFFD3E3F7), R.drawable.ic_documento)
    EstadoProtocolo.OBSERVADO -> ColoresChip(AmbarObservado, AmbarObservadoFondo, R.drawable.ic_advertencia)
    EstadoProtocolo.FORMALIZADO -> ColoresChip(MoradoFormalizado, MoradoFormalizadoFondo, R.drawable.ic_verificado)
    EstadoProtocolo.ANULADO -> ColoresChip(RojoRechazado, RojoRechazadoFondo, R.drawable.ic_bloqueo)
}

@Composable
fun Chip(texto: String, colores: ColoresChip, modifier: Modifier = Modifier, grande: Boolean = false) {
    val fondo by animateColorAsState(colores.fondo, tween(350), label = "fondoChip")
    val color by animateColorAsState(colores.texto, tween(350), label = "textoChip")
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = fondo, contentColor = color) {
        Row(
            modifier = Modifier.padding(horizontal = if (grande) 14.dp else 10.dp, vertical = if (grande) 8.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(colores.icono), contentDescription = null, modifier = Modifier.size(if (grande) 20.dp else 16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                texto,
                style = if (grande) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ChipResultado(resultado: Resultado?, modifier: Modifier = Modifier, grande: Boolean = false, textoNulo: String = "Sin resultado") {
    Chip(resultado?.etiqueta ?: textoNulo, coloresResultado(resultado), modifier, grande)
}

@Composable
fun ChipEstado(estado: EstadoProtocolo, modifier: Modifier = Modifier, etiquetaCliente: String? = null) {
    Chip(etiquetaCliente ?: estado.etiqueta, coloresEstado(estado), modifier)
}

@Composable
fun BannerSinConexion(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.espacioMedio, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_sin_senal), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Dimens.espacioChico))
                Text(
                    "Sin conexión · todo se guarda en el teléfono y no se pierde",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun TarjetaErrores(titulo: String?, mensajes: List<String>, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = titulo != null || mensajes.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(Dimens.espacioMedio), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_error), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(Dimens.espacioChico))
                    Text(titulo ?: "Revise lo siguiente", style = MaterialTheme.typography.titleSmall)
                }
                mensajes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
fun AvisoInformativo(texto: String, modifier: Modifier = Modifier, advertencia: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (advertencia) AmbarObservadoFondo else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (advertencia) AmbarObservado else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(if (advertencia) R.drawable.ic_advertencia else R.drawable.ic_info),
                contentDescription = null, modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(Dimens.espacioChico))
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EstadoVacio(@DrawableRes icono: Int, titulo: String, mensaje: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = Dimens.espacioGrande),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painterResource(icono), contentDescription = null, modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(Dimens.espacioMedio))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Dimens.espacioMini))
        Text(
            mensaje, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Preview(showBackground = true)
@Composable
private fun RetroalimentacionPreview() {
    InduztekTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipResultado(Resultado.APROBADO); ChipResultado(Resultado.OBSERVADO); ChipResultado(Resultado.RECHAZADO)
            }
            ChipEstado(EstadoProtocolo.REGISTRADO_TERRENO, etiquetaCliente = "Pendiente de formalización")
            BannerSinConexion(true)
            TarjetaErrores("No se puede cerrar la visita", listOf("PT-2026-0403 · TR-SS-01 · Borrador"))
        }
    }
}
