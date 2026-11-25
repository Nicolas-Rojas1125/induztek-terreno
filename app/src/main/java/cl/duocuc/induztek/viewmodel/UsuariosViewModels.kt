package cl.duocuc.induztek.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duocuc.induztek.InduztekApp
import cl.duocuc.induztek.data.remote.NuevaCuenta
import cl.duocuc.induztek.data.repository.UsuariosFuente
import cl.duocuc.induztek.data.repository.Vibrador
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.ReglasCuentas
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Usuario
import cl.duocuc.induztek.domain.validacion.Validadores
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FiltroEstadoCuenta(val etiqueta: String, val activo: Boolean?) {
    TODAS("Todas", null),
    ACTIVAS("Activas", true),
    INACTIVAS("Inactivas", false)
}

data class UsuariosUiState(
    val cargando: Boolean = true,
    val pagina: Pagina<Usuario> = Pagina.vacia(),
    val busqueda: String = "",
    val rol: RolCodigo? = null,
    val estado: FiltroEstadoCuenta = FiltroEstadoCuenta.TODAS,
    val puedeCrear: Boolean = false,
    val error: String? = null
)

class UsuariosViewModel(private val fuente: UsuariosFuente, vibrador: Vibrador) : ViewModelSimple(vibrador) {
    private val _ui = MutableStateFlow(UsuariosUiState())
    val ui: StateFlow<UsuariosUiState> = _ui.asStateFlow()

    init { recargar() }

    fun recargar() = cargar(_ui.value.pagina.pagina)

    private fun cargar(pagina: Int) {
        val s = fuente.sesion.value
        _ui.update { it.copy(cargando = true, puedeCrear = s?.puede(Permiso.CREAR_CUENTAS) == true) }
        lanzar {
            val u = _ui.value
            when (val r = fuente.listar(u.busqueda, u.rol, u.estado.activo, pagina)) {
                is Operacion.Exito -> _ui.update { it.copy(cargando = false, pagina = r.valor, error = null) }
                is Operacion.Fallo -> _ui.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    fun onBuscar(texto: String) {
        _ui.update { it.copy(busqueda = texto) }
        cargar(0)
    }

    fun onRol(rol: RolCodigo?) {
        _ui.update { it.copy(rol = if (it.rol == rol) null else rol) }
        cargar(0)
    }

    fun onEstado(e: FiltroEstadoCuenta) {
        _ui.update { it.copy(estado = e) }
        cargar(0)
    }

    fun irAPagina(pagina: Int) = cargar(pagina)

    companion object {
        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as InduztekApp).contenedor
                UsuariosViewModel(c.usuarios, c.haptica)
            }
        }
    }
}

data class UsuarioFormUiState(
    val editando: Boolean = false,
    val rolesPermitidos: List<RolCodigo> = emptyList(),
    val rol: RolCodigo? = null,
    val errorRol: String? = null,
    val nombre: Campo = Campo(),
    val rut: Campo = Campo(),
    val correo: Campo = Campo(),
    val telefono: Campo = Campo(),
    val cargo: Campo = Campo(),
    val contrasena: Campo = Campo(),
    val repetida: Campo = Campo(),
    val contrasenaVisible: Boolean = false,
    val empresas: List<Cliente> = emptyList(),
    val cliente: Cliente? = null,
    val errorCliente: String? = null,
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val guardadoId: Long? = null,
    val sinPermiso: String? = null
)

class UsuarioFormViewModel(
    private val fuente: UsuariosFuente,
    vibrador: Vibrador,
    private val idUsuario: Long?
) : ViewModelSimple(vibrador) {
    private val vTelefono: (String) -> String? = { Validadores.telefono(it, obligatorio = false) }
    private val vCargo: (String) -> String? = { Validadores.textoOpcional(it, "El cargo", 80) }

    private val _ui = MutableStateFlow(UsuarioFormUiState(editando = idUsuario != null))
    val ui: StateFlow<UsuarioFormUiState> = _ui.asStateFlow()

    init {
        val s = fuente.sesion.value
        val permitidos = s?.rol?.rolesQuePuedeCrear.orEmpty()
        _ui.update {
            it.copy(
                rolesPermitidos = permitidos,
                sinPermiso = if (s == null || (permitidos.isEmpty() && idUsuario == null)) ReglasCuentas.mensajeNoPuedeCrear(s?.rol ?: RolCodigo.CLIENTE) else null
            )
        }
        lanzar {
            _ui.update { it.copy(empresas = fuente.empresas()) }
            if (idUsuario != null) {
                when (val r = fuente.obtener(idUsuario)) {
                    is Operacion.Exito -> {
                        val u = r.valor
                        _ui.update {
                            it.copy(
                                rol = u.rol, nombre = Campo(u.nombre, null, true), rut = Campo(u.rut, null, true),
                                correo = Campo(u.correo, null, true), telefono = Campo(u.telefono, null, true),
                                cargo = Campo(u.cargo, null, true), cliente = it.empresas.firstOrNull { e -> e.id == u.idCliente }
                            )
                        }
                    }
                    is Operacion.Fallo -> mostrar(r.mensaje)
                }
            }
        }
    }

    fun onRol(r: RolCodigo) = _ui.update {
        it.copy(rol = r, errorRol = null, errorCliente = null, cliente = if (r == RolCodigo.CLIENTE) it.cliente else null)
    }
    fun onNombre(v: String) = _ui.update { it.copy(nombre = it.nombre.cambiar(v, Validadores::nombrePersona)) }
    fun onRut(v: String) = _ui.update { it.copy(rut = it.rut.cambiar(v, Validadores::rut)) }
    fun onCorreo(v: String) = _ui.update { it.copy(correo = it.correo.cambiar(v.trim(), Validadores::correo)) }
    fun onTelefono(v: String) = _ui.update { it.copy(telefono = it.telefono.cambiar(v, vTelefono)) }
    fun onCargo(v: String) = _ui.update { it.copy(cargo = it.cargo.cambiar(v, vCargo)) }
    fun onContrasena(v: String) = _ui.update {
        it.copy(
            contrasena = it.contrasena.cambiar(v, Validadores::contrasena),
            repetida = if (it.repetida.tocado) it.repetida.validar { r -> Validadores.confirmacion(v, r) } else it.repetida
        )
    }
    fun onRepetida(v: String) = _ui.update { s -> s.copy(repetida = s.repetida.cambiar(v) { r -> Validadores.confirmacion(s.contrasena.valor, r) }) }
    fun onAlternarVisible() = _ui.update { it.copy(contrasenaVisible = !it.contrasenaVisible) }
    fun onCliente(c: Cliente) = _ui.update { it.copy(cliente = c, errorCliente = null) }

    fun validar(): Boolean {
        val u = _ui.value
        val creando = !u.editando
        val rol = u.rol
        val v = u.copy(
            errorRol = when {
                !creando -> null
                rol == null -> "Seleccione el rol de la cuenta"
                rol !in u.rolesPermitidos -> "Su rol no puede crear cuentas de ${rol.etiqueta}"
                else -> null
            },
            nombre = u.nombre.validar(Validadores::nombrePersona),
            rut = if (creando) u.rut.validar(Validadores::rut) else u.rut,
            correo = u.correo.validar(Validadores::correo),
            telefono = u.telefono.validar(vTelefono),
            cargo = u.cargo.validar(vCargo),
            contrasena = if (creando) u.contrasena.validar(Validadores::contrasena) else u.contrasena,
            repetida = if (creando) u.repetida.validar { Validadores.confirmacion(u.contrasena.valor, it) } else u.repetida,
            errorCliente = if (creando && rol == RolCodigo.CLIENTE && u.cliente == null) "Seleccione la empresa del cliente (RN-18)" else null
        )
        val errores = listOfNotNull(
            v.errorRol, v.nombre.error, v.correo.error, v.telefono.error, v.cargo.error, v.errorCliente,
            if (creando) v.rut.error else null,
            if (creando) v.contrasena.error else null,
            if (creando) v.repetida.error else null
        )
        _ui.value = if (errores.isEmpty()) v else v.copy(intentosFallidos = u.intentosFallidos + 1)
        if (errores.isNotEmpty()) vibrar(TipoVibracion.ERROR)
        return errores.isEmpty()
    }

    fun guardar() {
        if (_ui.value.guardando || !validar()) return
        val v = _ui.value
        _ui.update { it.copy(guardando = true) }
        lanzar {
            val r = if (idUsuario == null) {
                val rol = v.rol ?: RolCodigo.TECNICO
                fuente.crear(
                    NuevaCuenta(
                        rut = v.rut.valor, nombre = v.nombre.valor, correo = v.correo.valor, telefono = v.telefono.valor,
                        cargo = v.cargo.valor, rol = rol, idCliente = v.cliente?.id, nombreCliente = v.cliente?.razonSocial,
                        contrasena = v.contrasena.valor
                    )
                )
            } else {
                fuente.editar(idUsuario, v.nombre.valor, v.correo.valor, v.telefono.valor, v.cargo.valor)
            }
            when (r) {
                is Operacion.Exito -> {
                    vibrar(TipoVibracion.EXITO)
                    _ui.update { it.copy(guardando = false, guardadoId = r.valor.id) }
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
        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as InduztekApp).contenedor
                UsuarioFormViewModel(c.usuarios, c.haptica, createSavedStateHandle().get<Long>("id"))
            }
        }
    }
}

data class UsuarioDetalleUiState(
    val cargando: Boolean = true,
    val usuario: Usuario? = null,
    val puedeEditar: Boolean = false,
    val puedeCambiarEstado: Boolean = false,
    val puedeEliminar: Boolean = false,
    val procesando: Boolean = false,
    val eliminado: Boolean = false,
    val error: String? = null
)

class UsuarioDetalleViewModel(
    private val fuente: UsuariosFuente,
    vibrador: Vibrador,
    private val idUsuario: Long
) : ViewModelSimple(vibrador) {
    private val _ui = MutableStateFlow(UsuarioDetalleUiState())
    val ui: StateFlow<UsuarioDetalleUiState> = _ui.asStateFlow()

    init { recargar() }

    fun recargar() {
        lanzar {
            when (val r = fuente.obtener(idUsuario)) {
                is Operacion.Exito -> aplicar(r.valor)
                is Operacion.Fallo -> _ui.update { it.copy(cargando = false, error = r.mensaje) }
            }
        }
    }

    private fun aplicar(u: Usuario) {
        val s = fuente.sesion.value
        _ui.update {
            it.copy(
                cargando = false, usuario = u, error = null, procesando = false,
                puedeEditar = s != null && ReglasCuentas.puedeEditarPerfil(s, u),
                puedeCambiarEstado = s != null && ReglasCuentas.puedeAdministrar(s, u),
                puedeEliminar = s != null && ReglasCuentas.puedeEliminar(s, u)
            )
        }
    }

    fun alternarActivo() {
        val u = _ui.value.usuario ?: return
        if (!_ui.value.puedeCambiarEstado) { mostrar("No tiene permiso para administrar esta cuenta"); return }
        _ui.update { it.copy(procesando = true) }
        lanzar {
            when (val r = fuente.cambiarEstado(u.id, !u.activo)) {
                is Operacion.Exito -> {
                    vibrar(TipoVibracion.EXITO)
                    aplicar(r.valor)
                    mostrar(if (r.valor.activo) "Cuenta activada" else "Cuenta desactivada: ya no puede iniciar sesión")
                }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(procesando = false) }
                    mostrar(r.mensaje)
                }
            }
        }
    }

    fun eliminar() {
        val u = _ui.value.usuario ?: return
        if (!_ui.value.puedeEliminar) { mostrar("Solo un administrador puede eliminar cuentas"); return }
        _ui.update { it.copy(procesando = true) }
        lanzar {
            when (val r = fuente.eliminar(u.id)) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(procesando = false, eliminado = true) } }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); _ui.update { it.copy(procesando = false) }; mostrar(r.mensaje) }
            }
        }
    }

    companion object {
        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as InduztekApp).contenedor
                UsuarioDetalleViewModel(c.usuarios, c.haptica, createSavedStateHandle().get<Long>("id") ?: 0L)
            }
        }
    }
}
