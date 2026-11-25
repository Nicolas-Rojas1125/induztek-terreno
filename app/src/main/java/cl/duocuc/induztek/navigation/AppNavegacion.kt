package cl.duocuc.induztek.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.ui.components.BarraInferior
import cl.duocuc.induztek.ui.components.DestinoInferior
import cl.duocuc.induztek.ui.components.LocalConectado
import cl.duocuc.induztek.ui.screens.admin.InstrumentoDetalleScreen
import cl.duocuc.induztek.ui.screens.admin.InstrumentoFormScreen
import cl.duocuc.induztek.ui.screens.admin.InstrumentosScreen
import cl.duocuc.induztek.ui.screens.admin.UsuarioDetalleScreen
import cl.duocuc.induztek.ui.screens.admin.UsuarioFormScreen
import cl.duocuc.induztek.ui.screens.admin.UsuariosScreen
import cl.duocuc.induztek.ui.screens.auth.LoginScreen
import cl.duocuc.induztek.ui.screens.auth.RecuperarClaveScreen
import cl.duocuc.induztek.ui.screens.clientes.ClienteDetalleScreen
import cl.duocuc.induztek.ui.screens.clientes.ClienteFormScreen
import cl.duocuc.induztek.ui.screens.clientes.ClientesScreen
import cl.duocuc.induztek.ui.screens.clientes.InstalacionDetalleScreen
import cl.duocuc.induztek.ui.screens.clientes.InstalacionFormScreen
import cl.duocuc.induztek.ui.screens.equipos.EquipoDetalleScreen
import cl.duocuc.induztek.ui.screens.equipos.EquipoFormScreen
import cl.duocuc.induztek.ui.screens.equipos.EquiposScreen
import cl.duocuc.induztek.ui.screens.inicio.InicioScreen
import cl.duocuc.induztek.ui.screens.perfil.CambiarClaveScreen
import cl.duocuc.induztek.ui.screens.perfil.EditarPerfilScreen
import cl.duocuc.induztek.ui.screens.perfil.MasScreen
import cl.duocuc.induztek.ui.screens.perfil.ServidorScreen
import cl.duocuc.induztek.ui.screens.perfil.SincronizacionScreen
import cl.duocuc.induztek.ui.screens.protocolos.ProtocoloScreen
import cl.duocuc.induztek.ui.screens.protocolos.ProtocolosScreen
import cl.duocuc.induztek.ui.screens.protocolos.PruebaFormScreen
import cl.duocuc.induztek.ui.screens.visitas.ActaScreen
import cl.duocuc.induztek.ui.screens.visitas.RecepcionScreen
import cl.duocuc.induztek.ui.screens.visitas.VisitaDetalleScreen
import cl.duocuc.induztek.ui.screens.visitas.VisitaFormScreen
import cl.duocuc.induztek.ui.screens.visitas.VisitasScreen
import cl.duocuc.induztek.ui.theme.InduztekTheme
import cl.duocuc.induztek.viewmodel.AppViewModel

private val destinosPersonal = listOf(
    DestinoInferior(Ruta.Inicio.ruta, "Inicio", R.drawable.ic_inicio),
    DestinoInferior(Ruta.Visitas.ruta, "Visitas", R.drawable.ic_calendario),
    DestinoInferior(Ruta.Protocolos.ruta, "Protocolos", R.drawable.ic_documento),
    DestinoInferior(Ruta.Equipos.ruta, "Equipos", R.drawable.ic_rayo),
    DestinoInferior(Ruta.Mas.ruta, "Más", R.drawable.ic_menu)
)

private val destinosCliente = listOf(
    DestinoInferior(Ruta.Inicio.ruta, "Inicio", R.drawable.ic_inicio),
    DestinoInferior(Ruta.Equipos.ruta, "Equipos", R.drawable.ic_rayo),
    DestinoInferior(Ruta.Actas.ruta, "Actas", R.drawable.ic_verificado),
    DestinoInferior(Ruta.Protocolos.ruta, "Protocolos", R.drawable.ic_documento),
    DestinoInferior(Ruta.Mas.ruta, "Más", R.drawable.ic_menu)
)

private fun NavHostController.irAPrincipal(ruta: String) {
    navigate(ruta) {
        popUpTo(Ruta.Inicio.ruta) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.reemplazar(ruta: String) {
    val actual = currentBackStackEntry?.destination?.route
    navigate(ruta) {
        if (actual != null) popUpTo(actual) { inclusive = true }
    }
}

private val rutasPublicas = setOf(Ruta.Login.ruta, Ruta.RecuperarClave.ruta, Ruta.Servidor.ruta)

private val argId = listOf(navArgument("id") { type = NavType.LongType })

@Composable
fun AppNavegacion(appViewModel: AppViewModel = viewModel()) {
    val sesion by appViewModel.sesion.collectAsStateWithLifecycle()
    val conectado by appViewModel.conectado.collectAsStateWithLifecycle()
    val tema by appViewModel.tema.collectAsStateWithLifecycle()

    InduztekTheme(preferencia = tema) {
        CompositionLocalProvider(LocalConectado provides conectado) {
            Grafo(sesion, tema, appViewModel)
        }
    }
}

@Composable
private fun Grafo(sesion: Sesion?, tema: String, appViewModel: AppViewModel) {
    val nav = rememberNavController()
    val inicio = remember { if (sesion != null) Ruta.Inicio.ruta else Ruta.Login.ruta }
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val sincronizacion by appViewModel.sincronizacion.collectAsStateWithLifecycle()
    val servidor by appViewModel.servidor.collectAsStateWithLifecycle()
    val conectado by appViewModel.conectado.collectAsStateWithLifecycle()

    LaunchedEffect(sesion) {
        if (sesion == null && rutaActual != null && rutaActual !in rutasPublicas) {
            nav.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } }
        }
    }

    val barra: @Composable () -> Unit = {
        BarraInferior(
            destinos = if (sesion?.esCliente == true) destinosCliente else destinosPersonal,
            rutaActual = rutaActual,
            onNavegar = { nav.irAPrincipal(it) }
        )
    }
    val volver: () -> Unit = { nav.popBackStack() }

    NavHost(
        navController = nav,
        startDestination = inicio,
        enterTransition = { fadeIn(tween(220)) + slideIntoContainer(SlideDirection.Start, tween(260)) { it / 6 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) + slideIntoContainer(SlideDirection.End, tween(260)) { it / 6 } },
        popExitTransition = { fadeOut(tween(160)) + slideOutOfContainer(SlideDirection.End, tween(260)) { it / 6 } }
    ) {
        composable(Ruta.Login.ruta) {
            LoginScreen(
                onIngreso = { nav.navigate(Ruta.Inicio.ruta) { popUpTo(Ruta.Login.ruta) { inclusive = true } } },
                onRecuperar = { nav.navigate(Ruta.RecuperarClave.ruta) },
                onServidor = { nav.navigate(Ruta.Servidor.ruta) }
            )
        }
        composable(Ruta.RecuperarClave.ruta) {
            RecuperarClaveScreen(onVolver = volver)
        }
        composable(Ruta.Servidor.ruta) {
            ServidorScreen(urlActual = servidor, onGuardar = { appViewModel.cambiarServidor(it); volver() }, onVolver = volver)
        }
        composable(Ruta.Perfil.ruta) {
            EditarPerfilScreen(onVolver = volver)
        }
        composable(Ruta.CambiarClave.ruta) {
            CambiarClaveScreen(onVolver = volver)
        }
        composable(Ruta.Sincronizacion.ruta) {
            SincronizacionScreen(
                estado = sincronizacion, conectado = conectado, servidor = servidor,
                onSincronizar = appViewModel::sincronizar, onVolver = volver
            )
        }

        composable(Ruta.Inicio.ruta) {
            InicioScreen(
                barraInferior = barra,
                onVisita = { nav.navigate(Ruta.VisitaDetalle.crear(it)) },
                onProtocolo = { nav.navigate(Ruta.ProtocoloDetalle.crear(it)) },
                onInstalacion = { nav.navigate(Ruta.InstalacionDetalle.crear(it)) },
                onProtocolos = { nav.irAPrincipal(Ruta.Protocolos.ruta) },
                onInstrumentos = { nav.navigate(Ruta.Instrumentos.ruta) },
                onVisitas = { nav.irAPrincipal(if (sesion?.esCliente == true) Ruta.Actas.ruta else Ruta.Visitas.ruta) },
                onEquipos = { nav.irAPrincipal(Ruta.Equipos.ruta) },
                sincronizacion = sincronizacion,
                onSincronizacion = { nav.navigate(Ruta.Sincronizacion.ruta) }
            )
        }
        composable(Ruta.Visitas.ruta) {
            VisitasScreen(
                barraInferior = barra,
                onVisita = { nav.navigate(Ruta.VisitaDetalle.crear(it)) },
                onNueva = { nav.navigate(Ruta.VisitaNueva.crear()) }
            )
        }
        composable(Ruta.Actas.ruta) {
            VisitasScreen(
                barraInferior = barra,
                onVisita = { nav.navigate(Ruta.VisitaDetalle.crear(it)) },
                onNueva = {}
            )
        }
        composable(Ruta.Protocolos.ruta) {
            ProtocolosScreen(barraInferior = barra, onProtocolo = { nav.navigate(Ruta.ProtocoloDetalle.crear(it)) })
        }
        composable(Ruta.Equipos.ruta) {
            EquiposScreen(barraInferior = barra, onEquipo = { nav.navigate(Ruta.EquipoDetalle.crear(it)) })
        }
        composable(Ruta.Mas.ruta) {
            MasScreen(
                sesion = sesion,
                tema = tema,
                sincronizacion = sincronizacion,
                barraInferior = barra,
                onClientes = { nav.navigate(Ruta.Clientes.ruta) },
                onInstrumentos = { nav.navigate(Ruta.Instrumentos.ruta) },
                onUsuarios = { nav.navigate(Ruta.Usuarios.ruta) },
                onPerfil = { nav.navigate(Ruta.Perfil.ruta) },
                onCambiarClave = { nav.navigate(Ruta.CambiarClave.ruta) },
                onSincronizacion = { nav.navigate(Ruta.Sincronizacion.ruta) },
                onServidor = { nav.navigate(Ruta.Servidor.ruta) },
                onTema = appViewModel::cambiarTema,
                onCerrarSesion = appViewModel::cerrarSesion
            )
        }

        composable(Ruta.Clientes.ruta) {
            ClientesScreen(
                onVolver = volver,
                onCliente = { nav.navigate(Ruta.ClienteDetalle.crear(it)) },
                onNuevo = { nav.navigate(Ruta.ClienteNuevo.ruta) }
            )
        }
        composable(Ruta.ClienteNuevo.ruta) {
            ClienteFormScreen(onVolver = volver, onGuardado = { nav.reemplazar(Ruta.ClienteDetalle.crear(it)) })
        }
        composable(Ruta.ClienteDetalle.ruta, arguments = argId) {
            ClienteDetalleScreen(
                onVolver = volver,
                onInstalacion = { nav.navigate(Ruta.InstalacionDetalle.crear(it)) },
                onNuevaInstalacion = { nav.navigate(Ruta.InstalacionNueva.crear(it)) },
                onEditar = { nav.navigate(Ruta.ClienteEditar.crear(it)) }
            )
        }
        composable(Ruta.ClienteEditar.ruta, arguments = argId) {
            ClienteFormScreen(onVolver = volver, onGuardado = { volver() })
        }
        composable(
            Ruta.InstalacionNueva.ruta,
            arguments = listOf(navArgument("clienteId") { type = NavType.LongType })
        ) {
            InstalacionFormScreen(onVolver = volver, onGuardado = { nav.reemplazar(Ruta.InstalacionDetalle.crear(it)) })
        }
        composable(Ruta.InstalacionDetalle.ruta, arguments = argId) {
            InstalacionDetalleScreen(
                onVolver = volver,
                onEquipo = { nav.navigate(Ruta.EquipoDetalle.crear(it)) },
                onNuevoEquipo = { nav.navigate(Ruta.EquipoNuevo.crear(it)) },
                onVisita = { nav.navigate(Ruta.VisitaDetalle.crear(it)) },
                onEditar = { nav.navigate(Ruta.InstalacionEditar.crear(it)) }
            )
        }
        composable(Ruta.InstalacionEditar.ruta, arguments = argId) {
            InstalacionFormScreen(onVolver = volver, onGuardado = { volver() })
        }

        composable(
            Ruta.EquipoNuevo.ruta,
            arguments = listOf(navArgument("instalacionId") { type = NavType.LongType })
        ) {
            EquipoFormScreen(
                onVolver = volver,
                onGuardado = { volver() },
                onUsarExistente = { nav.reemplazar(Ruta.EquipoDetalle.crear(it)) }
            )
        }
        composable(Ruta.EquipoDetalle.ruta, arguments = argId) {
            EquipoDetalleScreen(
                onVolver = volver,
                onProtocolo = { nav.navigate(Ruta.ProtocoloDetalle.crear(it)) },
                onEditar = { nav.navigate(Ruta.EquipoEditar.crear(it)) }
            )
        }
        composable(Ruta.EquipoEditar.ruta, arguments = argId) {
            EquipoFormScreen(onVolver = volver, onGuardado = { volver() }, onUsarExistente = { nav.reemplazar(Ruta.EquipoDetalle.crear(it)) })
        }

        composable(
            Ruta.VisitaNueva.ruta,
            arguments = listOf(navArgument("instalacionId") { type = NavType.LongType; defaultValue = -1L })
        ) {
            VisitaFormScreen(onVolver = volver, onGuardado = { nav.reemplazar(Ruta.VisitaDetalle.crear(it)) })
        }
        composable(Ruta.VisitaDetalle.ruta, arguments = argId) {
            VisitaDetalleScreen(
                onVolver = volver,
                onProtocolo = { nav.navigate(Ruta.ProtocoloDetalle.crear(it)) },
                onNuevoEquipo = { nav.navigate(Ruta.EquipoNuevo.crear(it)) },
                onRecepcion = { nav.navigate(Ruta.Recepcion.crear(it)) },
                onActa = { nav.navigate(Ruta.Acta.crear(it)) }
            )
        }
        composable(Ruta.Recepcion.ruta, arguments = argId) {
            RecepcionScreen(onVolver = volver, onGuardado = { volver() })
        }
        composable(Ruta.Acta.ruta, arguments = argId) {
            ActaScreen(onVolver = volver)
        }

        composable(Ruta.ProtocoloDetalle.ruta, arguments = argId) {
            ProtocoloScreen(
                onVolver = volver,
                onPrueba = { protocoloId, tipo, pruebaId -> nav.navigate(Ruta.PruebaForm.crear(protocoloId, tipo, pruebaId)) },
                onProtocolo = { nav.reemplazar(Ruta.ProtocoloDetalle.crear(it)) },
                onEquipo = { nav.navigate(Ruta.EquipoDetalle.crear(it)) }
            )
        }
        composable(
            Ruta.PruebaForm.ruta,
            arguments = listOf(
                navArgument("protocoloId") { type = NavType.LongType },
                navArgument("tipo") { type = NavType.StringType },
                navArgument("pruebaId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) {
            PruebaFormScreen(onVolver = volver)
        }

        composable(Ruta.Instrumentos.ruta) {
            InstrumentosScreen(
                onVolver = volver,
                onInstrumento = { nav.navigate(Ruta.InstrumentoDetalle.crear(it)) },
                onNuevo = { nav.navigate(Ruta.InstrumentoNuevo.ruta) }
            )
        }
        composable(Ruta.InstrumentoNuevo.ruta) {
            InstrumentoFormScreen(onVolver = volver, onGuardado = { nav.reemplazar(Ruta.InstrumentoDetalle.crear(it)) })
        }
        composable(Ruta.InstrumentoDetalle.ruta, arguments = argId) {
            InstrumentoDetalleScreen(onVolver = volver, onEditar = { nav.navigate(Ruta.InstrumentoEditar.crear(it)) })
        }
        composable(Ruta.InstrumentoEditar.ruta, arguments = argId) {
            InstrumentoFormScreen(onVolver = volver, onGuardado = { volver() })
        }
        composable(Ruta.Usuarios.ruta) {
            UsuariosScreen(
                onVolver = volver,
                onUsuario = { nav.navigate(Ruta.UsuarioDetalle.crear(it)) },
                onNuevo = { nav.navigate(Ruta.UsuarioNuevo.ruta) }
            )
        }
        composable(Ruta.UsuarioNuevo.ruta) {
            UsuarioFormScreen(onVolver = volver, onGuardado = { nav.reemplazar(Ruta.UsuarioDetalle.crear(it)) })
        }
        composable(Ruta.UsuarioDetalle.ruta, arguments = argId) {
            UsuarioDetalleScreen(onVolver = volver, onEditar = { nav.navigate(Ruta.UsuarioEditar.crear(it)) })
        }
        composable(Ruta.UsuarioEditar.ruta, arguments = argId) {
            UsuarioFormScreen(onVolver = volver, onGuardado = { volver() })
        }
    }
}
