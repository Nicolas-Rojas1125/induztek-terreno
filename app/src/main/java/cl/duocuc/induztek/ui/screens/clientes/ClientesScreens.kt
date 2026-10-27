package cl.duocuc.induztek.ui.screens.clientes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.ui.components.AparicionEscalonada
import cl.duocuc.induztek.ui.components.AvisoInformativo
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.BotonSecundario
import cl.duocuc.induztek.ui.components.CampoSeleccion
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.Cargando
import cl.duocuc.induztek.ui.components.EstadoVacio
import cl.duocuc.induztek.ui.components.FilaDato
import cl.duocuc.induztek.ui.components.MostrarMensaje
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaLista
import cl.duocuc.induztek.ui.components.TarjetaSeccion
import cl.duocuc.induztek.ui.components.rememberSacudida
import cl.duocuc.induztek.ui.components.sacudir
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.viewmodel.ClienteDetalleViewModel
import cl.duocuc.induztek.viewmodel.ClienteFormViewModel
import cl.duocuc.induztek.viewmodel.ClientesViewModel
import cl.duocuc.induztek.viewmodel.InstalacionDetalleViewModel
import cl.duocuc.induztek.viewmodel.InstalacionFormViewModel

@Composable
fun ClientesScreen(
    onVolver: () -> Unit,
    onCliente: (Long) -> Unit,
    onNuevo: () -> Unit,
    viewModel: ClientesViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    PantallaBase(
        titulo = "Clientes",
        subtitulo = "${ui.clientes.size} cliente(s)",
        onVolver = onVolver,
        botonFlotante = {
            if (ui.puedeCrear) {
                ExtendedFloatingActionButton(
                    onClick = onNuevo,
                    icon = { Icon(painterResource(R.drawable.ic_agregar), contentDescription = null) },
                    text = { Text("Nuevo cliente") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CampoTexto(
                    valor = ui.busqueda, onValorCambia = viewModel::onBuscar, etiqueta = "Buscar por razón social o RUT",
                    icono = R.drawable.ic_buscar
                )
            }
            if (!ui.cargando && ui.clientes.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_empresa, "Sin resultados", "Pruebe con otro texto de búsqueda.") }
            }
            items(ui.clientes, key = { it.id }) { cl ->
                AparicionEscalonada(ui.clientes.indexOf(cl), Modifier.animateItem()) {
                    TarjetaLista(
                        titulo = cl.razonSocial,
                        subtitulo = "RUT ${cl.rut} · ${cl.giro}",
                        detalle = "${cl.cantidadInstalaciones} instalación(es) · ${cl.contactoNombre}",
                        icono = R.drawable.ic_empresa,
                        onClick = { onCliente(cl.id) }
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun ClienteFormScreen(onVolver: () -> Unit, onGuardado: (Long) -> Unit, viewModel: ClienteFormViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.guardadoId) { ui.guardadoId?.let(onGuardado) }
    val sacudida = rememberSacudida(ui.intentosFallidos)

    PantallaBase(titulo = "Nuevo cliente", subtitulo = "RF-01", onVolver = onVolver, snackbar = snackbar) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaSeccion("Datos de la empresa", icono = R.drawable.ic_empresa, modifier = Modifier.sacudir(sacudida)) {
                CampoTexto(ui.razonSocial.valor, viewModel::onRazon, "Razón social", R.drawable.ic_empresa,
                    error = ui.razonSocial.errorVisible, valido = ui.razonSocial.valido, capitalizacion = KeyboardCapitalization.Words)
                CampoTexto(ui.rut.valor, viewModel::onRut, "RUT de la empresa", R.drawable.ic_credencial,
                    error = ui.rut.errorVisible, valido = ui.rut.valido, ayuda = "Ej.: 76.482.915-8 (se valida el dígito verificador)",
                    capitalizacion = KeyboardCapitalization.Characters)
                CampoTexto(ui.giro.valor, viewModel::onGiro, "Giro", R.drawable.ic_etiqueta,
                    error = ui.giro.errorVisible, valido = ui.giro.valido)
            }
            TarjetaSeccion("Contacto principal", icono = R.drawable.ic_persona) {
                CampoTexto(ui.contacto.valor, viewModel::onContacto, "Nombre del contacto", R.drawable.ic_persona,
                    error = ui.contacto.errorVisible, valido = ui.contacto.valido, capitalizacion = KeyboardCapitalization.Words)
                CampoTexto(ui.telefono.valor, viewModel::onTelefono, "Teléfono", R.drawable.ic_telefono,
                    error = ui.telefono.errorVisible, valido = ui.telefono.valido, tipoTeclado = KeyboardType.Phone)
                CampoTexto(ui.correo.valor, viewModel::onCorreo, "Correo", R.drawable.ic_correo,
                    error = ui.correo.errorVisible, valido = ui.correo.valido, tipoTeclado = KeyboardType.Email,
                    capitalizacion = KeyboardCapitalization.None)
            }
            BotonPrincipal("Guardar cliente", viewModel::guardar, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ClienteDetalleScreen(
    onVolver: () -> Unit,
    onInstalacion: (Long) -> Unit,
    onNuevaInstalacion: (Long) -> Unit,
    viewModel: ClienteDetalleViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val cl = ui.cliente
    PantallaBase(
        titulo = cl?.razonSocial ?: "Cliente",
        subtitulo = cl?.let { "RUT ${it.rut}" },
        onVolver = onVolver,
        botonFlotante = {
            if (ui.puedeEditar && cl != null) {
                ExtendedFloatingActionButton(
                    onClick = { onNuevaInstalacion(cl.id) },
                    icon = { Icon(painterResource(R.drawable.ic_agregar), contentDescription = null) },
                    text = { Text("Nueva instalación") }
                )
            }
        }
    ) { padding ->
        if (ui.cargando) { Cargando(Modifier.padding(padding)); return@PantallaBase }
        if (cl == null) {
            EstadoVacio(R.drawable.ic_bloqueo, "Cliente no disponible", "No existe o no tiene acceso (RN-18).", Modifier.padding(padding))
            return@PantallaBase
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                TarjetaSeccion("Ficha del cliente", icono = R.drawable.ic_empresa) {
                    FilaDato("Giro", cl.giro)
                    FilaDato("Contacto", cl.contactoNombre, icono = R.drawable.ic_persona)
                    FilaDato("Teléfono", cl.telefono, icono = R.drawable.ic_telefono)
                    FilaDato("Correo", cl.correo, icono = R.drawable.ic_correo)
                }
            }
            item { Text("Instalaciones (${ui.instalaciones.size})", style = MaterialTheme.typography.titleMedium) }
            if (ui.instalaciones.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_ubicacion, "Sin instalaciones", "Registre la primera planta, subestación o faena.") }
            }
            items(ui.instalaciones, key = { it.id }) { inst ->
                TarjetaLista(
                    titulo = inst.nombre,
                    subtitulo = "${inst.nombreComuna} · ${inst.ubicacion}",
                    detalle = "${inst.cantidadEquipos} equipo(s)" + if (inst.tieneCoordenadas) " · con GPS" else "",
                    icono = R.drawable.ic_ubicacion,
                    onClick = { onInstalacion(inst.id) }
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun InstalacionFormScreen(onVolver: () -> Unit, onGuardado: (Long) -> Unit, viewModel: InstalacionFormViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.guardadoId) { ui.guardadoId?.let(onGuardado) }
    val sacudida = rememberSacudida(ui.intentosFallidos)

    val permisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) viewModel.capturarUbicacion() else viewModel.onPermisoDenegado()
    }
    val pedirUbicacion = {
        val concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (concedido) viewModel.capturarUbicacion()
        else permisoUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    PantallaBase(titulo = "Nueva instalación", subtitulo = ui.cliente?.razonSocial, onVolver = onVolver, snackbar = snackbar) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaSeccion("Datos de la instalación", icono = R.drawable.ic_ubicacion, modifier = Modifier.sacudir(sacudida)) {
                CampoTexto(ui.nombre.valor, viewModel::onNombre, "Nombre (ej.: Planta Lampa)", R.drawable.ic_empresa,
                    error = ui.nombre.errorVisible, valido = ui.nombre.valido, capitalizacion = KeyboardCapitalization.Words)
                CampoTexto(ui.ubicacion.valor, viewModel::onUbicacion, "Ubicación de referencia", R.drawable.ic_ubicacion,
                    error = ui.ubicacion.errorVisible, valido = ui.ubicacion.valido)
                CampoSeleccion(
                    etiqueta = "Región", opciones = ui.regiones, seleccion = ui.region, onSeleccionar = viewModel::onRegion,
                    texto = { it.nombre }, icono = R.drawable.ic_ubicacion
                )
                CampoSeleccion(
                    etiqueta = "Comuna", opciones = ui.comunas, seleccion = ui.comuna, onSeleccionar = viewModel::onComuna,
                    texto = { it.nombre }, icono = R.drawable.ic_ubicacion, error = ui.errorComuna,
                    ayuda = "La región se obtiene de la comuna (3FN)"
                )
                CampoTexto(ui.observaciones.valor, viewModel::onObservaciones, "Observaciones de acceso", R.drawable.ic_info,
                    error = ui.observaciones.errorVisible, ayuda = "Ej.: \"Sin señal en sala eléctrica\", \"inducción 30 min\"", lineas = 3)
            }

            TarjetaSeccion("Coordenadas GPS (recurso nativo)", icono = R.drawable.ic_ubicacion) {
                Text(
                    "Guardar la posición ayuda a la próxima cuadrilla a llegar a faenas rurales. El GPS funciona sin internet.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedContent(targetState = ui.latitud != null, label = "coordenadas") { tiene ->
                    if (tiene && ui.latitud != null && ui.longitud != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AvisoInformativo(
                                "Ubicación capturada: ${Formato.coordenadas(ui.latitud!!, ui.longitud!!)}" +
                                    (ui.precision?.let { " (± ${it.toInt()} m)" } ?: "")
                            )
                            TextButton(onClick = viewModel::quitarUbicacion) { Text("Quitar coordenadas") }
                        }
                    } else {
                        BotonSecundario(
                            texto = if (ui.obteniendoUbicacion) "Obteniendo ubicación…" else "Capturar ubicación actual",
                            onClick = pedirUbicacion,
                            modifier = Modifier.fillMaxWidth(),
                            icono = R.drawable.ic_ubicacion,
                            habilitado = !ui.obteniendoUbicacion
                        )
                    }
                }
            }
            BotonPrincipal("Guardar instalación", viewModel::guardar, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun InstalacionDetalleScreen(
    onVolver: () -> Unit,
    onEquipo: (Long) -> Unit,
    onNuevoEquipo: (Long) -> Unit,
    onVisita: (Long) -> Unit,
    viewModel: InstalacionDetalleViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val inst = ui.instalacion
    val s = ui.sesion
    val context = LocalContext.current
    PantallaBase(
        titulo = inst?.nombre ?: "Instalación",
        subtitulo = inst?.nombreCliente,
        onVolver = onVolver,
        botonFlotante = {
            if (inst != null && s != null && !s.esCliente) {
                ExtendedFloatingActionButton(
                    onClick = { onNuevoEquipo(inst.id) },
                    icon = { Icon(painterResource(R.drawable.ic_agregar), contentDescription = null) },
                    text = { Text("Registrar equipo") }
                )
            }
        }
    ) { padding ->
        if (ui.cargando) { Cargando(Modifier.padding(padding)); return@PantallaBase }
        if (inst == null || s == null) {
            EstadoVacio(R.drawable.ic_bloqueo, "Instalación no disponible", "No existe o no tiene acceso (RN-18).", Modifier.padding(padding))
            return@PantallaBase
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                TarjetaSeccion("Ficha", icono = R.drawable.ic_ubicacion) {
                    FilaDato("Comuna", "${inst.nombreComuna}, ${inst.nombreRegion}")
                    FilaDato("Ubicación", inst.ubicacion)
                    FilaDato("Acceso", inst.observacionesAcceso, icono = R.drawable.ic_info)
                    if (inst.latitud != null && inst.longitud != null) {
                        FilaDato("GPS", Formato.coordenadas(inst.latitud, inst.longitud), icono = R.drawable.ic_ubicacion)
                        TextButton(onClick = {
                            val uri = Uri.parse("geo:${inst.latitud},${inst.longitud}?q=${inst.latitud},${inst.longitud}(${Uri.encode(inst.nombre)})")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }) { Text("Abrir en mapas") }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Equipos (${ui.equipos.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
            }
            if (ui.equipos.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_rayo, "Sin equipos", "Registre el primer equipo de esta instalación (HU-01).") }
            }
            items(ui.equipos, key = { "e${it.id}" }) { e ->
                TarjetaLista(
                    titulo = "${e.tag} · ${e.tipo.etiqueta}",
                    subtitulo = "${e.marca} ${e.modelo} · Nº ${e.numeroSerie}".replace("  ", " "),
                    detalle = e.ultimaPrueba?.let { "Última prueba: ${Formato.fechaDesdeMillis(it)}" } ?: "Sin pruebas registradas",
                    icono = R.drawable.ic_rayo,
                    onClick = { onEquipo(e.id) }
                )
            }
            item {
                Text(
                    if (s.esCliente) "Actas de visita (${ui.visitas.size})" else "Visitas (${ui.visitas.size})",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(ui.visitas, key = { "v${it.id}" }) { v ->
                TarjetaLista(
                    titulo = v.numeroOt,
                    subtitulo = "${Formato.fecha(v.fecha)} · ${v.tipo.etiqueta}",
                    detalle = if (v.cerrada) "Cerrada · ${v.cantidadProtocolos} protocolo(s)" else "Abierta · ${v.cantidadProtocolos} protocolo(s)",
                    icono = R.drawable.ic_calendario,
                    onClick = { onVisita(v.id) }
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}
