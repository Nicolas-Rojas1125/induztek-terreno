package cl.duocuc.induztek.ui.screens.protocolos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.Momento
import cl.duocuc.induztek.domain.model.Prueba
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.reglas.AccionProtocolo
import cl.duocuc.induztek.domain.reglas.EvaluadorPruebas
import cl.duocuc.induztek.domain.reglas.VisibilidadCliente
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.ui.components.AvisoInformativo
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.BotonSecundario
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.Cargando
import cl.duocuc.induztek.ui.components.ChipEstado
import cl.duocuc.induztek.ui.components.ChipResultado
import cl.duocuc.induztek.ui.components.EstadoVacio
import cl.duocuc.induztek.ui.components.FilaDato
import cl.duocuc.induztek.ui.components.MostrarMensaje
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaErrores
import cl.duocuc.induztek.ui.components.TarjetaSeccion
import cl.duocuc.induztek.ui.components.coloresEstado
import cl.duocuc.induztek.ui.components.rememberPulso
import cl.duocuc.induztek.ui.components.rememberSacudida
import cl.duocuc.induztek.ui.components.sacudir
import cl.duocuc.induztek.ui.theme.AmbarObservado
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.MoradoFormalizado
import cl.duocuc.induztek.ui.theme.RojoRechazado
import cl.duocuc.induztek.ui.theme.RojoRechazadoFondo
import cl.duocuc.induztek.viewmodel.ProtocoloUiState
import cl.duocuc.induztek.viewmodel.ProtocoloViewModel

@Composable
fun ProtocoloScreen(
    onVolver: () -> Unit,
    onPrueba: (protocoloId: Long, tipo: TipoPrueba, pruebaId: Long?) -> Unit,
    onProtocolo: (Long) -> Unit,
    onEquipo: (Long) -> Unit,
    viewModel: ProtocoloViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.nuevoProtocoloId) {
        ui.nuevoProtocoloId?.let { onProtocolo(it); viewModel.onNavegadoANuevo() }
    }
    val sacudida = rememberSacudida(ui.intentosFallidos)
    var pruebaAEliminar by remember { mutableStateOf<Prueba?>(null) }
    val p = ui.protocolo
    val s = ui.sesion

    ui.accionPendiente?.let { accion ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarAccion,
            icon = {
                Icon(
                    painterResource(coloresEstado(accion.destino).icono), contentDescription = null,
                    tint = coloresEstado(accion.destino).texto
                )
            },
            title = { Text(accion.etiqueta) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (accion) {
                            AccionProtocolo.REGISTRAR_TERRENO -> "Se validarán las condiciones ambientales y las pruebas del equipo. Luego podrá cerrar la visita."
                            AccionProtocolo.FORMALIZAR -> "El protocolo pasará a ser el documento oficial, con su nombre y la fecha, y ya no se podrá modificar."
                            AccionProtocolo.OBSERVAR -> "El protocolo vuelve al técnico para su corrección. Indique el motivo."
                            AccionProtocolo.ANULAR -> "El protocolo quedará anulado en el historial. Indique el motivo."
                        }
                    )
                    if (accion.requiereMotivo) {
                        CampoTexto(
                            ui.motivo.valor, viewModel::onMotivo, "Motivo (obligatorio)", R.drawable.ic_editar,
                            error = ui.motivo.errorVisible, valido = ui.motivo.valido, lineas = 3
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmarAccion,
                    enabled = !accion.requiereMotivo || ui.motivo.valido
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelarAccion) { Text("Cancelar") } }
        )
    }

    ui.tipoNoRealizada?.let { tipo ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarNoRealizada,
            icon = { Icon(painterResource(R.drawable.ic_bloqueo), contentDescription = null) },
            title = { Text("${tipo.etiqueta}: No realizada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No cuenta para el resultado global, pero aparece en el acta (Aclaración nº 2). Reemplaza los registros de esta prueba.")
                    CampoTexto(
                        ui.motivoNoRealizada.valor, viewModel::onMotivoNoRealizada, "Motivo", R.drawable.ic_editar,
                        error = ui.motivoNoRealizada.errorVisible, valido = ui.motivoNoRealizada.valido,
                        ayuda = "Ej.: cliente no autorizó desconexión", lineas = 2
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmarNoRealizada) { Text("Marcar No realizada") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarNoRealizada) { Text("Cancelar") } }
        )
    }

    pruebaAEliminar?.let { pr ->
        AlertDialog(
            onDismissRequest = { pruebaAEliminar = null },
            title = { Text("¿Eliminar registro?") },
            text = { Text("${pr.tipo.etiqueta} · ${pr.elemento.ifBlank { "No realizada" }}") },
            confirmButton = {
                TextButton(onClick = { viewModel.eliminarPrueba(pr); pruebaAEliminar = null }) { Text("Eliminar", color = RojoRechazado) }
            },
            dismissButton = { TextButton(onClick = { pruebaAEliminar = null }) { Text("Cancelar") } }
        )
    }

    PantallaBase(
        titulo = p?.numero ?: "Protocolo",
        subtitulo = p?.let { "${it.tagEquipo} · ${it.nombreInstalacion}" },
        onVolver = onVolver,
        snackbar = snackbar,
        acciones = {
            if (p != null) {
                IconButton(onClick = { onEquipo(p.idEquipo) }) {
                    Icon(painterResource(R.drawable.ic_historial), contentDescription = "Historial del equipo")
                }
            }
        }
    ) { padding ->
        if (ui.cargando) { Cargando(Modifier.padding(padding)); return@PantallaBase }
        if (p == null || s == null) {
            EstadoVacio(R.drawable.ic_bloqueo, "Protocolo no disponible", "No existe o no tiene acceso (RN-18 / RN-19).", Modifier.padding(padding))
            return@PantallaBase
        }
        val editable = ui.puedeEditar
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Cabecera(ui) }

            item {
                TarjetaSeccion("Condiciones de la prueba", icono = R.drawable.ic_termometro) {
                    if (editable) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CampoTexto(
                                ui.temperatura.valor, viewModel::onTemperatura, "Temperatura", R.drawable.ic_termometro,
                                modifier = Modifier.weight(1f), error = ui.temperatura.errorVisible, valido = ui.temperatura.valido,
                                tipoTeclado = KeyboardType.Decimal, sufijo = "°C"
                            )
                            CampoTexto(
                                ui.humedad.valor, viewModel::onHumedad, "Humedad", R.drawable.ic_gota,
                                modifier = Modifier.weight(1f), error = ui.humedad.errorVisible, valido = ui.humedad.valido,
                                tipoTeclado = KeyboardType.Number, sufijo = "%"
                            )
                        }
                        Text(
                            "Obligatorias para pasar a Registrado en terreno (HU-02)",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (viewModel.visitaConMantenimiento()) {
                            CampoTexto(
                                ui.mantenimiento.valor, viewModel::onMantenimiento, "Mantenimiento realizado (Aclaración nº 4)",
                                R.drawable.ic_herramienta, error = ui.mantenimiento.errorVisible, lineas = 2
                            )
                        }
                        CampoTexto(
                            ui.observaciones.valor, viewModel::onObservaciones, "Observaciones del equipo (RF-17)", R.drawable.ic_info,
                            error = ui.observaciones.errorVisible, ayuda = "Ej.: fuga de aceite leve en válvula inferior", lineas = 2
                        )
                        AnimatedVisibility(visible = ui.cabeceraModificada) {
                            BotonSecundario("Guardar datos", viewModel::guardarCabecera, Modifier.fillMaxWidth(), icono = R.drawable.ic_check)
                        }
                    } else {
                        FilaDato("Temperatura", p.temperaturaC?.let { "${Formato.numero(it, 1)} °C" } ?: "", icono = R.drawable.ic_termometro)
                        FilaDato("Humedad relativa", p.humedadPct?.let { "${Formato.numero(it, 0)} %" } ?: "", icono = R.drawable.ic_gota)
                        if (p.descripcionMantenimiento.isNotBlank()) FilaDato("Mantenimiento", p.descripcionMantenimiento, icono = R.drawable.ic_herramienta)
                        if (p.observaciones.isNotBlank()) FilaDato("Observaciones", p.observaciones, icono = R.drawable.ic_info)
                    }
                }
            }

            items(p.tipoEquipo.pruebas, key = { it.name }) { tipo ->
                val lista = ui.pruebas.filter { it.tipo == tipo }
                val comparacionesTipo = ui.comparaciones.filter { it.prueba.tipo == tipo }
                TarjetaSeccion(
                    titulo = tipo.etiqueta,
                    icono = R.drawable.ic_medidor,
                    accion = { ChipResultado(viewModel.resultadoDeTipo(tipo), textoNulo = viewModel.resumenTipo(tipo)) }
                ) {
                    Text(tipo.descripcionCorta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (lista.isEmpty()) {
                        AvisoInformativo("Pendiente: registre las mediciones o márquela como No realizada.", advertencia = true)
                    }
                    lista.forEach { pr ->
                        val comp = comparacionesTipo.firstOrNull { it.prueba.id == pr.id }
                        FilaPrueba(
                            prueba = pr,
                            tipoEquipo = p.tipoEquipo,
                            maxContactos = p.maxContactosUohm,
                            variacionSignificativa = comp?.significativa == true,
                            textoComparacion = comp?.let { c ->
                                if (c.anterior == null) "Sin historial previo"
                                else c.comparaciones.joinToString(" · ") { "${it.parametro.etiqueta} ${Formato.porcentajeConSigno(it.variacionPct)} vs ${it.numeroProtocoloAnterior}" }
                                    .ifBlank { "Anterior: ${c.anterior.numeroProtocolo}" }
                            },
                            editable = editable,
                            onEditar = { onPrueba(p.id, tipo, pr.id) },
                            onEliminar = { pruebaAEliminar = pr }
                        )
                    }
                    if (editable) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BotonPrincipal("Registrar", { onPrueba(p.id, tipo, null) }, Modifier.weight(1f), icono = R.drawable.ic_agregar)
                            BotonSecundario("No realizada", { viewModel.pedirNoRealizada(tipo) }, Modifier.weight(1f), icono = R.drawable.ic_bloqueo, color = AmbarObservado)
                        }
                    }
                }
            }

            if (ui.hayVariacionSignificativa && !s.esCliente) {
                item {
                    val pulso = rememberPulso(p.estado == EstadoProtocolo.REGISTRADO_TERRENO && ui.comentarioVariacion.valor.isBlank())
                    Card(
                        modifier = Modifier.fillMaxWidth().scale(pulso),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = RojoRechazadoFondo)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.ic_tendencia), contentDescription = null, tint = RojoRechazado)
                                Spacer(Modifier.width(8.dp))
                                Text("Variación significativa (> 25 %)", style = MaterialTheme.typography.titleSmall, color = RojoRechazado)
                            }
                            ui.comparaciones.filter { it.significativa }.forEach { cp ->
                                cp.comparaciones.filter { it.significativa }.forEach { c ->
                                    Text(
                                        "${cp.prueba.tipo.etiqueta} ${cp.prueba.elemento} · ${c.parametro.etiqueta}: " +
                                            "${Formato.numeroCompacto(c.anterior)} → ${Formato.numeroCompacto(c.actual)} ${c.parametro.unidad.simbolo} " +
                                            "(${Formato.porcentajeConSigno(c.variacionPct)} vs ${c.numeroProtocoloAnterior})",
                                        style = MaterialTheme.typography.bodyMedium, color = RojoRechazado
                                    )
                                }
                            }
                            Text(
                                "No cambia el resultado, pero el ingeniero debe comentar antes de formalizar (RN-09).",
                                style = MaterialTheme.typography.bodySmall, color = RojoRechazado
                            )
                            if (ui.puedeComentarVariacion) {
                                CampoTexto(
                                    ui.comentarioVariacion.valor, viewModel::onComentarioVariacion, "Comentario del ingeniero",
                                    R.drawable.ic_editar, error = ui.comentarioVariacion.errorVisible, lineas = 2
                                )
                            } else if (p.comentarioVariacion.isNotBlank()) {
                                FilaDato("Comentario", p.comentarioVariacion)
                            }
                        }
                    }
                }
            }

            item {
                Column(Modifier.sacudir(sacudida).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TarjetaErrores(if (ui.erroresTransicion.isEmpty()) null else "No se puede continuar", ui.erroresTransicion)
                    ui.acciones.forEach { accion ->
                        when (accion) {
                            AccionProtocolo.REGISTRAR_TERRENO, AccionProtocolo.FORMALIZAR -> BotonPrincipal(
                                accion.etiqueta, { viewModel.pedirAccion(accion) }, Modifier.fillMaxWidth(),
                                icono = if (accion == AccionProtocolo.FORMALIZAR) R.drawable.ic_verificado else R.drawable.ic_enviar,
                                cargando = ui.procesando,
                                colorFondo = if (accion == AccionProtocolo.FORMALIZAR) MoradoFormalizado else MaterialTheme.colorScheme.primary
                            )
                            AccionProtocolo.OBSERVAR -> BotonSecundario(
                                accion.etiqueta, { viewModel.pedirAccion(accion) }, Modifier.fillMaxWidth(),
                                icono = R.drawable.ic_advertencia, color = AmbarObservado
                            )
                            AccionProtocolo.ANULAR -> BotonSecundario(
                                accion.etiqueta, { viewModel.pedirAccion(accion) }, Modifier.fillMaxWidth(),
                                icono = R.drawable.ic_bloqueo, color = RojoRechazado
                            )
                        }
                    }
                    if (ui.puedeReemplazar) {
                        AvisoInformativo("Formalizado: no se modifica. Si hay un error, emita un protocolo de reemplazo (RN-04).")
                        BotonSecundario("Emitir protocolo de reemplazo", viewModel::emitirReemplazo, Modifier.fillMaxWidth(), icono = R.drawable.ic_documento)
                    }
                    if (!editable && !s.esCliente && (p.estado == EstadoProtocolo.BORRADOR || p.estado == EstadoProtocolo.OBSERVADO)) {
                        AvisoInformativo("Solo el técnico que creó este protocolo puede modificarlo (RN-17).", advertencia = true)
                    }
                }
            }

            if (ui.historial.isNotEmpty()) {
                item {
                    TarjetaSeccion("Trazabilidad (quién y cuándo)", icono = R.drawable.ic_historial) {
                        ui.historial.forEachIndexed { i, h ->
                            Row {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(shape = CircleShape, color = coloresEstado(h.estadoNuevo).texto, modifier = Modifier.size(12.dp)) {}
                                    if (i < ui.historial.lastIndex) {
                                        Box(
                                            Modifier
                                                .width(2.dp)
                                                .height(44.dp)
                                                .padding(top = 2.dp)
                                        ) { Surface(color = MaterialTheme.colorScheme.outline, modifier = Modifier.fillMaxSize()) {} }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.padding(bottom = 8.dp)) {
                                    Text(
                                        (h.estadoAnterior?.let { "${it.etiqueta} → " } ?: "Creado: ") + h.estadoNuevo.etiqueta,
                                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold
                                    )
                                    Text("${h.nombreUsuario} · ${Formato.fechaHora(h.fechaHora)}", style = MaterialTheme.typography.bodySmall)
                                    if (h.motivo.isNotBlank()) {
                                        Text("“${h.motivo}”", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Cabecera(ui: ProtocoloUiState) {
    val p = ui.protocolo ?: return
    val s = ui.sesion ?: return
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${p.tagEquipo} · ${p.tipoEquipo.etiqueta}", style = MaterialTheme.typography.titleMedium)
                    Text("${p.nombreCliente} · OT ${p.numeroOt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ChipEstado(p.estado, etiquetaCliente = if (s.esCliente) VisibilidadCliente.etiquetaPara(p.estado) else null)
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Resultado global (RN-10)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                ChipResultado(EvaluadorPruebas.resultadoGlobal(ui.pruebas) ?: p.resultadoGlobal, grande = true)
            }
            FilaDato("Técnico", p.nombreTecnico, icono = R.drawable.ic_persona)
            FilaDato("Fecha y hora", Formato.fechaHora(p.fechaHoraPrueba), icono = R.drawable.ic_calendario)
            FilaDato("Visita", "${p.tipoVisita.etiqueta}" + if (p.visitaCerrada) " · cerrada" else " · abierta", icono = R.drawable.ic_documento)
            p.numeroReemplazado?.let { FilaDato("Reemplaza a", "nº $it", icono = R.drawable.ic_info) }
            p.reemplazadoPor?.let {
                AvisoInformativo("Reemplazado por nº $it", advertencia = true)
            }
            if (p.formalizadoPor != null && p.fechaFormalizacion != null) {
                FilaDato("Formalizado por", "${p.formalizadoPor} · ${Formato.fechaHora(p.fechaFormalizacion)}", icono = R.drawable.ic_verificado)
            }
            if (ui.atrasado && !s.esCliente) {
                AvisoInformativo("Atrasado: supera la meta de 2 días hábiles para formalizar (RN-16)", advertencia = true)
            }
            if (s.esCliente && p.estado != EstadoProtocolo.FORMALIZADO) {
                AvisoInformativo("Este protocolo aún no ha sido revisado por un ingeniero: ${VisibilidadCliente.PENDIENTE.lowercase()}.")
            }
        }
    }
}

@Composable
private fun FilaPrueba(
    prueba: Prueba,
    tipoEquipo: TipoEquipo,
    maxContactos: Double?,
    variacionSignificativa: Boolean,
    textoComparacion: String?,
    editable: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (variacionSignificativa) {
                Box(Modifier.width(4.dp).height(48.dp)) { Surface(color = RojoRechazado, modifier = Modifier.fillMaxHeight().fillMaxWidth()) {} }
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!prueba.realizada) {
                    Text("No realizada", style = MaterialTheme.typography.titleSmall, color = AmbarObservado)
                    Text(prueba.motivoNoRealizada, style = MaterialTheme.typography.bodySmall)
                } else {
                    val eval = EvaluadorPruebas.evaluar(prueba.tipo, tipoEquipo, prueba.valores, prueba.noOpera, maxContactos, prueba.resultado)
                    Text(
                        prueba.elemento + if (prueba.momento != Momento.UNICA) " · ${prueba.momento.etiqueta}" else "",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        prueba.mediciones.joinToString("  ·  ") {
                            "${it.parametro.etiqueta}: ${Formato.numeroCompacto(it.valor)} ${it.parametro.unidad.simbolo}".trimEnd()
                        } + if (prueba.noOpera) "  ·  NO OPERÓ" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                    eval.calculo?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                    if (prueba.nombreInstrumento.isNotBlank()) {
                        Text("Instrumento: ${prueba.nombreInstrumento}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Sin instrumento registrado (RF-05)", style = MaterialTheme.typography.labelSmall, color = RojoRechazado)
                    }
                    textoComparacion?.let {
                        Text(
                            it, style = MaterialTheme.typography.labelSmall,
                            color = if (variacionSignificativa) RojoRechazado else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (prueba.realizada) ChipResultado(prueba.resultado)
            if (editable) {
                if (prueba.realizada) {
                    IconButton(onClick = onEditar) { Icon(painterResource(R.drawable.ic_editar), contentDescription = "Editar") }
                }
                IconButton(onClick = onEliminar) { Icon(painterResource(R.drawable.ic_eliminar), contentDescription = "Eliminar", tint = RojoRechazado) }
            }
        }
    }
}
