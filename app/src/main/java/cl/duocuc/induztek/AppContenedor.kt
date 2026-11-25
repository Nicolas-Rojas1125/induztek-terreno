package cl.duocuc.induztek

import android.app.Application
import android.content.Context
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.SesionPreferencias
import cl.duocuc.induztek.data.remote.ActivosRemoto
import cl.duocuc.induztek.data.remote.ClimaRemoto
import cl.duocuc.induztek.data.remote.ProtocolosRemoto
import cl.duocuc.induztek.data.remote.ServiciosRemotos
import cl.duocuc.induztek.data.remote.UsuariosRemoto
import cl.duocuc.induztek.data.repository.ClienteRepositorio
import cl.duocuc.induztek.data.repository.ClimaFuente
import cl.duocuc.induztek.data.repository.EquipoRepositorio
import cl.duocuc.induztek.data.repository.InstrumentoRepositorio
import cl.duocuc.induztek.data.repository.NotificadorCambios
import cl.duocuc.induztek.data.repository.ProtocoloRepositorio
import cl.duocuc.induztek.data.repository.SesionRepositorio
import cl.duocuc.induztek.data.repository.SincronizacionRepositorio
import cl.duocuc.induztek.data.repository.UsuarioRepositorio
import cl.duocuc.induztek.data.repository.VisitaRepositorio
import cl.duocuc.induztek.nativo.MonitorConectividad
import cl.duocuc.induztek.nativo.RetroalimentacionHaptica
import cl.duocuc.induztek.nativo.ServicioUbicacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppContenedor(context: Context) {
    private val db = InduztekDbHelper.obtener(context)
    private val prefs = SesionPreferencias(context)
    val cambios = NotificadorCambios()

    private val remotos = ServiciosRemotos { prefs.servidor() }
    private val usuariosRemoto = UsuariosRemoto { remotos.usuarios }
    private val activosRemoto = ActivosRemoto { remotos.activos }
    private val protocolosRemoto = ProtocolosRemoto { remotos.protocolos }
    val clima: ClimaFuente = ClimaRemoto { remotos.clima }

    val sesion = SesionRepositorio(db, cambios, prefs, usuariosRemoto)
    val clientes = ClienteRepositorio(db, cambios, activosRemoto)
    val equipos = EquipoRepositorio(db, cambios, activosRemoto)
    val usuarios = UsuarioRepositorio(db, cambios, usuariosRemoto, sesion.sesion)
    val instrumentos = InstrumentoRepositorio(db, cambios, activosRemoto)
    val visitas = VisitaRepositorio(db, cambios)
    val protocolos = ProtocoloRepositorio(db, cambios, instrumentos)
    val sincronizacion = SincronizacionRepositorio(
        db, cambios, prefs, sesion.sesion, activosRemoto, usuariosRemoto, protocolosRemoto
    )

    val conectividad = MonitorConectividad(context)
    val haptica = RetroalimentacionHaptica(context)
    val ubicacion = ServicioUbicacion(context)

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun solicitarSincronizacion() {
        if (sesion.sesion.value == null) return
        if (!conectividad.conectado.value) return
        if (sincronizacion.estado.value.sincronizando) return
        alcance.launch { sincronizacion.sincronizar() }
    }

    fun iniciarAutomatizacion() {
        alcance.launch {
            conectividad.conectado.collect { conectado -> if (conectado) solicitarSincronizacion() }
        }
        alcance.launch {
            sesion.sesion.map { it?.idUsuario }.distinctUntilChanged().collect { id ->
                if (id != null) solicitarSincronizacion()
            }
        }
        alcance.launch {
            cambios.version.collect { sincronizacion.refrescarPendientes() }
        }
        alcance.launch {
            while (isActive) {
                delay(5 * 60 * 1000L)
                solicitarSincronizacion()
            }
        }
    }
}

class InduztekApp : Application() {
    lateinit var contenedor: AppContenedor
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContenedor(this)
        contenedor.iniciarAutomatizacion()
    }
}

val Application.contenedor: AppContenedor get() = (this as InduztekApp).contenedor
