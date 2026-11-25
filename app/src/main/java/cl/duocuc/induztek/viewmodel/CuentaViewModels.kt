package cl.duocuc.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duocuc.induztek.InduztekApp
import cl.duocuc.induztek.data.repository.CuentaFuente
import cl.duocuc.induztek.data.repository.Vibrador
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.validacion.Validadores
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

inline fun <reified VM : ViewModel> fabricaCuenta(noinline crear: (CuentaFuente, Vibrador) -> VM): ViewModelProvider.Factory =
    viewModelFactory {
        initializer<VM> {
            val c = (this[APPLICATION_KEY] as InduztekApp).contenedor
            crear(c.sesion, c.haptica)
        }
    }

enum class PasoRecuperacion { SOLICITAR, RESTABLECER, LISTO }

data class RecuperarClaveUiState(
    val paso: PasoRecuperacion = PasoRecuperacion.SOLICITAR,
    val correo: Campo = Campo(),
    val codigo: Campo = Campo(),
    val nueva: Campo = Campo(),
    val repetida: Campo = Campo(),
    val visible: Boolean = false,
    val codigoDemo: String? = null,
    val aviso: String? = null,
    val cargando: Boolean = false,
    val intentosFallidos: Int = 0
)

class RecuperarClaveViewModel(private val cuenta: CuentaFuente, vibrador: Vibrador) : ViewModelSimple(vibrador) {
    private val _ui = MutableStateFlow(RecuperarClaveUiState())
    val ui: StateFlow<RecuperarClaveUiState> = _ui.asStateFlow()

    fun onCorreo(v: String) = _ui.update { it.copy(correo = it.correo.cambiar(v.trim(), Validadores::correo)) }
    fun onCodigo(v: String) = _ui.update { it.copy(codigo = it.codigo.cambiar(v.filter { c -> c.isDigit() }.take(6), Validadores::codigoRecuperacion)) }
    fun onNueva(v: String) = _ui.update {
        it.copy(
            nueva = it.nueva.cambiar(v, Validadores::contrasena),
            repetida = if (it.repetida.tocado) it.repetida.validar { r -> Validadores.confirmacion(v, r) } else it.repetida
        )
    }
    fun onRepetida(v: String) = _ui.update { s -> s.copy(repetida = s.repetida.cambiar(v) { r -> Validadores.confirmacion(s.nueva.valor, r) }) }
    fun onAlternarVisible() = _ui.update { it.copy(visible = !it.visible) }

    fun solicitarCodigo() {
        val s = _ui.value
        val correo = s.correo.validar(Validadores::correo)
        if (correo.error != null) {
            _ui.update { it.copy(correo = correo, intentosFallidos = it.intentosFallidos + 1) }
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.update { it.copy(correo = correo, cargando = true) }
        lanzar {
            when (val r = cuenta.recuperar(correo.valor)) {
                is Operacion.Exito -> {
                    vibrar(TipoVibracion.EXITO)
                    _ui.update {
                        it.copy(cargando = false, paso = PasoRecuperacion.RESTABLECER, aviso = r.valor.mensaje, codigoDemo = r.valor.codigoDemo)
                    }
                }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(cargando = false, intentosFallidos = it.intentosFallidos + 1) }
                    mostrar(r.mensaje)
                }
            }
        }
    }

    fun restablecer() {
        val s = _ui.value
        val codigo = s.codigo.validar(Validadores::codigoRecuperacion)
        val nueva = s.nueva.validar(Validadores::contrasena)
        val repetida = s.repetida.validar { Validadores.confirmacion(nueva.valor, it) }
        if (listOf(codigo, nueva, repetida).any { it.error != null }) {
            _ui.update { it.copy(codigo = codigo, nueva = nueva, repetida = repetida, intentosFallidos = it.intentosFallidos + 1) }
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.update { it.copy(cargando = true) }
        lanzar {
            when (val r = cuenta.restablecer(s.correo.valor, codigo.valor, nueva.valor)) {
                is Operacion.Exito -> {
                    vibrar(TipoVibracion.EXITO)
                    _ui.update { it.copy(cargando = false, paso = PasoRecuperacion.LISTO, aviso = r.valor) }
                }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(cargando = false, codigo = it.codigo.copy(error = r.mensaje), intentosFallidos = it.intentosFallidos + 1) }
                }
            }
        }
    }

    fun volverASolicitar() = _ui.update { it.copy(paso = PasoRecuperacion.SOLICITAR, codigo = Campo(), codigoDemo = null) }

    companion object {
        val Fabrica = fabricaCuenta { c, v -> RecuperarClaveViewModel(c, v) }
    }
}

data class CambiarClaveUiState(
    val actual: Campo = Campo(),
    val nueva: Campo = Campo(),
    val repetida: Campo = Campo(),
    val visible: Boolean = false,
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val listo: Boolean = false
)

class CambiarClaveViewModel(private val cuenta: CuentaFuente, vibrador: Vibrador) : ViewModelSimple(vibrador) {
    private val _ui = MutableStateFlow(CambiarClaveUiState())
    val ui: StateFlow<CambiarClaveUiState> = _ui.asStateFlow()

    fun onActual(v: String) = _ui.update { it.copy(actual = it.actual.cambiar(v, Validadores::contrasenaLogin)) }
    fun onNueva(v: String) = _ui.update { s ->
        s.copy(
            nueva = s.nueva.cambiar(v) { n -> Validadores.nuevaContrasena(s.actual.valor, n) },
            repetida = if (s.repetida.tocado) s.repetida.validar { r -> Validadores.confirmacion(v, r) } else s.repetida
        )
    }
    fun onRepetida(v: String) = _ui.update { s -> s.copy(repetida = s.repetida.cambiar(v) { r -> Validadores.confirmacion(s.nueva.valor, r) }) }
    fun onAlternarVisible() = _ui.update { it.copy(visible = !it.visible) }

    fun guardar() {
        val s = _ui.value
        if (s.guardando) return
        val actual = s.actual.validar(Validadores::contrasenaLogin)
        val nueva = s.nueva.validar { Validadores.nuevaContrasena(actual.valor, it) }
        val repetida = s.repetida.validar { Validadores.confirmacion(nueva.valor, it) }
        if (listOf(actual, nueva, repetida).any { it.error != null }) {
            _ui.update { it.copy(actual = actual, nueva = nueva, repetida = repetida, intentosFallidos = it.intentosFallidos + 1) }
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.update { it.copy(guardando = true) }
        lanzar {
            when (val r = cuenta.cambiarContrasena(actual.valor, nueva.valor)) {
                is Operacion.Exito -> {
                    vibrar(TipoVibracion.EXITO)
                    _ui.update { CambiarClaveUiState(listo = true) }
                    mostrar("Contraseña actualizada")
                }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(guardando = false, actual = it.actual.copy(error = r.mensaje), intentosFallidos = it.intentosFallidos + 1) }
                }
            }
        }
    }

    companion object {
        val Fabrica = fabricaCuenta { c, v -> CambiarClaveViewModel(c, v) }
    }
}

data class EditarPerfilUiState(
    val nombre: Campo = Campo(),
    val correo: Campo = Campo(),
    val telefono: Campo = Campo(),
    val cargo: Campo = Campo(),
    val rut: String = "",
    val rol: String = "",
    val empresa: String? = null,
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val guardado: Boolean = false
)

class EditarPerfilViewModel(private val cuenta: CuentaFuente, vibrador: Vibrador) : ViewModelSimple(vibrador) {
    private val vTelefono: (String) -> String? = { Validadores.telefono(it, obligatorio = false) }
    private val vCargo: (String) -> String? = { Validadores.textoOpcional(it, "El cargo", 80) }

    private val _ui = MutableStateFlow(
        cuenta.sesion.value?.let { s ->
            EditarPerfilUiState(
                nombre = Campo(s.nombre), correo = Campo(s.correo), telefono = Campo(s.telefono),
                cargo = Campo(s.cargo.orEmpty()), rut = s.rut, rol = s.rol.etiqueta, empresa = s.nombreCliente
            )
        } ?: EditarPerfilUiState()
    )
    val ui: StateFlow<EditarPerfilUiState> = _ui.asStateFlow()

    fun onNombre(v: String) = _ui.update { it.copy(nombre = it.nombre.cambiar(v, Validadores::nombrePersona), guardado = false) }
    fun onCorreo(v: String) = _ui.update { it.copy(correo = it.correo.cambiar(v.trim(), Validadores::correo), guardado = false) }
    fun onTelefono(v: String) = _ui.update { it.copy(telefono = it.telefono.cambiar(v, vTelefono), guardado = false) }
    fun onCargo(v: String) = _ui.update { it.copy(cargo = it.cargo.cambiar(v, vCargo), guardado = false) }

    fun guardar() {
        val s = _ui.value
        if (s.guardando) return
        val v = s.copy(
            nombre = s.nombre.validar(Validadores::nombrePersona),
            correo = s.correo.validar(Validadores::correo),
            telefono = s.telefono.validar(vTelefono),
            cargo = s.cargo.validar(vCargo)
        )
        if (listOf(v.nombre, v.correo, v.telefono, v.cargo).any { it.error != null }) {
            _ui.value = v.copy(intentosFallidos = s.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.value = v.copy(guardando = true)
        lanzar {
            when (val r = cuenta.editarPerfil(v.nombre.valor, v.correo.valor, v.telefono.valor, v.cargo.valor)) {
                is Operacion.Exito -> {
                    vibrar(TipoVibracion.EXITO)
                    _ui.update { it.copy(guardando = false, guardado = true) }
                    mostrar("Perfil actualizado")
                }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(guardando = false, intentosFallidos = it.intentosFallidos + 1) }
                    mostrar(r.mensaje)
                }
            }
        }
    }

    companion object {
        val Fabrica = fabricaCuenta { c, v -> EditarPerfilViewModel(c, v) }
    }
}
