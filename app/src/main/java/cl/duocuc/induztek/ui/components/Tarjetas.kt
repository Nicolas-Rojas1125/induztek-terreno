package cl.duocuc.induztek.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duocuc.induztek.R
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.InduztekTheme

@Composable
fun TarjetaSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    accion: (@Composable RowScope.() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevacionTarjeta)
    ) {
        Column(Modifier.padding(Dimens.espacioMedio), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icono != null) {
                    Icon(
                        painterResource(icono), contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimens.tamanoIcono)
                    )
                    Spacer(Modifier.width(Dimens.espacioChico))
                }
                Text(
                    titulo, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
                )
                if (accion != null) accion.invoke(this)
            }
            contenido()
        }
    }
}

@Composable
fun FilaDato(etiqueta: String, valor: String, modifier: Modifier = Modifier, @DrawableRes icono: Int? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        if (icono != null) {
            Icon(
                painterResource(icono), contentDescription = null, modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(Dimens.espacioChico))
        }
        Text(
            etiqueta, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f)
        )
        Text(valor.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.58f))
    }
}

@Composable
fun TarjetaIndicador(
    valor: Int,
    etiqueta: String,
    @DrawableRes icono: Int,
    modifier: Modifier = Modifier,
    colorAcento: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val animado by animateIntAsState(valor, tween(700), label = "contadorIndicador")
    val contenido: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(14.dp)) {
            Surface(shape = CircleShape, color = colorAcento.copy(alpha = 0.14f), contentColor = colorAcento) {
                Box(Modifier.padding(8.dp)) {
                    Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.size(10.dp))
            Text("$animado", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colorAcento)
            Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
    val colores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, colors = colores, content = contenido)
    } else {
        Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = colores, content = contenido)
    }
}

@Composable
fun TarjetaLista(
    titulo: String,
    subtitulo: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    detalle: String? = null,
    colorIcono: Color = MaterialTheme.colorScheme.primary,
    pie: (@Composable RowScope.() -> Unit)? = null,
    derecha: (@Composable () -> Unit)? = null
) {
    val cuerpo: @Composable ColumnScope.() -> Unit = {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icono != null) {
                Surface(shape = CircleShape, color = colorIcono.copy(alpha = 0.12f), contentColor = colorIcono) {
                    Box(Modifier.padding(10.dp)) {
                        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitulo, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                if (detalle != null) {
                    Text(detalle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (pie != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                        content = pie
                    )
                }
            }
            if (derecha != null) {
                Spacer(Modifier.width(8.dp))
                derecha()
            } else if (onClick != null) {
                Icon(
                    painterResource(R.drawable.ic_flecha_derecha), contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
    val colores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colores, content = cuerpo)
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colores, content = cuerpo)
    }
}

@Preview(showBackground = true)
@Composable
private fun TarjetasPreview() {
    InduztekTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TarjetaIndicador(3, "Pendientes de formalizar", R.drawable.ic_documento, Modifier.weight(1f))
                TarjetaIndicador(2, "Atrasados (> 2 días hábiles)", R.drawable.ic_advertencia, Modifier.weight(1f))
            }
            TarjetaLista("TR-01 · Transformador", "Planta Lampa · Envasadora del Pacífico", {}, icono = R.drawable.ic_rayo)
            TarjetaSeccion("Datos de placa", icono = R.drawable.ic_etiqueta) {
                FilaDato("Marca", "Rhona"); FilaDato("Potencia", "1.500 kVA")
            }
        }
    }
}
