package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import cl.duocuc.induztek.data.repository.PuntoSerie
import cl.duocuc.induztek.domain.model.Clasificacion
import cl.duocuc.induztek.domain.model.Equipo
import cl.duocuc.induztek.domain.model.FichaTecnica
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.TipoCampoTecnico
import cl.duocuc.induztek.domain.model.Instalacion
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Parametro
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.ProtocoloResumen
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.domain.validacion.Validadores
import cl.duocuc.induztek.nativo.ArchivosFoto
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EquiposUiState(
    val cargando: Boolean = true,
    val pagina: Pagina<Equipo> = Pagina.vacia(),
    val busqueda: String = "",
    val clasificacion: Clasificacion? = null,
    val tipo: TipoEquipo? = null,
    val esCliente: Boolean = false
) {
    val equipos: List<Equipo> get() = pagina.items
}

class EquiposViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(EquiposUiState())
    val ui: StateFlow<EquiposUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar(pagina: Int = _ui.value.pagina.pagina) {
        val s = sesion ?: return
        val u = _ui.value
        val resultado = c.equipos.pagina(s, u.busqueda, u.clasificacion, u.tipo, pagina)
        _ui.update { it.copy(cargando = false, esCliente = s.esCliente, pagina = resultado) }
    }

    fun onBuscar(texto: String) {
        _ui.update { it.copy(busqueda = texto) }
        lanzar { cargar(0) }
    }

    fun onClasificacion(cl: Clasificacion?) {
        _ui.update { it.copy(clasificacion = if (it.clasificacion == cl) null else cl, tipo = null) }
        lanzar { cargar(0) }
    }

    fun onTipo(t: TipoEquipo?) {
        _ui.update { it.copy(tipo = if (it.tipo == t) null else t) }
        lanzar { cargar(0) }
    }

    fun irAPagina(pagina: Int) = lanzar { cargar(pagina) }
}

data class EquipoFormUiState(
    val editando: Boolean = false,
    val instalacion: Instalacion? = null,
    val tag: Campo = Campo(),
    val tipo: TipoEquipo? = null,
    val errorTipo: String? = null,
    val marca: Campo = Campo(),
    val modelo: Campo = Campo(),
    val serie: Campo = Campo(),
    val anio: Campo = Campo(),
    val potencia: Campo = Campo(),
    val tensionPrimaria: Campo = Campo(),
    val tensionSecundaria: Campo = Campo(),
    val corriente: Campo = Campo(),
    val maxContactos: Campo = Campo(),
    val observaciones: Campo = Campo(),
    val ficha: Map<String, String> = emptyMap(),
    val erroresFicha: Map<String, String> = emptyMap(),
    val fichaTocada: Set<String> = emptySet(),
    val fotoPlaca: String? = null,
    val duplicado: Equipo? = null,
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val guardadoId: Long? = null
) {
    fun errorFicha(clave: String): String? = if (clave in fichaTocada) erroresFicha[clave] else null
    val completitudFicha: Int get() = tipo?.let { FichaTecnica.completitud(it, ficha) } ?: 0
}

class EquipoFormViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idEquipo: Long? = estado.get<Long>("id")
    private var idInstalacion: Long = estado.get<Long>("instalacionId") ?: 0L
    private val _ui = MutableStateFlow(EquipoFormUiState(editando = idEquipo != null))
    val ui: StateFlow<EquipoFormUiState> = _ui.asStateFlow()

    private val vMarca: (String) -> String? = { Validadores.texto(it, "La marca", 2, 60) }
    private val vModelo: (String) -> String? = { Validadores.textoOpcional(it, "El modelo", 60) }
    private val vAnio: (String) -> String? = { Validadores.anioFabricacion(it) }
    private val vObs: (String) -> String? = { Validadores.textoOpcional(it, "Las observaciones", 300) }
    private val vPotencia: (String) -> String? = { Validadores.decimalEnRango(it, "La potencia", 1.0, 500_000.0, "kVA") }
    private val vTension1: (String) -> String? = { Validadores.decimalEnRango(it, "La tensión", 0.1, 500.0, "kV") }
    private val vTension2: (String) -> String? = { Validadores.decimalEnRango(it, "La tensión secundaria", 0.1, 500.0, "kV") }
    private val vCorriente: (String) -> String? = { Validadores.decimalEnRango(it, "La corriente", 0.1, 10_000.0, "A") }
    private val vMaxContactos: (String) -> String? = { Validadores.decimalEnRango(it, "El máximo de contactos", 1.0, 10_000.0, "µΩ") }

    init {
        lanzar {
            val s = sesion ?: return@lanzar
            val existente = idEquipo?.let { c.equipos.obtener(it, s) }
            if (existente != null) idInstalacion = existente.idInstalacion
            val instalacion = c.clientes.obtenerInstalacion(idInstalacion, s)
            _ui.update { u ->
                if (existente == null) u.copy(instalacion = instalacion)
                else {
                    val n: (Double?) -> Campo = { d -> Campo(d?.let { Formato.numeroEditable(it) } ?: "", null, true) }
                    u.copy(
                        instalacion = instalacion,
                        tag = Campo(existente.tag, null, true), tipo = existente.tipo,
                        marca = Campo(existente.marca, null, true), modelo = Campo(existente.modelo, null, true),
                        serie = Campo(existente.numeroSerie, null, true),
                        anio = Campo(existente.anioFabricacion?.toString() ?: "", null, true),
                        potencia = n(existente.potenciaKva), tensionPrimaria = n(existente.tensionPrimariaKv),
                        tensionSecundaria = n(existente.tensionSecundariaKv), corriente = n(existente.corrienteNominalA),
                        maxContactos = n(existente.maxContactosUohm),
                        observaciones = Campo(existente.observaciones, null, true),
                        ficha = existente.datosTecnicos.mapValues { (k, v) ->
                            val campo = FichaTecnica.campo(existente.tipo, k)
                            if (campo != null && (campo.tipo == TipoCampoTecnico.NUMERO || campo.tipo == TipoCampoTecnico.ENTERO)) v.replace('.', ',') else v
                        },
                        erroresFicha = FichaTecnica.validarTodo(existente.tipo, existente.datosTecnicos),
                        fotoPlaca = existente.fotoPlaca
                    )
                }
            }
        }
    }

    fun onTag(v: String) {
        _ui.update { it.copy(tag = it.tag.cambiar(v.uppercase(), Validadores::tag), duplicado = null) }
    }

    fun verificarTag() {
        val tag = _ui.value.tag.valor
        if (Validadores.tag(tag) != null) return
        lanzar {
            val existente = c.equipos.buscarPorTag(idInstalacion, tag)?.takeIf { it.id != idEquipo }
            _ui.update { it.copy(duplicado = existente) }
            if (existente != null) vibrar(TipoVibracion.ADVERTENCIA)
        }
    }

    fun onTipo(t: TipoEquipo) = _ui.update {
        val claves = FichaTecnica.campos(t).map { c -> c.clave }.toSet()
        val ficha = it.ficha.filterKeys { k -> k in claves }
        it.copy(tipo = t, errorTipo = null, ficha = ficha, erroresFicha = FichaTecnica.validarTodo(t, ficha), fichaTocada = emptySet())
    }
    fun onMarca(v: String) = _ui.update { it.copy(marca = it.marca.cambiar(v, vMarca)) }
    fun onModelo(v: String) = _ui.update { it.copy(modelo = it.modelo.cambiar(v, vModelo)) }
    fun onSerie(v: String) = _ui.update { it.copy(serie = it.serie.cambiar(v, Validadores::numeroSerie)) }
    fun onAnio(v: String) = _ui.update { it.copy(anio = it.anio.cambiar(v.filter { ch -> ch.isDigit() }.take(4), vAnio)) }
    fun onPotencia(v: String) = _ui.update { it.copy(potencia = it.potencia.cambiar(v, vPotencia)) }
    fun onTensionPrimaria(v: String) = _ui.update { it.copy(tensionPrimaria = it.tensionPrimaria.cambiar(v, vTension1)) }
    fun onTensionSecundaria(v: String) = _ui.update { it.copy(tensionSecundaria = it.tensionSecundaria.cambiar(v, vTension2)) }
    fun onCorriente(v: String) = _ui.update { it.copy(corriente = it.corriente.cambiar(v, vCorriente)) }
    fun onMaxContactos(v: String) = _ui.update { it.copy(maxContactos = it.maxContactos.cambiar(v, vMaxContactos)) }
    fun onObservaciones(v: String) = _ui.update { it.copy(observaciones = it.observaciones.cambiar(v, vObs)) }

    fun onDatoTecnico(clave: String, valor: String) = _ui.update { u ->
        val t = u.tipo ?: return@update u
        val ficha = u.ficha + (clave to valor)
        u.copy(ficha = ficha, erroresFicha = FichaTecnica.validarTodo(t, ficha), fichaTocada = u.fichaTocada + clave)
    }

    fun onFotoTomada(ruta: String) {
        ArchivosFoto.eliminar(_ui.value.fotoPlaca)
        _ui.update { it.copy(fotoPlaca = ruta) }
        vibrar(TipoVibracion.EXITO)
        mostrar("Foto de la placa guardada en el teléfono")
    }

    fun quitarFoto() {
        ArchivosFoto.eliminar(_ui.value.fotoPlaca)
        _ui.update { it.copy(fotoPlaca = null) }
    }

    fun onAvisoCamara(texto: String) = mostrar(texto)

    fun guardar() {
        val s = sesion ?: return
        if (!s.puede(Permiso.GESTIONAR_CLIENTES_EQUIPOS)) { mostrar("No tiene permiso para registrar equipos"); return }
        val u = _ui.value
        if (u.guardando) return
        val t = u.tipo
        var v = u.copy(
            tag = u.tag.validar(Validadores::tag),
            errorTipo = Validadores.seleccion(t, "el tipo de equipo"),
            marca = u.marca.validar(vMarca),
            modelo = u.modelo.validar(vModelo),
            serie = u.serie.validar(Validadores::numeroSerie),
            anio = u.anio.validar(vAnio),
            observaciones = u.observaciones.validar(vObs)
        )
        if (t != null) {
            if (t != TipoEquipo.RELE) v = v.copy(tensionPrimaria = u.tensionPrimaria.validar(vTension1))
            if (t.usaPotencia) v = v.copy(potencia = u.potencia.validar(vPotencia))
            if (t.usaTensionSecundaria) v = v.copy(tensionSecundaria = u.tensionSecundaria.validar(vTension2))
            if (t.usaCorriente) v = v.copy(corriente = u.corriente.validar(vCorriente))
            if (t.usaMaxContactos) v = v.copy(maxContactos = u.maxContactos.validar(vMaxContactos))
            v = v.copy(
                erroresFicha = FichaTecnica.validarTodo(t, u.ficha),
                fichaTocada = FichaTecnica.campos(t).map { it.clave }.toSet()
            )
        }
        val errores = listOfNotNull(
            v.tag.error, v.errorTipo, v.marca.error, v.modelo.error, v.serie.error, v.anio.error, v.observaciones.error,
            if (t != null && t != TipoEquipo.RELE) v.tensionPrimaria.error else null,
            if (t?.usaPotencia == true) v.potencia.error else null,
            if (t?.usaTensionSecundaria == true) v.tensionSecundaria.error else null,
            if (t?.usaCorriente == true) v.corriente.error else null,
            if (t?.usaMaxContactos == true) v.maxContactos.error else null
        ) + v.erroresFicha.values
        if (errores.isNotEmpty() || t == null) {
            _ui.value = v.copy(intentosFallidos = u.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            if (v.erroresFicha.isNotEmpty()) mostrar("Revise la ficha técnica: ${v.erroresFicha.size} dato(s) con error")
            return
        }
        _ui.value = v.copy(guardando = true)
        lanzar {
            val existente = c.equipos.buscarPorTag(idInstalacion, v.tag.valor)?.takeIf { it.id != idEquipo }
            if (existente != null) {
                _ui.update { it.copy(guardando = false, duplicado = existente, intentosFallidos = it.intentosFallidos + 1) }
                vibrar(TipoVibracion.ERROR)
                return@lanzar
            }
            val d = Validadores::aDecimal
            val r = c.equipos.guardar(
                s.idUsuario,
                Equipo(
                    id = idEquipo ?: 0L,
                    idInstalacion = idInstalacion, tag = v.tag.valor, tipo = t, marca = v.marca.valor, modelo = v.modelo.valor,
                    numeroSerie = v.serie.valor, anioFabricacion = v.anio.valor.toIntOrNull(),
                    potenciaKva = if (t.usaPotencia) d(v.potencia.valor) else null,
                    tensionPrimariaKv = if (t != TipoEquipo.RELE) d(v.tensionPrimaria.valor) else null,
                    tensionSecundariaKv = if (t.usaTensionSecundaria) d(v.tensionSecundaria.valor) else null,
                    corrienteNominalA = if (t.usaCorriente) d(v.corriente.valor) else null,
                    maxContactosUohm = if (t.usaMaxContactos) d(v.maxContactos.valor) else null,
                    observaciones = v.observaciones.valor,
                    datosTecnicos = FichaTecnica.normalizar(t, v.ficha),
                    fotoPlaca = v.fotoPlaca
                )
            )
            when (r) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(guardando = false, guardadoId = r.valor) } }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); _ui.update { it.copy(guardando = false) }; mostrar(r.mensaje) }
            }
        }
    }
}

data class SerieSeleccionada(val tipo: TipoPrueba, val elemento: String, val parametro: Parametro)

data class EquipoDetalleUiState(
    val cargando: Boolean = true,
    val sesion: Sesion? = null,
    val equipo: Equipo? = null,
    val historial: List<ProtocoloResumen> = emptyList(),
    val series: List<Pair<TipoPrueba, String>> = emptyList(),
    val serie: SerieSeleccionada? = null,
    val puntos: List<PuntoSerie> = emptyList(),
    val puedeEditar: Boolean = false,
    val puedeEliminar: Boolean = false,
    val eliminado: Boolean = false
)

class EquipoDetalleViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idEquipo: Long = estado.get<Long>("id") ?: 0L
    private val _ui = MutableStateFlow(EquipoDetalleUiState())
    val ui: StateFlow<EquipoDetalleUiState> = _ui.asStateFlow()

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        val equipo = c.equipos.obtener(idEquipo, s)
        val series = if (equipo != null) c.protocolos.seriesDisponibles(idEquipo, s) else emptyList()
        val actual = _ui.value.serie
        val serie = actual?.takeIf { sel -> series.any { it.first == sel.tipo && it.second == sel.elemento } }
            ?: series.firstOrNull()?.let { (t, e) -> SerieSeleccionada(t, e, t.parametrosPrincipales.first()) }
        _ui.value = EquipoDetalleUiState(
            cargando = false,
            sesion = s,
            equipo = equipo,
            historial = if (equipo != null) c.protocolos.historialEquipo(idEquipo, s) else emptyList(),
            series = series,
            serie = serie,
            puntos = serie?.let { c.protocolos.serieHistorica(idEquipo, it.tipo, it.elemento, it.parametro, s) }.orEmpty(),
            puedeEditar = s.puede(Permiso.GESTIONAR_CLIENTES_EQUIPOS),
            puedeEliminar = s.puede(Permiso.ELIMINAR_REGISTROS)
        )
    }

    fun seleccionarSerie(tipo: TipoPrueba, elemento: String, parametro: Parametro = tipo.parametrosPrincipales.first()) {
        val s = sesion ?: return
        val sel = SerieSeleccionada(tipo, elemento, parametro)
        _ui.update { it.copy(serie = sel) }
        lanzar { _ui.update { it.copy(puntos = c.protocolos.serieHistorica(idEquipo, tipo, elemento, parametro, s)) } }
    }

    fun onFotoTomada(ruta: String) {
        lanzar {
            when (val r = c.equipos.actualizarFoto(idEquipo, ruta)) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); mostrar("Foto de la placa actualizada") }
                is Operacion.Fallo -> mostrar(r.mensaje)
            }
        }
    }

    fun onAvisoCamara(texto: String) = mostrar(texto)

    fun eliminar() {
        lanzar {
            when (val r = c.equipos.eliminar(idEquipo)) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(eliminado = true) } }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); mostrar(r.mensaje) }
            }
        }
    }

    fun fechaCorta(millis: Long): String = Formato.fechaDesdeMillis(millis)
}
