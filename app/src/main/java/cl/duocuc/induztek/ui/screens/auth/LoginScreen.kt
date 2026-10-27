package cl.duocuc.induztek.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.CampoContrasena
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.rememberSacudida
import cl.duocuc.induztek.ui.components.sacudir
import cl.duocuc.induztek.ui.theme.Ambar
import cl.duocuc.induztek.ui.theme.AzulInduztek
import cl.duocuc.induztek.ui.theme.AzulOscuro
import cl.duocuc.induztek.ui.theme.Blanco
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.InduztekTheme
import cl.duocuc.induztek.viewmodel.LoginUiState
import cl.duocuc.induztek.viewmodel.LoginViewModel

@Composable
fun LoginScreen(onIngreso: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.exito) { if (ui.exito) onIngreso() }
    LoginContenido(
        ui = ui,
        onCorreo = viewModel::onCorreo,
        onContrasena = viewModel::onContrasena,
        onAlternarVisible = viewModel::onAlternarVisible,
        onIngresar = viewModel::ingresar,
        onCuentaDemo = viewModel::usarCuentaDemo
    )
}

private data class CuentaDemo(val etiqueta: String, val correo: String)

private val cuentasDemo = listOf(
    CuentaDemo("Técnico · Matías", "mgodoy@induztek.ejemplo"),
    CuentaDemo("Ingeniera · Andrea", "acontreras@induztek.ejemplo"),
    CuentaDemo("Jefe técnico · Ricardo", "respinoza@induztek.ejemplo"),
    CuentaDemo("Cliente · Marcela", "mfuentes@envpacifico.ejemplo")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginContenido(
    ui: LoginUiState,
    onCorreo: (String) -> Unit,
    onContrasena: (String) -> Unit,
    onAlternarVisible: () -> Unit,
    onIngresar: () -> Unit,
    onCuentaDemo: (String) -> Unit
) {
    val sacudida = rememberSacudida(ui.intentosFallidos)

    val transicion = rememberInfiniteTransition(label = "logo")
    val escalaLogo by transicion.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "escalaLogo"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AzulOscuro, AzulInduztek, MaterialTheme.colorScheme.background)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.espacioPantalla),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Surface(shape = CircleShape, color = Blanco.copy(alpha = 0.12f), modifier = Modifier.scale(escalaLogo)) {
            Box(Modifier.padding(22.dp)) {
                Icon(painterResource(R.drawable.ic_rayo), contentDescription = null, tint = Ambar, modifier = Modifier.size(56.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Induztek Terreno", style = MaterialTheme.typography.headlineSmall, color = Blanco)
        Text(
            "Protocolos de prueba eléctrica con respaldo inmediato para el cliente",
            style = MaterialTheme.typography.bodyMedium, color = Blanco.copy(alpha = 0.85f), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .sacudir(sacudida),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Cada acción queda registrada con su usuario (RF-14)",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                CampoTexto(
                    valor = ui.correo.valor,
                    onValorCambia = onCorreo,
                    etiqueta = "Correo",
                    icono = R.drawable.ic_correo,
                    error = ui.correo.errorVisible,
                    valido = ui.correo.valido,
                    tipoTeclado = KeyboardType.Email,
                    capitalizacion = KeyboardCapitalization.None
                )
                CampoContrasena(
                    valor = ui.contrasena.valor,
                    onValorCambia = onContrasena,
                    etiqueta = "Contraseña",
                    visible = ui.contrasenaVisible,
                    onAlternarVisible = onAlternarVisible,
                    error = ui.contrasena.errorVisible
                )
                AnimatedVisibility(
                    visible = ui.errorGeneral != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(ui.errorGeneral.orEmpty(), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                BotonPrincipal(
                    texto = "Ingresar",
                    onClick = onIngresar,
                    modifier = Modifier.fillMaxWidth(),
                    icono = R.drawable.ic_candado,
                    cargando = ui.cargando
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Cuentas de demostración (datos ficticios del Anexo 9)",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            cuentasDemo.forEach { cuenta ->
                AssistChip(
                    onClick = { onCuentaDemo(cuenta.correo) },
                    label = { Text(cuenta.etiqueta) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_persona), contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }
        Text(
            "Contraseña de todas las cuentas: Induztek2026",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginPreview() {
    InduztekTheme {
        LoginContenido(LoginUiState(), {}, {}, {}, {}, {})
    }
}
