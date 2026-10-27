package cl.duocuc.induztek.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.duocuc.induztek.R

val LocalConectado = compositionLocalOf { true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBase(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    onVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
    barraInferior: @Composable () -> Unit = {},
    botonFlotante: @Composable () -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    contenido: @Composable (PaddingValues) -> Unit
) {
    val conectado = LocalConectado.current
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (subtitulo != null) {
                                Text(
                                    subtitulo, style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (onVolver != null) {
                            IconButton(onClick = onVolver) {
                                Icon(painterResource(R.drawable.ic_volver), contentDescription = "Volver")
                            }
                        }
                    },
                    actions = acciones,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                BannerSinConexion(visible = !conectado)
            }
        },
        bottomBar = barraInferior,
        floatingActionButton = botonFlotante,
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        content = contenido
    )
}

@Composable
fun MostrarMensaje(mensaje: String?, snackbar: SnackbarHostState, onMostrado: () -> Unit) {
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            snackbar.showSnackbar(mensaje)
            onMostrado()
        }
    }
}

data class DestinoInferior(val ruta: String, val etiqueta: String, @DrawableRes val icono: Int)

@Composable
fun BarraInferior(destinos: List<DestinoInferior>, rutaActual: String?, onNavegar: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        destinos.forEach { d ->
            NavigationBarItem(
                selected = rutaActual == d.ruta,
                onClick = { if (rutaActual != d.ruta) onNavegar(d.ruta) },
                icon = { Icon(painterResource(d.icono), contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = { Text(d.etiqueta, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
