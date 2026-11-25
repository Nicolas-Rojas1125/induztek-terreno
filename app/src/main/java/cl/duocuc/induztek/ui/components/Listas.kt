package cl.duocuc.induztek.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.Pagina

@Composable
fun BarraBusqueda(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    capitalizacion: KeyboardCapitalization = KeyboardCapitalization.None
) {
    CampoTexto(
        valor = valor,
        onValorCambia = onCambio,
        etiqueta = etiqueta,
        icono = R.drawable.ic_buscar,
        modifier = modifier,
        capitalizacion = capitalizacion,
        accionTeclado = ImeAction.Search,
        iconoFinal = {
            if (valor.isNotEmpty()) {
                IconButton(onClick = { onCambio("") }) {
                    Icon(painterResource(R.drawable.ic_cerrar), contentDescription = "Limpiar búsqueda", modifier = Modifier.size(20.dp))
                }
            }
        }
    )
}

@Composable
fun <T> FiltrosChips(
    opciones: List<T>,
    seleccion: T?,
    texto: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(opciones) { op ->
            val elegido = op == seleccion
            FilterChip(
                selected = elegido,
                onClick = { onSeleccion(op) },
                label = { Text(texto(op)) },
                leadingIcon = if (elegido) {
                    { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

@Composable
fun ControlPaginacion(pagina: Pagina<*>, onIr: (Int) -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = pagina.totalElementos > 0, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalIconButton(onClick = { onIr(pagina.pagina - 1) }, enabled = pagina.hayAnterior) {
                Icon(painterResource(R.drawable.ic_flecha_izquierda), contentDescription = "Página anterior")
            }
            Text(
                pagina.textoPosicion,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(onClick = { onIr(pagina.pagina + 1) }, enabled = pagina.haySiguiente) {
                Icon(painterResource(R.drawable.ic_flecha_derecha), contentDescription = "Página siguiente")
            }
        }
    }
}

@Composable
fun DialogoConfirmacion(
    titulo: String,
    texto: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    peligroso: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = {
            Icon(
                painterResource(if (peligroso) R.drawable.ic_advertencia else R.drawable.ic_info),
                contentDescription = null,
                tint = if (peligroso) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(textoConfirmar, color = if (peligroso) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 6.dp)
    )
}
