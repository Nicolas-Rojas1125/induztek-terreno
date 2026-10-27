package cl.duocuc.induztek.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import cl.duocuc.induztek.data.repository.ComparacionPrueba
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.HistorialEstado
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.Protocolo
import cl.duocuc.induztek.domain.model.ProtocoloResumen
import cl.duocuc.induztek.domain.model.Prueba
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.model.TipoVisita
import cl.duocuc.induztek.domain.model.Usuario
import cl.duocuc.induztek.domain.reglas.AccionProtocolo
import cl.duocuc.induztek.domain.reglas.ContextoTransicion
import cl.duocuc.induztek.domain.reglas.EvaluadorPruebas
import cl.duocuc.induztek.domain.reglas.MaquinaEstados
import cl.duocuc.induztek.domain.reglas.Plazos
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.domain.validacion.Validadores
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProtocolosUiState(
    val cargando: Boolean = true,
    val sesion: Sesion? = null,
    val protocolos: List<ProtocoloResumen> = emptyList(),
    val estado: EstadoProtocolo? = null,
    val clientes: List<Cliente> = emptyList(),
    val cliente: Cliente? = null,
    val tecnicos: List<Usuario> = emptyList(),
    val tecnico: Usuario? = null,
    val busqueda: String = "",
    val soloAtrasados: Boolean = false
)

class ProtocolosViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(ProtocolosUiState())
    val ui: StateFlow<ProtocolosUiState> = _ui.asStateFlow()

    init {
        lanzar {
            val s = sesion ?: return@lanzar
            if (!s.esCliente) {
                _ui.update {
                    it.copy(
                        clientes = c.clientes.listarClientes(s),
                        tecnicos = c.usuarios.activosPorRol(RolCodigo.TECNICO, RolCodigo.INGENIERO, RolCodigo.JEFE_TECNICO)
                    )
                }
            }
        }
        observarCambios { cargar() }
    }

    private suspend fun cargar() {
        val s = sesion ?: return
        val u = _ui.value
        var lista = c.protocolos.listar(s, u.estado, u.cliente?.id, u.tecnico?.id, u.busqueda)
        if (u.soloAtrasados) {
            val hoy = Formato.hoy()
            lista = lista.filter { Plazos.estaAtrasado(it.estado, Formato.aFecha(it.fechaHoraPrueba), hoy) }
        }
        _ui.update { it.copy(cargando = false, sesion = s, protocolos = lista) }
    }

    private fun recargar() { lanzar { cargar() } }

    fun onEstado(e: EstadoProtocolo?) { _ui.update { it.copy(estado = if (it.estado == e) null else e) }; recargar() }
    fun onCliente(cl: Cliente?) { _ui.update { it.copy(cliente = cl) }; recargar() }
    fun onTecnico(t: Usuario?) { _ui.update { it.copy(tecnico = t) }; recargar() }
    fun onBuscar(t: String) { _ui.update { it.copy(busqueda = t) }; recargar() }
    fun onSoloAtrasados() { _ui.update { it.copy(soloAtrasados = !it.soloAtrasados) }; recargar() }
    fun limpiarFiltros() {
        _ui.update { it.copy(estado = null, cliente = null, tecnico = null, busqueda = "", soloAtrasados = false) }
        recargar()
    }
}

data class ProtocoloUiState(
    val cargando: Boolean = true,
    val sesion: Sesion? = null,
    val protocolo: Protocolo? = null,
    val pruebas: List<Prueba> = emptyList(),
    val comparaciones: List<ComparacionPrueba> = emptyList(),
    val historial: List<HistorialEstado> = emptyList(),
    val temperatura: Campo = Campo(),
    val humedad: Campo = Campo(),
    val mantenimiento: Campo = Campo(),
    val observaciones: Campo = Campo(),
    val comentarioVariacion: Campo = Campo(),
    val cabeceraModificada: Boolean = false,
    val accionPendiente: AccionProtocolo? = null,
    val motivo: Campo = Campo(),
    val erroresTransicion: List<String> = emptyList(),
    val procesando: Boolean = false,
    val intentosFallidos: Int = 0,
    val nuevoProtocoloId: Long? = null,
    val tipoNoRealizada: TipoPrueba? = null,
    val motivoNoRealizada: Campo = Campo()
) {
    val puedeEditar: Boolean
        get() {
            val s = sesion ?: return false
            val p = protocolo ?: return false
            return MaquinaEstados.puedeEditar(p.estado, s, p.idUsuarioRegistra)
        }

    val acciones: List<AccionProtocolo>
        get() {
            val s = sesion ?: return emptyList()
            val p = protocolo ?: return emptyList()
            return MaquinaEstados.accionesDisponibles(p.estado, s, p.idUsuarioRegistra)
        }

    val hayVariacionSignificativa: Boolean get() = comparaciones.any { it.significativa }

    val atrasado: Boolean
        get() = protocolo?.let { Plazos.estaAtrasado(it.estado, Formato.aFecha(it.fechaHoraPrueba), Formato.hoy()) } == true

    val puedeReemplazar: Boolean
        get() {
            val p = protocolo ?: return false
            return p.estado == EstadoProtocolo.FORMALIZADO && p.reemplazadoPor == null &&
                sesion?.puede(Permiso.FORMALIZAR_OBSERVAR) == true
        }

    val puedeComentarVariacion: Boolean
        get() = protocolo?.estado == EstadoProtocolo.REGISTRADO_TERRENO && sesion?.puede(Permiso.FORMALIZAR_OBSERVAR) == true
}

class ProtocoloViewModel(application: Application, estado: SavedStateHandle) : BaseViewModel(application) {
    private val idProtocolo: Long = estado.get<Long>("id") ?: 0L
    private val _ui = MutableStateFlow(ProtocoloUiState())
    val ui: StateFlow<ProtocoloUiState> = _ui.asStateFlow()

    private val vMant: (String) -> String? = { Validadores.textoOpcional(it, "La descripción", 400) }
    private val vObs: (String) -> String? = { Validadores.textoOpcional(it, "Las observaciones", 600) }
    private val vComentario: (String) -> String? = { Validadores.textoOpcional(it, "El comentario", 400) }

    init { observarCambios { cargar() } }

    private suspend fun cargar() {
        val s = sesion ?: return
        val p = c.protocolos.obtener(idProtocolo, s)
        if (p == null) {
            _ui.update { it.copy(cargando = false, sesion = s, protocolo = null) }
            return
        }
        val pruebas = c.protocolos.pruebas(idProtocolo)
        val comparaciones = c.protocolos.comparaciones(p, s)
        val historial = if (s.esCliente) emptyList() else c.protocolos.historialEstados(idProtocolo)
        _ui.update { actual ->
            val mantener = actual.cabeceraModificada && actual.protocolo?.id == p.id
            actual.copy(
                cargando = false, sesion = s, protocolo = p, pruebas = pruebas, comparaciones = comparaciones, historial = historial,
                temperatura = if (mantener) actual.temperatura else Campo(p.temperaturaC?.let { Formato.numeroEditable(it) } ?: ""),
                humedad = if (mantener) actual.humedad else Campo(p.humedadPct?.let { Formato.numeroEditable(it) } ?: ""),
                mantenimiento = if (mantener) actual.mantenimiento else Campo(p.descripcionMantenimiento),
                observaciones = if (mantener) actual.observaciones else Campo(p.observaciones),
                comentarioVariacion = if (mantener) actual.comentarioVariacion else Campo(p.comentarioVariacion)
            )
        }
    }

    fun onTemperatura(v: String) = _ui.update { it.copy(temperatura = it.temperatura.cambiar(v, Validadores::temperatura), cabeceraModificada = true) }
    fun onHumedad(v: String) = _ui.update { it.copy(humedad = it.humedad.cambiar(v, Validadores::humedad), cabeceraModificada = true) }
    fun onMantenimiento(v: String) = _ui.update { it.copy(mantenimiento = it.mantenimiento.cambiar(v, vMant), cabeceraModificada = true) }
    fun onObservaciones(v: String) = _ui.update { it.copy(observaciones = it.observaciones.cambiar(v, vObs), cabeceraModificada = true) }
    fun onComentarioVariacion(v: String) =
        _ui.update { it.copy(comentarioVariacion = it.comentarioVariacion.cambiar(v, vComentario), cabeceraModificada = true) }

    private suspend fun guardarCabeceraInterno(): Boolean {
        val u = _ui.value
        val p = u.protocolo ?: return false
        val v = u.copy(
            temperatura = u.temperatura.validar(Validadores::temperatura),
            humedad = u.humedad.validar(Validadores::humedad),
            mantenimiento = u.mantenimiento.validar(vMant),
            observaciones = u.observaciones.validar(vObs)
        )
        if (listOf(v.temperatura, v.humedad, v.mantenimiento, v.observaciones).any { it.error != null }) {
            _ui.value = v.copy(intentosFallidos = u.intentosFallidos + 1)
            vibrar(TipoVibracion.ERROR)
            return false
        }
        val r = c.protocolos.actualizarCabecera(
            p.id, Validadores.aDecimal(v.temperatura.valor), Validadores.aDecimal(v.humedad.valor),
            v.mantenimiento.valor, v.observaciones.valor, v.comentarioVariacion.valor
        )
        return when (r) {
            is Operacion.Exito -> { _ui.update { it.copy(cabeceraModificada = false) }; true }
            is Operacion.Fallo -> { mostrar(r.mensaje); false }
        }
    }

    fun guardarCabecera() {
        if (!_ui.value.puedeEditar) return
        lanzar {
            if (guardarCabeceraInterno()) { vibrar(TipoVibracion.EXITO); mostrar("Datos del protocolo guardados") }
        }
    }

    fun pedirNoRealizada(tipo: TipoPrueba) = _ui.update { it.copy(tipoNoRealizada = tipo, motivoNoRealizada = Campo()) }
    fun cancelarNoRealizada() = _ui.update { it.copy(tipoNoRealizada = null) }
    fun onMotivoNoRealizada(v: String) =
        _ui.update { it.copy(motivoNoRealizada = it.motivoNoRealizada.cambiar(v) { m -> Validadores.motivo(m) }) }

    fun confirmarNoRealizada() {
        val s = sesion ?: return
        val u = _ui.value
        val tipo = u.tipoNoRealizada ?: return
        val motivo = u.motivoNoRealizada.validar { Validadores.motivo(it) }
        if (motivo.error != null) {
            _ui.update { it.copy(motivoNoRealizada = motivo) }
            vibrar(TipoVibracion.ERROR)
            return
        }
        lanzar {
            val r = c.protocolos.guardarPrueba(
                Prueba(idProtocolo = idProtocolo, tipo = tipo, elemento = "", realizada = false, motivoNoRealizada = motivo.valor), s
            )
            when (r) {
                is Operacion.Exito -> { vibrar(TipoVibracion.ADVERTENCIA); _ui.update { it.copy(tipoNoRealizada = null) }; mostrar("${tipo.etiqueta}: marcada como No realizada") }
                is Operacion.Fallo -> mostrar(r.mensaje)
            }
        }
    }

    fun eliminarPrueba(p: Prueba) {
        if (!_ui.value.puedeEditar) return
        lanzar {
            when (val r = c.protocolos.eliminarPrueba(p.id, idProtocolo)) {
                is Operacion.Exito -> mostrar("Registro eliminado")
                is Operacion.Fallo -> mostrar(r.mensaje)
            }
        }
    }

    fun pedirAccion(accion: AccionProtocolo) {
        _ui.update { it.copy(erroresTransicion = emptyList()) }
        if (accion.requiereMotivo) {
            _ui.update { it.copy(accionPendiente = accion, motivo = Campo()) }
        } else {
            _ui.update { it.copy(accionPendiente = accion) }
        }
    }

    fun cancelarAccion() = _ui.update { it.copy(accionPendiente = null) }

    fun onMotivo(v: String) = _ui.update { it.copy(motivo = it.motivo.cambiar(v) { m -> Validadores.motivo(m, MaquinaEstados.LARGO_MINIMO_MOTIVO) }) }

    fun confirmarAccion() {
        val s = sesion ?: return
        val u = _ui.value
        val accion = u.accionPendiente ?: return
        val p = u.protocolo ?: return
        _ui.update { it.copy(procesando = true, accionPendiente = null) }
        lanzar {
            if (accion == AccionProtocolo.REGISTRAR_TERRENO && u.puedeEditar && !guardarCabeceraInterno()) {
                _ui.update { it.copy(procesando = false) }
                return@lanzar
            }
            if (accion == AccionProtocolo.FORMALIZAR) {
                c.protocolos.guardarComentarioVariacion(p.id, u.comentarioVariacion.valor)
            }
            val pruebas = c.protocolos.pruebas(p.id)
            val ctx = ContextoTransicion(
                estado = p.estado,
                sesion = s,
                idAutor = p.idUsuarioRegistra,
                temperatura = Validadores.aDecimal(_ui.value.temperatura.valor),
                humedad = Validadores.aDecimal(_ui.value.humedad.valor),
                pruebas = pruebas,
                tiposRequeridos = p.tipoEquipo.pruebas,
                hayVariacionSignificativa = u.hayVariacionSignificativa,
                comentarioVariacion = u.comentarioVariacion.valor,
                motivo = u.motivo.valor
            )
            val errores = MaquinaEstados.validar(accion, ctx)
            if (errores.isNotEmpty()) {
                vibrar(TipoVibracion.ERROR)
                _ui.update { it.copy(procesando = false, erroresTransicion = errores, intentosFallidos = it.intentosFallidos + 1) }
                return@lanzar
            }
            val motivo = when (accion) {
                AccionProtocolo.OBSERVAR, AccionProtocolo.ANULAR -> u.motivo.valor
                AccionProtocolo.FORMALIZAR -> "Revisado y aprobado"
                AccionProtocolo.REGISTRAR_TERRENO ->
                    if (p.estado == EstadoProtocolo.OBSERVADO) "Corregido por el técnico" else "Pruebas completas en terreno"
            }
            when (val r = c.protocolos.cambiarEstado(p.id, p.estado, accion.destino, s.idUsuario, motivo)) {
                is Operacion.Exito -> {
                    vibrar(if (accion == AccionProtocolo.ANULAR || accion == AccionProtocolo.OBSERVAR) TipoVibracion.ADVERTENCIA else TipoVibracion.EXITO)
                    _ui.update { it.copy(procesando = false, erroresTransicion = emptyList()) }
                    mostrar("Protocolo ${accion.destino.etiqueta.lowercase()}")
                }
                is Operacion.Fallo -> {
                    vibrar(TipoVibracion.ERROR)
                    _ui.update { it.copy(procesando = false, erroresTransicion = listOf(r.mensaje)) }
                }
            }
        }
    }

    fun emitirReemplazo() {
        val s = sesion ?: return
        _ui.update { it.copy(procesando = true) }
        lanzar {
            when (val r = c.protocolos.emitirReemplazo(idProtocolo, s, c.sesion.codigoDispositivo)) {
                is Operacion.Exito -> { vibrar(TipoVibracion.EXITO); _ui.update { it.copy(procesando = false, nuevoProtocoloId = r.valor) } }
                is Operacion.Fallo -> { vibrar(TipoVibracion.ERROR); _ui.update { it.copy(procesando = false) }; mostrar(r.mensaje) }
            }
        }
    }

    fun onNavegadoANuevo() = _ui.update { it.copy(nuevoProtocoloId = null) }

    fun resumenTipo(tipo: TipoPrueba): String {
        val lista = _ui.value.pruebas.filter { it.tipo == tipo }
        return when {
            lista.isEmpty() -> "Pendiente"
            lista.any { !it.realizada } -> "No realizada"
            else -> "${lista.size} registro(s)"
        }
    }

    fun visitaConMantenimiento(): Boolean = _ui.value.protocolo?.tipoVisita == TipoVisita.PRUEBAS_Y_MANT

    fun resultadoDeTipo(tipo: TipoPrueba): Resultado? =
        EvaluadorPruebas.resultadoGlobal(_ui.value.pruebas.filter { it.tipo == tipo })
}
