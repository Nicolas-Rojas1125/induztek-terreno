package cl.duocuc.induztek.fakes

import cl.duocuc.induztek.data.remote.ActivosApi
import cl.duocuc.induztek.data.remote.ActualizarPerfilRequest
import cl.duocuc.induztek.data.remote.CalibracionRequest
import cl.duocuc.induztek.data.remote.CambiarContrasenaRequest
import cl.duocuc.induztek.data.remote.ClienteDto
import cl.duocuc.induztek.data.remote.ClienteRequest
import cl.duocuc.induztek.data.remote.ClimaActualDto
import cl.duocuc.induztek.data.remote.ClimaApi
import cl.duocuc.induztek.data.remote.ClimaRespuestaDto
import cl.duocuc.induztek.data.remote.CrearUsuarioRequest
import cl.duocuc.induztek.data.remote.EquipoDto
import cl.duocuc.induztek.data.remote.EquipoRequest
import cl.duocuc.induztek.data.remote.EstadoRequest
import cl.duocuc.induztek.data.remote.InstalacionDto
import cl.duocuc.induztek.data.remote.InstalacionRequest
import cl.duocuc.induztek.data.remote.InstrumentoDto
import cl.duocuc.induztek.data.remote.InstrumentoRequest
import cl.duocuc.induztek.data.remote.LoginRequest
import cl.duocuc.induztek.data.remote.MensajeDto
import cl.duocuc.induztek.data.remote.NuevaCuenta
import cl.duocuc.induztek.data.remote.PaginaDto
import cl.duocuc.induztek.data.remote.RecuperarRequest
import cl.duocuc.induztek.data.remote.RecuperarResponse
import cl.duocuc.induztek.data.remote.RestablecerRequest
import cl.duocuc.induztek.data.remote.SyncActivosDto
import cl.duocuc.induztek.data.remote.UsuarioDto
import cl.duocuc.induztek.data.remote.UsuariosApi
import cl.duocuc.induztek.data.repository.Autenticacion
import cl.duocuc.induztek.data.repository.ClimaFuente
import cl.duocuc.induztek.data.repository.CuentaFuente
import cl.duocuc.induztek.data.repository.UsuariosFuente
import cl.duocuc.induztek.data.repository.Vibrador
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Clima
import cl.duocuc.induztek.domain.model.CodigoRecuperacion
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.Usuario
import cl.duocuc.induztek.domain.util.Paginador
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ReglaMain : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class VibradorFalso : Vibrador {
    val registro = mutableListOf<TipoVibracion>()
    override fun vibrar(tipo: TipoVibracion) { registro += tipo }
    val ultima: TipoVibracion? get() = registro.lastOrNull()
}

object Datos {
    fun sesion(rol: RolCodigo, id: Long = 1, idCliente: Long? = null) =
        Sesion(id, "Usuario ${rol.name.lowercase()}", "${rol.name.lowercase()}@induztek.cl", rol, idCliente = idCliente, rut = "11.111.111-1")

    fun usuario(id: Long, rol: RolCodigo, activo: Boolean = true, idCliente: Long? = null) = Usuario(
        id = id, rol = rol, rut = "12.345.678-5", nombre = "Persona $id", correo = "p$id@induztek.cl",
        activo = activo, idCliente = idCliente
    )

    val empresa = Cliente(7, "76.482.915-8", "Minera Andes SpA", "Minería", "Ana Pérez", "+56911112222", "ana@andes.cl")

    fun usuarioDto(id: Long, rol: String, activo: Boolean? = true) = UsuarioDto(
        id, "12.345.678-5", "Persona $id", "p$id@induztek.cl", "+56912345678", "Cargo", rol, null, null, null, activo, 1000L, 2000L
    )
}

fun httpError(codigo: Int, cuerpo: String): HttpException =
    HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

class CuentaFalsa(sesionInicial: Sesion? = null) : CuentaFuente, Autenticacion {
    private val _sesion = MutableStateFlow(sesionInicial)
    override val sesion: StateFlow<Sesion?> = _sesion
    override var ultimoCorreo: String = ""
    var respuestaLogin: Operacion<Sesion> = Operacion.Fallo("Correo o contraseña incorrectos")
    var respuestaRecuperar: Operacion<CodigoRecuperacion> = Operacion.Exito(CodigoRecuperacion("Código enviado", "123456"))
    var respuestaRestablecer: Operacion<String> = Operacion.Exito("Contraseña restablecida")
    var respuestaCambio: Operacion<Unit> = Operacion.Exito(Unit)
    val llamadas = mutableListOf<String>()

    override suspend fun iniciarSesion(correo: String, contrasena: String): Operacion<Sesion> {
        llamadas += "login:$correo"
        val r = respuestaLogin
        if (r is Operacion.Exito) _sesion.value = r.valor
        return r
    }

    override suspend fun recuperar(correo: String): Operacion<CodigoRecuperacion> {
        llamadas += "recuperar:$correo"
        return respuestaRecuperar
    }

    override suspend fun restablecer(correo: String, codigo: String, nueva: String): Operacion<String> {
        llamadas += "restablecer:$correo:$codigo"
        return respuestaRestablecer
    }

    override suspend fun cambiarContrasena(actual: String, nueva: String): Operacion<Unit> {
        llamadas += "cambiar"
        return respuestaCambio
    }

    override suspend fun editarPerfil(nombre: String, correo: String, telefono: String, cargo: String): Operacion<Sesion> {
        llamadas += "perfil:$nombre"
        val actual = _sesion.value ?: return Operacion.Fallo("Sin sesión")
        val nueva = actual.copy(nombre = nombre, correo = correo, telefono = telefono, cargo = cargo)
        _sesion.value = nueva
        return Operacion.Exito(nueva)
    }
}

class UsuariosFalsos(sesion: Sesion?, iniciales: List<Usuario> = emptyList()) : UsuariosFuente {
    private val _sesion = MutableStateFlow(sesion)
    override val sesion: StateFlow<Sesion?> = _sesion
    val usuarios = iniciales.toMutableList()
    var fallarConexion = false
    var ultimaConsulta: Triple<String, RolCodigo?, Boolean?>? = null
    var creada: NuevaCuenta? = null

    private fun sinRed() = Operacion.Fallo("No se pudo conectar con el servidor")

    override suspend fun listar(buscar: String, rol: RolCodigo?, activo: Boolean?, pagina: Int): Operacion<Pagina<Usuario>> {
        if (fallarConexion) return sinRed()
        ultimaConsulta = Triple(buscar, rol, activo)
        val filtrados = usuarios.filter {
            (rol == null || it.rol == rol) && (activo == null || it.activo == activo) && Paginador.coincide(buscar, it.nombre, it.correo, it.rut)
        }
        return Operacion.Exito(Paginador.paginar(filtrados, pagina))
    }

    override suspend fun obtener(id: Long): Operacion<Usuario> =
        usuarios.firstOrNull { it.id == id }?.let { Operacion.Exito(it) } ?: Operacion.Fallo("El registro no existe en el servidor")

    override suspend fun crear(cuenta: NuevaCuenta): Operacion<Usuario> {
        if (fallarConexion) return sinRed()
        val actor = sesion.value ?: return Operacion.Fallo("Sin sesión")
        if (!actor.rol.puedeCrear(cuenta.rol)) return Operacion.Fallo("No tiene permiso para esta acción")
        if (usuarios.any { it.correo == cuenta.correo }) return Operacion.Fallo("Ya existe una cuenta con ese correo")
        creada = cuenta
        val u = Usuario(
            id = (usuarios.maxOfOrNull { it.id } ?: 0) + 1, rol = cuenta.rol, rut = cuenta.rut, nombre = cuenta.nombre,
            correo = cuenta.correo, telefono = cuenta.telefono, cargo = cuenta.cargo, idCliente = cuenta.idCliente
        )
        usuarios += u
        return Operacion.Exito(u)
    }

    override suspend fun editar(id: Long, nombre: String, correo: String, telefono: String, cargo: String): Operacion<Usuario> {
        val i = usuarios.indexOfFirst { it.id == id }
        if (i < 0) return Operacion.Fallo("El registro no existe en el servidor")
        usuarios[i] = usuarios[i].copy(nombre = nombre, correo = correo, telefono = telefono, cargo = cargo)
        return Operacion.Exito(usuarios[i])
    }

    override suspend fun cambiarEstado(id: Long, activo: Boolean): Operacion<Usuario> {
        val i = usuarios.indexOfFirst { it.id == id }
        if (i < 0) return Operacion.Fallo("El registro no existe en el servidor")
        usuarios[i] = usuarios[i].copy(activo = activo)
        return Operacion.Exito(usuarios[i])
    }

    override suspend fun eliminar(id: Long): Operacion<Unit> =
        if (usuarios.removeIf { it.id == id }) Operacion.Exito(Unit) else Operacion.Fallo("El registro no existe en el servidor")

    override suspend fun empresas(): List<Cliente> = listOf(Datos.empresa)
}

class ClimaFalso(var respuesta: Operacion<Clima>) : ClimaFuente {
    var consultas = 0
    override suspend fun actual(latitud: Double, longitud: Double): Operacion<Clima> {
        consultas++
        return respuesta
    }
}

class UsuariosApiFalsa : UsuariosApi {
    var error: Exception? = null
    var ultimoActor: Long? = null
    var ultimaCreacion: CrearUsuarioRequest? = null
    var ultimaLista: List<Any?> = emptyList()
    var codigoEliminar = 204

    private fun revisar() { error?.let { throw it } }

    override suspend fun login(datos: LoginRequest): UsuarioDto {
        revisar()
        return Datos.usuarioDto(5, "JEFE_TECNICO").copy(correo = datos.correo)
    }

    override suspend fun recuperar(datos: RecuperarRequest): RecuperarResponse {
        revisar()
        return RecuperarResponse(null, "654321", 0)
    }

    override suspend fun restablecer(datos: RestablecerRequest): MensajeDto { revisar(); return MensajeDto(null) }

    override suspend fun listar(buscar: String?, rol: String?, activo: Boolean?, pagina: Int, tamano: Int): PaginaDto<UsuarioDto> {
        revisar()
        ultimaLista = listOf(buscar, rol, activo, pagina, tamano)
        return PaginaDto(listOf(Datos.usuarioDto(1, "TECNICO"), Datos.usuarioDto(2, "CLIENTE", null)), pagina, tamano, 12, 0)
    }

    override suspend fun directorio(): List<UsuarioDto> { revisar(); return listOf(Datos.usuarioDto(1, "INGENIERO")) }

    override suspend fun obtener(id: Long): UsuarioDto { revisar(); return Datos.usuarioDto(id, "TECNICO") }

    override suspend fun crear(actor: Long, datos: CrearUsuarioRequest): UsuarioDto {
        revisar()
        ultimoActor = actor
        ultimaCreacion = datos
        return Datos.usuarioDto(99, datos.rol)
    }

    override suspend fun actualizar(actor: Long, id: Long, datos: ActualizarPerfilRequest): UsuarioDto {
        revisar()
        ultimoActor = actor
        return Datos.usuarioDto(id, "TECNICO").copy(nombre = datos.nombre, correo = datos.correo)
    }

    override suspend fun cambiarContrasena(actor: Long, id: Long, datos: CambiarContrasenaRequest): MensajeDto {
        revisar()
        ultimoActor = actor
        return MensajeDto("ok")
    }

    override suspend fun cambiarEstado(actor: Long, id: Long, datos: EstadoRequest): UsuarioDto {
        revisar()
        return Datos.usuarioDto(id, "TECNICO", datos.activo)
    }

    override suspend fun eliminar(actor: Long, id: Long): Response<Unit> {
        revisar()
        return if (codigoEliminar in 200..299) Response.success(Unit)
        else Response.error(codigoEliminar, """{"status":$codigoEliminar,"message":"Debe quedar al menos un administrador activo"}""".toResponseBody("application/json".toMediaType()))
    }
}

class ClimaApiFalsa(var respuesta: ClimaRespuestaDto? = null, var error: Exception? = null) : ClimaApi {
    override suspend fun actual(latitud: Double, longitud: Double, variables: String, zona: String): ClimaRespuestaDto {
        error?.let { throw it }
        return respuesta ?: ClimaRespuestaDto(latitud, longitud, ClimaActualDto("2026-11-24T10:00", 18.4, 62.0, 12.5, 2))
    }
}

class ActivosApiSoloEliminar(private val codigo: Int, private val sinRed: Boolean = false) : ActivosApi {
    private fun no(): Nothing = throw UnsupportedOperationException()
    private fun respuesta(): Response<Unit> {
        if (sinRed) throw IOException("sin red")
        return if (codigo in 200..299) Response.success(Unit)
        else Response.error(codigo, """{"status":$codigo,"message":"El cliente tiene instalaciones asociadas"}""".toResponseBody("application/json".toMediaType()))
    }
    override suspend fun exportar(idCliente: Long?): SyncActivosDto = no()
    override suspend fun obtenerCliente(id: Long): ClienteDto = no()
    override suspend fun crearCliente(actor: Long, datos: ClienteRequest): ClienteDto = no()
    override suspend fun actualizarCliente(id: Long, datos: ClienteRequest): ClienteDto = no()
    override suspend fun eliminarCliente(id: Long): Response<Unit> = respuesta()
    override suspend fun crearInstalacion(actor: Long, datos: InstalacionRequest): InstalacionDto = no()
    override suspend fun actualizarInstalacion(id: Long, datos: InstalacionRequest): InstalacionDto = no()
    override suspend fun eliminarInstalacion(id: Long): Response<Unit> = respuesta()
    override suspend fun crearEquipo(actor: Long, datos: EquipoRequest): EquipoDto = no()
    override suspend fun actualizarEquipo(id: Long, datos: EquipoRequest): EquipoDto = no()
    override suspend fun eliminarEquipo(id: Long): Response<Unit> = respuesta()
    override suspend fun crearInstrumento(actor: Long, datos: InstrumentoRequest): InstrumentoDto = no()
    override suspend fun actualizarInstrumento(id: Long, datos: InstrumentoRequest): InstrumentoDto = no()
    override suspend fun calibrar(id: Long, datos: CalibracionRequest): InstrumentoDto = no()
    override suspend fun eliminarInstrumento(id: Long): Response<Unit> = respuesta()
}
