package cl.duocuc.induztek.ui.screens.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.ui.components.BotonSecundario
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaLista
import cl.duocuc.induztek.ui.components.TarjetaSeccion
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.RojoRechazado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasScreen(
    sesion: Sesion?,
    tema: String,
    barraInferior: @Composable () -> Unit,
    onClientes: () -> Unit,
    onInstrumentos: () -> Unit,
    onUsuarios: () -> Unit,
    onTema: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    var confirmar by remember { mutableStateOf(false) }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            icon = { Icon(painterResource(R.drawable.ic_salir), contentDescription = null) },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Los datos registrados quedan guardados en el teléfono.") },
            confirmButton = { TextButton(onClick = { confirmar = false; onCerrarSesion() }) { Text("Cerrar sesión", color = RojoRechazado) } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    }
    PantallaBase(titulo = "Más", barraInferior = barraInferior) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (sesion != null) {
                item {
                    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                Text(sesion.iniciales, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(sesion.nombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${sesion.rol.etiqueta} (${sesion.rol.perfil})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(sesion.correo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                sesion.nombreCliente?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                            }
                        }
                    }
                }
                if (!sesion.esCliente) {
                    item { TarjetaLista("Clientes e instalaciones", "Registro de clientes, plantas y equipos (RF-01, RF-02)", onClientes, icono = R.drawable.ic_empresa) }
                    item { TarjetaLista("Instrumentos", "Calibraciones y vencimientos (RN-11, RF-19)", onInstrumentos, icono = R.drawable.ic_herramienta) }
                    if (sesion.puede(Permiso.GESTIONAR_PERSONAL) || sesion.puede(Permiso.OTORGAR_CUENTAS_CLIENTE)) {
                        item { TarjetaLista("Personal y cuentas de cliente", "Agregar o desactivar usuarios (RF-22, RN-20)", onUsuarios, icono = R.drawable.ic_grupo) }
                    }
                }
                item {
                    TarjetaSeccion("Mis permisos", icono = R.drawable.ic_verificado) {
                        sesion.permisos.sortedBy { it.ordinal }.forEach {
                            Text("• ${it.descripcion}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                TarjetaSeccion("Apariencia", icono = R.drawable.ic_tema) {
                    Text("Modo oscuro para salas con poca luz; claro para trabajo a pleno sol (RNF-04).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val opciones = listOf("sistema" to "Sistema", "claro" to "Claro", "oscuro" to "Oscuro")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        opciones.forEachIndexed { i, (valor, etiqueta) ->
                            SegmentedButton(
                                selected = tema == valor, onClick = { onTema(valor) },
                                shape = SegmentedButtonDefaults.itemShape(index = i, count = opciones.size),
                                label = { Text(etiqueta) }
                            )
                        }
                    }
                }
            }
            item {
                TarjetaSeccion("Recursos del dispositivo", icono = R.drawable.ic_info) {
                    Text("• Cámara: foto de la placa de características (PB-03)", style = MaterialTheme.typography.bodySmall)
                    Text("• GPS: coordenadas de instalaciones y del cierre de visita", style = MaterialTheme.typography.bodySmall)
                    Text("• Vibración: confirmación táctil al trabajar con guantes", style = MaterialTheme.typography.bodySmall)
                    Text("• Estado de red: aviso de trabajo sin conexión (RNF-02)", style = MaterialTheme.typography.bodySmall)
                    Text("• PDF y compartir: acta en terreno (RF-07)", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                BotonSecundario("Cerrar sesión", { confirmar = true }, Modifier.fillMaxWidth(), icono = R.drawable.ic_salir, color = RojoRechazado)
            }
            item {
                Text(
                    "Induztek Terreno · MVP académico DSY1105 · Datos ficticios",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
