package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.induztek.contenedor
import cl.duocuc.induztek.domain.model.EstadoSincronizacion
import cl.duocuc.induztek.domain.model.Sesion
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val c = application.contenedor

    val sesion: StateFlow<Sesion?> = c.sesion.sesion
    val conectado: StateFlow<Boolean> = c.conectividad.conectado
    val tema: StateFlow<String> = c.sesion.tema
    val sincronizacion: StateFlow<EstadoSincronizacion> = c.sincronizacion.estado
    val servidor: StateFlow<String> = c.sesion.servidor

    fun cerrarSesion() = c.sesion.cerrarSesion()

    fun cambiarTema(valor: String) = c.sesion.cambiarTema(valor)

    fun cambiarServidor(url: String) {
        c.sesion.cambiarServidor(url)
        c.solicitarSincronizacion()
    }

    fun sincronizar() {
        viewModelScope.launch { c.sincronizacion.sincronizar() }
    }
}
