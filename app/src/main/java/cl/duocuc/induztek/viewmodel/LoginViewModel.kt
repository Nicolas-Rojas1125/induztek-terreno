package cl.duocuc.induztek.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duocuc.induztek.InduztekApp
import cl.duocuc.induztek.data.repository.Autenticacion
import cl.duocuc.induztek.data.repository.Vibrador
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.validacion.Validadores
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val correo: Campo = Campo(),
    val contrasena: Campo = Campo(),
    val contrasenaVisible: Boolean = false,
    val cargando: Boolean = false,
    val errorGeneral: String? = null,
    val intentosFallidos: Int = 0,
    val exito: Boolean = false
)

class LoginViewModel(private val auth: Autenticacion, vibrador: Vibrador) : ViewModelSimple(vibrador) {
    private val _ui = MutableStateFlow(LoginUiState(correo = Campo(auth.ultimoCorreo)))
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    fun onCorreo(v: String) = _ui.update { it.copy(correo = it.correo.cambiar(v.trim(), Validadores::correo), errorGeneral = null) }

    fun onContrasena(v: String) =
        _ui.update { it.copy(contrasena = it.contrasena.cambiar(v, Validadores::contrasenaLogin), errorGeneral = null) }

    fun onAlternarVisible() = _ui.update { it.copy(contrasenaVisible = !it.contrasenaVisible) }

    fun ingresar() {
        val s = _ui.value
        if (s.cargando) return
        val correo = s.correo.validar(Validadores::correo)
        val clave = s.contrasena.validar(Validadores::contrasenaLogin)
        if (correo.error != null || clave.error != null) {
            _ui.update { it.copy(correo = correo, contrasena = clave, intentosFallidos = it.intentosFallidos + 1) }
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.update { it.copy(cargando = true, errorGeneral = null) }
        lanzar {
            when (val r = auth.iniciarSesion(correo.valor, clave.valor)) {
                is Operacion.Exito -> {
                    vibrar(TipoVibracion.EXITO)
                    _ui.update { it.copy(cargando = false, exito = true) }
                }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update {
                        it.copy(cargando = false, errorGeneral = r.mensaje, intentosFallidos = it.intentosFallidos + 1)
                    }
                }
            }
        }
    }

    companion object {
        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as InduztekApp).contenedor
                LoginViewModel(c.sesion, c.haptica)
            }
        }
    }
}
