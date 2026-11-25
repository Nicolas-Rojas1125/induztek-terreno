package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Comuna
import cl.duocuc.induztek.domain.model.Equipo
import cl.duocuc.induztek.domain.model.Instalacion
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.Region
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.Visita
import cl.duocuc.induztek.domain.validacion.Validadores
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ClientesUiState(
    val cargando: Boolean = true,
    val pagina: Pagina<Cliente> = Pagina.vacia(),
    val busqueda: String = "",
    val puedeCrear: Boolean = false
)

class ClientesViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(ClientesUiState())
    val ui: StateFlow<ClientesUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar(pagina: Int = _ui.value.pagina.pagina) {
        val s = sesion ?: return
        val resultado = c.clientes.paginaClientes(s, _ui.value.busqueda, pagina)
        _ui.update { it.copy(cargando = false, pagina = resultado, puedeCrear = s.puede(Permiso.GESTIONAR_CLIENTES_EQUIPOS)) }
    }

    fun onBuscar(texto: String) {
        _ui.update { it.copy(busqueda = texto) }
        lanzar { cargar(0) }
    }

    fun irAPagina(pagina: Int) = lanzar { cargar(pagina) }

    fun sincronizar() {
        c.solicitarSincronizacion()
        mostrar("Actualizando datos desde el servidor…")
    }
}

data class ClienteFormUiState(
    val editando: Boolean = false,
    val razonSocial: Campo = Campo(),
    val rut: Campo = Campo(),
    val giro: Campo = Campo(),
    val contacto: Campo = Campo(),
    val telefono: Campo = Campo(),
    val correo: Campo = Campo(),
    val direccion: Campo = Campo(),
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val guardadoId: Long? = null
)

class ClienteFormViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idCliente: Long? = estado.get<Long>("id")
    private val _ui = MutableStateFlow(ClienteFormUiState(editando = idCliente != null))
    val ui: StateFlow<ClienteFormUiState> = _ui.asStateFlow()

    private val vRazon: (String) -> String? = { Validadores.texto(it, "La razón social", 3, 120) }
    private val vGiro: (String) -> String? = { Validadores.texto(it, "El giro", 3, 80) }
    private val vTelefono: (String) -> String? = { Validadores.telefono(it) }
    private val vDireccion: (String) -> String? = { Validadores.direccion(it) }

    init {
        if (idCliente != null) {
            lanzar {
                val s = sesion ?: return@lanzar
                val cl = c.clientes.obtenerCliente(idCliente, s) ?: return@lanzar
                _ui.update {
                    it.copy(
                        razonSocial = Campo(cl.razonSocial, null, true), rut = Campo(cl.rut, null, true),
                        giro = Campo(cl.giro, null, true), contacto = Campo(cl.contactoNombre, null, true),
                        telefono = Campo(cl.telefono, null, true), correo = Campo(cl.correo, null, true),
                        direccion = Campo(cl.direccion, null, true)
                    )
                }
            }
        }
    }

    fun onRazon(v: String) = _ui.update { it.copy(razonSocial = it.razonSocial.cambiar(v, vRazon)) }
    fun onRut(v: String) = _ui.update { it.copy(rut = it.rut.cambiar(v, Validadores::rut)) }
    fun onGiro(v: String) = _ui.update { it.copy(giro = it.giro.cambiar(v, vGiro)) }
    fun onContacto(v: String) = _ui.update { it.copy(contacto = it.contacto.cambiar(v, Validadores::nombrePersona)) }
    fun onTelefono(v: String) = _ui.update { it.copy(telefono = it.telefono.cambiar(v, vTelefono)) }
    fun onCorreo(v: String) = _ui.update { it.copy(correo = it.correo.cambiar(v.trim(), Validadores::correo)) }
    fun onDireccion(v: String) = _ui.update { it.copy(direccion = it.direccion.cambiar(v, vDireccion)) }

    fun guardar() {
        val s = sesion ?: return
        if (!s.puede(Permiso.GESTIONAR_CLIENTES_EQUIPOS)) { mostrar("No tiene permiso para registrar clientes"); return }
        val u = _ui.value
        if (u.guardando) return
        val validado = u.copy(
            razonSocial = u.razonSocial.validar(vRazon),
            rut = u.rut.validar(Validadores::rut),
            giro = u.giro.validar(vGiro),
            contacto = u.contacto.validar(Validadores::nombrePersona),
            telefono = u.telefono.validar(vTelefono),
            correo = u.correo.validar(Validadores::correo),
            direccion = u.direccion.validar(vDireccion)
        )
        val campos = listOf(
            validado.razonSocial, validado.rut, validado.giro, validado.contacto, validado.telefono, validado.correo, validado.direccion
        )
        if (campos.any { it.error != null }) {
            _ui.value = validado.copy(intentosFallidos = u.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.value = validado.copy(guardando = true)
        lanzar {
            if (idCliente == null && c.clientes.existeRutCliente(validado.rut.valor)) {
                _ui.update { it.copy(guardando = false, rut = it.rut.copy(error = "Ya existe un cliente con ese RUT"), intentosFallidos = it.intentosFallidos + 1) }
                vibrar(TipoVibracion.ERROR)
                return@lanzar
            }
            val r = c.clientes.guardarCliente(
                s.idUsuario,
                Cliente(
                    id = idCliente ?: 0L, rut = validado.rut.valor, razonSocial = validado.razonSocial.valor,
                    giro = validado.giro.valor, contactoNombre = validado.contacto.valor, telefono = validado.telefono.valor,
                    correo = validado.correo.valor, direccion = validado.direccion.valor
                )
            )
            when (r) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(guardando = false, guardadoId = r.valor) } }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); _ui.update { it.copy(guardando = false) }; mostrar(r.mensaje) }
            }
        }
    }
}

data class ClienteDetalleUiState(
    val cargando: Boolean = true,
    val cliente: Cliente? = null,
    val instalaciones: List<Instalacion> = emptyList(),
    val cuentasCliente: Int = 0,
    val puedeEditar: Boolean = false,
    val puedeEliminar: Boolean = false,
    val eliminado: Boolean = false
)

class ClienteDetalleViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idCliente: Long = estado.get<Long>("id") ?: 0L
    private val _ui = MutableStateFlow(ClienteDetalleUiState())
    val ui: StateFlow<ClienteDetalleUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        _ui.update {
            it.copy(
                cargando = false,
                cliente = c.clientes.obtenerCliente(idCliente, s),
                instalaciones = c.clientes.listarInstalaciones(s, idCliente),
                cuentasCliente = c.usuarios.activosPorRol(cl.duocuc.induztek.domain.model.RolCodigo.CLIENTE).count { u -> u.idCliente == idCliente },
                puedeEditar = s.puede(Permiso.GESTIONAR_CLIENTES_EQUIPOS),
                puedeEliminar = s.puede(Permiso.ELIMINAR_REGISTROS)
            )
        }
    }

    fun eliminar() {
        lanzar {
            when (val r = c.clientes.eliminarCliente(idCliente)) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(eliminado = true) } }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); mostrar(r.mensaje) }
            }
        }
    }
}

data class InstalacionFormUiState(
    val editando: Boolean = false,
    val cliente: Cliente? = null,
    val nombre: Campo = Campo(),
    val ubicacion: Campo = Campo(),
    val observaciones: Campo = Campo(),
    val regiones: List<Region> = emptyList(),
    val region: Region? = null,
    val comunas: List<Comuna> = emptyList(),
    val comuna: Comuna? = null,
    val errorComuna: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val precision: Float? = null,
    val obteniendoUbicacion: Boolean = false,
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val guardadoId: Long? = null
)

class InstalacionFormViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idInstalacion: Long? = estado.get<Long>("id")
    private var idCliente: Long = estado.get<Long>("clienteId") ?: 0L
    private val _ui = MutableStateFlow(InstalacionFormUiState(editando = idInstalacion != null))
    val ui: StateFlow<InstalacionFormUiState> = _ui.asStateFlow()

    private val vNombre: (String) -> String? = { Validadores.texto(it, "El nombre", 3, 80) }
    private val vUbicacion: (String) -> String? = { Validadores.texto(it, "La ubicación", 3, 120) }
    private val vObs: (String) -> String? = { Validadores.textoOpcional(it, "Las observaciones", 300) }

    init {
        lanzar {
            val s = sesion ?: return@lanzar
            val regiones = c.clientes.regiones()
            val existente = idInstalacion?.let { c.clientes.obtenerInstalacion(it, s) }
            if (existente != null) idCliente = existente.idCliente
            val comunaExistente = existente?.let { c.clientes.comuna(it.idComuna) }
            val region = comunaExistente?.let { co -> regiones.firstOrNull { it.id == co.idRegion } } ?: regiones.firstOrNull()
            val comunas = region?.let { r -> c.clientes.comunas(r.id) }.orEmpty()
            _ui.update {
                it.copy(
                    cliente = c.clientes.obtenerCliente(idCliente, s),
                    regiones = regiones,
                    region = region,
                    comunas = comunas,
                    comuna = comunaExistente?.let { co -> comunas.firstOrNull { x -> x.id == co.id } },
                    nombre = existente?.let { e -> Campo(e.nombre, null, true) } ?: it.nombre,
                    ubicacion = existente?.let { e -> Campo(e.ubicacion, null, true) } ?: it.ubicacion,
                    observaciones = existente?.let { e -> Campo(e.observacionesAcceso, null, true) } ?: it.observaciones,
                    latitud = existente?.latitud,
                    longitud = existente?.longitud
                )
            }
        }
    }

    fun onNombre(v: String) = _ui.update { it.copy(nombre = it.nombre.cambiar(v, vNombre)) }
    fun onUbicacion(v: String) = _ui.update { it.copy(ubicacion = it.ubicacion.cambiar(v, vUbicacion)) }
    fun onObservaciones(v: String) = _ui.update { it.copy(observaciones = it.observaciones.cambiar(v, vObs)) }

    fun onRegion(r: Region) {
        _ui.update { it.copy(region = r, comuna = null, comunas = emptyList()) }
        lanzar { _ui.update { it.copy(comunas = c.clientes.comunas(r.id)) } }
    }

    fun onComuna(co: Comuna) = _ui.update { it.copy(comuna = co, errorComuna = null) }

    fun capturarUbicacion() {
        if (!c.ubicacion.tienePermiso()) { mostrar("Permiso de ubicación denegado"); return }
        if (!c.ubicacion.gpsActivo()) { mostrar("Active la ubicación (GPS) del teléfono"); vibrar(TipoVibracion.ADVERTENCIA); return }
        _ui.update { it.copy(obteniendoUbicacion = true) }
        lanzar {
            val coord = c.ubicacion.obtener()
            if (coord == null) {
                _ui.update { it.copy(obteniendoUbicacion = false) }
                mostrar("No se pudo obtener la ubicación. Intente a cielo abierto.")
                vibrar(TipoVibracion.ADVERTENCIA)
            } else {
                _ui.update {
                    it.copy(obteniendoUbicacion = false, latitud = coord.latitud, longitud = coord.longitud, precision = coord.precisionMetros)
                }
                vibrar(TipoVibracion.EXITO)
            }
        }
    }

    fun quitarUbicacion() = _ui.update { it.copy(latitud = null, longitud = null, precision = null) }

    fun onPermisoDenegado() = mostrar("Sin permiso de ubicación: puede guardar la instalación sin coordenadas")

    fun guardar() {
        val s = sesion ?: return
        if (!s.puede(Permiso.GESTIONAR_CLIENTES_EQUIPOS)) { mostrar("No tiene permiso para registrar instalaciones"); return }
        val u = _ui.value
        if (u.guardando) return
        val v = u.copy(
            nombre = u.nombre.validar(vNombre),
            ubicacion = u.ubicacion.validar(vUbicacion),
            observaciones = u.observaciones.validar(vObs),
            errorComuna = Validadores.seleccion(u.comuna, "la comuna")
        )
        if (listOf(v.nombre.error, v.ubicacion.error, v.observaciones.error, v.errorComuna).any { it != null }) {
            _ui.value = v.copy(intentosFallidos = u.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            return
        }
        val comuna = v.comuna ?: return
        _ui.value = v.copy(guardando = true)
        lanzar {
            val r = c.clientes.guardarInstalacion(
                s.idUsuario,
                Instalacion(
                    id = idInstalacion ?: 0L, idCliente = idCliente, nombre = v.nombre.valor, ubicacion = v.ubicacion.valor,
                    idComuna = comuna.id, observacionesAcceso = v.observaciones.valor,
                    latitud = v.latitud, longitud = v.longitud
                )
            )
            when (r) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(guardando = false, guardadoId = r.valor) } }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(guardando = false, intentosFallidos = it.intentosFallidos + 1) }
                    mostrar(r.mensaje)
                }
            }
        }
    }
}

data class InstalacionDetalleUiState(
    val cargando: Boolean = true,
    val sesion: Sesion? = null,
    val instalacion: Instalacion? = null,
    val equipos: List<Equipo> = emptyList(),
    val visitas: List<Visita> = emptyList(),
    val puedeEditar: Boolean = false,
    val puedeEliminar: Boolean = false,
    val eliminada: Boolean = false
)

class InstalacionDetalleViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idInstalacion: Long = estado.get<Long>("id") ?: 0L
    private val _ui = MutableStateFlow(InstalacionDetalleUiState())
    val ui: StateFlow<InstalacionDetalleUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        _ui.update {
            it.copy(
                cargando = false,
                sesion = s,
                instalacion = c.clientes.obtenerInstalacion(idInstalacion, s),
                equipos = c.equipos.buscar(s, idInstalacion = idInstalacion),
                visitas = c.visitas.listar(s).filter { v -> v.idInstalacion == idInstalacion },
                puedeEditar = s.puede(Permiso.GESTIONAR_CLIENTES_EQUIPOS),
                puedeEliminar = s.puede(Permiso.ELIMINAR_REGISTROS)
            )
        }
    }

    fun eliminar() {
        lanzar {
            when (val r = c.clientes.eliminarInstalacion(idInstalacion)) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(eliminada = true) } }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); mostrar(r.mensaje) }
            }
        }
    }
}
