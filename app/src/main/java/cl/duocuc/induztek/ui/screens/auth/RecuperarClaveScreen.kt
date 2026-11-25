package cl.duocuc.induztek.ui.screens.auth

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.ui.components.AvisoInformativo
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.CampoContrasena
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.MostrarMensaje
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaSeccion
import cl.duocuc.induztek.ui.components.rememberSacudida
import cl.duocuc.induztek.ui.components.sacudir
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.viewmodel.PasoRecuperacion
import cl.duocuc.induztek.viewmodel.RecuperarClaveViewModel

@Composable
fun RecuperarClaveScreen(
    onVolver: () -> Unit,
    viewModel: RecuperarClaveViewModel = viewModel(factory = RecuperarClaveViewModel.Fabrica)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    val sacudida = rememberSacudida(ui.intentosFallidos)

    PantallaBase(titulo = "Recuperar contraseña", subtitulo = "Código de verificación de 6 dígitos", onVolver = onVolver, snackbar = snackbar) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedContent(targetState = ui.paso, label = "pasoRecuperacion") { paso ->
                when (paso) {
                    PasoRecuperacion.SOLICITAR -> TarjetaSeccion("1. Ingrese su correo", icono = R.drawable.ic_correo, modifier = Modifier.sacudir(sacudida)) {
                        Text(
                            "Le enviaremos un código para crear una contraseña nueva. Vence en 15 minutos.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CampoTexto(
                            ui.correo.valor, viewModel::onCorreo, "Correo de la cuenta", R.drawable.ic_correo,
                            error = ui.correo.errorVisible, valido = ui.correo.valido,
                            tipoTeclado = KeyboardType.Email, capitalizacion = KeyboardCapitalization.None
                        )
                        BotonPrincipal("Enviar código", viewModel::solicitarCodigo, Modifier.fillMaxWidth(), icono = R.drawable.ic_enviar, cargando = ui.cargando)
                    }
                    PasoRecuperacion.RESTABLECER -> TarjetaSeccion("2. Nueva contraseña", icono = R.drawable.ic_llave, modifier = Modifier.sacudir(sacudida)) {
                        ui.aviso?.let { AvisoInformativo(it) }
                        ui.codigoDemo?.let {
                            AvisoInformativo("Modo demostración (sin servidor de correo): su código es $it", advertencia = true)
                        }
                        CampoTexto(
                            ui.codigo.valor, viewModel::onCodigo, "Código de 6 dígitos", R.drawable.ic_numero,
                            error = ui.codigo.errorVisible, valido = ui.codigo.valido, tipoTeclado = KeyboardType.NumberPassword
                        )
                        CampoContrasena(
                            ui.nueva.valor, viewModel::onNueva, "Nueva contraseña", ui.visible, viewModel::onAlternarVisible,
                            error = ui.nueva.errorVisible
                        )
                        CampoContrasena(
                            ui.repetida.valor, viewModel::onRepetida, "Repita la contraseña", ui.visible, viewModel::onAlternarVisible,
                            error = ui.repetida.errorVisible
                        )
                        Text(
                            "Mínimo 8 caracteres, combinando letras y números.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BotonPrincipal("Cambiar contraseña", viewModel::restablecer, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.cargando)
                        TextButton(onClick = viewModel::volverASolicitar) { Text("Solicitar otro código") }
                    }
                    PasoRecuperacion.LISTO -> TarjetaSeccion("Contraseña restablecida", icono = R.drawable.ic_verificado) {
                        AvisoInformativo(ui.aviso ?: "Ya puede iniciar sesión con su nueva contraseña")
                        BotonPrincipal("Volver a iniciar sesión", onVolver, Modifier.fillMaxWidth(), icono = R.drawable.ic_candado)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
