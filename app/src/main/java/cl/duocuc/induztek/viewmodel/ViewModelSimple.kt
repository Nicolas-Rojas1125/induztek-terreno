package cl.duocuc.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.induztek.data.repository.Vibrador
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class ViewModelSimple(private val vibrador: Vibrador) : ViewModel() {
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    protected fun mostrar(texto: String) {
        _mensaje.value = texto
    }

    fun onMensajeMostrado() {
        _mensaje.value = null
    }

    protected fun vibrar(tipo: TipoVibracion) = vibrador.vibrar(tipo)

    protected fun lanzar(bloque: suspend () -> Unit): Job = viewModelScope.launch { bloque() }
}
