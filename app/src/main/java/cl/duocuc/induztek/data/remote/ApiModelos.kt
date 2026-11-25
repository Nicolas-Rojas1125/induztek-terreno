package cl.duocuc.induztek.data.remote

import com.google.gson.annotations.SerializedName

data class PaginaDto<T>(
    val contenido: List<T>?,
    val pagina: Int,
    val tamano: Int,
    val totalElementos: Long,
    val totalPaginas: Int
)

data class ErrorDto(val status: Int?, val message: String?, val errores: List<String>?)

data class MensajeDto(val message: String?)

data class LoginRequest(val correo: String, val contrasena: String)

data class UsuarioDto(
    val id: Long,
    val rut: String?,
    val nombre: String?,
    val correo: String?,
    val telefono: String?,
    val cargo: String?,
    val rol: String?,
    val rolEtiqueta: String?,
    val idCliente: Long?,
    val nombreCliente: String?,
    val activo: Boolean?,
    val creadoEn: Long?,
    val actualizadoEn: Long?
)

data class CrearUsuarioRequest(
    val rut: String,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val cargo: String,
    val rol: String,
    val idCliente: Long?,
    val nombreCliente: String?,
    val contrasena: String
)

data class ActualizarPerfilRequest(val nombre: String, val correo: String, val telefono: String, val cargo: String)

data class CambiarContrasenaRequest(val actual: String, val nueva: String)

data class RecuperarRequest(val correo: String)

data class RecuperarResponse(val message: String?, val codigoDemo: String?, val expiraEn: Long?)

data class RestablecerRequest(val correo: String, val codigo: String, val nueva: String)

data class EstadoRequest(val activo: Boolean)

data class ClienteDto(
    val id: Long,
    val rut: String?,
    val razonSocial: String?,
    val giro: String?,
    val contactoNombre: String?,
    val telefono: String?,
    val correo: String?,
    val direccion: String?,
    val cantidadInstalaciones: Long?,
    val actualizadoEn: Long?
)

data class ClienteRequest(
    val rut: String,
    val razonSocial: String,
    val giro: String,
    val contactoNombre: String,
    val telefono: String,
    val correo: String,
    val direccion: String
)

data class InstalacionDto(
    val id: Long,
    val idCliente: Long,
    val nombreCliente: String?,
    val nombre: String?,
    val ubicacion: String?,
    val idComuna: Long,
    val nombreComuna: String?,
    val nombreRegion: String?,
    val observacionesAcceso: String?,
    val latitud: Double?,
    val longitud: Double?,
    val cantidadEquipos: Long?,
    val actualizadoEn: Long?
)

data class InstalacionRequest(
    val idCliente: Long,
    val nombre: String,
    val ubicacion: String,
    val idComuna: Long,
    val nombreComuna: String,
    val nombreRegion: String,
    val observacionesAcceso: String,
    val latitud: Double?,
    val longitud: Double?
)

data class EquipoDto(
    val id: Long,
    val idInstalacion: Long,
    val nombreInstalacion: String?,
    val idCliente: Long,
    val nombreCliente: String?,
    val tag: String?,
    val tipo: String?,
    val marca: String?,
    val modelo: String?,
    val numeroSerie: String?,
    val anioFabricacion: Int?,
    val potenciaKva: Double?,
    val tensionPrimariaKv: Double?,
    val tensionSecundariaKv: Double?,
    val corrienteNominalA: Double?,
    val maxContactosUohm: Double?,
    val observaciones: String?,
    val datosTecnicos: Map<String, String>?,
    val actualizadoEn: Long?
)

data class EquipoRequest(
    val idInstalacion: Long,
    val tag: String,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val numeroSerie: String,
    val anioFabricacion: Int?,
    val potenciaKva: Double?,
    val tensionPrimariaKv: Double?,
    val tensionSecundariaKv: Double?,
    val corrienteNominalA: Double?,
    val maxContactosUohm: Double?,
    val observaciones: String,
    val datosTecnicos: Map<String, String>
)

data class CalibracionDto(val id: Long, val fechaCalibracion: String?, val fechaVencimiento: String?, val registradaPor: String?)

data class InstrumentoDto(
    val id: Long,
    val nombre: String?,
    val categoria: String?,
    val marca: String?,
    val modelo: String?,
    val numeroSerie: String?,
    val ultimaCalibracion: String?,
    val vencimiento: String?,
    val vigencia: String?,
    val calibraciones: List<CalibracionDto>?,
    val actualizadoEn: Long?
)

data class InstrumentoRequest(
    val nombre: String,
    val categoria: String,
    val marca: String,
    val modelo: String,
    val numeroSerie: String,
    val fechaCalibracion: String?,
    val fechaVencimiento: String?,
    val registradaPor: String?
)

data class CalibracionRequest(
    val fechaCalibracion: String,
    val fechaVencimiento: String,
    val idUsuarioRegistra: Long?,
    val registradaPor: String?
)

data class SyncActivosDto(
    val clientes: List<ClienteDto>?,
    val instalaciones: List<InstalacionDto>?,
    val equipos: List<EquipoDto>?,
    val instrumentos: List<InstrumentoDto>?,
    val generadoEn: Long?
)

data class RecepcionDto(
    val receptorNombre: String?,
    val receptorRut: String?,
    val receptorCargo: String?,
    val fechaHoraRecepcion: Long?
)

data class VisitaDto(
    val uuid: String,
    val numeroOt: String,
    val idInstalacion: Long,
    val idCliente: Long,
    val fecha: String,
    val tipoVisita: String,
    val idIngenieroCargo: Long,
    val fechaHoraCierre: Long?,
    val latitudCierre: Double?,
    val longitudCierre: Double?,
    val idUsuarioCierra: Long?,
    val participantes: List<Long>?,
    val recepcion: RecepcionDto?,
    val origen: String?,
    val actualizadoEn: Long?
)

data class MedicionDto(val parametro: String, val valor: Double)

data class PruebaDto(
    val tipoPrueba: String,
    val elemento: String,
    val momento: String?,
    val realizada: Boolean,
    val motivoNoRealizada: String?,
    val idInstrumento: Long?,
    val resultado: String?,
    val noOpera: Boolean,
    val comentario: String?,
    val mediciones: List<MedicionDto>?
)

data class HistorialDto(
    val estadoAnterior: String?,
    val estadoNuevo: String,
    val idUsuario: Long,
    val fechaHora: Long,
    val motivo: String?
)

data class ProtocoloDto(
    val uuid: String,
    val numero: String,
    val visitaUuid: String,
    val idEquipo: Long,
    val idInstalacion: Long,
    val idCliente: Long,
    val idUsuarioRegistra: Long,
    val fechaHoraPrueba: Long,
    val temperaturaC: Double?,
    val humedadPct: Double?,
    val descripcionMantenimiento: String?,
    val observaciones: String?,
    val comentarioVariacion: String?,
    val estado: String,
    val resultadoGlobal: String?,
    val reemplazaUuid: String?,
    val pruebas: List<PruebaDto>?,
    val historial: List<HistorialDto>?,
    val actualizadoEn: Long?
)

data class SyncProtocolosDto(
    val visitas: List<VisitaDto>?,
    val protocolos: List<ProtocoloDto>?,
    val servidorMillis: Long
)

data class ClimaActualDto(
    val time: String?,
    @SerializedName("temperature_2m") val temperatura: Double?,
    @SerializedName("relative_humidity_2m") val humedad: Double?,
    @SerializedName("wind_speed_10m") val viento: Double?,
    @SerializedName("weather_code") val codigo: Int?
)

data class ClimaRespuestaDto(
    val latitude: Double?,
    val longitude: Double?,
    val current: ClimaActualDto?
)
