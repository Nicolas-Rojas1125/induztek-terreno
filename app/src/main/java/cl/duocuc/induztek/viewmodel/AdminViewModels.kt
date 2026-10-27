package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import cl.duocuc.induztek.domain.model.Calibracion
import cl.duocuc.induztek.domain.model.CategoriaInstrumento
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Instrumento
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Usuario
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.domain.validacion.Validadores
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class InstrumentosUiState(
    val cargando: Boolean = true,
    val instrumentos: List<Instrumento> = emptyList(),
    val puedeGestionar: Boolean = false
)

class InstrumentosViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(InstrumentosUiState())
    val ui: StateFlow<InstrumentosUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        _ui.value = InstrumentosUiState(false, c.instrumentos.listar(), s.puede(Permiso.GESTIONAR_INSTRUMENTOS))
    }
}

data class InstrumentoFormUiState(
    val nombre: Campo = Campo(),
    val categoria: CategoriaInstrumento? = null,
    val errorCategoria: String? = null,
    val marca: Campo = Campo(),
    val modelo: Campo = Campo(),
    val serie: Campo = Campo(),
    val fechaCalibracion: LocalDate? = null,
    val errorCalibracion: String? = null,
    val fechaVencimiento: LocalDate? = null,
    val errorVencimiento: String? = null,
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val guardadoId: Long? = null
)

class InstrumentoFormViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(InstrumentoFormUiState())
    val ui: StateFlow<InstrumentoFormUiState> = _ui.asStateFlow()

    private val vNombre: (String) -> String? = { Validadores.texto(it, "El nombre", 3, 60) }
    private val vMarca: (String) -> String? = { Validadores.texto(it, "La marca", 2, 40) }
    private val vModelo: (String) -> String? = { Validadores.texto(it, "El modelo", 2, 40) }

    fun onNombre(v: String) = _ui.update { it.copy(nombre = it.nombre.cambiar(v, vNombre)) }
    fun onCategoria(cat: CategoriaInstrumento) = _ui.update {
        it.copy(categoria = cat, errorCategoria = null, nombre = if (it.nombre.valor.isBlank()) Campo(cat.etiqueta, null, true) else it.nombre)
    }
    fun onMarca(v: String) = _ui.update { it.copy(marca = it.marca.cambiar(v, vMarca)) }
    fun onModelo(v: String) = _ui.update { it.copy(modelo = it.modelo.cambiar(v, vModelo)) }
    fun onSerie(v: String) = _ui.update { it.copy(serie = it.serie.cambiar(v.uppercase(), Validadores::numeroSerie)) }
    fun onFechaCalibracion(f: LocalDate) = _ui.update {
        it.copy(fechaCalibracion = f, errorCalibracion = null, fechaVencimiento = it.fechaVencimiento ?: f.plusYears(1))
    }
    fun onFechaVencimiento(f: LocalDate) = _ui.update { it.copy(fechaVencimiento = f, errorVencimiento = null) }

    fun guardar() {
        val s = sesion ?: return
        if (!s.puede(Permiso.GESTIONAR_INSTRUMENTOS)) { mostrar("Solo el Jefe técnico registra instrumentos (RN-12)"); return }
        val u = _ui.value
        val v = u.copy(
            nombre = u.nombre.validar(vNombre),
            errorCategoria = Validadores.seleccion(u.categoria, "la categoría"),
            marca = u.marca.validar(vMarca),
            modelo = u.modelo.validar(vModelo),
            serie = u.serie.validar(Validadores::numeroSerie),
            errorCalibracion = Validadores.fechaNoFutura(u.fechaCalibracion, "la fecha de calibración"),
            errorVencimiento = Validadores.fechaVencimiento(u.fechaCalibracion, u.fechaVencimiento)
        )
        if (listOf(v.nombre.error, v.errorCategoria, v.marca.error, v.modelo.error, v.serie.error, v.errorCalibracion, v.errorVencimiento).any { it != null }) {
            _ui.value = v.copy(intentosFallidos = u.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.value = v.copy(guardando = true)
        lanzar {
            val r = c.instrumentos.crear(
                Instrumento(nombre = v.nombre.valor, categoria = v.categoria!!, marca = v.marca.valor, modelo = v.modelo.valor, numeroSerie = v.serie.valor),
                v.fechaCalibracion!!, v.fechaVencimiento!!, s.idUsuario
            )
            when (r) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(guardando = false, guardadoId = r.valor) } }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(guardando = false, serie = it.serie.copy(error = r.mensaje), intentosFallidos = it.intentosFallidos + 1) }
                }
            }
        }
    }
}

data class InstrumentoDetalleUiState(
    val cargando: Boolean = true,
    val instrumento: Instrumento? = null,
    val calibraciones: List<Calibracion> = emptyList(),
    val puedeGestionar: Boolean = false,
    val mostrarFormulario: Boolean = false,
    val fechaCalibracion: LocalDate? = Formato.hoy(),
    val fechaVencimiento: LocalDate? = Formato.hoy().plusYears(1),
    val errorCalibracion: String? = null,
    val errorVencimiento: String? = null,
    val guardando: Boolean = false
)

class InstrumentoDetalleViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idInstrumento: Long = estado.get<Long>("id") ?: 0L
    private val _ui = MutableStateFlow(InstrumentoDetalleUiState())
    val ui: StateFlow<InstrumentoDetalleUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        _ui.update {
            it.copy(
                cargando = false,
                instrumento = c.instrumentos.obtener(idInstrumento),
                calibraciones = c.instrumentos.calibraciones(idInstrumento),
                puedeGestionar = s.puede(Permiso.GESTIONAR_INSTRUMENTOS)
            )
        }
    }

    fun alternarFormulario() = _ui.update { it.copy(mostrarFormulario = !it.mostrarFormulario) }
    fun onFechaCalibracion(f: LocalDate) = _ui.update { it.copy(fechaCalibracion = f, fechaVencimiento = f.plusYears(1), errorCalibracion = null, errorVencimiento = null) }
    fun onFechaVencimiento(f: LocalDate) = _ui.update { it.copy(fechaVencimiento = f, errorVencimiento = null) }

    fun registrarCalibracion() {
        val s = sesion ?: return
        if (!s.puede(Permiso.GESTIONAR_INSTRUMENTOS)) { mostrar("Solo el Jefe técnico actualiza calibraciones (RN-12)"); return }
        val u = _ui.value
        val ultima = u.calibraciones.firstOrNull()?.fechaCalibracion
        val errCal = Validadores.fechaNoFutura(u.fechaCalibracion, "la fecha de calibración")
            ?: if (ultima != null && u.fechaCalibracion != null && !u.fechaCalibracion.isAfter(ultima))
                "Debe ser posterior a la última calibración (${Formato.fecha(ultima)})" else null
        val errVen = Validadores.fechaVencimiento(u.fechaCalibracion, u.fechaVencimiento)
        if (errCal != null || errVen != null) {
            _ui.update { it.copy(errorCalibracion = errCal, errorVencimiento = errVen) }
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.update { it.copy(guardando = true) }
        lanzar {
            val r = c.instrumentos.registrarCalibracion(
                Calibracion(idInstrumento = idInstrumento, fechaCalibracion = u.fechaCalibracion!!, fechaVencimiento = u.fechaVencimiento!!),
                s.idUsuario
            )
            when (r) {
                is Operacion.Exito -> {
                    vibrar(TipoVibracion.EXITO)
                    _ui.update { it.copy(guardando = false, mostrarFormulario = false) }
                    mostrar("Calibración registrada")
                }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); _ui.update { it.copy(guardando = false) }; mostrar(r.mensaje) }
            }
        }
    }
}

data class UsuariosUiState(
    val cargando: Boolean = true,
    val usuarios: List<Usuario> = emptyList(),
    val busqueda: String = "",
    val rol: RolCodigo? = null,
    val idPropio: Long = 0,
    val puedeGestionar: Boolean = false
)

class UsuariosViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(UsuariosUiState())
    val ui: StateFlow<UsuariosUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        val u = _ui.value
        _ui.update {
            it.copy(
                cargando = false, usuarios = c.usuarios.listar(u.busqueda, u.rol), idPropio = s.idUsuario,
                puedeGestionar = s.puede(Permiso.GESTIONAR_PERSONAL)
            )
        }
    }

    fun onBuscar(t: String) { _ui.update { it.copy(busqueda = t) }; lanzar { cargar() } }
    fun onRol(r: RolCodigo?) { _ui.update { it.copy(rol = if (it.rol == r) null else r) }; lanzar { cargar() } }

    fun cambiarActivo(u: Usuario) {
        val s = sesion ?: return
        if (!s.puede(Permiso.GESTIONAR_PERSONAL)) return
        if (u.id == s.idUsuario) { mostrar("No puede desactivar su propia cuenta"); return }
        lanzar {
            when (val r = c.usuarios.cambiarActivo(u.id, !u.activo)) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); mostrar(if (u.activo) "${u.nombre} desactivado" else "${u.nombre} activado") }
                is Operacion.Fallo -> mostrar(r.mensaje)
            }
        }
    }
}

data class UsuarioFormUiState(
    val rol: RolCodigo = RolCodigo.TECNICO,
    val nombre: Campo = Campo(),
    val rut: Campo = Campo(),
    val correo: Campo = Campo(),
    val telefono: Campo = Campo(),
    val cargo: Campo = Campo(),
    val contrasena: Campo = Campo(),
    val contrasenaVisible: Boolean = false,
    val clientes: List<Cliente> = emptyList(),
    val cliente: Cliente? = null,
    val errorCliente: String? = null,
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val guardadoId: Long? = null
)

class UsuarioFormViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(UsuarioFormUiState())
    val ui: StateFlow<UsuarioFormUiState> = _ui.asStateFlow()

    private val vCargo: (String) -> String? = { Validadores.texto(it, "El cargo", 3, 80) }
    private val vTelefono: (String) -> String? = { Validadores.telefono(it) }

    init {
        lanzar {
            val s = sesion ?: return@lanzar
            _ui.update { it.copy(clientes = c.clientes.listarClientes(s)) }
        }
    }

    fun onRol(r: RolCodigo) = _ui.update { it.copy(rol = r, errorCliente = null) }
    fun onNombre(v: String) = _ui.update { it.copy(nombre = it.nombre.cambiar(v, Validadores::nombrePersona)) }
    fun onRut(v: String) = _ui.update { it.copy(rut = it.rut.cambiar(v, Validadores::rut)) }
    fun onCorreo(v: String) = _ui.update { it.copy(correo = it.correo.cambiar(v.trim(), Validadores::correo)) }
    fun onTelefono(v: String) = _ui.update { it.copy(telefono = it.telefono.cambiar(v, vTelefono)) }
    fun onCargo(v: String) = _ui.update { it.copy(cargo = it.cargo.cambiar(v, vCargo)) }
    fun onContrasena(v: String) = _ui.update { it.copy(contrasena = it.contrasena.cambiar(v, Validadores::contrasena)) }
    fun onAlternarVisible() = _ui.update { it.copy(contrasenaVisible = !it.contrasenaVisible) }
    fun onCliente(cl: Cliente) = _ui.update { it.copy(cliente = cl, errorCliente = null) }

    fun guardar() {
        val s = sesion ?: return
        val u = _ui.value
        val permiso = if (u.rol == RolCodigo.CLIENTE) Permiso.OTORGAR_CUENTAS_CLIENTE else Permiso.GESTIONAR_PERSONAL
        if (!s.puede(permiso)) { mostrar("Solo el Jefe técnico otorga cuentas (RN-20)"); return }
        val v = u.copy(
            nombre = u.nombre.validar(Validadores::nombrePersona),
            rut = u.rut.validar(Validadores::rut),
            correo = u.correo.validar(Validadores::correo),
            telefono = u.telefono.validar(vTelefono),
            cargo = u.cargo.validar(vCargo),
            contrasena = u.contrasena.validar(Validadores::contrasena),
            errorCliente = if (u.rol == RolCodigo.CLIENTE && u.cliente == null) "Seleccione la empresa cliente (RN-18)" else null
        )
        val errores = listOf(v.nombre, v.rut, v.correo, v.telefono, v.cargo, v.contrasena).any { it.error != null } || v.errorCliente != null
        if (errores) {
            _ui.value = v.copy(intentosFallidos = u.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.value = v.copy(guardando = true)
        lanzar {
            when {
                c.usuarios.existeRut(v.rut.valor) -> {
                    _ui.update { it.copy(guardando = false, rut = it.rut.copy(error = "Ya existe un usuario con ese RUT"), intentosFallidos = it.intentosFallidos + 1) }
                    vibrar(TipoVibracion.ERROR)
                }
                c.usuarios.existeCorreo(v.correo.valor) -> {
                    _ui.update { it.copy(guardando = false, correo = it.correo.copy(error = "Ese correo ya está en uso"), intentosFallidos = it.intentosFallidos + 1) }
                    vibrar(TipoVibracion.ERROR)
                }
                else -> {
                    val r = c.usuarios.crear(
                        Usuario(
                            rol = v.rol, rut = v.rut.valor, nombre = v.nombre.valor, correo = v.correo.valor,
                            telefono = v.telefono.valor, cargo = v.cargo.valor, idCliente = v.cliente?.id
                        ),
                        v.contrasena.valor
                    )
                    when (r) {
                        is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(guardando = false, guardadoId = r.valor) } }
                        is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); _ui.update { it.copy(guardando = false) }; mostrar(r.mensaje) }
                    }
                }
            }
        }
    }
}
