package cl.duocuc.induztek.ui.screens.visitas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.TipoVisita
import cl.duocuc.induztek.domain.reglas.VisibilidadCliente
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.ui.components.AparicionEscalonada
import cl.duocuc.induztek.ui.components.AvisoInformativo
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.BotonSecundario
import cl.duocuc.induztek.ui.components.CampoFecha
import cl.duocuc.induztek.ui.components.CampoSeleccion
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.Cargando
import cl.duocuc.induztek.ui.components.ChipEstado
import cl.duocuc.induztek.ui.components.ChipResultado
import cl.duocuc.induztek.ui.components.EstadoVacio
import cl.duocuc.induztek.ui.components.FilaDato
import cl.duocuc.induztek.ui.components.MostrarMensaje
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaErrores
import cl.duocuc.induztek.ui.components.TarjetaIndicador
import cl.duocuc.induztek.ui.components.TarjetaLista
import cl.duocuc.induztek.ui.components.TarjetaSeccion
import cl.duocuc.induztek.ui.components.rememberSacudida
import cl.duocuc.induztek.ui.components.sacudir
import cl.duocuc.induztek.ui.theme.AmbarObservado
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.RojoRechazado
import cl.duocuc.induztek.ui.theme.VerdeAprobado
import cl.duocuc.induztek.viewmodel.FiltroVisitas
import cl.duocuc.induztek.viewmodel.RecepcionViewModel
import cl.duocuc.induztek.viewmodel.VisitaDetalleViewModel
import cl.duocuc.induztek.viewmodel.VisitaFormViewModel
import cl.duocuc.induztek.viewmodel.VisitasViewModel

@Composable
fun VisitasScreen(
    barraInferior: @Composable () -> Unit,
    onVisita: (Long) -> Unit,
    onNueva: () -> Unit,
    viewModel: VisitasViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    PantallaBase(
        titulo = if (ui.esCliente) "Actas de visita" else "Visitas",
        subtitulo = if (ui.esCliente) "Respaldo entregado en terreno (RF-15)" else "Órdenes de trabajo en terreno",
        barraInferior = barraInferior,
        botonFlotante = {
            if (ui.puedeCrear) {
                ExtendedFloatingActionButton(
                    onClick = onNueva,
                    icon = { Icon(painterResource(R.drawable.ic_agregar), contentDescription = null) },
                    text = { Text("Nueva visita") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!ui.esCliente) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FiltroVisitas.entries.forEach { f ->
                            FilterChip(selected = ui.filtro == f, onClick = { viewModel.onFiltro(f) }, label = { Text(f.etiqueta) })
                        }
                    }
                }
            }
            if (!ui.cargando && ui.visitas.isEmpty()) {
                item {
                    EstadoVacio(
                        R.drawable.ic_calendario, if (ui.esCliente) "Sin actas" else "Sin visitas",
                        if (ui.esCliente) "Aún no hay visitas cerradas para su empresa." else "Cree una visita con el número de OT."
                    )
                }
            }
            items(ui.visitas, key = { it.id }) { v ->
                AparicionEscalonada(ui.visitas.indexOf(v), Modifier.animateItem()) {
                    TarjetaLista(
                        titulo = "${v.numeroOt} · ${v.nombreInstalacion}",
                        subtitulo = "${v.nombreCliente} · ${v.tipo.etiqueta}",
                        detalle = "${Formato.fecha(v.fecha)} · ${v.cantidadProtocolos} protocolo(s) · Ing. ${v.nombreIngeniero}",
                        icono = if (v.cerrada) R.drawable.ic_verificado else R.drawable.ic_calendario,
                        colorIcono = if (v.cerrada) VerdeAprobado else MaterialTheme.colorScheme.primary,
                        onClick = { onVisita(v.id) },
                        pie = {
                            Text(
                                if (v.cerrada) "Cerrada ${v.fechaHoraCierre?.let { Formato.fechaHora(it) }.orEmpty()}" else "Abierta",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (v.cerrada) VerdeAprobado else AmbarObservado
                            )
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitaFormScreen(onVolver: () -> Unit, onGuardado: (Long) -> Unit, viewModel: VisitaFormViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.guardadoId) { ui.guardadoId?.let(onGuardado) }
    val sacudida = rememberSacudida(ui.intentosFallidos)

    PantallaBase(titulo = "Nueva visita", subtitulo = "La OT se emite como hoy; aquí se registra su número (FA-02)", onVolver = onVolver, snackbar = snackbar) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaSeccion("Orden de trabajo", icono = R.drawable.ic_numero, modifier = Modifier.sacudir(sacudida)) {
                CampoTexto(ui.ot.valor, viewModel::onOt, "Número de OT", R.drawable.ic_numero,
                    error = ui.ot.errorVisible, valido = ui.ot.valido, ayuda = "Formato OT-AAAA-NNNN",
                    capitalizacion = KeyboardCapitalization.Characters)
                CampoSeleccion(
                    etiqueta = "Instalación del cliente", opciones = ui.instalaciones, seleccion = ui.instalacion,
                    onSeleccionar = viewModel::onInstalacion, texto = { "${it.nombre} · ${it.nombreCliente}" },
                    icono = R.drawable.ic_ubicacion, error = ui.errorInstalacion,
                    detalle = { it.observacionesAcceso.ifBlank { null } }
                )
                CampoFecha("Fecha de la visita", ui.fecha, viewModel::onFecha, error = ui.errorFecha)
                Text("Tipo de visita (Aclaración nº 4)", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    TipoVisita.entries.forEachIndexed { i, t ->
                        SegmentedButton(
                            selected = ui.tipo == t,
                            onClick = { viewModel.onTipo(t) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = TipoVisita.entries.size),
                            label = { Text(t.etiqueta) }
                        )
                    }
                }
            }
            TarjetaSeccion("Cuadrilla", icono = R.drawable.ic_grupo) {
                CampoSeleccion(
                    etiqueta = "Ingeniero a cargo", opciones = ui.ingenieros, seleccion = ui.ingeniero,
                    onSeleccionar = viewModel::onIngeniero, texto = { it.nombre }, icono = R.drawable.ic_persona,
                    error = ui.errorIngeniero, ayuda = "Obligatorio aunque no asista (Aclaración nº 7)"
                )
                Text("Participantes en terreno", style = MaterialTheme.typography.labelLarge)
                ui.personal.forEach { p ->
                    val marcado = p.id in ui.participantes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { viewModel.onParticipante(p.id, !marcado) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = marcado, onCheckedChange = { viewModel.onParticipante(p.id, it) })
                        Column {
                            Text(p.nombre, style = MaterialTheme.typography.bodyLarge)
                            Text(p.rol.etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                ui.errorParticipantes?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            BotonPrincipal("Crear visita", viewModel::guardar, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun VisitaDetalleScreen(
    onVolver: () -> Unit,
    onProtocolo: (Long) -> Unit,
    onNuevoEquipo: (Long) -> Unit,
    onRecepcion: (Long) -> Unit,
    onActa: (Long) -> Unit,
    viewModel: VisitaDetalleViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.protocoloCreadoId) {
        ui.protocoloCreadoId?.let { onProtocolo(it); viewModel.onNavegadoAProtocolo() }
    }
    var dialogoEquipos by remember { mutableStateOf(false) }
    var confirmarCierre by remember { mutableStateOf(false) }
    val sacudida = rememberSacudida(ui.intentosFallidos)
    val v = ui.visita
    val s = ui.sesion

    if (dialogoEquipos && v != null) {
        AlertDialog(
            onDismissRequest = { dialogoEquipos = false },
            title = { Text("Iniciar protocolo") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Un protocolo por equipo en esta visita (Aclaración nº 5).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (ui.equiposDisponibles.isEmpty()) {
                        Text("Todos los equipos registrados ya tienen protocolo.", modifier = Modifier.padding(vertical = 8.dp))
                    }
                    ui.equiposDisponibles.forEach { e ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable {
                                    dialogoEquipos = false
                                    viewModel.iniciarProtocolo(e)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(R.drawable.ic_rayo), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(12.dp))
                            Column {
                                Text("${e.tag} · ${e.tipo.etiqueta}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("Nº ${e.numeroSerie}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider()
                    }
                    TextButton(onClick = {
                        dialogoEquipos = false
                        onNuevoEquipo(v.idInstalacion)
                    }) { Text("+ Registrar equipo nuevo (HU-01)") }
                }
            },
            confirmButton = { TextButton(onClick = { dialogoEquipos = false }) { Text("Cerrar") } }
        )
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            icon = { Icon(painterResource(R.drawable.ic_verificado), contentDescription = null) },
            title = { Text("¿Cerrar la visita?") },
            text = {
                Text(
                    "Se generará el Acta de prueba en terreno marcada \"Pendiente de formalización\". " +
                        "Si el GPS está activo, se registrarán las coordenadas del cierre. Funciona sin conexión."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmarCierre = false; viewModel.cerrarVisita() }) { Text("Cerrar visita") }
            },
            dismissButton = { TextButton(onClick = { confirmarCierre = false }) { Text("Cancelar") } }
        )
    }

    PantallaBase(
        titulo = v?.numeroOt ?: "Visita",
        subtitulo = v?.let { "${it.nombreInstalacion} · ${it.nombreCliente}" },
        onVolver = onVolver,
        snackbar = snackbar,
        botonFlotante = {
            if (ui.puedeOperar) {
                ExtendedFloatingActionButton(
                    onClick = { dialogoEquipos = true },
                    icon = { Icon(painterResource(R.drawable.ic_agregar), contentDescription = null) },
                    text = { Text(if (ui.creandoProtocolo) "Creando…" else "Agregar equipo") }
                )
            }
        }
    ) { padding ->
        if (ui.cargando) { Cargando(Modifier.padding(padding)); return@PantallaBase }
        if (v == null || s == null) {
            EstadoVacio(R.drawable.ic_bloqueo, "Visita no disponible", "No existe o no tiene acceso.", Modifier.padding(padding))
            return@PantallaBase
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                TarjetaSeccion("Datos de la visita", icono = R.drawable.ic_calendario) {
                    FilaDato("Fecha", Formato.fecha(v.fecha))
                    FilaDato("Tipo", v.tipo.etiqueta)
                    FilaDato("Ingeniero a cargo", v.nombreIngeniero, icono = R.drawable.ic_persona)
                    FilaDato("Cuadrilla", v.participantes.joinToString { it.nombre }, icono = R.drawable.ic_grupo)
                    FilaDato("Estado", if (v.cerrada) "Cerrada · ${Formato.fechaHora(v.fechaHoraCierre!!)}" else "Abierta")
                    if (v.latitudCierre != null && v.longitudCierre != null) {
                        FilaDato("GPS del cierre", Formato.coordenadas(v.latitudCierre, v.longitudCierre), icono = R.drawable.ic_ubicacion)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TarjetaIndicador(ui.aprobados, "Aprobados", R.drawable.ic_check, Modifier.weight(1f), colorAcento = VerdeAprobado)
                    TarjetaIndicador(ui.observados, "Observados", R.drawable.ic_advertencia, Modifier.weight(1f), colorAcento = AmbarObservado)
                    TarjetaIndicador(ui.rechazados, "Rechazados", R.drawable.ic_error, Modifier.weight(1f), colorAcento = RojoRechazado)
                }
            }
            item { Text("Protocolos (${ui.protocolos.size})", style = MaterialTheme.typography.titleMedium) }
            if (ui.protocolos.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_documento, "Sin protocolos", "Toque \"Agregar equipo\" para iniciar el primero.") }
            }
            items(ui.protocolos, key = { it.id }) { p ->
                TarjetaLista(
                    titulo = "${p.tagEquipo} · ${p.tipoEquipo.etiqueta}",
                    subtitulo = "${p.numero} · ${p.nombreTecnico}",
                    icono = R.drawable.ic_documento,
                    onClick = { onProtocolo(p.id) },
                    pie = {
                        ChipEstado(p.estado, etiquetaCliente = if (s.esCliente) VisibilidadCliente.etiquetaPara(p.estado) else null)
                        ChipResultado(p.resultadoGlobal)
                    }
                )
            }

            item {
                Column(Modifier.sacudir(sacudida), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TarjetaErrores(ui.tituloErrores, ui.erroresCierre)
                    if (ui.puedeCerrar) {
                        BotonPrincipal(
                            "Cerrar visita y generar acta", { confirmarCierre = true }, Modifier.fillMaxWidth(),
                            icono = R.drawable.ic_verificado, cargando = ui.cerrando
                        )
                    }
                    if (v.cerrada) {
                        AvisoInformativo("Acta generada: \"Pendiente de formalización\" (RN-14).")
                        BotonPrincipal("Ver y compartir el acta", { onActa(v.id) }, Modifier.fillMaxWidth(), icono = R.drawable.ic_compartir)
                        val rec = ui.recepcion
                        if (rec == null) {
                            if (!s.esCliente && s.puede(Permiso.CERRAR_VISITA)) {
                                BotonSecundario("Registrar quién recibe el acta", { onRecepcion(v.id) }, Modifier.fillMaxWidth(), icono = R.drawable.ic_credencial)
                            }
                        } else {
                            TarjetaSeccion("Recepción del acta (RF-08)", icono = R.drawable.ic_credencial) {
                                FilaDato("Recibió", rec.receptorNombre)
                                FilaDato("RUT", rec.receptorRut)
                                FilaDato("Cargo", rec.receptorCargo)
                                FilaDato("Fecha y hora", Formato.fechaHora(rec.fechaHoraRecepcion))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun RecepcionScreen(onVolver: () -> Unit, onGuardado: () -> Unit, viewModel: RecepcionViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.guardado) { if (ui.guardado) onGuardado() }
    val sacudida = rememberSacudida(ui.intentosFallidos)

    PantallaBase(titulo = "Recepción del acta", subtitulo = ui.visita?.numeroOt, onVolver = onVolver, snackbar = snackbar) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvisoInformativo("Constancia de quién recibió el respaldo por parte del cliente (RF-08, HU-08). Queda la fecha y hora actual.")
            TarjetaSeccion("Persona que recibe", icono = R.drawable.ic_credencial, modifier = Modifier.sacudir(sacudida)) {
                CampoTexto(ui.nombre.valor, viewModel::onNombre, "Nombre completo", R.drawable.ic_persona,
                    error = ui.nombre.errorVisible, valido = ui.nombre.valido, capitalizacion = KeyboardCapitalization.Words)
                CampoTexto(ui.rut.valor, viewModel::onRut, "RUT", R.drawable.ic_credencial,
                    error = ui.rut.errorVisible, valido = ui.rut.valido, capitalizacion = KeyboardCapitalization.Characters,
                    ayuda = "Se valida el dígito verificador (RN-03)")
                CampoTexto(ui.cargo.valor, viewModel::onCargo, "Cargo", R.drawable.ic_etiqueta,
                    error = ui.cargo.errorVisible, valido = ui.cargo.valido, accionTeclado = ImeAction.Done)
                FilaDato("Fecha y hora", Formato.fechaHora(ui.fechaHora), icono = R.drawable.ic_calendario)
            }
            BotonPrincipal("Registrar recepción", viewModel::guardar, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando)
        }
    }
}
