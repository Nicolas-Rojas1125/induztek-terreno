package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import cl.duocuc.induztek.domain.model.Calibracion
import cl.duocuc.induztek.domain.model.CategoriaInstrumento
import cl.duocuc.induztek.domain.model.Instrumento
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.reglas.VigenciaInstrumento
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
    val pagina: Pagina<Instrumento> = Pagina.vacia(),
    val busqueda: String = "",
    val categoria: CategoriaInstrumento? = null,
    val vigencia: VigenciaInstrumento? = null,
    val puedeGestionar: Boolean = false
)

class InstrumentosViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(InstrumentosUiState())
    val ui: StateFlow<InstrumentosUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar(pagina: Int = _ui.value.pagina.pagina) {
        val s = sesion ?: return
        val u = _ui.value
        val resultado = c.instrumentos.pagina(u.busqueda, u.categoria, u.vigencia, Formato.hoy(), pagina)
        _ui.update { it.copy(cargando = false, pagina = resultado, puedeGestionar = s.puede(Permiso.GESTIONAR_INSTRUMENTOS)) }
    }

    fun onBuscar(texto: String) {
        _ui.update { it.copy(busqueda = texto) }
        lanzar { cargar(0) }
    }

    fun onCategoria(cat: CategoriaInstrumento?) {
        _ui.update { it.copy(categoria = if (it.categoria == cat) null else cat) }
        lanzar { cargar(0) }
    }

    fun onVigencia(v: VigenciaInstrumento?) {
        _ui.update { it.copy(vigencia = if (it.vigencia == v) null else v) }
        lanzar { cargar(0) }
    }

    fun irAPagina(pagina: Int) = lanzar { cargar(pagina) }
}

data class InstrumentoFormUiState(
    val editando: Boolean = false,
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

class InstrumentoFormViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idInstrumento: Long? = estado.get<Long>("id")
    private val _ui = MutableStateFlow(InstrumentoFormUiState(editando = idInstrumento != null))
    val ui: StateFlow<InstrumentoFormUiState> = _ui.asStateFlow()

    private val vNombre: (String) -> String? = { Validadores.texto(it, "El nombre", 3, 60) }
    private val vMarca: (String) -> String? = { Validadores.texto(it, "La marca", 2, 40) }
    private val vModelo: (String) -> String? = { Validadores.texto(it, "El modelo", 2, 40) }

    init {
        if (idInstrumento != null) {
            lanzar {
                val i = c.instrumentos.obtener(idInstrumento) ?: return@lanzar
                _ui.update {
                    it.copy(
                        nombre = Campo(i.nombre, null, true), categoria = i.categoria, marca = Campo(i.marca, null, true),
                        modelo = Campo(i.modelo, null, true), serie = Campo(i.numeroSerie, null, true)
                    )
                }
            }
        }
    }

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
        if (!s.puede(Permiso.GESTIONAR_INSTRUMENTOS)) { mostrar("Solo el administrador registra instrumentos (RN-12)"); return }
        val u = _ui.value
        if (u.guardando) return
        val v = u.copy(
            nombre = u.nombre.validar(vNombre),
            errorCategoria = Validadores.seleccion(u.categoria, "la categoría"),
            marca = u.marca.validar(vMarca),
            modelo = u.modelo.validar(vModelo),
            serie = u.serie.validar(Validadores::numeroSerie),
            errorCalibracion = if (u.editando) null else Validadores.fechaNoFutura(u.fechaCalibracion, "la fecha de calibración"),
            errorVencimiento = if (u.editando) null else Validadores.fechaVencimiento(u.fechaCalibracion, u.fechaVencimiento)
        )
        if (listOf(v.nombre.error, v.errorCategoria, v.marca.error, v.modelo.error, v.serie.error, v.errorCalibracion, v.errorVencimiento).any { it != null }) {
            _ui.value = v.copy(intentosFallidos = u.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            return
        }
        val categoria = v.categoria ?: return
        _ui.value = v.copy(guardando = true)
        lanzar {
            val r = c.instrumentos.guardar(
                s.idUsuario, s.nombre,
                Instrumento(
                    id = idInstrumento ?: 0L, nombre = v.nombre.valor, categoria = categoria, marca = v.marca.valor,
                    modelo = v.modelo.valor, numeroSerie = v.serie.valor
                ),
                if (v.editando) null else v.fechaCalibracion,
                if (v.editando) null else v.fechaVencimiento
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

data class InstrumentoDetalleUiState(
    val cargando: Boolean = true,
    val instrumento: Instrumento? = null,
    val calibraciones: List<Calibracion> = emptyList(),
    val usos: Int = 0,
    val puedeGestionar: Boolean = false,
    val mostrarFormulario: Boolean = false,
    val fechaCalibracion: LocalDate? = Formato.hoy(),
    val fechaVencimiento: LocalDate? = Formato.hoy().plusYears(1),
    val errorCalibracion: String? = null,
    val errorVencimiento: String? = null,
    val guardando: Boolean = false,
    val eliminado: Boolean = false
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
        if (!s.puede(Permiso.GESTIONAR_INSTRUMENTOS)) { mostrar("Solo el administrador actualiza calibraciones (RN-12)"); return }
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
        val fc = u.fechaCalibracion ?: return
        val fv = u.fechaVencimiento ?: return
        _ui.update { it.copy(guardando = true) }
        lanzar {
            val r = c.instrumentos.registrarCalibracion(
                Calibracion(idInstrumento = idInstrumento, fechaCalibracion = fc, fechaVencimiento = fv), s.idUsuario, s.nombre
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

    fun eliminar() {
        val s = sesion ?: return
        if (!s.puede(Permiso.ELIMINAR_REGISTROS)) { mostrar("Solo el administrador elimina instrumentos"); return }
        lanzar {
            when (val r = c.instrumentos.eliminar(idInstrumento)) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(eliminado = true) } }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); mostrar(r.mensaje) }
            }
        }
    }
}
