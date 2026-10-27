package cl.duocuc.induztek.domain.model

import java.time.LocalDate

data class Region(val id: Long, val nombre: String)

data class Comuna(val id: Long, val nombre: String, val idRegion: Long, val nombreRegion: String = "")

data class Cliente(
    val id: Long = 0,
    val rut: String,
    val razonSocial: String,
    val giro: String,
    val contactoNombre: String,
    val telefono: String,
    val correo: String,
    val cantidadInstalaciones: Int = 0
)

data class Instalacion(
    val id: Long = 0,
    val idCliente: Long,
    val nombre: String,
    val ubicacion: String,
    val idComuna: Long,
    val observacionesAcceso: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val nombreCliente: String = "",
    val nombreComuna: String = "",
    val nombreRegion: String = "",
    val cantidadEquipos: Int = 0
) {
    val tieneCoordenadas: Boolean get() = latitud != null && longitud != null
}

data class Equipo(
    val id: Long = 0,
    val idInstalacion: Long,
    val tag: String,
    val tipo: TipoEquipo,
    val marca: String,
    val modelo: String,
    val numeroSerie: String,
    val anioFabricacion: Int?,
    val potenciaKva: Double? = null,
    val tensionPrimariaKv: Double? = null,
    val tensionSecundariaKv: Double? = null,
    val corrienteNominalA: Double? = null,
    val maxContactosUohm: Double? = null,
    val observaciones: String = "",
    val fotoPlaca: String? = null,
    val nombreInstalacion: String = "",
    val idCliente: Long = 0,
    val nombreCliente: String = "",
    val ultimaPrueba: Long? = null
)

data class Usuario(
    val id: Long = 0,
    val rol: RolCodigo,
    val rut: String,
    val nombre: String,
    val correo: String,
    val telefono: String = "",
    val activo: Boolean = true,
    val idCliente: Long? = null,
    val cargo: String = "",
    val nombreCliente: String = ""
)

data class Calibracion(
    val id: Long = 0,
    val idInstrumento: Long,
    val fechaCalibracion: LocalDate,
    val fechaVencimiento: LocalDate,
    val registradaPor: String = ""
)

data class Instrumento(
    val id: Long = 0,
    val nombre: String,
    val categoria: CategoriaInstrumento,
    val marca: String,
    val modelo: String,
    val numeroSerie: String,
    val ultimaCalibracion: LocalDate? = null,
    val vencimiento: LocalDate? = null
) {
    val descripcion: String get() = "$nombre · $marca $modelo · Nº $numeroSerie"
}

data class Visita(
    val id: Long = 0,
    val numeroOt: String,
    val idInstalacion: Long,
    val fecha: LocalDate,
    val tipo: TipoVisita,
    val idIngenieroCargo: Long,
    val fechaHoraCierre: Long? = null,
    val latitudCierre: Double? = null,
    val longitudCierre: Double? = null,
    val nombreInstalacion: String = "",
    val idCliente: Long = 0,
    val nombreCliente: String = "",
    val nombreIngeniero: String = "",
    val participantes: List<Usuario> = emptyList(),
    val cantidadProtocolos: Int = 0
) {
    val cerrada: Boolean get() = fechaHoraCierre != null
}

data class RecepcionActa(
    val idVisita: Long,
    val receptorNombre: String,
    val receptorRut: String,
    val receptorCargo: String,
    val fechaHoraRecepcion: Long
)

data class Medicion(
    val parametro: Parametro,
    val valor: Double
)

data class Prueba(
    val id: Long = 0,
    val idProtocolo: Long,
    val tipo: TipoPrueba,
    val elemento: String,
    val momento: Momento = Momento.UNICA,
    val realizada: Boolean = true,
    val motivoNoRealizada: String = "",
    val idInstrumento: Long? = null,
    val resultado: Resultado? = null,
    val noOpera: Boolean = false,
    val comentario: String = "",
    val mediciones: List<Medicion> = emptyList(),
    val nombreInstrumento: String = ""
) {
    fun valor(parametro: Parametro): Double? = mediciones.firstOrNull { it.parametro == parametro }?.valor
    val valores: Map<Parametro, Double> get() = mediciones.associate { it.parametro to it.valor }
}

data class HistorialEstado(
    val id: Long = 0,
    val estadoAnterior: EstadoProtocolo?,
    val estadoNuevo: EstadoProtocolo,
    val nombreUsuario: String,
    val fechaHora: Long,
    val motivo: String
)

data class Protocolo(
    val id: Long = 0,
    val uuid: String,
    val numero: String,
    val idVisita: Long,
    val idEquipo: Long,
    val idUsuarioRegistra: Long,
    val fechaHoraPrueba: Long,
    val temperaturaC: Double?,
    val humedadPct: Double?,
    val descripcionMantenimiento: String = "",
    val observaciones: String = "",
    val comentarioVariacion: String = "",
    val estado: EstadoProtocolo = EstadoProtocolo.BORRADOR,
    val idProtocoloReemplazado: Long? = null,
    val tagEquipo: String = "",
    val tipoEquipo: TipoEquipo = TipoEquipo.TRANSFORMADOR,
    val maxContactosUohm: Double? = null,
    val nombreInstalacion: String = "",
    val idCliente: Long = 0,
    val nombreCliente: String = "",
    val nombreTecnico: String = "",
    val numeroOt: String = "",
    val tipoVisita: TipoVisita = TipoVisita.SOLO_PRUEBAS,
    val visitaCerrada: Boolean = false,
    val numeroReemplazado: String? = null,
    val reemplazadoPor: String? = null,
    val resultadoGlobal: Resultado? = null,
    val formalizadoPor: String? = null,
    val fechaFormalizacion: Long? = null
)

data class ProtocoloResumen(
    val id: Long,
    val numero: String,
    val estado: EstadoProtocolo,
    val fechaHoraPrueba: Long,
    val tagEquipo: String,
    val idEquipo: Long,
    val tipoEquipo: TipoEquipo,
    val nombreInstalacion: String,
    val idCliente: Long,
    val nombreCliente: String,
    val idTecnico: Long,
    val nombreTecnico: String,
    val numeroOt: String,
    val resultadoGlobal: Resultado?,
    val reemplazadoPor: String? = null
)

data class ActaTerreno(
    val visita: Visita,
    val instalacion: Instalacion,
    val cliente: Cliente,
    val protocolos: List<Pair<Protocolo, List<Prueba>>>,
    val recepcion: RecepcionActa?
)

data class Indicadores(
    val pendientesFormalizar: Int = 0,
    val atrasados: Int = 0,
    val borradoresPropios: Int = 0,
    val observadosPropios: Int = 0,
    val visitasAbiertas: Int = 0,
    val instrumentosPorVencer: Int = 0,
    val instrumentosVencidos: Int = 0,
    val promedioDiasFormalizacion: Double? = null,
    val equiposCliente: Int = 0,
    val actasCliente: Int = 0
)
