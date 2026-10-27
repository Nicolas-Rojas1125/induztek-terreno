package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import cl.duocuc.induztek.domain.model.ActaTerreno
import cl.duocuc.induztek.domain.model.Equipo
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.Instalacion
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.ProtocoloResumen
import cl.duocuc.induztek.domain.model.RecepcionActa
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.TipoVisita
import cl.duocuc.induztek.domain.model.Usuario
import cl.duocuc.induztek.domain.model.Visita
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.domain.validacion.Validadores
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

enum class FiltroVisitas(val etiqueta: String) { ABIERTAS("Abiertas"), MIAS("Donde participo"), TODAS("Todas") }

data class VisitasUiState(
    val cargando: Boolean = true,
    val visitas: List<Visita> = emptyList(),
    val filtro: FiltroVisitas = FiltroVisitas.ABIERTAS,
    val esCliente: Boolean = false,
    val puedeCrear: Boolean = false
)

class VisitasViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(VisitasUiState())
    val ui: StateFlow<VisitasUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        val f = _ui.value.filtro
        val lista = if (s.esCliente) c.visitas.listar(s) else c.visitas.listar(
            s, soloAbiertas = f == FiltroVisitas.ABIERTAS, soloMias = f == FiltroVisitas.MIAS
        )
        _ui.update {
            it.copy(
                cargando = false, visitas = lista, esCliente = s.esCliente,
                puedeCrear = s.puede(Permiso.REGISTRAR_PROTOCOLO)
            )
        }
    }

    fun onFiltro(f: FiltroVisitas) {
        _ui.update { it.copy(filtro = f) }
        lanzar { cargar() }
    }
}

data class VisitaFormUiState(
    val ot: Campo = Campo(),
    val instalaciones: List<Instalacion> = emptyList(),
    val instalacion: Instalacion? = null,
    val errorInstalacion: String? = null,
    val fecha: LocalDate? = Formato.hoy(),
    val errorFecha: String? = null,
    val tipo: TipoVisita = TipoVisita.SOLO_PRUEBAS,
    val ingenieros: List<Usuario> = emptyList(),
    val ingeniero: Usuario? = null,
    val errorIngeniero: String? = null,
    val personal: List<Usuario> = emptyList(),
    val participantes: Set<Long> = emptySet(),
    val errorParticipantes: String? = null,
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val guardadoId: Long? = null
)

class VisitaFormViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val instalacionInicial: Long? = estado.get<Long>("instalacionId")?.takeIf { it > 0 }
    private val _ui = MutableStateFlow(VisitaFormUiState())
    val ui: StateFlow<VisitaFormUiState> = _ui.asStateFlow()

    init {
        lanzar {
            val s = sesion ?: return@lanzar
            val instalaciones = c.clientes.listarInstalaciones(s)
            val ingenieros = c.usuarios.activosPorRol(RolCodigo.INGENIERO)
            val personal = c.usuarios.activosPorRol(RolCodigo.TECNICO, RolCodigo.INGENIERO)
            _ui.update {
                it.copy(
                    instalaciones = instalaciones,
                    instalacion = instalaciones.firstOrNull { i -> i.id == instalacionInicial },
                    ingenieros = ingenieros,
                    ingeniero = ingenieros.firstOrNull { u -> u.id == s.idUsuario },
                    personal = personal,
                    participantes = if (s.rol == RolCodigo.TECNICO || s.rol == RolCodigo.INGENIERO) setOf(s.idUsuario) else emptySet()
                )
            }
        }
    }

    fun onOt(v: String) = _ui.update { it.copy(ot = it.ot.cambiar(v.uppercase().trim(), Validadores::numeroOt)) }
    fun onInstalacion(i: Instalacion) = _ui.update { it.copy(instalacion = i, errorInstalacion = null) }
    fun onFecha(f: LocalDate) = _ui.update { it.copy(fecha = f, errorFecha = null) }
    fun onTipo(t: TipoVisita) = _ui.update { it.copy(tipo = t) }
    fun onIngeniero(u: Usuario) = _ui.update { it.copy(ingeniero = u, errorIngeniero = null) }
    fun onParticipante(id: Long, marcado: Boolean) = _ui.update {
        it.copy(participantes = if (marcado) it.participantes + id else it.participantes - id, errorParticipantes = null)
    }

    fun guardar() {
        val s = sesion ?: return
        if (!s.puede(Permiso.REGISTRAR_PROTOCOLO)) { mostrar("No tiene permiso para crear visitas"); return }
        val u = _ui.value
        val hoy = Formato.hoy()
        val v = u.copy(
            ot = u.ot.validar(Validadores::numeroOt),
            errorInstalacion = Validadores.seleccion(u.instalacion, "la instalación"),
            errorFecha = when {
                u.fecha == null -> "Seleccione la fecha"
                u.fecha.isAfter(hoy.plusDays(30)) -> "La fecha no puede superar 30 días desde hoy"
                else -> null
            },
            errorIngeniero = Validadores.seleccion(u.ingeniero, "el ingeniero a cargo (Aclaración nº 7)"),
            errorParticipantes = if (u.participantes.isEmpty()) "Seleccione al menos un participante de la cuadrilla" else null
        )
        if (listOf(v.ot.error, v.errorInstalacion, v.errorFecha, v.errorIngeniero, v.errorParticipantes).any { it != null }) {
            _ui.value = v.copy(intentosFallidos = u.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.value = v.copy(guardando = true)
        lanzar {
            if (c.visitas.existeOt(v.ot.valor)) {
                _ui.update { it.copy(guardando = false, ot = it.ot.copy(error = "Ya existe una visita con esa OT"), intentosFallidos = it.intentosFallidos + 1) }
                vibrar(TipoVibracion.ERROR)
                return@lanzar
            }
            val r = c.visitas.crear(
                Visita(
                    numeroOt = v.ot.valor, idInstalacion = v.instalacion!!.id, fecha = v.fecha!!,
                    tipo = v.tipo, idIngenieroCargo = v.ingeniero!!.id
                ),
                v.participantes.toList()
            )
            when (r) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(guardando = false, guardadoId = r.valor) } }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); _ui.update { it.copy(guardando = false) }; mostrar(r.mensaje) }
            }
        }
    }
}

data class VisitaDetalleUiState(
    val cargando: Boolean = true,
    val sesion: Sesion? = null,
    val visita: Visita? = null,
    val protocolos: List<ProtocoloResumen> = emptyList(),
    val equiposDisponibles: List<Equipo> = emptyList(),
    val recepcion: RecepcionActa? = null,
    val cerrando: Boolean = false,
    val creandoProtocolo: Boolean = false,
    val erroresCierre: List<String> = emptyList(),
    val tituloErrores: String? = null,
    val protocoloCreadoId: Long? = null,
    val intentosFallidos: Int = 0
) {
    val aprobados: Int get() = protocolos.count { it.resultadoGlobal == Resultado.APROBADO && it.estado != EstadoProtocolo.ANULADO }
    val observados: Int get() = protocolos.count { it.resultadoGlobal == Resultado.OBSERVADO && it.estado != EstadoProtocolo.ANULADO }
    val rechazados: Int get() = protocolos.count { it.resultadoGlobal == Resultado.RECHAZADO && it.estado != EstadoProtocolo.ANULADO }
    val puedeOperar: Boolean get() = sesion?.puede(Permiso.REGISTRAR_PROTOCOLO) == true && visita?.cerrada == false
    val puedeCerrar: Boolean get() = sesion?.puede(Permiso.CERRAR_VISITA) == true && visita?.cerrada == false
}

class VisitaDetalleViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idVisita: Long = estado.get<Long>("id") ?: 0L
    private val _ui = MutableStateFlow(VisitaDetalleUiState())
    val ui: StateFlow<VisitaDetalleUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        val v = c.visitas.obtener(idVisita, s)
        val protocolos = if (v != null) c.protocolos.deVisita(idVisita, s) else emptyList()
        val conProtocolo = protocolos.filter { it.estado != EstadoProtocolo.ANULADO }.map { it.idEquipo }.toSet()
        _ui.update {
            it.copy(
                cargando = false,
                sesion = s,
                visita = v,
                protocolos = protocolos,
                equiposDisponibles = if (v != null && !s.esCliente)
                    c.equipos.buscar(s, idInstalacion = v.idInstalacion).filter { e -> e.id !in conProtocolo }
                else emptyList(),
                recepcion = if (v != null) c.visitas.recepcion(idVisita) else null
            )
        }
    }

    fun iniciarProtocolo(equipo: Equipo) {
        val s = sesion ?: return
        _ui.update { it.copy(creandoProtocolo = true) }
        lanzar {
            when (val r = c.protocolos.crear(idVisita, equipo.id, s, c.sesion.codigoDispositivo)) {
                is Operacion.Exito -> {
                    vibrar(TipoVibracion.EXITO)
                    _ui.update { it.copy(creandoProtocolo = false, protocoloCreadoId = r.valor) }
                }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(creandoProtocolo = false) }
                    mostrar(r.mensaje)
                }
            }
        }
    }

    fun onNavegadoAProtocolo() = _ui.update { it.copy(protocoloCreadoId = null) }

    fun cerrarVisita() {
        val s = sesion ?: return
        if (!s.puede(Permiso.CERRAR_VISITA)) { mostrar("Solo técnicos o ingenieros cierran la visita"); return }
        _ui.update { it.copy(cerrando = true, erroresCierre = emptyList(), tituloErrores = null) }
        lanzar {
            val coord = if (c.ubicacion.tienePermiso() && c.ubicacion.gpsActivo()) c.ubicacion.obtener(timeoutMs = 8_000) else null
            when (val r = c.visitas.cerrar(idVisita, s.idUsuario, coord?.latitud, coord?.longitud)) {
                is Operacion.Exito -> {
                    vibrar(TipoVibracion.EXITO)
                    _ui.update { it.copy(cerrando = false) }
                    mostrar("Visita cerrada: el Acta de prueba en terreno está lista")
                }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update {
                        it.copy(cerrando = false, tituloErrores = r.mensaje, erroresCierre = r.detalles, intentosFallidos = it.intentosFallidos + 1)
                    }
                }
            }
        }
    }
}

data class RecepcionUiState(
    val visita: Visita? = null,
    val nombre: Campo = Campo(),
    val rut: Campo = Campo(),
    val cargo: Campo = Campo(),
    val fechaHora: Long = Formato.ahora(),
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val guardado: Boolean = false
)

class RecepcionViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idVisita: Long = estado.get<Long>("id") ?: 0L
    private val _ui = MutableStateFlow(RecepcionUiState())
    val ui: StateFlow<RecepcionUiState> = _ui.asStateFlow()

    private val vCargo: (String) -> String? = { Validadores.texto(it, "El cargo", 3, 80) }

    init {
        lanzar {
            val s = sesion ?: return@lanzar
            _ui.update { it.copy(visita = c.visitas.obtener(idVisita, s)) }
        }
    }

    fun onNombre(v: String) = _ui.update { it.copy(nombre = it.nombre.cambiar(v, Validadores::nombrePersona)) }
    fun onRut(v: String) = _ui.update { it.copy(rut = it.rut.cambiar(v, Validadores::rut)) }
    fun onCargo(v: String) = _ui.update { it.copy(cargo = it.cargo.cambiar(v, vCargo)) }

    fun guardar() {
        val s = sesion ?: return
        if (!s.puede(Permiso.CERRAR_VISITA)) { mostrar("No tiene permiso para registrar la recepción"); return }
        val u = _ui.value
        val v = u.copy(
            nombre = u.nombre.validar(Validadores::nombrePersona),
            rut = u.rut.validar(Validadores::rut),
            cargo = u.cargo.validar(vCargo)
        )
        if (listOf(v.nombre, v.rut, v.cargo).any { it.error != null }) {
            _ui.value = v.copy(intentosFallidos = u.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            return
        }
        _ui.value = v.copy(guardando = true)
        lanzar {
            val ahora = Formato.ahora()
            val r = c.visitas.registrarRecepcion(
                RecepcionActa(idVisita, v.nombre.valor, v.rut.valor, v.cargo.valor, ahora)
            )
            when (r) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(guardando = false, guardado = true, fechaHora = ahora) } }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); _ui.update { it.copy(guardando = false) }; mostrar(r.mensaje) }
            }
        }
    }
}

data class ActaUiState(
    val cargando: Boolean = true,
    val acta: ActaTerreno? = null,
    val generandoPdf: Boolean = false
)

class ActaViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idVisita: Long = estado.get<Long>("id") ?: 0L
    private val _ui = MutableStateFlow(ActaUiState())
    val ui: StateFlow<ActaUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        _ui.update { it.copy(cargando = false, acta = c.protocolos.acta(idVisita, s, c.visitas, c.clientes)) }
    }

    fun onGenerando(activo: Boolean) = _ui.update { it.copy(generandoPdf = activo) }

    fun onErrorPdf(texto: String) {
        vibrar(TipoVibracion.ERROR)
        mostrar(texto)
    }
}
