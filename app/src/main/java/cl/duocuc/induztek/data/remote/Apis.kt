package cl.duocuc.induztek.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

const val ENCABEZADO_ACTOR = "X-Usuario-Id"

interface UsuariosApi {
    @POST("api/usuarios/login")
    suspend fun login(@Body datos: LoginRequest): UsuarioDto

    @POST("api/usuarios/recuperar")
    suspend fun recuperar(@Body datos: RecuperarRequest): RecuperarResponse

    @POST("api/usuarios/restablecer")
    suspend fun restablecer(@Body datos: RestablecerRequest): MensajeDto

    @GET("api/usuarios")
    suspend fun listar(
        @Query("buscar") buscar: String?,
        @Query("rol") rol: String?,
        @Query("activo") activo: Boolean?,
        @Query("pagina") pagina: Int,
        @Query("tamano") tamano: Int
    ): PaginaDto<UsuarioDto>

    @GET("api/usuarios/directorio")
    suspend fun directorio(): List<UsuarioDto>

    @GET("api/usuarios/{id}")
    suspend fun obtener(@Path("id") id: Long): UsuarioDto

    @POST("api/usuarios")
    suspend fun crear(@Header(ENCABEZADO_ACTOR) actor: Long, @Body datos: CrearUsuarioRequest): UsuarioDto

    @PUT("api/usuarios/{id}")
    suspend fun actualizar(
        @Header(ENCABEZADO_ACTOR) actor: Long,
        @Path("id") id: Long,
        @Body datos: ActualizarPerfilRequest
    ): UsuarioDto

    @PUT("api/usuarios/{id}/contrasena")
    suspend fun cambiarContrasena(
        @Header(ENCABEZADO_ACTOR) actor: Long,
        @Path("id") id: Long,
        @Body datos: CambiarContrasenaRequest
    ): MensajeDto

    @PUT("api/usuarios/{id}/estado")
    suspend fun cambiarEstado(
        @Header(ENCABEZADO_ACTOR) actor: Long,
        @Path("id") id: Long,
        @Body datos: EstadoRequest
    ): UsuarioDto

    @DELETE("api/usuarios/{id}")
    suspend fun eliminar(@Header(ENCABEZADO_ACTOR) actor: Long, @Path("id") id: Long): Response<Unit>
}

interface ActivosApi {
    @GET("api/sync/activos")
    suspend fun exportar(@Query("idCliente") idCliente: Long?): SyncActivosDto

    @GET("api/clientes/{id}")
    suspend fun obtenerCliente(@Path("id") id: Long): ClienteDto

    @POST("api/clientes")
    suspend fun crearCliente(@Header(ENCABEZADO_ACTOR) actor: Long, @Body datos: ClienteRequest): ClienteDto

    @PUT("api/clientes/{id}")
    suspend fun actualizarCliente(@Path("id") id: Long, @Body datos: ClienteRequest): ClienteDto

    @DELETE("api/clientes/{id}")
    suspend fun eliminarCliente(@Path("id") id: Long): Response<Unit>

    @POST("api/instalaciones")
    suspend fun crearInstalacion(@Header(ENCABEZADO_ACTOR) actor: Long, @Body datos: InstalacionRequest): InstalacionDto

    @PUT("api/instalaciones/{id}")
    suspend fun actualizarInstalacion(@Path("id") id: Long, @Body datos: InstalacionRequest): InstalacionDto

    @DELETE("api/instalaciones/{id}")
    suspend fun eliminarInstalacion(@Path("id") id: Long): Response<Unit>

    @POST("api/equipos")
    suspend fun crearEquipo(@Header(ENCABEZADO_ACTOR) actor: Long, @Body datos: EquipoRequest): EquipoDto

    @PUT("api/equipos/{id}")
    suspend fun actualizarEquipo(@Path("id") id: Long, @Body datos: EquipoRequest): EquipoDto

    @DELETE("api/equipos/{id}")
    suspend fun eliminarEquipo(@Path("id") id: Long): Response<Unit>

    @POST("api/instrumentos")
    suspend fun crearInstrumento(@Header(ENCABEZADO_ACTOR) actor: Long, @Body datos: InstrumentoRequest): InstrumentoDto

    @PUT("api/instrumentos/{id}")
    suspend fun actualizarInstrumento(@Path("id") id: Long, @Body datos: InstrumentoRequest): InstrumentoDto

    @POST("api/instrumentos/{id}/calibraciones")
    suspend fun calibrar(@Path("id") id: Long, @Body datos: CalibracionRequest): InstrumentoDto

    @DELETE("api/instrumentos/{id}")
    suspend fun eliminarInstrumento(@Path("id") id: Long): Response<Unit>
}

interface ProtocolosApi {
    @PUT("api/visitas/{uuid}")
    suspend fun guardarVisita(@Path("uuid") uuid: String, @Body datos: VisitaDto): VisitaDto

    @PUT("api/protocolos/{uuid}")
    suspend fun guardarProtocolo(@Path("uuid") uuid: String, @Body datos: ProtocoloDto): ProtocoloDto

    @GET("api/sync/protocolos")
    suspend fun cambios(@Query("desde") desde: Long, @Query("idCliente") idCliente: Long?): SyncProtocolosDto
}

interface ClimaApi {
    @GET("v1/forecast")
    suspend fun actual(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") variables: String = "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code",
        @Query("timezone") zona: String = "auto"
    ): ClimaRespuestaDto
}
