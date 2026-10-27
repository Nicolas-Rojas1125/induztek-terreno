package cl.duocuc.induztek.ui.screens.protocolos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.reglas.Plazos
import cl.duocuc.induztek.domain.reglas.VisibilidadCliente
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.ui.components.AparicionEscalonada
import cl.duocuc.induztek.ui.components.CampoSeleccion
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.ChipEstado
import cl.duocuc.induztek.ui.components.ChipResultado
import cl.duocuc.induztek.ui.components.EstadoVacio
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaLista
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.MoradoFormalizado
import cl.duocuc.induztek.ui.theme.RojoRechazado
import cl.duocuc.induztek.viewmodel.ProtocolosViewModel

@Composable
fun ProtocolosScreen(
    barraInferior: @Composable () -> Unit,
    onProtocolo: (Long) -> Unit,
    viewModel: ProtocolosViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val s = ui.sesion
    val esCliente = s?.esCliente == true
    val hoy = remember { Formato.hoy() }
    val estados = EstadoProtocolo.entries

    PantallaBase(titulo = "Protocolos", subtitulo = "${ui.protocolos.size} protocolo(s)", barraInferior = barraInferior) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CampoTexto(
                    ui.busqueda, viewModel::onBuscar, "Buscar por Nº de protocolo, TAG u OT", R.drawable.ic_buscar,
                    capitalizacion = KeyboardCapitalization.Characters
                )
            }
            if (!esCliente) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(estados) { e ->
                            FilterChip(
                                selected = ui.estado == e,
                                onClick = { viewModel.onEstado(e) },
                                label = { Text(e.etiqueta) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = ui.soloAtrasados, onClick = viewModel::onSoloAtrasados,
                                label = { Text("Atrasados (RN-16)") },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_advertencia), contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoSeleccion(
                            etiqueta = "Cliente", opciones = ui.clientes, seleccion = ui.cliente, onSeleccionar = viewModel::onCliente,
                            texto = { it.razonSocial }, icono = R.drawable.ic_empresa, modifier = Modifier.weight(1f)
                        )
                        CampoSeleccion(
                            etiqueta = "Técnico", opciones = ui.tecnicos, seleccion = ui.tecnico, onSeleccionar = viewModel::onTecnico,
                            texto = { it.nombre }, icono = R.drawable.ic_persona, modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (ui.estado != null || ui.cliente != null || ui.tecnico != null || ui.busqueda.isNotBlank() || ui.soloAtrasados) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_filtro), contentDescription = null, modifier = Modifier.size(18.dp))
                            TextButton(onClick = viewModel::limpiarFiltros) { Text("Quitar filtros") }
                        }
                    }
                }
            }
            if (!ui.cargando && ui.protocolos.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_documento, "Sin protocolos", "No hay protocolos con esos filtros.") }
            }
            items(ui.protocolos, key = { it.id }) { p ->
                val atrasado = !esCliente && Plazos.estaAtrasado(p.estado, Formato.aFecha(p.fechaHoraPrueba), hoy)
                AparicionEscalonada(ui.protocolos.indexOf(p), Modifier.animateItem()) {
                    TarjetaLista(
                        titulo = "${p.numero} · ${p.tagEquipo}",
                        subtitulo = "${p.nombreInstalacion} · ${p.nombreCliente}",
                        detalle = "${Formato.fechaHora(p.fechaHoraPrueba)} · ${p.nombreTecnico} · OT ${p.numeroOt}" +
                            (if (atrasado) " · ATRASADO" else "") +
                            (p.reemplazadoPor?.let { " · Reemplazado por nº $it" } ?: ""),
                        icono = if (atrasado) R.drawable.ic_advertencia else R.drawable.ic_documento,
                        colorIcono = if (atrasado) RojoRechazado else MoradoFormalizado,
                        onClick = { onProtocolo(p.id) },
                        pie = {
                            ChipEstado(p.estado, etiquetaCliente = if (esCliente) VisibilidadCliente.etiquetaPara(p.estado) else null)
                            ChipResultado(p.resultadoGlobal)
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(Dimens.espacioGrande)) }
        }
    }
}
