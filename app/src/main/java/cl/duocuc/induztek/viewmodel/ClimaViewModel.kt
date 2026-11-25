package cl.duocuc.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duocuc.induztek.InduztekApp
import cl.duocuc.induztek.data.repository.ClimaFuente
import cl.duocuc.induztek.domain.model.Clima
import cl.duocuc.induztek.domain.model.Operacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClimaUiState(
    val cargando: Boolean = false,
    val clima: Clima? = null,
    val error: String? = null,
    val consultado: Boolean = false
) {
    val alertaHumedad: Boolean get() = (clima?.humedadPct ?: 0.0) > 80.0
}

class ClimaViewModel(private val fuente: ClimaFuente) : ViewModel() {
    companion object {
        const val SIN_COORDENADAS = "La instalación no tiene coordenadas GPS: captúrelas para consultar el clima"

        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as InduztekApp).contenedor
                ClimaViewModel(c.clima)
            }
        }
    }

    private val _ui = MutableStateFlow(ClimaUiState())
    val ui: StateFlow<ClimaUiState> = _ui.asStateFlow()

    fun consultar(latitud: Double?, longitud: Double?) {
        if (latitud == null || longitud == null) {
            _ui.value = ClimaUiState(error = SIN_COORDENADAS, consultado = true)
            return
        }
        if (_ui.value.cargando) return
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = fuente.actual(latitud, longitud)) {
                is Operacion.Exito -> _ui.value = ClimaUiState(clima = r.valor, consultado = true)
                is Operacion.Fallo -> _ui.update { it.copy(cargando = false, error = r.mensaje, consultado = true) }
            }
        }
    }
}
