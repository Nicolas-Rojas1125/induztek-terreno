package cl.duocuc.induztek.ui.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.reglas.Plazos
import cl.duocuc.induztek.domain.reglas.VisibilidadCliente
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.ui.components.AparicionEscalonada
import cl.duocuc.induztek.ui.components.Cargando
import cl.duocuc.induztek.ui.components.ChipEstado
import cl.duocuc.induztek.ui.components.ChipResultado
import cl.duocuc.induztek.ui.components.EstadoVacio
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaIndicador
import cl.duocuc.induztek.ui.components.TarjetaLista
import cl.duocuc.induztek.ui.theme.AmbarObservado
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.MoradoFormalizado
import cl.duocuc.induztek.ui.theme.RojoRechazado
import cl.duocuc.induztek.viewmodel.InicioViewModel

@Composable
fun InicioScreen(
    barraInferior: @Composable () -> Unit,
    onVisita: (Long) -> Unit,
    onProtocolo: (Long) -> Unit,
    onInstalacion: (Long) -> Unit,
    onProtocolos: () -> Unit,
    onInstrumentos: () -> Unit,
    onVisitas: () -> Unit,
    onEquipos: () -> Unit,
    viewModel: InicioViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val s = ui.sesion
    PantallaBase(
        titulo = "Inicio",
        subtitulo = s?.let { if (it.esCliente) it.nombreCliente else it.rol.etiqueta },
        barraInferior = barraInferior
    ) { padding ->
        if (ui.cargando || s == null) {
            Cargando(Modifier.padding(padding))
            return@PantallaBase
        }
        val hoy = remember { Formato.hoy() }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Saludo(nombre = s.nombre.substringBefore(" "), detalle = if (s.esCliente) s.cargo.orEmpty() else s.rol.etiqueta, iniciales = s.iniciales) }

            item {
                val ind = ui.indicadores
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (s.esCliente) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TarjetaIndicador(ind.equiposCliente, "Equipos de mi empresa", R.drawable.ic_rayo, Modifier.weight(1f), onClick = onEquipos)
                            TarjetaIndicador(ind.actasCliente, "Actas de visita recibidas", R.drawable.ic_documento, Modifier.weight(1f), onClick = onVisitas)
                        }
                        TarjetaIndicador(
                            ind.pendientesFormalizar, VisibilidadCliente.PENDIENTE, R.drawable.ic_historial,
                            Modifier.fillMaxWidth(), colorAcento = AmbarObservado
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (s.rol == RolCodigo.TECNICO) {
                                TarjetaIndicador(ind.visitasAbiertas, "Visitas abiertas donde participo", R.drawable.ic_calendario, Modifier.weight(1f), onClick = onVisitas)
                                TarjetaIndicador(ind.borradoresPropios, "Mis protocolos en borrador", R.drawable.ic_editar, Modifier.weight(1f), onClick = onProtocolos)
                            } else {
                                TarjetaIndicador(ind.pendientesFormalizar, "Pendientes de formalizar", R.drawable.ic_documento, Modifier.weight(1f), onClick = onProtocolos)
                                TarjetaIndicador(ind.visitasAbiertas, "Visitas abiertas", R.drawable.ic_calendario, Modifier.weight(1f), onClick = onVisitas)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (s.rol == RolCodigo.TECNICO) {
                                TarjetaIndicador(ind.observadosPropios, "Observados por corregir", R.drawable.ic_advertencia, Modifier.weight(1f), colorAcento = AmbarObservado, onClick = onProtocolos)
                            } else {
                                TarjetaIndicador(
                                    ind.atrasados, "Atrasados (> ${Plazos.DIAS_HABILES_META} días hábiles, RN-16)", R.drawable.ic_advertencia,
                                    Modifier.weight(1f), colorAcento = RojoRechazado, onClick = onProtocolos
                                )
                            }
                            if (s.puede(Permiso.GESTIONAR_INSTRUMENTOS)) {
                                TarjetaIndicador(
                                    ind.instrumentosPorVencer + ind.instrumentosVencidos,
                                    "Instrumentos vencidos o por vencer (RF-19)", R.drawable.ic_herramienta,
                                    Modifier.weight(1f), colorAcento = AmbarObservado, onClick = onInstrumentos
                                )
                            } else if (s.rol == RolCodigo.TECNICO) {
                                TarjetaIndicador(ind.atrasados, "Protocolos atrasados (RN-16)", R.drawable.ic_historial, Modifier.weight(1f), colorAcento = RojoRechazado)
                            } else {
                                TarjetaIndicador(
                                    ind.instrumentosVencidos, "Instrumentos con calibración vencida", R.drawable.ic_herramienta,
                                    Modifier.weight(1f), colorAcento = RojoRechazado
                                )
                            }
                        }
                        ind.promedioDiasFormalizacion?.let { prom ->
                            Card(
                                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        Formato.numero(prom, 1), style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "días hábiles promedio entre la prueba y la formalización (RF-21). Meta: ${Plazos.DIAS_HABILES_META}.",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!s.esCliente) {
                item { TituloSeccion("Visitas abiertas") }
                if (ui.visitasAbiertas.isEmpty()) {
                    item { EstadoVacio(R.drawable.ic_calendario, "Sin visitas abiertas", "Cree una visita desde la pestaña Visitas.") }
                }
                items(ui.visitasAbiertas, key = { "v${it.id}" }) { v ->
                    AparicionEscalonada(ui.visitasAbiertas.indexOf(v)) {
                        TarjetaLista(
                            titulo = "${v.numeroOt} · ${v.nombreInstalacion}",
                            subtitulo = "${v.nombreCliente} · ${v.tipo.etiqueta}",
                            detalle = "${Formato.fecha(v.fecha)} · ${v.cantidadProtocolos} protocolo(s) · Ing. ${v.nombreIngeniero}",
                            icono = R.drawable.ic_calendario,
                            onClick = { onVisita(v.id) }
                        )
                    }
                }
            }

            if (s.esCliente) {
                item { TituloSeccion("Mis instalaciones") }
                items(ui.instalaciones, key = { "i${it.id}" }) { inst ->
                    TarjetaLista(
                        titulo = inst.nombre,
                        subtitulo = "${inst.nombreComuna} · ${inst.cantidadEquipos} equipo(s)",
                        icono = R.drawable.ic_ubicacion,
                        onClick = { onInstalacion(inst.id) }
                    )
                }
            }

            item {
                TituloSeccion(
                    when {
                        s.esCliente -> "Últimos protocolos"
                        s.rol == RolCodigo.TECNICO -> "Mis protocolos por completar o corregir"
                        else -> "Esperan tu revisión (Registrados en terreno)"
                    }
                )
            }
            if (ui.pendientes.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_check, "Todo al día", "No hay protocolos que requieran tu acción.") }
            }
            items(ui.pendientes, key = { "p${it.id}" }) { p ->
                val atrasado = !s.esCliente && Plazos.estaAtrasado(p.estado, Formato.aFecha(p.fechaHoraPrueba), hoy)
                TarjetaLista(
                    titulo = "${p.numero} · ${p.tagEquipo}",
                    subtitulo = "${p.nombreInstalacion} · ${p.nombreCliente}",
                    detalle = "${Formato.fechaHora(p.fechaHoraPrueba)} · ${p.nombreTecnico}" + if (atrasado) " · ATRASADO" else "",
                    icono = R.drawable.ic_documento,
                    colorIcono = if (atrasado) RojoRechazado else MoradoFormalizado,
                    onClick = { onProtocolo(p.id) },
                    pie = {
                        ChipEstado(p.estado, etiquetaCliente = if (s.esCliente) VisibilidadCliente.etiquetaPara(p.estado) else null)
                        ChipResultado(p.resultadoGlobal)
                    }
                )
            }
            item { Spacer(Modifier.size(Dimens.espacioGrande)) }
        }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Saludo(nombre: String, detalle: String, iniciales: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                Text(
                    iniciales, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Hola, $nombre", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    "$detalle · ${Formato.fecha(Formato.hoy())}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }
    }
}
