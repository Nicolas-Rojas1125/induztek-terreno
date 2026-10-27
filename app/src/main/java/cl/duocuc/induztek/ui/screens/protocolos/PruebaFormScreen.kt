package cl.duocuc.induztek.ui.screens.protocolos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.Parametro
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.reglas.ReglasInstrumento
import cl.duocuc.induztek.domain.reglas.VigenciaInstrumento
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.ui.components.AvisoInformativo
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.BotonSecundario
import cl.duocuc.induztek.ui.components.CampoSeleccion
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.Cargando
import cl.duocuc.induztek.ui.components.ChipResultado
import cl.duocuc.induztek.ui.components.MostrarMensaje
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaSeccion
import cl.duocuc.induztek.ui.components.coloresResultado
import cl.duocuc.induztek.ui.components.rememberSacudida
import cl.duocuc.induztek.ui.components.sacudir
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.RojoRechazado
import cl.duocuc.induztek.ui.theme.RojoRechazadoFondo
import cl.duocuc.induztek.viewmodel.PruebaFormViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PruebaFormScreen(onVolver: () -> Unit, viewModel: PruebaFormViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.terminado) { if (ui.terminado) onVolver() }
    val sacudida = rememberSacudida(ui.intentosFallidos)
    val p = ui.protocolo

    PantallaBase(
        titulo = ui.tipo.etiqueta,
        subtitulo = p?.let { "${it.tagEquipo} · ${it.numero}" },
        onVolver = onVolver,
        snackbar = snackbar
    ) { padding ->
        if (p == null) { Cargando(Modifier.padding(padding)); return@PantallaBase }
        val fechaPrueba = Formato.aFecha(p.fechaHoraPrueba)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ui.bloqueado) {
                AvisoInformativo("Este protocolo no se puede modificar en su estado actual (RN-17).", advertencia = true)
            }

            TarjetaSeccion("¿Qué se prueba?", icono = R.drawable.ic_rayo) {
                Text("Elemento (devanado, fase o polo)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.elementos.forEach { e ->
                        val registrado = e in ui.elementosRegistrados
                        FilterChip(
                            selected = ui.elemento == e,
                            onClick = { viewModel.onElemento(e) },
                            enabled = !ui.esEdicion,
                            label = { Text(e) },
                            leadingIcon = if (registrado) {
                                { Icon(painterResource(R.drawable.ic_check), contentDescription = "Ya registrado", modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                ui.errorElemento?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (ui.momentos.size > 1) {
                    Text("Momento (Aclaración nº 4: vale la posterior)", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ui.momentos.forEachIndexed { i, m ->
                            SegmentedButton(
                                selected = ui.momento == m,
                                onClick = { viewModel.onMomento(m) },
                                shape = SegmentedButtonDefaults.itemShape(index = i, count = ui.momentos.size),
                                label = { Text(m.etiqueta.substringBefore(" al")) }
                            )
                        }
                    }
                }
            }

            TarjetaSeccion("Instrumento utilizado", icono = R.drawable.ic_herramienta) {
                CampoSeleccion(
                    etiqueta = "Instrumento", opciones = ui.instrumentos, seleccion = ui.instrumento,
                    onSeleccionar = viewModel::onInstrumento, texto = { "${it.nombre} · ${it.modelo}" },
                    icono = R.drawable.ic_herramienta, error = ui.errorInstrumento,
                    detalle = { inst ->
                        val vig = ReglasInstrumento.vigencia(inst.vencimiento, fechaPrueba)
                        "Nº ${inst.numeroSerie} · " + when (vig) {
                            VigenciaInstrumento.VENCIDO -> "VENCIDO el ${inst.vencimiento?.let { Formato.fecha(it) }}"
                            VigenciaInstrumento.SIN_CALIBRACION -> "Sin calibración"
                            else -> "Vigente hasta ${inst.vencimiento?.let { Formato.fecha(it) }}"
                        }
                    }
                )
                ui.instrumento?.let { inst ->
                    Text(
                        "Nº de serie ${inst.numeroSerie} · calibración vigente hasta ${inst.vencimiento?.let { Formato.fecha(it) } ?: "—"}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TarjetaSeccion("Valores medidos", icono = R.drawable.ic_medidor, modifier = Modifier.sacudir(sacudida)) {
                if (ui.tipo == TipoPrueba.OPERACION_RELE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("El relé NO operó (RN-08b → Rechazado)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(checked = ui.noOpera, onCheckedChange = viewModel::onNoOpera, enabled = !ui.bloqueado)
                    }
                }
                ui.parametros.forEachIndexed { i, par ->
                    val campo = ui.valores[par]
                    val giga = ui.enGigaohm[par] == true
                    val deshabilitado = ui.bloqueado || (ui.tipo == TipoPrueba.OPERACION_RELE && ui.noOpera && par == Parametro.TIEMPO_MEDIDO)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CampoTexto(
                            valor = campo?.valor ?: "",
                            onValorCambia = { viewModel.onValor(par, it) },
                            etiqueta = par.etiqueta,
                            icono = R.drawable.ic_medidor,
                            error = campo?.errorVisible,
                            valido = campo?.valido == true,
                            tipoTeclado = if (par.entero) KeyboardType.Number else KeyboardType.Decimal,
                            accionTeclado = if (i == ui.parametros.lastIndex) ImeAction.Done else ImeAction.Next,
                            sufijo = if (par.admiteGigaohm) (if (giga) "GΩ" else "MΩ") else par.unidad.simbolo.ifBlank { null },
                            habilitado = !deshabilitado
                        )
                        if (par.admiteGigaohm) {
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                listOf(false, true).forEachIndexed { idx, esGiga ->
                                    SegmentedButton(
                                        selected = giga == esGiga,
                                        onClick = { viewModel.onUnidadGiga(par, esGiga) },
                                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                                        label = { Text(if (esGiga) "Ingresar en GΩ" else "Ingresar en MΩ") },
                                        enabled = !ui.bloqueado
                                    )
                                }
                            }
                            if (giga) {
                                Validacion(campo?.valor, "Se guardará en MΩ (× 1.000) para evitar errores de unidad (PB-03)")
                            }
                        }
                    }
                }
                if (ui.tipo.resultadoManual) {
                    Text("Resultado (indicado manualmente, RN-08c)", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Resultado.entries.forEachIndexed { i, r ->
                            SegmentedButton(
                                selected = ui.resultadoManual == r,
                                onClick = { viewModel.onResultadoManual(r) },
                                shape = SegmentedButtonDefaults.itemShape(index = i, count = Resultado.entries.size),
                                label = { Text(r.etiqueta) },
                                enabled = !ui.bloqueado
                            )
                        }
                    }
                    ui.errorResultadoManual?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }

            val colores = coloresResultado(ui.evaluacion.resultado)
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = colores.fondo),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(
                        targetState = ui.evaluacion.resultado,
                        transitionSpec = { (scaleIn(tween(250), initialScale = 0.7f) + fadeIn()) togetherWith fadeOut(tween(150)) },
                        label = "resultadoVivo"
                    ) { r ->
                        Icon(
                            painterResource(coloresResultado(r).icono), contentDescription = null,
                            tint = coloresResultado(r).texto, modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            ui.evaluacion.resultado?.etiqueta ?: "Sin resultado aún",
                            style = MaterialTheme.typography.titleLarge, color = colores.texto, fontWeight = FontWeight.Bold
                        )
                        ui.evaluacion.calculo?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = colores.texto) }
                        Text(ui.evaluacion.motivo, style = MaterialTheme.typography.bodySmall, color = colores.texto)
                    }
                }
            }

            TarjetaSeccion("Comparación con la prueba anterior", icono = R.drawable.ic_tendencia) {
                val ant = ui.anterior
                if (ant == null) {
                    Text("Sin historial previo para ${ui.elemento ?: "este elemento"}.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        "Anterior: ${ant.numeroProtocolo} · ${Formato.fechaDesdeMillis(ant.fechaHora)} · ${ant.resultado?.etiqueta ?: ""}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ui.tipo.parametrosPrincipales.forEach { par ->
                        val valorAnterior = ant.valores[par] ?: return@forEach
                        val comp = ui.comparaciones.firstOrNull { it.parametro == par }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(par.etiqueta, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "${Formato.numeroCompacto(valorAnterior)} → ${comp?.let { Formato.numeroCompacto(it.actual) } ?: "…"} ${par.unidad.simbolo}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (comp != null) {
                                Text(
                                    Formato.porcentajeConSigno(comp.variacionPct),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (comp.significativa) RojoRechazado else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = ui.hayVariacionSignificativa) {
                        Card(colors = CardDefaults.cardColors(containerColor = RojoRechazadoFondo), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Variación significativa (> 25 %): quedará destacada y el ingeniero deberá comentarla (RN-09).",
                                color = RojoRechazado, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            CampoTexto(
                ui.comentario.valor, viewModel::onComentario, "Comentario (opcional)", R.drawable.ic_editar,
                error = ui.comentario.errorVisible, lineas = 2, habilitado = !ui.bloqueado
            )

            if (!ui.bloqueado) {
                val quedan = ui.elementos.any { it !in ui.elementosRegistrados && it != ui.elemento }
                if (!ui.esEdicion && quedan) {
                    BotonPrincipal(
                        "Guardar y siguiente elemento", { viewModel.guardar(continuar = true) }, Modifier.fillMaxWidth(),
                        icono = R.drawable.ic_flecha_derecha, cargando = ui.guardando
                    )
                    BotonSecundario("Guardar y volver", { viewModel.guardar(continuar = false) }, Modifier.fillMaxWidth(), icono = R.drawable.ic_check)
                } else {
                    BotonPrincipal(
                        if (ui.esEdicion) "Guardar cambios" else "Guardar prueba", { viewModel.guardar(continuar = false) },
                        Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Validacion(valor: String?, texto: String) {
    if (valor.isNullOrBlank()) return
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
}
