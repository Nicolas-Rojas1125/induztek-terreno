package cl.duocuc.induztek.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.ui.components.AparicionEscalonada
import cl.duocuc.induztek.ui.components.AvisoInformativo
import cl.duocuc.induztek.ui.components.BarraBusqueda
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.BotonSecundario
import cl.duocuc.induztek.ui.components.CampoContrasena
import cl.duocuc.induztek.ui.components.CampoSeleccion
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.Cargando
import cl.duocuc.induztek.ui.components.Chip
import cl.duocuc.induztek.ui.components.ColoresChip
import cl.duocuc.induztek.ui.components.ControlPaginacion
import cl.duocuc.induztek.ui.components.DialogoConfirmacion
import cl.duocuc.induztek.ui.components.EstadoVacio
import cl.duocuc.induztek.ui.components.FilaDato
import cl.duocuc.induztek.ui.components.FiltrosChips
import cl.duocuc.induztek.ui.components.MostrarMensaje
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaLista
import cl.duocuc.induztek.ui.components.TarjetaSeccion
import cl.duocuc.induztek.ui.components.rememberSacudida
import cl.duocuc.induztek.ui.components.sacudir
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.GrisNeutro
import cl.duocuc.induztek.ui.theme.GrisNeutroFondo
import cl.duocuc.induztek.ui.theme.RojoRechazado
import cl.duocuc.induztek.ui.theme.VerdeAprobado
import cl.duocuc.induztek.ui.theme.VerdeAprobadoFondo
import cl.duocuc.induztek.viewmodel.FiltroEstadoCuenta
import cl.duocuc.induztek.viewmodel.UsuarioDetalleViewModel
import cl.duocuc.induztek.viewmodel.UsuarioFormViewModel
import cl.duocuc.induztek.viewmodel.UsuariosViewModel

private val chipActivo = ColoresChip(VerdeAprobado, VerdeAprobadoFondo, R.drawable.ic_check)
private val chipInactivo = ColoresChip(GrisNeutro, GrisNeutroFondo, R.drawable.ic_bloqueo)

@Composable
fun UsuariosScreen(
    onVolver: () -> Unit,
    onUsuario: (Long) -> Unit,
    onNuevo: () -> Unit,
    viewModel: UsuariosViewModel = viewModel(factory = UsuariosViewModel.Fabrica)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.recargar() }
    PantallaBase(
        titulo = "Cuentas de usuario",
        subtitulo = "Datos en línea desde ms-usuarios",
        onVolver = onVolver,
        acciones = {
            IconButton(onClick = viewModel::recargar) { Icon(painterResource(R.drawable.ic_sincronizar), contentDescription = "Recargar") }
        },
        botonFlotante = {
            if (ui.puedeCrear) {
                ExtendedFloatingActionButton(
                    onClick = onNuevo,
                    icon = { Icon(painterResource(R.drawable.ic_agregar), contentDescription = null) },
                    text = { Text("Nueva cuenta") }
                )
            }
        }
    ) { padding ->
        val lista = ui.pagina.items
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { BarraBusqueda(ui.busqueda, viewModel::onBuscar, "Buscar por nombre, correo o RUT") }
            item { FiltrosChips(RolCodigo.entries, ui.rol, { it.etiqueta.substringBefore(" (") }, { viewModel.onRol(it) }) }
            item { FiltrosChips(FiltroEstadoCuenta.entries, ui.estado, { it.etiqueta }, { viewModel.onEstado(it) }) }
            ui.error?.let { e -> item { AvisoInformativo(e, advertencia = true) } }
            if (ui.cargando && lista.isEmpty()) item { Cargando(Modifier.height(120.dp)) }
            if (!ui.cargando && lista.isEmpty() && ui.error == null) {
                item { EstadoVacio(R.drawable.ic_grupo, "Sin cuentas", "No hay cuentas que coincidan con los filtros.") }
            }
            items(lista, key = { it.id }) { u ->
                AparicionEscalonada(lista.indexOf(u), Modifier.animateItem()) {
                    TarjetaLista(
                        titulo = u.nombre,
                        subtitulo = "${u.rol.etiqueta} · ${u.correo}",
                        detalle = listOfNotNull(
                            "RUT ${u.rut}",
                            u.cargo.ifBlank { null },
                            u.nombreCliente.ifBlank { null }
                        ).joinToString(" · "),
                        icono = if (u.rol == RolCodigo.CLIENTE) R.drawable.ic_empresa else R.drawable.ic_persona,
                        colorIcono = if (u.activo) MaterialTheme.colorScheme.primary else GrisNeutro,
                        onClick = { onUsuario(u.id) },
                        pie = { Chip(if (u.activo) "Activa" else "Desactivada", if (u.activo) chipActivo else chipInactivo) }
                    )
                }
            }
            item { ControlPaginacion(ui.pagina, viewModel::irAPagina) }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun UsuarioFormScreen(
    onVolver: () -> Unit,
    onGuardado: (Long) -> Unit,
    viewModel: UsuarioFormViewModel = viewModel(factory = UsuarioFormViewModel.Fabrica)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.guardadoId) { ui.guardadoId?.let(onGuardado) }
    val sacudida = rememberSacudida(ui.intentosFallidos)

    PantallaBase(
        titulo = if (ui.editando) "Editar cuenta" else "Nueva cuenta",
        subtitulo = if (ui.editando) ui.rol?.etiqueta else "Según su rol puede crear: " + ui.rolesPermitidos.joinToString { it.etiqueta.substringBefore(" (") },
        onVolver = onVolver,
        snackbar = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val sinPermiso = ui.sinPermiso
            if (sinPermiso != null) {
                AvisoInformativo(sinPermiso, advertencia = true)
                return@Column
            }
            TarjetaSeccion("Rol y empresa", icono = R.drawable.ic_verificado, modifier = Modifier.sacudir(sacudida)) {
                CampoSeleccion(
                    etiqueta = "Rol", opciones = ui.rolesPermitidos, seleccion = ui.rol, onSeleccionar = viewModel::onRol,
                    texto = { it.etiqueta }, icono = R.drawable.ic_verificado, error = ui.errorRol, habilitado = !ui.editando,
                    ayuda = if (ui.editando) "El rol no se puede cambiar" else "Administrador: todos · Ingeniero: técnicos y clientes",
                    detalle = { "Perfil ${it.perfil}" }
                )
                if (ui.rol == RolCodigo.CLIENTE) {
                    CampoSeleccion(
                        etiqueta = "Empresa del cliente", opciones = ui.empresas, seleccion = ui.cliente, onSeleccionar = viewModel::onCliente,
                        texto = { it.razonSocial }, icono = R.drawable.ic_empresa, error = ui.errorCliente, habilitado = !ui.editando,
                        detalle = { "RUT ${it.rut}" }, ayuda = "Solo verá datos de esta empresa (RN-18)"
                    )
                }
            }
            TarjetaSeccion("Datos personales", icono = R.drawable.ic_persona) {
                CampoTexto(ui.nombre.valor, viewModel::onNombre, "Nombre y apellido", R.drawable.ic_persona,
                    error = ui.nombre.errorVisible, valido = ui.nombre.valido, capitalizacion = KeyboardCapitalization.Words)
                CampoTexto(ui.rut.valor, viewModel::onRut, "RUT", R.drawable.ic_credencial,
                    error = ui.rut.errorVisible, valido = ui.rut.valido, habilitado = !ui.editando,
                    capitalizacion = KeyboardCapitalization.Characters, ayuda = if (ui.editando) "El RUT no se modifica" else "Se valida el dígito verificador")
                CampoTexto(ui.correo.valor, viewModel::onCorreo, "Correo", R.drawable.ic_correo,
                    error = ui.correo.errorVisible, valido = ui.correo.valido, tipoTeclado = KeyboardType.Email,
                    capitalizacion = KeyboardCapitalization.None)
                CampoTexto(ui.telefono.valor, viewModel::onTelefono, "Teléfono (opcional)", R.drawable.ic_telefono,
                    error = ui.telefono.errorVisible, tipoTeclado = KeyboardType.Phone)
                CampoTexto(ui.cargo.valor, viewModel::onCargo, "Cargo (opcional)", R.drawable.ic_etiqueta,
                    error = ui.cargo.errorVisible, capitalizacion = KeyboardCapitalization.Words)
            }
            if (!ui.editando) {
                TarjetaSeccion("Contraseña inicial", icono = R.drawable.ic_llave) {
                    CampoContrasena(ui.contrasena.valor, viewModel::onContrasena, "Contraseña", ui.contrasenaVisible,
                        viewModel::onAlternarVisible, error = ui.contrasena.errorVisible)
                    CampoContrasena(ui.repetida.valor, viewModel::onRepetida, "Repita la contraseña", ui.contrasenaVisible,
                        viewModel::onAlternarVisible, error = ui.repetida.errorVisible)
                    Text(
                        "Mínimo 8 caracteres con letras y números. El usuario puede cambiarla en Más › Cambiar contraseña.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            BotonPrincipal(
                if (ui.editando) "Guardar cambios" else "Crear cuenta", viewModel::guardar, Modifier.fillMaxWidth(),
                icono = R.drawable.ic_check, cargando = ui.guardando
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun UsuarioDetalleScreen(
    onVolver: () -> Unit,
    onEditar: (Long) -> Unit,
    viewModel: UsuarioDetalleViewModel = viewModel(factory = UsuarioDetalleViewModel.Fabrica)
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.eliminado) { if (ui.eliminado) onVolver() }
    LaunchedEffect(Unit) { viewModel.recargar() }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }
    var confirmarEstado by rememberSaveable { mutableStateOf(false) }
    val u = ui.usuario

    if (confirmarEliminar && u != null) {
        DialogoConfirmacion(
            "¿Eliminar cuenta?", "Se eliminará la cuenta de ${u.nombre}. Si solo quiere impedir el acceso, desactívela.",
            "Eliminar", { confirmarEliminar = false; viewModel.eliminar() }, { confirmarEliminar = false }, peligroso = true
        )
    }
    if (confirmarEstado && u != null) {
        DialogoConfirmacion(
            if (u.activo) "¿Desactivar cuenta?" else "¿Activar cuenta?",
            if (u.activo) "${u.nombre} no podrá iniciar sesión. Sus registros se conservan (RF-22)." else "${u.nombre} podrá volver a iniciar sesión.",
            if (u.activo) "Desactivar" else "Activar",
            { confirmarEstado = false; viewModel.alternarActivo() }, { confirmarEstado = false }, peligroso = u.activo
        )
    }

    PantallaBase(
        titulo = u?.nombre ?: "Cuenta",
        subtitulo = u?.rol?.etiqueta,
        onVolver = onVolver,
        snackbar = snackbar,
        acciones = {
            if (u != null && ui.puedeEditar) {
                IconButton(onClick = { onEditar(u.id) }) { Icon(painterResource(R.drawable.ic_editar), contentDescription = "Editar") }
            }
        }
    ) { padding ->
        if (ui.cargando) { Cargando(Modifier.padding(padding)); return@PantallaBase }
        if (u == null) {
            EstadoVacio(R.drawable.ic_bloqueo, "Cuenta no disponible", ui.error ?: "No existe o no hay conexión", Modifier.padding(padding))
            return@PantallaBase
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                TarjetaSeccion("Ficha de la cuenta", icono = R.drawable.ic_credencial, accion = {
                    Chip(if (u.activo) "Activa" else "Desactivada", if (u.activo) chipActivo else chipInactivo)
                }) {
                    FilaDato("RUT", u.rut, icono = R.drawable.ic_credencial)
                    FilaDato("Correo", u.correo, icono = R.drawable.ic_correo)
                    FilaDato("Teléfono", u.telefono.ifBlank { "—" }, icono = R.drawable.ic_telefono)
                    FilaDato("Cargo", u.cargo.ifBlank { "—" }, icono = R.drawable.ic_etiqueta)
                    FilaDato("Rol", "${u.rol.etiqueta} (${u.rol.perfil})", icono = R.drawable.ic_verificado)
                    if (u.rol == RolCodigo.CLIENTE) FilaDato("Empresa", u.nombreCliente.ifBlank { "—" }, icono = R.drawable.ic_empresa)
                    u.creadoEn?.let { FilaDato("Creada", Formato.fechaHora(it), icono = R.drawable.ic_calendario) }
                    u.actualizadoEn?.let { FilaDato("Última modificación", Formato.fechaHora(it), icono = R.drawable.ic_historial) }
                }
            }
            if (ui.puedeCambiarEstado) {
                item {
                    BotonSecundario(
                        if (u.activo) "Desactivar cuenta" else "Activar cuenta", { confirmarEstado = true }, Modifier.fillMaxWidth(),
                        icono = if (u.activo) R.drawable.ic_bloqueo else R.drawable.ic_check,
                        habilitado = !ui.procesando, color = if (u.activo) RojoRechazado else VerdeAprobado
                    )
                }
            }
            if (ui.puedeEliminar) {
                item {
                    BotonSecundario(
                        "Eliminar cuenta", { confirmarEliminar = true }, Modifier.fillMaxWidth(),
                        icono = R.drawable.ic_eliminar, habilitado = !ui.procesando, color = RojoRechazado
                    )
                }
            }
            if (!ui.puedeCambiarEstado && !ui.puedeEditar) {
                item { AvisoInformativo("Su rol no permite administrar esta cuenta.") }
            }
        }
    }
}
