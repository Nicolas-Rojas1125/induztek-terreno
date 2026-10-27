package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Comuna
import cl.duocuc.induztek.domain.model.Equipo
import cl.duocuc.induztek.domain.model.Instalacion
import cl.duocuc.induztek.domain.model.Operacion
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
    val clientes: List<Cliente> = emptyList(),
    val busqueda: String = "",
    val puedeCrear: Boolean = false
)

class ClientesViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(ClientesUiState())
    val ui: StateFlow<ClientesUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        _ui.update {
            it.copy(
                cargando = false,
                clientes = c.clientes.listarClientes(s, it.busqueda),
                puedeCrear = s.puede(Permiso.GESTIONAR_CLIENTES_EQUIPOS)
            )
        }
    }

    fun onBuscar(texto: String) {
        _ui.update { it.copy(busqueda = texto) }
        lanzar { cargar() }
    }
}

data class ClienteFormUiState(
    val razonSocial: Campo = Campo(),
    val rut: Campo = Campo(),
    val giro: Campo = Campo(),
    val contacto: Campo = Campo(),
    val telefono: Campo = Campo(),
    val correo: Campo = Campo(),
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val guardadoId: Long? = null
)

class ClienteFormViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(ClienteFormUiState())
    val ui: StateFlow<ClienteFormUiState> = _ui.asStateFlow()

    private val vRazon: (String) -> String? = { Validadores.texto(it, "La razón social", 3, 120) }
    private val vGiro: (String) -> String? = { Validadores.texto(it, "El giro", 3, 80) }
    private val vTelefono: (String) -> String? = { Validadores.telefono(it) }

    fun onRazon(v: String) = _ui.update { it.copy(razonSocial = it.razonSocial.cambiar(v, vRazon)) }
    fun onRut(v: String) = _ui.update { it.copy(rut = it.rut.cambiar(v, Validadores::rut)) }
    fun onGiro(v: String) = _ui.update { it.copy(giro = it.giro.cambiar(v, vGiro)) }
    fun onContacto(v: String) = _ui.update { it.copy(contacto = it.contacto.cambiar(v, Validadores::nombrePersona)) }
    fun onTelefono(v: String) = _ui.update { it.copy(telefono = it.telefono.cambiar(v, vTelefono)) }
    fun onCorreo(v: String) = _ui.update { it.copy(correo = it.correo.cambiar(v.trim(), Validadores::correo)) }

    fun guardar() {
        val s = sesion ?: return
        if (!s.puede(Permiso.GESTIONAR_CLIENTES_EQUIPOS)) { mostrar("No tiene permiso para registrar clientes"); return }
        val u = _ui.value
        val validado = u.copy(
            razonSocial = u.razonSocial.validar(vRazon),
            rut = u.rut.validar(Validadores::rut),
            giro = u.giro.validar(vGiro),
            contacto = u.contacto.validar(Validadores::nombrePersona),
            telefono = u.telefono.validar(vTelefono),
            correo = u.correo.validar(Validadores::correo)
        )
        val campos = listOf(validado.razonSocial, validado.rut, validado.giro, validado.contacto, validado.telefono, validado.correo)
        if (campos.any { it.error != null }) {
            _ui.value = validado.copy(intentosFallidos = u.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.value = validado.copy(guardando = true)
        lanzar {
            if (c.clientes.existeRutCliente(validado.rut.valor)) {
                _ui.update { it.copy(guardando = false, rut = it.rut.copy(error = "Ya existe un cliente con ese RUT"), intentosFallidos = it.intentosFallidos + 1) }
                vibrar(TipoVibracion.ERROR)
                return@lanzar
            }
            val r = c.clientes.crearCliente(
                Cliente(
                    rut = validado.rut.valor, razonSocial = validado.razonSocial.valor, giro = validado.giro.valor,
                    contactoNombre = validado.contacto.valor, telefono = validado.telefono.valor, correo = validado.correo.valor
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
    val puedeEditar: Boolean = false
)

class ClienteDetalleViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idCliente: Long = estado.get<Long>("id") ?: 0L
    private val _ui = MutableStateFlow(ClienteDetalleUiState())
    val ui: StateFlow<ClienteDetalleUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        _ui.value = ClienteDetalleUiState(
            cargando = false,
            cliente = c.clientes.obtenerCliente(idCliente, s),
            instalaciones = c.clientes.listarInstalaciones(s, idCliente),
            puedeEditar = s.puede(Permiso.GESTIONAR_CLIENTES_EQUIPOS)
        )
    }
}

data class InstalacionFormUiState(
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
    private val idCliente: Long = estado.get<Long>("clienteId") ?: 0L
    private val _ui = MutableStateFlow(InstalacionFormUiState())
    val ui: StateFlow<InstalacionFormUiState> = _ui.asStateFlow()

    private val vNombre: (String) -> String? = { Validadores.texto(it, "El nombre", 3, 80) }
    private val vUbicacion: (String) -> String? = { Validadores.texto(it, "La ubicación", 3, 120) }
    private val vObs: (String) -> String? = { Validadores.textoOpcional(it, "Las observaciones", 300) }

    init {
        lanzar {
            val s = sesion ?: return@lanzar
            val regiones = c.clientes.regiones()
            val region = regiones.firstOrNull()
            _ui.update {
                it.copy(
                    cliente = c.clientes.obtenerCliente(idCliente, s),
                    regiones = regiones,
                    region = region,
                    comunas = region?.let { r -> c.clientes.comunas(r.id) }.orEmpty()
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
        _ui.value = v.copy(guardando = true)
        lanzar {
            val r = c.clientes.crearInstalacion(
                Instalacion(
                    idCliente = idCliente, nombre = v.nombre.valor, ubicacion = v.ubicacion.valor,
                    idComuna = v.comuna!!.id, observacionesAcceso = v.observaciones.valor,
                    latitud = v.latitud, longitud = v.longitud
                )
            )
            when (r) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(guardando = false, guardadoId = r.valor) } }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(guardando = false, nombre = it.nombre.copy(error = r.mensaje), intentosFallidos = it.intentosFallidos + 1) }
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
    val visitas: List<Visita> = emptyList()
)

class InstalacionDetalleViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idInstalacion: Long = estado.get<Long>("id") ?: 0L
    private val _ui = MutableStateFlow(InstalacionDetalleUiState())
    val ui: StateFlow<InstalacionDetalleUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        _ui.value = InstalacionDetalleUiState(
            cargando = false,
            sesion = s,
            instalacion = c.clientes.obtenerInstalacion(idInstalacion, s),
            equipos = c.equipos.buscar(s, idInstalacion = idInstalacion),
            visitas = c.visitas.listar(s).filter { it.idInstalacion == idInstalacion }
        )
    }
}
