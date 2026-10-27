package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.induztek.AppContenedor
import cl.duocuc.induztek.contenedor
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected val c: AppContenedor = application.contenedor

    protected val sesion: Sesion? get() = c.sesion.sesion.value

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    protected fun mostrar(texto: String) {
        _mensaje.value = texto
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }

    protected fun vibrar(tipo: TipoVibracion) = c.haptica.vibrar(tipo)

    protected fun lanzar(bloque: suspend () -> Unit): Job = viewModelScope.launch { bloque() }

    protected fun observarCambios(recargar: suspend () -> Unit) {
        viewModelScope.launch {
            c.cambios.version.collect { recargar() }
        }
    }
}

data class Campo(val valor: String = "", val error: String? = null, val tocado: Boolean = false) {
    val errorVisible: String? get() = if (tocado) error else null

    val valido: Boolean get() = tocado && error == null && valor.isNotBlank()

    fun cambiar(nuevo: String, validador: (String) -> String?): Campo =
        copy(valor = nuevo, error = validador(nuevo), tocado = true)

    fun validar(validador: (String) -> String?): Campo = copy(error = validador(valor), tocado = true)
}
