package cl.duocuc.induztek.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.viewmodel.ClimaViewModel

@Composable
fun TarjetaClima(
    latitud: Double?,
    longitud: Double?,
    modifier: Modifier = Modifier,
    onUsarValores: ((Double, Double) -> Unit)? = null
) {
    val vm: ClimaViewModel = viewModel(key = "clima-$latitud-$longitud", factory = ClimaViewModel.Fabrica)
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(latitud, longitud) { if (!ui.consultado) vm.consultar(latitud, longitud) }

    TarjetaSeccion("Clima en la instalación (Open-Meteo)", modifier = modifier, icono = R.drawable.ic_nube) {
        AnimatedContent(targetState = Triple(ui.cargando, ui.clima, ui.error), label = "clima") { (cargando, clima, error) ->
            when {
                cargando -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Consultando el clima actual…", style = MaterialTheme.typography.bodyMedium)
                }
                clima != null -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Dato(R.drawable.ic_termometro, "${Formato.numero(clima.temperaturaC, 1)} °C", "Temperatura")
                        Dato(R.drawable.ic_gota, "${Formato.numero(clima.humedadPct, 0)} %", "Humedad")
                        clima.vientoKmh?.let { Dato(R.drawable.ic_viento, "${Formato.numero(it, 0)} km/h", "Viento") }
                    }
                    Text(
                        "${clima.descripcion} · medición de las ${clima.horaMedicion} (hora local)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (ui.alertaHumedad) {
                        AvisoInformativo("Humedad sobre 80 %: la prueba de aislación puede dar valores bajos. Regístrelo en observaciones.", advertencia = true)
                    }
                    if (onUsarValores != null) {
                        BotonSecundario(
                            "Usar como condiciones ambientales",
                            { onUsarValores(clima.temperaturaC, clima.humedadPct) },
                            Modifier.fillMaxWidth(),
                            icono = R.drawable.ic_termometro
                        )
                    }
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(error ?: "Sin datos de clima", style = MaterialTheme.typography.bodyMedium)
                    if (latitud != null && longitud != null) {
                        BotonSecundario("Reintentar", { vm.consultar(latitud, longitud) }, Modifier.fillMaxWidth(), icono = R.drawable.ic_sincronizar)
                    }
                }
            }
        }
        Text(
            "Dato de referencia de un servicio externo: no reemplaza la medición con termohigrómetro.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Dato(icono: Int, valor: String, etiqueta: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icono), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(valor, style = MaterialTheme.typography.titleMedium)
            Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
