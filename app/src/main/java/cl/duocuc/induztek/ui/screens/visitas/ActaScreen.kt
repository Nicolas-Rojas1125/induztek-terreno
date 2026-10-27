package cl.duocuc.induztek.ui.screens.visitas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.Momento
import cl.duocuc.induztek.domain.reglas.EvaluadorPruebas
import cl.duocuc.induztek.domain.reglas.VisibilidadCliente
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.nativo.GeneradorActaPdf
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.Cargando
import cl.duocuc.induztek.ui.components.ChipResultado
import cl.duocuc.induztek.ui.components.EstadoVacio
import cl.duocuc.induztek.ui.components.FilaDato
import cl.duocuc.induztek.ui.components.MostrarMensaje
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.theme.Ambar
import cl.duocuc.induztek.ui.theme.AzulInduztek
import cl.duocuc.induztek.ui.theme.Blanco
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.TextoOscuro
import cl.duocuc.induztek.viewmodel.ActaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ActaScreen(onVolver: () -> Unit, viewModel: ActaViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val acta = ui.acta

    PantallaBase(titulo = "Acta de prueba en terreno", subtitulo = acta?.visita?.numeroOt, onVolver = onVolver, snackbar = snackbar) { padding ->
        if (ui.cargando) { Cargando(Modifier.padding(padding)); return@PantallaBase }
        if (acta == null || !acta.visita.cerrada) {
            EstadoVacio(R.drawable.ic_documento, "Acta no disponible", "La visita debe estar cerrada para generar el acta.", Modifier.padding(padding))
            return@PantallaBase
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                BotonPrincipal(
                    texto = "Generar PDF y compartir",
                    onClick = {
                        viewModel.onGenerando(true)
                        alcance.launch {
                            try {
                                val archivo = withContext(Dispatchers.IO) { GeneradorActaPdf.generar(context, acta) }
                                GeneradorActaPdf.compartir(context, archivo, acta.visita.numeroOt)
                            } catch (e: Exception) {
                                viewModel.onErrorPdf("No se pudo generar el PDF: ${e.message}")
                            } finally {
                                viewModel.onGenerando(false)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icono = R.drawable.ic_compartir,
                    cargando = ui.generandoPdf
                )
            }
            item {
                Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = AzulInduztek)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("INDUZTEK INGENIERÍA SpA", color = Blanco, style = MaterialTheme.typography.titleMedium)
                        Text("Acta de prueba en terreno · OT ${acta.visita.numeroOt}", color = Blanco.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        Surface(color = Ambar, contentColor = TextoOscuro, shape = MaterialTheme.shapes.small) {
                            Text(
                                VisibilidadCliente.PENDIENTE.uppercase(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item {
                Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilaDato("Cliente", "${acta.cliente.razonSocial} (${acta.cliente.rut})")
                        FilaDato("Instalación", "${acta.instalacion.nombre}, ${acta.instalacion.nombreComuna}")
                        FilaDato("Fecha", Formato.fecha(acta.visita.fecha))
                        FilaDato("Técnicos", acta.visita.participantes.joinToString { it.nombre })
                        FilaDato("Ingeniero a cargo", acta.visita.nombreIngeniero)
                        acta.visita.fechaHoraCierre?.let { FilaDato("Cierre", Formato.fechaHora(it)) }
                        val lat = acta.visita.latitudCierre
                        val lon = acta.visita.longitudCierre
                        if (lat != null && lon != null) FilaDato("GPS", Formato.coordenadas(lat, lon))
                    }
                }
            }
            items(acta.protocolos, key = { it.first.id }) { (prot, pruebas) ->
                Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${prot.tagEquipo} · ${prot.tipoEquipo.etiqueta}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${prot.numero} · ${prot.nombreTecnico} · T° ${prot.temperaturaC?.let { Formato.numero(it, 1) } ?: "—"} °C · " +
                                        "HR ${prot.humedadPct?.let { Formato.numero(it, 0) } ?: "—"} %",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ChipResultado(prot.resultadoGlobal)
                        }
                        HorizontalDivider()
                        pruebas.forEach { p ->
                            if (!p.realizada) {
                                Text("• ${p.tipo.etiqueta}: No realizada — ${p.motivoNoRealizada}", style = MaterialTheme.typography.bodySmall)
                            } else {
                                val eval = EvaluadorPruebas.evaluar(p.tipo, prot.tipoEquipo, p.valores, p.noOpera, prot.maxContactosUohm, p.resultado)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${p.tipo.etiqueta} · ${p.elemento}" + if (p.momento != Momento.UNICA) " (${p.momento.etiqueta})" else "",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            p.mediciones.joinToString("  ·  ") { "${it.parametro.etiqueta}: ${Formato.numeroCompacto(it.valor)} ${it.parametro.unidad.simbolo}".trimEnd() } +
                                                (eval.calculo?.let { "  ·  $it" } ?: ""),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    ChipResultado(p.resultado)
                                }
                            }
                        }
                    }
                }
            }
            item {
                val r = acta.recepcion
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
                        .padding(16.dp)
                ) {
                    if (r == null) {
                        Text("Recepción pendiente: registre quién recibe el acta (RF-08).", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Column {
                            Text("Recibido por", style = MaterialTheme.typography.labelLarge)
                            Text("${r.receptorNombre} · RUT ${r.receptorRut}", style = MaterialTheme.typography.bodyMedium)
                            Text("${r.receptorCargo} · ${Formato.fechaHora(r.fechaHoraRecepcion)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
