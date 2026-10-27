package cl.duocuc.induztek.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.CategoriaInstrumento
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.reglas.ReglasInstrumento
import cl.duocuc.induztek.domain.reglas.VigenciaInstrumento
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.ui.components.AparicionEscalonada
import cl.duocuc.induztek.ui.components.AvisoInformativo
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.BotonSecundario
import cl.duocuc.induztek.ui.components.CampoContrasena
import cl.duocuc.induztek.ui.components.CampoFecha
import cl.duocuc.induztek.ui.components.CampoSeleccion
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.Cargando
import cl.duocuc.induztek.ui.components.Chip
import cl.duocuc.induztek.ui.components.ColoresChip
import cl.duocuc.induztek.ui.components.EstadoVacio
import cl.duocuc.induztek.ui.components.FilaDato
import cl.duocuc.induztek.ui.components.MostrarMensaje
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaLista
import cl.duocuc.induztek.ui.components.TarjetaSeccion
import cl.duocuc.induztek.ui.components.rememberSacudida
import cl.duocuc.induztek.ui.components.sacudir
import cl.duocuc.induztek.ui.theme.AmbarObservado
import cl.duocuc.induztek.ui.theme.AmbarObservadoFondo
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.GrisNeutro
import cl.duocuc.induztek.ui.theme.GrisNeutroFondo
import cl.duocuc.induztek.ui.theme.RojoRechazado
import cl.duocuc.induztek.ui.theme.RojoRechazadoFondo
import cl.duocuc.induztek.ui.theme.VerdeAprobado
import cl.duocuc.induztek.ui.theme.VerdeAprobadoFondo
import cl.duocuc.induztek.viewmodel.InstrumentoDetalleViewModel
import cl.duocuc.induztek.viewmodel.InstrumentoFormViewModel
import cl.duocuc.induztek.viewmodel.InstrumentosViewModel
import cl.duocuc.induztek.viewmodel.UsuarioFormViewModel
import cl.duocuc.induztek.viewmodel.UsuariosViewModel

private fun chipVigencia(v: VigenciaInstrumento): ColoresChip = when (v) {
    VigenciaInstrumento.VIGENTE -> ColoresChip(VerdeAprobado, VerdeAprobadoFondo, R.drawable.ic_check)
    VigenciaInstrumento.POR_VENCER -> ColoresChip(AmbarObservado, AmbarObservadoFondo, R.drawable.ic_advertencia)
    VigenciaInstrumento.VENCIDO -> ColoresChip(RojoRechazado, RojoRechazadoFondo, R.drawable.ic_error)
    VigenciaInstrumento.SIN_CALIBRACION -> ColoresChip(GrisNeutro, GrisNeutroFondo, R.drawable.ic_info)
}

@Composable
fun InstrumentosScreen(
    onVolver: () -> Unit,
    onInstrumento: (Long) -> Unit,
    onNuevo: () -> Unit,
    viewModel: InstrumentosViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val hoy = remember { Formato.hoy() }
    PantallaBase(
        titulo = "Instrumentos",
        subtitulo = "Calibraciones · aviso a 30 días (RF-19)",
        onVolver = onVolver,
        botonFlotante = {
            if (ui.puedeGestionar) {
                ExtendedFloatingActionButton(
                    onClick = onNuevo,
                    icon = { Icon(painterResource(R.drawable.ic_agregar), contentDescription = null) },
                    text = { Text("Nuevo instrumento") }
                )
            }
        }
    ) { padding ->
        if (ui.cargando) { Cargando(Modifier.padding(padding)); return@PantallaBase }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!ui.puedeGestionar) {
                item { AvisoInformativo("Solo el Jefe técnico registra instrumentos y calibraciones (RN-12).") }
            }
            items(ui.instrumentos, key = { it.id }) { inst ->
                val vig = ReglasInstrumento.vigencia(inst.vencimiento, hoy)
                AparicionEscalonada(ui.instrumentos.indexOf(inst), Modifier.animateItem()) {
                    TarjetaLista(
                        titulo = inst.nombre,
                        subtitulo = "${inst.marca} ${inst.modelo} · Nº ${inst.numeroSerie}",
                        detalle = "Sirve para: ${inst.categoria.pruebas.joinToString { it.etiqueta.lowercase() }}",
                        icono = R.drawable.ic_herramienta,
                        colorIcono = chipVigencia(vig).texto,
                        onClick = { onInstrumento(inst.id) },
                        pie = {
                            Chip(
                                when (vig) {
                                    VigenciaInstrumento.VENCIDO -> "Vencido ${inst.vencimiento?.let { Formato.fecha(it) }}"
                                    VigenciaInstrumento.POR_VENCER -> "Vence ${inst.vencimiento?.let { Formato.fecha(it) }}"
                                    VigenciaInstrumento.VIGENTE -> "Vigente hasta ${inst.vencimiento?.let { Formato.fecha(it) }}"
                                    VigenciaInstrumento.SIN_CALIBRACION -> vig.etiqueta
                                },
                                chipVigencia(vig)
                            )
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun InstrumentoFormScreen(onVolver: () -> Unit, onGuardado: (Long) -> Unit, viewModel: InstrumentoFormViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.guardadoId) { ui.guardadoId?.let(onGuardado) }
    val sacudida = rememberSacudida(ui.intentosFallidos)

    PantallaBase(titulo = "Nuevo instrumento", subtitulo = "RN-12", onVolver = onVolver, snackbar = snackbar) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaSeccion("Datos del instrumento", icono = R.drawable.ic_herramienta, modifier = Modifier.sacudir(sacudida)) {
                CampoSeleccion(
                    etiqueta = "Categoría", opciones = CategoriaInstrumento.entries, seleccion = ui.categoria,
                    onSeleccionar = viewModel::onCategoria, texto = { it.etiqueta }, icono = R.drawable.ic_medidor,
                    error = ui.errorCategoria, detalle = { c -> "Pruebas: " + c.pruebas.joinToString { it.etiqueta } }
                )
                CampoTexto(ui.nombre.valor, viewModel::onNombre, "Nombre (ej.: Megóhmetro 5 kV)", R.drawable.ic_etiqueta,
                    error = ui.nombre.errorVisible, valido = ui.nombre.valido)
                CampoTexto(ui.marca.valor, viewModel::onMarca, "Marca", R.drawable.ic_empresa,
                    error = ui.marca.errorVisible, valido = ui.marca.valido, capitalizacion = KeyboardCapitalization.Words)
                CampoTexto(ui.modelo.valor, viewModel::onModelo, "Modelo", R.drawable.ic_etiqueta,
                    error = ui.modelo.errorVisible, valido = ui.modelo.valido, capitalizacion = KeyboardCapitalization.Characters)
                CampoTexto(ui.serie.valor, viewModel::onSerie, "Número de serie", R.drawable.ic_numero,
                    error = ui.serie.errorVisible, valido = ui.serie.valido, capitalizacion = KeyboardCapitalization.Characters)
            }
            TarjetaSeccion("Certificado de calibración", icono = R.drawable.ic_verificado) {
                CampoFecha("Fecha de calibración", ui.fechaCalibracion, viewModel::onFechaCalibracion, error = ui.errorCalibracion, permitirFuturas = false)
                CampoFecha("Fecha de vencimiento", ui.fechaVencimiento, viewModel::onFechaVencimiento, error = ui.errorVencimiento)
            }
            BotonPrincipal("Guardar instrumento", viewModel::guardar, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando)
        }
    }
}

@Composable
fun InstrumentoDetalleScreen(onVolver: () -> Unit, viewModel: InstrumentoDetalleViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    val inst = ui.instrumento
    val hoy = remember { Formato.hoy() }

    PantallaBase(titulo = inst?.nombre ?: "Instrumento", subtitulo = inst?.let { "Nº ${it.numeroSerie}" }, onVolver = onVolver, snackbar = snackbar) { padding ->
        if (ui.cargando) { Cargando(Modifier.padding(padding)); return@PantallaBase }
        if (inst == null) { EstadoVacio(R.drawable.ic_herramienta, "No encontrado", "", Modifier.padding(padding)); return@PantallaBase }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val vig = ReglasInstrumento.vigencia(inst.vencimiento, hoy)
                TarjetaSeccion("Ficha", icono = R.drawable.ic_herramienta, accion = { Chip(vig.etiqueta, chipVigencia(vig)) }) {
                    FilaDato("Categoría", inst.categoria.etiqueta)
                    FilaDato("Marca / modelo", "${inst.marca} ${inst.modelo}")
                    FilaDato("Nº de serie", inst.numeroSerie)
                    FilaDato("Última calibración", inst.ultimaCalibracion?.let { Formato.fecha(it) } ?: "")
                    FilaDato("Vencimiento", inst.vencimiento?.let { Formato.fecha(it) } ?: "")
                }
            }
            if (ui.puedeGestionar) {
                item {
                    TarjetaSeccion("Registrar nueva calibración", icono = R.drawable.ic_verificado) {
                        AnimatedVisibility(visible = ui.mostrarFormulario) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CampoFecha("Fecha de calibración", ui.fechaCalibracion, viewModel::onFechaCalibracion, error = ui.errorCalibracion, permitirFuturas = false)
                                CampoFecha("Fecha de vencimiento", ui.fechaVencimiento, viewModel::onFechaVencimiento, error = ui.errorVencimiento)
                                BotonPrincipal("Guardar calibración", viewModel::registrarCalibracion, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando)
                            }
                        }
                        BotonSecundario(if (ui.mostrarFormulario) "Cancelar" else "Nueva calibración", viewModel::alternarFormulario, Modifier.fillMaxWidth())
                    }
                }
            }
            item { Text("Historial de calibraciones", style = MaterialTheme.typography.titleMedium) }
            items(ui.calibraciones, key = { it.id }) { cal ->
                TarjetaLista(
                    titulo = "${Formato.fecha(cal.fechaCalibracion)} → ${Formato.fecha(cal.fechaVencimiento)}",
                    subtitulo = if (cal.registradaPor.isBlank()) "Registro" else "Registrada por ${cal.registradaPor}",
                    icono = R.drawable.ic_calendario,
                    onClick = null
                )
            }
            item {
                Text(
                    "Se conserva el historial: así se puede demostrar que el instrumento estaba vigente el día de un protocolo antiguo.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun UsuariosScreen(onVolver: () -> Unit, onNuevo: () -> Unit, viewModel: UsuariosViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    PantallaBase(
        titulo = "Personal y cuentas",
        subtitulo = "RF-22 · RN-20",
        onVolver = onVolver,
        snackbar = snackbar,
        botonFlotante = {
            if (ui.puedeGestionar) {
                ExtendedFloatingActionButton(
                    onClick = onNuevo,
                    icon = { Icon(painterResource(R.drawable.ic_agregar), contentDescription = null) },
                    text = { Text("Nueva cuenta") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { CampoTexto(ui.busqueda, viewModel::onBuscar, "Buscar por nombre, correo o RUT", R.drawable.ic_buscar) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(RolCodigo.entries) { r ->
                        FilterChip(selected = ui.rol == r, onClick = { viewModel.onRol(r) }, label = { Text(r.etiqueta) })
                    }
                }
            }
            item {
                Text(
                    "Desactivar no borra: el usuario no podrá ingresar, pero sus protocolos conservan su nombre (Aclaración nº 8).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(ui.usuarios, key = { it.id }) { u ->
                TarjetaLista(
                    titulo = u.nombre,
                    subtitulo = "${u.rol.etiqueta} · ${u.correo}",
                    detalle = "RUT ${u.rut}" + if (u.nombreCliente.isNotBlank()) " · ${u.nombreCliente}" else "",
                    icono = R.drawable.ic_persona,
                    colorIcono = if (u.activo) MaterialTheme.colorScheme.primary else GrisNeutro,
                    onClick = null,
                    derecha = {
                        Switch(
                            checked = u.activo,
                            onCheckedChange = { viewModel.cambiarActivo(u) },
                            enabled = ui.puedeGestionar && u.id != ui.idPropio
                        )
                    }
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioFormScreen(onVolver: () -> Unit, onGuardado: (Long) -> Unit, viewModel: UsuarioFormViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.guardadoId) { ui.guardadoId?.let(onGuardado) }
    val sacudida = rememberSacudida(ui.intentosFallidos)
    val roles = RolCodigo.entries

    PantallaBase(titulo = "Nueva cuenta", subtitulo = "Personal o usuario cliente", onVolver = onVolver, snackbar = snackbar) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaSeccion("Perfil", icono = R.drawable.ic_credencial) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    roles.forEachIndexed { i, r ->
                        SegmentedButton(
                            selected = ui.rol == r, onClick = { viewModel.onRol(r) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = roles.size),
                            label = { Text(r.etiqueta.substringBefore(" "), maxLines = 1) }
                        )
                    }
                }
                Text("${ui.rol.perfil} · ${ui.rol.etiqueta}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedVisibility(visible = ui.rol == RolCodigo.CLIENTE) {
                    CampoSeleccion(
                        etiqueta = "Empresa cliente", opciones = ui.clientes, seleccion = ui.cliente, onSeleccionar = viewModel::onCliente,
                        texto = { it.razonSocial }, icono = R.drawable.ic_empresa, error = ui.errorCliente,
                        ayuda = "Solo verá la información de esta empresa (RN-18)"
                    )
                }
            }
            TarjetaSeccion("Datos personales", icono = R.drawable.ic_persona, modifier = Modifier.sacudir(sacudida)) {
                CampoTexto(ui.nombre.valor, viewModel::onNombre, "Nombre completo", R.drawable.ic_persona,
                    error = ui.nombre.errorVisible, valido = ui.nombre.valido, capitalizacion = KeyboardCapitalization.Words)
                CampoTexto(ui.rut.valor, viewModel::onRut, "RUT", R.drawable.ic_credencial,
                    error = ui.rut.errorVisible, valido = ui.rut.valido, capitalizacion = KeyboardCapitalization.Characters)
                CampoTexto(ui.cargo.valor, viewModel::onCargo, "Cargo", R.drawable.ic_etiqueta,
                    error = ui.cargo.errorVisible, valido = ui.cargo.valido)
                CampoTexto(ui.telefono.valor, viewModel::onTelefono, "Teléfono", R.drawable.ic_telefono,
                    error = ui.telefono.errorVisible, valido = ui.telefono.valido, tipoTeclado = KeyboardType.Phone)
            }
            TarjetaSeccion("Credenciales de ingreso (RF-14)", icono = R.drawable.ic_candado) {
                CampoTexto(ui.correo.valor, viewModel::onCorreo, "Correo", R.drawable.ic_correo,
                    error = ui.correo.errorVisible, valido = ui.correo.valido, tipoTeclado = KeyboardType.Email,
                    capitalizacion = KeyboardCapitalization.None)
                CampoContrasena(ui.contrasena.valor, viewModel::onContrasena, "Contraseña inicial", ui.contrasenaVisible,
                    viewModel::onAlternarVisible, error = ui.contrasena.errorVisible)
                Text("Mínimo 8 caracteres con letras y números. Se guarda cifrada (hash con sal).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BotonPrincipal("Crear cuenta", viewModel::guardar, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando)
            Spacer(Modifier.height(24.dp))
        }
    }
}
