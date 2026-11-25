package cl.duocuc.induztek.data.remote

import cl.duocuc.induztek.data.repository.ClimaFuente
import cl.duocuc.induztek.domain.model.Clima
import cl.duocuc.induztek.domain.model.CodigoRecuperacion
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Equipo
import cl.duocuc.induztek.domain.model.Instalacion
import cl.duocuc.induztek.domain.model.Instrumento
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.Usuario
import java.time.LocalDate

private inline fun <A, B> Operacion<A>.mapear(f: (A) -> B): Operacion<B> = when (this) {
    is Operacion.Exito -> Operacion.Exito(f(valor))
    is Operacion.Fallo -> this
}

class UsuariosRemoto(private val api: () -> UsuariosApi) {
    suspend fun login(correo: String, contrasena: String): Operacion<Sesion> =
        LlamadaSegura.llamar { api().login(LoginRequest(correo.trim(), contrasena)) }.mapear { it.aSesion() }

    suspend fun recuperar(correo: String): Operacion<CodigoRecuperacion> =
        LlamadaSegura.llamar { api().recuperar(RecuperarRequest(correo.trim())) }
            .mapear { CodigoRecuperacion(it.message ?: "Solicitud registrada", it.codigoDemo) }

    suspend fun restablecer(correo: String, codigo: String, nueva: String): Operacion<String> =
        LlamadaSegura.llamar { api().restablecer(RestablecerRequest(correo.trim(), codigo.trim(), nueva)) }
            .mapear { it.message ?: "Contraseña restablecida" }

    suspend fun listar(buscar: String, rol: RolCodigo?, activo: Boolean?, pagina: Int, tamano: Int): Operacion<Pagina<Usuario>> =
        LlamadaSegura.llamar { api().listar(buscar.trim().ifBlank { null }, rol?.name, activo, pagina, tamano) }
            .mapear { p -> p.aPagina { it.aUsuario() } }

    suspend fun directorio(): Operacion<List<Usuario>> =
        LlamadaSegura.llamar { api().directorio() }.mapear { l -> l.map { it.aUsuario() } }

    suspend fun obtener(id: Long): Operacion<Usuario> =
        LlamadaSegura.llamar { api().obtener(id) }.mapear { it.aUsuario() }

    suspend fun crear(actor: Long, c: NuevaCuenta): Operacion<Usuario> =
        LlamadaSegura.llamar {
            api().crear(
                actor,
                CrearUsuarioRequest(
                    rut = c.rut.trim(), nombre = c.nombre.trim(), correo = c.correo.trim().lowercase(),
                    telefono = c.telefono.trim(), cargo = c.cargo.trim(), rol = c.rol.name,
                    idCliente = if (c.rol == RolCodigo.CLIENTE) c.idCliente else null,
                    nombreCliente = if (c.rol == RolCodigo.CLIENTE) c.nombreCliente else null,
                    contrasena = c.contrasena
                )
            )
        }.mapear { it.aUsuario() }

    suspend fun actualizar(actor: Long, id: Long, nombre: String, correo: String, telefono: String, cargo: String): Operacion<Usuario> =
        LlamadaSegura.llamar {
            api().actualizar(actor, id, ActualizarPerfilRequest(nombre.trim(), correo.trim().lowercase(), telefono.trim(), cargo.trim()))
        }.mapear { it.aUsuario() }

    suspend fun cambiarContrasena(actor: Long, actual: String, nueva: String): Operacion<Unit> =
        LlamadaSegura.llamar { api().cambiarContrasena(actor, actor, CambiarContrasenaRequest(actual, nueva)) }.mapear { }

    suspend fun cambiarEstado(actor: Long, id: Long, activo: Boolean): Operacion<Usuario> =
        LlamadaSegura.llamar { api().cambiarEstado(actor, id, EstadoRequest(activo)) }.mapear { it.aUsuario() }

    suspend fun eliminar(actor: Long, id: Long): Operacion<Unit> =
        LlamadaSegura.sinCuerpo { api().eliminar(actor, id) }
}

class ActivosRemoto(private val api: () -> ActivosApi) {
    suspend fun exportar(idCliente: Long?): Operacion<DatosMaestros> =
        LlamadaSegura.llamar { api().exportar(idCliente) }.mapear { it.aDatosMaestros() }

    suspend fun guardarCliente(actor: Long, c: Cliente): Operacion<Cliente> =
        LlamadaSegura.llamar {
            if (c.id == 0L) api().crearCliente(actor, c.aRequest()) else api().actualizarCliente(c.id, c.aRequest())
        }.mapear { it.aCliente() }

    suspend fun eliminarCliente(id: Long): Operacion<Unit> = LlamadaSegura.sinCuerpo { api().eliminarCliente(id) }

    suspend fun guardarInstalacion(actor: Long, i: Instalacion): Operacion<Instalacion> =
        LlamadaSegura.llamar {
            if (i.id == 0L) api().crearInstalacion(actor, i.aRequest()) else api().actualizarInstalacion(i.id, i.aRequest())
        }.mapear { it.aInstalacion() }

    suspend fun eliminarInstalacion(id: Long): Operacion<Unit> = LlamadaSegura.sinCuerpo { api().eliminarInstalacion(id) }

    suspend fun guardarEquipo(actor: Long, e: Equipo): Operacion<Equipo> =
        LlamadaSegura.llamar {
            if (e.id == 0L) api().crearEquipo(actor, e.aRequest()) else api().actualizarEquipo(e.id, e.aRequest())
        }.mapear { it.aEquipo() }

    suspend fun eliminarEquipo(id: Long): Operacion<Unit> = LlamadaSegura.sinCuerpo { api().eliminarEquipo(id) }

    suspend fun guardarInstrumento(
        actor: Long, i: Instrumento, calibracion: LocalDate?, vencimiento: LocalDate?, registradaPor: String
    ): Operacion<Instrumento> {
        val datos = InstrumentoRequest(
            nombre = i.nombre.trim(), categoria = i.categoria.name, marca = i.marca.trim(), modelo = i.modelo.trim(),
            numeroSerie = i.numeroSerie.trim().uppercase(),
            fechaCalibracion = calibracion?.toString(), fechaVencimiento = vencimiento?.toString(), registradaPor = registradaPor
        )
        return LlamadaSegura.llamar {
            if (i.id == 0L) api().crearInstrumento(actor, datos) else api().actualizarInstrumento(i.id, datos)
        }.mapear { it.aInstrumento() }
    }

    suspend fun calibrar(idInstrumento: Long, calibracion: LocalDate, vencimiento: LocalDate, actor: Long, registradaPor: String): Operacion<Instrumento> =
        LlamadaSegura.llamar {
            api().calibrar(idInstrumento, CalibracionRequest(calibracion.toString(), vencimiento.toString(), actor, registradaPor))
        }.mapear { it.aInstrumento() }

    suspend fun eliminarInstrumento(id: Long): Operacion<Unit> = LlamadaSegura.sinCuerpo { api().eliminarInstrumento(id) }
}

class ProtocolosRemoto(private val api: () -> ProtocolosApi) {
    suspend fun enviarVisita(v: VisitaDto): Operacion<VisitaDto> = LlamadaSegura.llamar { api().guardarVisita(v.uuid, v) }

    suspend fun enviarProtocolo(p: ProtocoloDto): Operacion<ProtocoloDto> = LlamadaSegura.llamar { api().guardarProtocolo(p.uuid, p) }

    suspend fun cambios(desde: Long, idCliente: Long?): Operacion<SyncProtocolosDto> =
        LlamadaSegura.llamar { api().cambios(desde, idCliente) }
}

class ClimaRemoto(private val api: () -> ClimaApi) : ClimaFuente {
    override suspend fun actual(latitud: Double, longitud: Double): Operacion<Clima> {
        if (latitud !in -90.0..90.0 || longitud !in -180.0..180.0) return Operacion.Fallo("Coordenadas inválidas")
        return when (val r = LlamadaSegura.llamar { api().actual(latitud, longitud) }) {
            is Operacion.Fallo -> Operacion.Fallo(
                if (r.mensaje == LlamadaSegura.SIN_CONEXION) "Sin conexión: el clima requiere internet" else r.mensaje
            )
            is Operacion.Exito -> {
                val c = r.valor.current
                val t = c?.temperatura
                val h = c?.humedad
                if (c == null || t == null || h == null) Operacion.Fallo("El servicio de clima no entregó datos")
                else Operacion.Exito(
                    Clima(
                        temperaturaC = t,
                        humedadPct = h,
                        vientoKmh = c.viento,
                        codigoTiempo = c.codigo,
                        descripcion = descripcionClima(c.codigo),
                        horaMedicion = c.time?.substringAfter('T').orEmpty()
                    )
                )
            }
        }
    }
}
