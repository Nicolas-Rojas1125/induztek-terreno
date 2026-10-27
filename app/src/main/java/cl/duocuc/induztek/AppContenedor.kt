package cl.duocuc.induztek

import android.app.Application
import android.content.Context
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.SesionPreferencias
import cl.duocuc.induztek.data.repository.ClienteRepositorio
import cl.duocuc.induztek.data.repository.EquipoRepositorio
import cl.duocuc.induztek.data.repository.InstrumentoRepositorio
import cl.duocuc.induztek.data.repository.NotificadorCambios
import cl.duocuc.induztek.data.repository.ProtocoloRepositorio
import cl.duocuc.induztek.data.repository.SesionRepositorio
import cl.duocuc.induztek.data.repository.UsuarioRepositorio
import cl.duocuc.induztek.data.repository.VisitaRepositorio
import cl.duocuc.induztek.nativo.MonitorConectividad
import cl.duocuc.induztek.nativo.RetroalimentacionHaptica
import cl.duocuc.induztek.nativo.ServicioUbicacion

class AppContenedor(context: Context) {
    private val db = InduztekDbHelper.obtener(context)
    private val prefs = SesionPreferencias(context)
    val cambios = NotificadorCambios()

    val sesion = SesionRepositorio(db, cambios, prefs)
    val clientes = ClienteRepositorio(db, cambios)
    val equipos = EquipoRepositorio(db, cambios)
    val usuarios = UsuarioRepositorio(db, cambios)
    val instrumentos = InstrumentoRepositorio(db, cambios)
    val visitas = VisitaRepositorio(db, cambios)
    val protocolos = ProtocoloRepositorio(db, cambios, instrumentos)

    val conectividad = MonitorConectividad(context)
    val haptica = RetroalimentacionHaptica(context)
    val ubicacion = ServicioUbicacion(context)
}

class InduztekApp : Application() {
    lateinit var contenedor: AppContenedor
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContenedor(this)
    }
}

val Application.contenedor: AppContenedor get() = (this as InduztekApp).contenedor
