package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import cl.duocuc.induztek.data.repository.PruebaAnterior
import cl.duocuc.induztek.domain.model.Instrumento
import cl.duocuc.induztek.domain.model.Medicion
import cl.duocuc.induztek.domain.model.Momento
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Parametro
import cl.duocuc.induztek.domain.model.Protocolo
import cl.duocuc.induztek.domain.model.Prueba
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.model.TipoVisita
import cl.duocuc.induztek.domain.reglas.Comparacion
import cl.duocuc.induztek.domain.reglas.ComparadorHistorico
import cl.duocuc.induztek.domain.reglas.EvaluacionPrueba
import cl.duocuc.induztek.domain.reglas.EvaluadorPruebas
import cl.duocuc.induztek.domain.reglas.MaquinaEstados
import cl.duocuc.induztek.domain.reglas.ReglasInstrumento
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.domain.validacion.Validadores
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PruebaFormUiState(
    val protocolo: Protocolo? = null,
    val tipo: TipoPrueba = TipoPrueba.AISLACION,
    val idPrueba: Long = 0,
    val elementos: List<String> = emptyList(),
    val elementosRegistrados: Set<String> = emptySet(),
    val elemento: String? = null,
    val errorElemento: String? = null,
    val momentos: List<Momento> = listOf(Momento.UNICA),
    val momento: Momento = Momento.UNICA,
    val instrumentos: List<Instrumento> = emptyList(),
    val instrumento: Instrumento? = null,
    val errorInstrumento: String? = null,
    val parametros: List<Parametro> = emptyList(),
    val valores: Map<Parametro, Campo> = emptyMap(),
    val enGigaohm: Map<Parametro, Boolean> = emptyMap(),
    val noOpera: Boolean = false,
    val resultadoManual: Resultado? = null,
    val errorResultadoManual: String? = null,
    val comentario: Campo = Campo(),
    val evaluacion: EvaluacionPrueba = EvaluacionPrueba(null, null, "Ingrese los valores"),
    val anterior: PruebaAnterior? = null,
    val comparaciones: List<Comparacion> = emptyList(),
    val guardando: Boolean = false,
    val intentosFallidos: Int = 0,
    val terminado: Boolean = false,
    val bloqueado: Boolean = false
) {
    val esEdicion: Boolean get() = idPrueba != 0L
    val hayVariacionSignificativa: Boolean get() = comparaciones.any { it.significativa }
}

class PruebaFormViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idProtocolo: Long = estado.get<Long>("protocoloId") ?: 0L
    private val tipo: TipoPrueba = TipoPrueba.entries.firstOrNull { it.name == estado.get<String>("tipo") } ?: TipoPrueba.AISLACION
    private val idPruebaInicial: Long = estado.get<Long>("pruebaId") ?: 0L

    private val _ui = MutableStateFlow(PruebaFormUiState(tipo = tipo))
    val ui: StateFlow<PruebaFormUiState> = _ui.asStateFlow()

    init {
        lanzar { cargar() }
    }

    private suspend fun cargar() {
        val s = sesion ?: return
        val p = c.protocolos.obtener(idProtocolo, s) ?: return
        val bloqueado = !MaquinaEstados.puedeEditar(p.estado, s, p.idUsuarioRegistra)
        val parametros = tipo.parametros(p.tipoEquipo)
        val existentes = c.protocolos.pruebas(idProtocolo).filter { it.tipo == tipo && it.realizada }
        val momentos = if (p.tipoVisita == TipoVisita.PRUEBAS_Y_MANT)
            listOf(Momento.UNICA, Momento.PREVIA, Momento.POSTERIOR) else listOf(Momento.UNICA)
        val instrumentos = c.instrumentos.paraPrueba(tipo)
        val fecha = Formato.aFecha(p.fechaHoraPrueba)
        val elementos = tipo.elementos(p.tipoEquipo)

        var nuevo = PruebaFormUiState(
            protocolo = p, tipo = tipo, elementos = elementos,
            elementosRegistrados = existentes.map { it.elemento }.toSet(),
            momentos = momentos, instrumentos = instrumentos, parametros = parametros,
            valores = parametros.associateWith { Campo() },
            enGigaohm = parametros.filter { it.admiteGigaohm }.associateWith { false },
            bloqueado = bloqueado
        )
        val editar = existentes.firstOrNull { it.id == idPruebaInicial }
        if (editar != null) {
            nuevo = nuevo.copy(
                idPrueba = editar.id,
                elemento = editar.elemento,
                momento = editar.momento,
                instrumento = instrumentos.firstOrNull { it.id == editar.idInstrumento },
                valores = parametros.associateWith { par -> Campo(editar.valor(par)?.let { Formato.numeroEditable(it) } ?: "") },
                noOpera = editar.noOpera,
                resultadoManual = if (tipo.resultadoManual) editar.resultado else null,
                comentario = Campo(editar.comentario)
            )
        } else {
            val sugerido = elementos.firstOrNull { it !in nuevo.elementosRegistrados } ?: elementos.firstOrNull()
            val vigente = instrumentos.firstOrNull { ReglasInstrumento.validarUso(it, tipo, fecha) == null }
            nuevo = nuevo.copy(elemento = sugerido, instrumento = vigente)
        }
        _ui.value = recalcular(nuevo)
        actualizarAnterior()
    }

    private fun factor(u: PruebaFormUiState, p: Parametro): Double = if (u.enGigaohm[p] == true) 1000.0 else 1.0

    private fun validador(p: Parametro, f: Double): (String) -> String? = { Validadores.medicion(it, p, f) }

    fun onValor(p: Parametro, texto: String) {
        _ui.update { u ->
            val campo = (u.valores[p] ?: Campo()).cambiar(texto, validador(p, factor(u, p)))
            recalcular(u.copy(valores = u.valores + (p to campo)))
        }
    }

    fun onUnidadGiga(p: Parametro, giga: Boolean) {
        _ui.update { u ->
            val nuevoMapa = u.enGigaohm + (p to giga)
            val u2 = u.copy(enGigaohm = nuevoMapa)
            val campo = u.valores[p] ?: Campo()
            val revalidado = if (campo.valor.isBlank()) campo else campo.validar(validador(p, factor(u2, p)))
            recalcular(u2.copy(valores = u2.valores + (p to revalidado)))
        }
    }

    fun onElemento(e: String) {
        _ui.update { it.copy(elemento = e, errorElemento = null) }
        actualizarAnterior()
    }

    fun onMomento(m: Momento) = _ui.update { it.copy(momento = m) }

    fun onInstrumento(i: Instrumento) {
        val p = _ui.value.protocolo ?: return
        val error = ReglasInstrumento.validarUso(i, tipo, Formato.aFecha(p.fechaHoraPrueba))
        _ui.update { it.copy(instrumento = if (error == null) i else it.instrumento, errorInstrumento = error) }
        if (error != null) vibrar(TipoVibracion.ERROR)
    }

    fun onNoOpera(v: Boolean) = _ui.update { recalcular(it.copy(noOpera = v)) }

    fun onResultadoManual(r: Resultado) = _ui.update { recalcular(it.copy(resultadoManual = r, errorResultadoManual = null)) }

    fun onComentario(v: String) =
        _ui.update { it.copy(comentario = it.comentario.cambiar(v) { t -> Validadores.textoOpcional(t, "El comentario", 300) }) }

    private fun valoresBase(u: PruebaFormUiState): Map<Parametro, Double> =
        u.valores.mapNotNull { (p, campo) ->
            if (campo.valor.isBlank() || Validadores.medicion(campo.valor, p, factor(u, p)) != null) null
            else Validadores.aDecimal(campo.valor)?.let { p to it * factor(u, p) }
        }.toMap()

    private fun recalcular(u: PruebaFormUiState): PruebaFormUiState {
        val prot = u.protocolo ?: return u
        val base = valoresBase(u)
        val eval = EvaluadorPruebas.evaluar(tipo, prot.tipoEquipo, base, u.noOpera, prot.maxContactosUohm, u.resultadoManual)
        val comps = u.anterior?.let {
            ComparadorHistorico.comparar(tipo.parametrosPrincipales, base, it.valores, it.numeroProtocolo)
        }.orEmpty()
        return u.copy(evaluacion = eval, comparaciones = comps)
    }

    private fun actualizarAnterior() {
        val s = sesion ?: return
        val u = _ui.value
        val p = u.protocolo ?: return
        val elemento = u.elemento ?: return
        lanzar {
            val ant = c.protocolos.anterior(p.idEquipo, tipo, elemento, p.fechaHoraPrueba, p.id, s)
            _ui.update { recalcular(it.copy(anterior = ant)) }
        }
    }

    fun guardar(continuar: Boolean) {
        val s = sesion ?: return
        val u = _ui.value
        val prot = u.protocolo ?: return
        if (u.bloqueado) { mostrar("El protocolo no se puede modificar en su estado actual (RN-17)"); return }

        val validados = u.valores.mapValues { (p, campo) ->
            if (tipo == TipoPrueba.OPERACION_RELE && u.noOpera && p == Parametro.TIEMPO_MEDIDO && campo.valor.isBlank()) campo.copy(error = null)
            else campo.validar(validador(p, factor(u, p)))
        }
        val errorElemento = Validadores.seleccion(u.elemento, "el elemento probado")
        val errorInstrumento = u.errorInstrumento ?: Validadores.seleccion(u.instrumento, "el instrumento (RF-05)")
        val errorManual = if (tipo.resultadoManual && u.resultadoManual == null) "Indique el resultado (RN-08c)" else null
        val hayErrores = validados.values.any { it.error != null } || errorElemento != null || errorInstrumento != null || errorManual != null

        if (hayErrores) {
            _ui.value = recalcular(
                u.copy(
                    valores = validados, errorElemento = errorElemento, errorInstrumento = errorInstrumento,
                    errorResultadoManual = errorManual, intentosFallidos = u.intentosFallidos + 1
                )
            )
            vibrar(TipoVibracion.ERROR)
            return
        }
        val evaluado = recalcular(u.copy(valores = validados))
        val resultado = evaluado.evaluacion.resultado
        if (resultado == null) {
            _ui.value = evaluado.copy(intentosFallidos = u.intentosFallidos + 1)
            mostrar(evaluado.evaluacion.motivo)
            vibrar(TipoVibracion.ERROR)
            return
        }
        val base = valoresBase(evaluado)
        val elemento = u.elemento!!
        val instrumento = u.instrumento!!
        _ui.value = evaluado.copy(guardando = true)
        lanzar {
            val r = c.protocolos.guardarPrueba(
                Prueba(
                    id = u.idPrueba,
                    idProtocolo = prot.id,
                    tipo = tipo,
                    elemento = elemento,
                    momento = u.momento,
                    realizada = true,
                    idInstrumento = instrumento.id,
                    resultado = resultado,
                    noOpera = u.noOpera,
                    comentario = u.comentario.valor,
                    mediciones = base.map { (p, v) -> Medicion(p, v) }
                ),
                s
            )
            when (r) {
                is Operacion.Exito -> {
                    vibrar(
                        when (resultado) {
                            Resultado.APROBADO -> TipoVibracion.EXITO
                            Resultado.OBSERVADO -> TipoVibracion.ADVERTENCIA
                            Resultado.RECHAZADO -> TipoVibracion.ERROR
                        }
                    )
                    val registrados = _ui.value.elementosRegistrados + elemento
                    val siguiente = u.elementos.firstOrNull { it !in registrados }
                    if (continuar && siguiente != null && !u.esEdicion) {
                        _ui.update { actual ->
                            recalcular(
                                actual.copy(
                                    guardando = false,
                                    elementosRegistrados = registrados,
                                    elemento = siguiente,
                                    valores = actual.valores.mapValues { (p, campo) ->
                                        if (p == Parametro.TENSION_PRUEBA || p == Parametro.RELACION_TEORICA ||
                                            p == Parametro.POSICION_TAP || p == Parametro.CORRIENTE_AJUSTE || p == Parametro.TIEMPO_ESPERADO
                                        ) campo else Campo()
                                    },
                                    noOpera = false,
                                    resultadoManual = null,
                                    comentario = Campo(),
                                    anterior = null,
                                    comparaciones = emptyList()
                                )
                            )
                        }
                        actualizarAnterior()
                        mostrar("$elemento guardado (${resultado.etiqueta}). Siguiente: $siguiente")
                    } else {
                        _ui.update { it.copy(guardando = false, terminado = true) }
                    }
                }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(guardando = false, intentosFallidos = it.intentosFallidos + 1) }
                    mostrar(r.mensaje)
                }
            }
        }
    }
}
