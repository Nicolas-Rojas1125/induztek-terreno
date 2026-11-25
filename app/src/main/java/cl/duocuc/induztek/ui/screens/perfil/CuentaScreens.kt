package cl.duocuc.induztek.ui.screens.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.data.remote.ServiciosRemotos
import cl.duocuc.induztek.domain.model.EstadoSincronizacion
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.domain.validacion.Validadores
import cl.duocuc.induztek.ui.components.AvisoInformativo
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.BotonSecundario
import cl.duocuc.induztek.ui.components.CampoContrasena
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.FilaDato
import cl.duocuc.induztek.ui.components.MostrarMensaje
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaErrores
import cl.duocuc.induztek.ui.components.TarjetaSeccion
import cl.duocuc.induztek.ui.components.rememberSacudida
import cl.duocuc.induztek.ui.components.sacudir
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.viewmodel.CambiarClaveViewModel
import cl.duocuc.induztek.viewmodel.EditarPerfilViewModel

@Composable
private fun Formulario(padding: androidx.compose.foundation.layout.PaddingValues, contenido: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.espacioPantalla),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        contenido()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun EditarPerfilScreen(
    onVolver: () -> Unit,
    viewModel: EditarPerfilViewModel = viewModel(factory = EditarPerfilViewModel.Fabrica)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    val sacudida = rememberSacudida(ui.intentosFallidos)

    PantallaBase(titulo = "Mi perfil", subtitulo = ui.rol, onVolver = onVolver, snackbar = snackbar) { padding ->
        Formulario(padding) {
            TarjetaSeccion("Datos de la cuenta", icono = R.drawable.ic_credencial) {
                FilaDato("RUT", ui.rut.ifBlank { "—" }, icono = R.drawable.ic_credencial)
                FilaDato("Rol", ui.rol, icono = R.drawable.ic_verificado)
                ui.empresa?.let { FilaDato("Empresa", it, icono = R.drawable.ic_empresa) }
                Text(
                    "El RUT y el rol solo los cambia un administrador.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TarjetaSeccion("Datos editables", icono = R.drawable.ic_editar, modifier = Modifier.sacudir(sacudida)) {
                CampoTexto(ui.nombre.valor, viewModel::onNombre, "Nombre y apellido", R.drawable.ic_persona,
                    error = ui.nombre.errorVisible, valido = ui.nombre.valido, capitalizacion = KeyboardCapitalization.Words)
                CampoTexto(ui.correo.valor, viewModel::onCorreo, "Correo", R.drawable.ic_correo,
                    error = ui.correo.errorVisible, valido = ui.correo.valido, tipoTeclado = KeyboardType.Email,
                    capitalizacion = KeyboardCapitalization.None)
                CampoTexto(ui.telefono.valor, viewModel::onTelefono, "Teléfono (opcional)", R.drawable.ic_telefono,
                    error = ui.telefono.errorVisible, valido = ui.telefono.valido && ui.telefono.valor.isNotBlank(),
                    tipoTeclado = KeyboardType.Phone)
                CampoTexto(ui.cargo.valor, viewModel::onCargo, "Cargo (opcional)", R.drawable.ic_etiqueta,
                    error = ui.cargo.errorVisible, capitalizacion = KeyboardCapitalization.Words)
            }
            BotonPrincipal("Guardar cambios", viewModel::guardar, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando)
        }
    }
}

@Composable
fun CambiarClaveScreen(
    onVolver: () -> Unit,
    viewModel: CambiarClaveViewModel = viewModel(factory = CambiarClaveViewModel.Fabrica)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    val sacudida = rememberSacudida(ui.intentosFallidos)
    LaunchedEffect(ui.listo) { if (ui.listo) onVolver() }

    PantallaBase(titulo = "Cambiar contraseña", onVolver = onVolver, snackbar = snackbar) { padding ->
        Formulario(padding) {
            TarjetaSeccion("Contraseña", icono = R.drawable.ic_llave, modifier = Modifier.sacudir(sacudida)) {
                CampoContrasena(ui.actual.valor, viewModel::onActual, "Contraseña actual", ui.visible, viewModel::onAlternarVisible,
                    error = ui.actual.errorVisible)
                CampoContrasena(ui.nueva.valor, viewModel::onNueva, "Nueva contraseña", ui.visible, viewModel::onAlternarVisible,
                    error = ui.nueva.errorVisible)
                CampoContrasena(ui.repetida.valor, viewModel::onRepetida, "Repita la nueva contraseña", ui.visible, viewModel::onAlternarVisible,
                    error = ui.repetida.errorVisible)
                Text(
                    "Mínimo 8 caracteres, combinando letras y números, y distinta de la actual.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BotonPrincipal("Actualizar contraseña", viewModel::guardar, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando)
        }
    }
}

@Composable
fun ServidorScreen(urlActual: String, onGuardar: (String) -> Unit, onVolver: () -> Unit) {
    var url by rememberSaveable { mutableStateOf(urlActual) }
    var tocado by rememberSaveable { mutableStateOf(false) }
    val error = if (tocado) Validadores.urlServidor(url) else null
    PantallaBase(titulo = "Servidor", subtitulo = "API Gateway de los microservicios", onVolver = onVolver) { padding ->
        Formulario(padding) {
            TarjetaSeccion("Dirección del API Gateway", icono = R.drawable.ic_servidor) {
                CampoTexto(
                    url, { url = it; tocado = true }, "URL del servidor", R.drawable.ic_servidor,
                    error = error, valido = tocado && error == null, tipoTeclado = KeyboardType.Uri,
                    capitalizacion = KeyboardCapitalization.None
                )
                Text(
                    "Emulador: http://10.0.2.2:8081/ · Teléfono real: http://IP-del-PC:8081/ (misma red Wi-Fi).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { url = ServiciosRemotos.URL_POR_DEFECTO; tocado = true }) { Text("Usar valor del emulador") }
            }
            BotonPrincipal(
                "Guardar", {
                    tocado = true
                    if (Validadores.urlServidor(url) == null) { onGuardar(url.trim()); onVolver() }
                },
                Modifier.fillMaxWidth(), icono = R.drawable.ic_check
            )
        }
    }
}

@Composable
fun SincronizacionScreen(
    estado: EstadoSincronizacion,
    conectado: Boolean,
    servidor: String,
    onSincronizar: () -> Unit,
    onVolver: () -> Unit
) {
    PantallaBase(titulo = "Sincronización", subtitulo = servidor, onVolver = onVolver) { padding ->
        Formulario(padding) {
            TarjetaSeccion("Estado", icono = R.drawable.ic_sincronizar) {
                if (estado.sincronizando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                FilaDato("Conexión", if (conectado) "Con internet" else "Sin conexión", icono = R.drawable.ic_sin_senal)
                FilaDato("Cambios por enviar", "${estado.pendientes} registro(s)", icono = R.drawable.ic_enviar)
                FilaDato("Última sincronización", Formato.hace(estado.ultimaSincronizacion), icono = R.drawable.ic_historial)
                estado.ultimoResultado?.let { r ->
                    FilaDato("Último intento", "${Formato.fechaHora(r.fechaHora)} · ${r.enviados} enviado(s) · ${r.recibidos} recibido(s)")
                }
            }
            val r = estado.ultimoResultado
            if (r != null && r.errores.isNotEmpty()) {
                TarjetaErrores("No todo se pudo sincronizar", r.errores)
            } else if (r != null) {
                AvisoInformativo("Todo está al día con el servidor")
            }
            TarjetaSeccion("Cómo funciona", icono = R.drawable.ic_info) {
                Text("• Las visitas y protocolos se guardan primero en el teléfono: puede trabajar sin internet.", style = MaterialTheme.typography.bodySmall)
                Text("• Al volver la conexión, la app envía lo pendiente al microservicio de protocolos.", style = MaterialTheme.typography.bodySmall)
                Text("• Clientes, equipos, instrumentos y cuentas se administran en el servidor y se descargan al teléfono.", style = MaterialTheme.typography.bodySmall)
                Text("• Un protocolo Formalizado no se modifica, ni en el teléfono ni en el servidor (RN-04).", style = MaterialTheme.typography.bodySmall)
            }
            BotonSecundario(
                if (estado.sincronizando) "Sincronizando…" else "Sincronizar ahora", onSincronizar,
                Modifier.fillMaxWidth(), icono = R.drawable.ic_sincronizar, habilitado = conectado && !estado.sincronizando
            )
        }
    }
}
