package cl.duocuc.induztek.domain

import cl.duocuc.induztek.domain.model.FichaTecnica
import cl.duocuc.induztek.domain.model.MatrizPermisos
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.ReglasCuentas
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.model.Usuario
import cl.duocuc.induztek.domain.util.Paginador
import cl.duocuc.induztek.domain.validacion.Validadores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CuentasYFichaTest {
    private fun sesion(rol: RolCodigo, id: Long = 1) = Sesion(id, "Prueba", "p@induztek.cl", rol)
    private fun usuario(id: Long, rol: RolCodigo) = Usuario(id = id, rol = rol, rut = "1-9", nombre = "U", correo = "u@x.cl")

    @Test
    fun rolesQuePuedeCrearCadaRol() {
        assertEquals(RolCodigo.entries.toSet(), RolCodigo.ADMINISTRADOR.rolesQuePuedeCrear.toSet())
        assertEquals(listOf(RolCodigo.TECNICO, RolCodigo.CLIENTE), RolCodigo.INGENIERO.rolesQuePuedeCrear)
        assertTrue(RolCodigo.TECNICO.rolesQuePuedeCrear.isEmpty())
        assertTrue(RolCodigo.CLIENTE.rolesQuePuedeCrear.isEmpty())
        assertFalse(RolCodigo.INGENIERO.puedeCrear(RolCodigo.INGENIERO))
        assertTrue(RolCodigo.INGENIERO.puedeCrear(RolCodigo.CLIENTE))
    }

    @Test
    fun codigoAntiguoJefeTecnico_esAdministrador() {
        assertEquals(RolCodigo.ADMINISTRADOR, RolCodigo.desdeCodigo("JEFE_TECNICO"))
        assertEquals(RolCodigo.TECNICO, RolCodigo.desdeCodigo("TECNICO"))
        assertEquals(RolCodigo.CLIENTE, RolCodigo.desdeCodigo("DESCONOCIDO"))
    }

    @Test
    fun permisosDeCuentas() {
        assertTrue(MatrizPermisos.permisosDe(RolCodigo.ADMINISTRADOR).containsAll(listOf(Permiso.CREAR_CUENTAS, Permiso.ELIMINAR_CUENTAS)))
        assertTrue(Permiso.CREAR_CUENTAS in MatrizPermisos.permisosDe(RolCodigo.INGENIERO))
        assertFalse(Permiso.ELIMINAR_CUENTAS in MatrizPermisos.permisosDe(RolCodigo.INGENIERO))
        assertFalse(Permiso.CREAR_CUENTAS in MatrizPermisos.permisosDe(RolCodigo.TECNICO))
        assertEquals(setOf(Permiso.CONSULTAR_PROPIA_EMPRESA), MatrizPermisos.permisosDe(RolCodigo.CLIENTE))
    }

    @Test
    fun reglasCuentas() {
        val admin = sesion(RolCodigo.ADMINISTRADOR, 1)
        val ing = sesion(RolCodigo.INGENIERO, 2)
        assertTrue(ReglasCuentas.puedeAdministrar(admin, usuario(5, RolCodigo.INGENIERO)))
        assertFalse(ReglasCuentas.puedeAdministrar(admin, usuario(1, RolCodigo.ADMINISTRADOR)))
        assertFalse(ReglasCuentas.puedeAdministrar(ing, usuario(6, RolCodigo.INGENIERO)))
        assertTrue(ReglasCuentas.puedeEliminar(admin, usuario(6, RolCodigo.TECNICO)))
        assertFalse(ReglasCuentas.puedeEliminar(ing, usuario(6, RolCodigo.TECNICO)))
        assertTrue(ReglasCuentas.puedeEditarPerfil(ing, usuario(2, RolCodigo.INGENIERO)))
        assertEquals("Su rol no permite crear cuentas", ReglasCuentas.mensajeNoPuedeCrear(RolCodigo.TECNICO))
        assertTrue(ReglasCuentas.mensajeNoPuedeCrear(RolCodigo.INGENIERO).contains("Técnico"))
    }

    @Test
    fun sesion_iniciales() {
        assertEquals("NR", Sesion(1, "Nicolás Rojas", "n@x.cl", RolCodigo.TECNICO).iniciales)
        assertTrue(sesion(RolCodigo.CLIENTE).esCliente)
    }

    @Test
    fun paginador_cortaYAjustaLaPagina() {
        val lista = (1..23).toList()
        val p0 = Paginador.paginar(lista, 0)
        assertEquals((1..10).toList(), p0.items)
        assertEquals(3, p0.totalPaginas)
        assertFalse(p0.hayAnterior)
        assertTrue(p0.haySiguiente)
        val ultima = Paginador.paginar(lista, 99)
        assertEquals(2, ultima.pagina)
        assertEquals(listOf(21, 22, 23), ultima.items)
        assertEquals("Página 3 de 3 · 23 resultado(s)", ultima.textoPosicion)
        val vacia = Paginador.paginar(emptyList<Int>(), 0)
        assertEquals(1, vacia.totalPaginas)
        assertEquals("Sin resultados", vacia.textoPosicion)
        assertEquals(Pagina.vacia<Int>().items, vacia.items)
        assertEquals(5, Paginador.paginar(lista, 0, tamano = 5).items.size)
    }

    @Test
    fun paginador_busquedaSinTildesNiMayusculas() {
        assertTrue(Paginador.coincide("concepcion", "Planta Concepción"))
        assertTrue(Paginador.coincide("  ", "x"))
        assertTrue(Paginador.coincide("tr-01", null, "TR-01"))
        assertFalse(Paginador.coincide("lampa", "Planta Quilicura", null))
    }

    @Test
    fun fichaTecnica_camposPorTipo() {
        TipoEquipo.entries.forEach { t -> assertTrue("Sin campos para $t", FichaTecnica.campos(t).isNotEmpty()) }
        val claves = FichaTecnica.campos(TipoEquipo.TRANSFORMADOR).map { it.clave }
        assertTrue(claves.containsAll(listOf("grupoConexion", "impedanciaPct", "medioAislante", "numeroTaps")))
        assertNotNull(FichaTecnica.campo(TipoEquipo.CABLE, "seccionMm2"))
        assertNull(FichaTecnica.campo(TipoEquipo.CABLE, "grupoConexion"))
    }

    @Test
    fun fichaTecnica_validaciones() {
        val grupo = FichaTecnica.campo(TipoEquipo.TRANSFORMADOR, "grupoConexion")!!
        val impedancia = FichaTecnica.campo(TipoEquipo.TRANSFORMADOR, "impedanciaPct")!!
        val taps = FichaTecnica.campo(TipoEquipo.TRANSFORMADOR, "numeroTaps")!!
        assertNotNull(FichaTecnica.validar(grupo, ""))
        assertNull(FichaTecnica.validar(grupo, "Dyn11"))
        assertEquals("Seleccione una opción válida", FichaTecnica.validar(grupo, "XYZ"))
        assertNull(FichaTecnica.validar(impedancia, ""))
        assertNull(FichaTecnica.validar(impedancia, "5,5"))
        assertEquals("Debe ser un número", FichaTecnica.validar(impedancia, "cinco"))
        assertEquals("Debe ser un número", FichaTecnica.validar(impedancia, "1.000,5"))
        assertNotNull(FichaTecnica.validar(impedancia, "35"))
        assertNotNull(FichaTecnica.validar(impedancia, "0.1"))
        assertEquals("Debe ser un número entero", FichaTecnica.validar(taps, "5.5"))
    }

    @Test
    fun fichaTecnica_validarTodoNormalizarYCompletitud() {
        val datos = mapOf("grupoConexion" to "Dyn11", "frecuenciaHz" to "50", "impedanciaPct" to " 5,5 ", "otro" to "x")
        val errores = FichaTecnica.validarTodo(TipoEquipo.TRANSFORMADOR, datos)
        assertEquals(setOf("medioAislante"), errores.keys)
        val normal = FichaTecnica.normalizar(TipoEquipo.TRANSFORMADOR, datos)
        assertEquals("5.5", normal["impedanciaPct"])
        assertFalse(normal.containsKey("otro"))
        assertEquals(33, FichaTecnica.completitud(TipoEquipo.TRANSFORMADOR, datos))
        assertEquals(0, FichaTecnica.completitud(TipoEquipo.RELE, emptyMap()))
    }

    @Test
    fun validadoresDeCuenta() {
        assertNull(Validadores.codigoRecuperacion("123456"))
        assertNotNull(Validadores.codigoRecuperacion("12345"))
        assertNotNull(Validadores.codigoRecuperacion(""))
        assertNull(Validadores.confirmacion("Clave2026", "Clave2026"))
        assertEquals("Las contraseñas no coinciden", Validadores.confirmacion("Clave2026", "Clave2027"))
        assertEquals("Debe combinar letras y números", Validadores.contrasena("soloLetras"))
        assertEquals("Debe ser distinta de la actual", Validadores.nuevaContrasena("Clave2026", "Clave2026"))
        assertNull(Validadores.nuevaContrasena("Clave2026", "Clave2027"))
        assertNull(Validadores.urlServidor("http://10.0.2.2:8081/"))
        assertNull(Validadores.urlServidor("https://api.induztek.cl"))
        assertNotNull(Validadores.urlServidor("10.0.2.2:8081"))
        assertNull(Validadores.direccion(""))
        assertNotNull(Validadores.direccion("x".repeat(201)))
    }
}
