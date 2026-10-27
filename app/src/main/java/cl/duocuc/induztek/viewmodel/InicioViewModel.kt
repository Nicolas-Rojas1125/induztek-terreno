package cl.duocuc.induztek.viewmodel

import android.app.Application
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.Indicadores
import cl.duocuc.induztek.domain.model.Instalacion
import cl.duocuc.induztek.domain.model.ProtocoloResumen
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.Visita
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InicioUiState(
    val cargando: Boolean = true,
    val sesion: Sesion? = null,
    val indicadores: Indicadores = Indicadores(),
    val visitasAbiertas: List<Visita> = emptyList(),
    val pendientes: List<ProtocoloResumen> = emptyList(),
    val instalaciones: List<Instalacion> = emptyList()
)

class InicioViewModel(application: Application) : BaseViewModel(application) {
    private val _ui = MutableStateFlow(InicioUiState())
    val ui: StateFlow<InicioUiState> = _ui.asStateFlow()

    init {
        observarCambios { cargar() }
    }

    private suspend fun cargar() {
        val s = sesion ?: return
        val indicadores = c.protocolos.indicadores(s)
        if (s.esCliente) {
            _ui.value = InicioUiState(
                cargando = false,
                sesion = s,
                indicadores = indicadores,
                pendientes = c.protocolos.listar(s).take(5),
                instalaciones = c.clientes.listarInstalaciones(s)
            )
            return
        }
        val todos = c.protocolos.listar(s)
        val pendientes = when (s.rol) {
            RolCodigo.TECNICO -> todos.filter {
                it.idTecnico == s.idUsuario && (it.estado == EstadoProtocolo.BORRADOR || it.estado == EstadoProtocolo.OBSERVADO)
            }
            else -> todos.filter { it.estado == EstadoProtocolo.REGISTRADO_TERRENO }
        }
        _ui.value = InicioUiState(
            cargando = false,
            sesion = s,
            indicadores = indicadores,
            visitasAbiertas = c.visitas.listar(s, soloAbiertas = true, soloMias = s.rol == RolCodigo.TECNICO),
            pendientes = pendientes.take(6)
        )
    }
}
