package cl.duocuc.induztek.data

import cl.duocuc.induztek.data.remote.ActivosRemoto
import cl.duocuc.induztek.data.remote.ClimaActualDto
import cl.duocuc.induztek.data.remote.ClimaRemoto
import cl.duocuc.induztek.data.remote.ClimaRespuestaDto
import cl.duocuc.induztek.data.remote.LlamadaSegura
import cl.duocuc.induztek.data.remote.NuevaCuenta
import cl.duocuc.induztek.data.remote.UsuariosRemoto
import cl.duocuc.induztek.data.remote.descripcionClima
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.fakes.ActivosApiSoloEliminar
import cl.duocuc.induztek.fakes.ClimaApiFalsa
import cl.duocuc.induztek.fakes.UsuariosApiFalsa
import cl.duocuc.induztek.fakes.httpError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RemotosTest {
    private val api = UsuariosApiFalsa()
    private val usuarios = UsuariosRemoto { api }

    private fun <T> exito(o: Operacion<T>): T {
        assertTrue("Se esperaba éxito y llegó $o", o is Operacion.Exito)
        return (o as Operacion.Exito).valor
    }

    private fun fallo(o: Operacion<*>): String {
        assertTrue("Se esperaba fallo y llegó $o", o is Operacion.Fallo)
        return (o as Operacion.Fallo).mensaje
    }

    @Test
    fun login_rolAntiguoJefeTecnico_seConvierteEnAdministrador() = runTest {
        val s = exito(usuarios.login("  admin@induztek.cl ", "Induztek2026"))
        assertEquals(RolCodigo.ADMINISTRADOR, s.rol)
        assertEquals("admin@induztek.cl", s.correo)
        assertTrue(s.puede(Permiso.ELIMINAR_CUENTAS))
    }

    @Test
    fun login_credencialesMalas_usaElMensajeDelServidor() = runTest {
        api.error = httpError(401, """{"status":401,"message":"Correo o contraseña incorrectos"}""")
        assertEquals("Correo o contraseña incorrectos", fallo(usuarios.login("a@b.cl", "x")))
    }

    @Test
    fun sinConexion_devuelveMensajeAmigable() = runTest {
        api.error = IOException("timeout")
        assertEquals(LlamadaSegura.SIN_CONEXION, fallo(usuarios.directorio()))
    }

    @Test
    fun listar_mapeaPaginaYEnviaFiltros() = runTest {
        val p = exito(usuarios.listar("  ", RolCodigo.TECNICO, true, 1, 10))
        assertEquals(listOf(null, "TECNICO", true, 1, 10), api.ultimaLista)
        assertEquals(2, p.items.size)
        assertEquals(12L, p.totalElementos)
        assertEquals(1, p.totalPaginas)
        assertTrue(p.items[1].activo)
        assertEquals(RolCodigo.CLIENTE, p.items[1].rol)
    }

    @Test
    fun crear_normalizaCorreoYSoloEnviaEmpresaParaClientes() = runTest {
        val base = NuevaCuenta(" 12.345.678-5 ", " Ana Soto ", " ANA@Induztek.CL ", "", "", RolCodigo.TECNICO, 7, "Minera", "Clave2026")
        exito(usuarios.crear(1, base))
        assertEquals(1L, api.ultimoActor)
        assertEquals("ana@induztek.cl", api.ultimaCreacion?.correo)
        assertEquals("Ana Soto", api.ultimaCreacion?.nombre)
        assertNull(api.ultimaCreacion?.idCliente)
        exito(usuarios.crear(1, base.copy(rol = RolCodigo.CLIENTE)))
        assertEquals(7L, api.ultimaCreacion?.idCliente)
        assertEquals("CLIENTE", api.ultimaCreacion?.rol)
    }

    @Test
    fun crear_sinPermiso403() = runTest {
        api.error = httpError(403, "")
        val r = usuarios.crear(3, NuevaCuenta("1-9", "A B", "a@b.cl", "", "", RolCodigo.ADMINISTRADOR, null, null, "Clave2026"))
        assertEquals("No tiene permiso para esta acción", fallo(r))
    }

    @Test
    fun recuperar_sinMensaje_usaTextoPorDefecto() = runTest {
        val c = exito(usuarios.recuperar("a@b.cl"))
        assertEquals("Solicitud registrada", c.mensaje)
        assertEquals("654321", c.codigoDemo)
        assertEquals("Contraseña restablecida", exito(usuarios.restablecer("a@b.cl", "654321", "Nueva2026")))
    }

    @Test
    fun cambiarEstadoYContrasena() = runTest {
        assertEquals(false, exito(usuarios.cambiarEstado(1, 3, false)).activo)
        exito(usuarios.cambiarContrasena(4, "Vieja2026", "Nueva2026"))
        assertEquals(4L, api.ultimoActor)
    }

    @Test
    fun eliminar_conflicto_muestraMotivo() = runTest {
        exito(usuarios.eliminar(1, 3))
        api.codigoEliminar = 409
        assertEquals("Debe quedar al menos un administrador activo", fallo(usuarios.eliminar(1, 1)))
    }

    @Test
    fun activos_eliminarConDependencias_devuelveMensajeDelServidor() = runTest {
        assertEquals("El cliente tiene instalaciones asociadas", fallo(ActivosRemoto { ActivosApiSoloEliminar(409) }.eliminarCliente(1)))
        exito(ActivosRemoto { ActivosApiSoloEliminar(204) }.eliminarEquipo(1))
        assertEquals(LlamadaSegura.SIN_CONEXION, fallo(ActivosRemoto { ActivosApiSoloEliminar(204, sinRed = true) }.eliminarInstrumento(1)))
    }

    @Test
    fun clima_mapeaOpenMeteo() = runTest {
        val c = exito(ClimaRemoto { ClimaApiFalsa() }.actual(-33.45, -70.66))
        assertEquals(18.4, c.temperaturaC, 0.0)
        assertEquals(62.0, c.humedadPct, 0.0)
        assertEquals("Parcialmente nublado", c.descripcion)
        assertEquals("10:00", c.horaMedicion)
    }

    @Test
    fun clima_coordenadasInvalidas_noLlamaALaApi() = runTest {
        val falsa = ClimaApiFalsa(error = IllegalStateException("no debía llamarse"))
        assertEquals("Coordenadas inválidas", fallo(ClimaRemoto { falsa }.actual(120.0, 0.0)))
    }

    @Test
    fun clima_sinDatosOSinRed() = runTest {
        val vacia = ClimaApiFalsa(ClimaRespuestaDto(0.0, 0.0, ClimaActualDto(null, null, 50.0, null, null)))
        assertEquals("El servicio de clima no entregó datos", fallo(ClimaRemoto { vacia }.actual(0.0, 0.0)))
        val sinRed = ClimaApiFalsa(error = IOException())
        assertEquals("Sin conexión: el clima requiere internet", fallo(ClimaRemoto { sinRed }.actual(0.0, 0.0)))
    }

    @Test
    fun mensajesHttp() {
        assertEquals("Datos inválidos", LlamadaSegura.mensajeHttp(400, null))
        assertEquals("El registro no existe en el servidor", LlamadaSegura.mensajeHttp(404, "no es json {"))
        assertEquals("El registro choca con otro existente", LlamadaSegura.mensajeHttp(409, ""))
        assertEquals("El servidor está iniciando o no responde. Intente en unos segundos", LlamadaSegura.mensajeHttp(503, null))
        assertEquals("Error del servidor (500)", LlamadaSegura.mensajeHttp(500, null))
        assertEquals("RUT inválido", LlamadaSegura.mensajeHttp(400, """{"status":400,"message":"RUT inválido","errores":[]}"""))
    }

    @Test
    fun descripcionesDeClima() {
        assertEquals("Despejado", descripcionClima(0))
        assertEquals("Lluvia", descripcionClima(63))
        assertEquals("Tormenta eléctrica", descripcionClima(95))
        assertEquals("Sin información", descripcionClima(null))
        assertEquals("Condición 4", descripcionClima(4))
    }
}
