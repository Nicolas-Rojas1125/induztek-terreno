package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.duocuc.induztek.contenedor
import cl.duocuc.induztek.domain.model.Sesion
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val c = application.contenedor

    val sesion: StateFlow<Sesion?> = c.sesion.sesion
    val conectado: StateFlow<Boolean> = c.conectividad.conectado
    val tema: StateFlow<String> = c.sesion.tema

    fun cerrarSesion() = c.sesion.cerrarSesion()

    fun cambiarTema(valor: String) = c.sesion.cambiarTema(valor)
}
