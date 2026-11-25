package cl.duocuc.induztek.viewmodel

import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.fakes.Datos
import cl.duocuc.induztek.fakes.ReglaMain
import cl.duocuc.induztek.fakes.UsuariosFalsos
import cl.duocuc.induztek.fakes.VibradorFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UsuariosViewModelsTest {
    @get:Rule val main = ReglaMain()

    private val vibrador = VibradorFalso()
    private val admin = Datos.sesion(RolCodigo.ADMINISTRADOR, id = 1)
    private val ingeniero = Datos.sesion(RolCodigo.INGENIERO, id = 2)
    private val tecnico = Datos.sesion(RolCodigo.TECNICO, id = 3)

    private fun base() = listOf(
        Datos.usuario(1, RolCodigo.ADMINISTRADOR),
        Datos.usuario(2, RolCodigo.INGENIERO),
        Datos.usuario(3, RolCodigo.TECNICO),
        Datos.usuario(4, RolCodigo.CLIENTE, idCliente = 7),
        Datos.usuario(5, RolCodigo.TECNICO, activo = false)
    ) + (6L..25L).map { Datos.usuario(it, RolCodigo.TECNICO) }

    private fun llenarFormulario(vm: UsuarioFormViewModel, rol: RolCodigo, correo: String = "nuevo@induztek.cl") {
        vm.onRol(rol)
        vm.onNombre("Camila Soto")
        vm.onRut("12.345.678-5")
        vm.onCorreo(correo)
        vm.onContrasena("Inicial2026")
        vm.onRepetida("Inicial2026")
    }

    @Test
    fun lista_cargaPaginadaYPermiteCrearAlAdmin() {
        val vm = UsuariosViewModel(UsuariosFalsos(admin, base()), vibrador)
        val ui = vm.ui.value
        assertFalse(ui.cargando)
        assertEquals(10, ui.pagina.items.size)
        assertEquals(25L, ui.pagina.totalElementos)
        assertEquals(3, ui.pagina.totalPaginas)
        assertTrue(ui.puedeCrear)
    }

    @Test
    fun lista_tecnicoNoPuedeCrear() {
        val vm = UsuariosViewModel(UsuariosFalsos(tecnico, base()), vibrador)
        assertFalse(vm.ui.value.puedeCrear)
    }

    @Test
    fun lista_filtrosYBusquedaSeEnvianALaFuente() {
        val fuente = UsuariosFalsos(admin, base())
        val vm = UsuariosViewModel(fuente, vibrador)
        vm.onRol(RolCodigo.CLIENTE)
        assertEquals(1, vm.ui.value.pagina.items.size)
        vm.onRol(RolCodigo.CLIENTE)
        assertNull(vm.ui.value.rol)
        vm.onEstado(FiltroEstadoCuenta.entries.first { it.activo == false })
        assertEquals(listOf(5L), vm.ui.value.pagina.items.map { it.id })
        vm.onEstado(FiltroEstadoCuenta.TODAS)
        vm.onBuscar("Persona 2")
        assertEquals("Persona 2", fuente.ultimaConsulta?.first)
        assertTrue(vm.ui.value.pagina.items.all { it.nombre.contains("Persona 2") })
    }

    @Test
    fun lista_irAPagina() {
        val vm = UsuariosViewModel(UsuariosFalsos(admin, base()), vibrador)
        vm.irAPagina(2)
        assertEquals(2, vm.ui.value.pagina.pagina)
        assertEquals(5, vm.ui.value.pagina.items.size)
        assertFalse(vm.ui.value.pagina.haySiguiente)
    }

    @Test
    fun lista_sinConexion_muestraError() {
        val fuente = UsuariosFalsos(admin, base()).apply { fallarConexion = true }
        val vm = UsuariosViewModel(fuente, vibrador)
        assertNotNull(vm.ui.value.error)
        assertFalse(vm.ui.value.cargando)
    }

    @Test
    fun form_rolesPermitidosSegunQuienCrea() {
        assertEquals(RolCodigo.entries.toSet(), UsuarioFormViewModel(UsuariosFalsos(admin), vibrador, null).ui.value.rolesPermitidos.toSet())
        assertEquals(
            listOf(RolCodigo.TECNICO, RolCodigo.CLIENTE),
            UsuarioFormViewModel(UsuariosFalsos(ingeniero), vibrador, null).ui.value.rolesPermitidos
        )
        val vmTecnico = UsuarioFormViewModel(UsuariosFalsos(tecnico), vibrador, null)
        assertTrue(vmTecnico.ui.value.rolesPermitidos.isEmpty())
        assertEquals("Su rol no permite crear cuentas", vmTecnico.ui.value.sinPermiso)
    }

    @Test
    fun form_ingenieroNoPuedeCrearAdministrador() {
        val vm = UsuarioFormViewModel(UsuariosFalsos(ingeniero, base()), vibrador, null)
        llenarFormulario(vm, RolCodigo.ADMINISTRADOR)
        assertFalse(vm.validar())
        assertNotNull(vm.ui.value.errorRol)
    }

    @Test
    fun form_clienteSinEmpresa_esRechazado() {
        val vm = UsuarioFormViewModel(UsuariosFalsos(admin, base()), vibrador, null)
        llenarFormulario(vm, RolCodigo.CLIENTE)
        assertFalse(vm.validar())
        assertNotNull(vm.ui.value.errorCliente)
        vm.onCliente(Datos.empresa)
        assertTrue(vm.validar())
    }

    @Test
    fun form_cambiarRolAOtroQueNoEsCliente_quitaLaEmpresa() {
        val vm = UsuarioFormViewModel(UsuariosFalsos(admin, base()), vibrador, null)
        vm.onRol(RolCodigo.CLIENTE)
        vm.onCliente(Datos.empresa)
        vm.onRol(RolCodigo.TECNICO)
        assertNull(vm.ui.value.cliente)
    }

    @Test
    fun form_rutInvalidoYContrasenaDebil_sonRechazados() {
        val vm = UsuarioFormViewModel(UsuariosFalsos(admin, base()), vibrador, null)
        llenarFormulario(vm, RolCodigo.TECNICO)
        vm.onRut("12.345.678-9")
        vm.onContrasena("corta")
        assertFalse(vm.validar())
        assertNotNull(vm.ui.value.rut.error)
        assertNotNull(vm.ui.value.contrasena.error)
        assertEquals(1, vm.ui.value.intentosFallidos)
    }

    @Test
    fun form_ingenieroCreaTecnico() {
        val fuente = UsuariosFalsos(ingeniero, base())
        val vm = UsuarioFormViewModel(fuente, vibrador, null)
        llenarFormulario(vm, RolCodigo.TECNICO)
        vm.guardar()
        val id = vm.ui.value.guardadoId
        assertNotNull(id)
        assertEquals(RolCodigo.TECNICO, fuente.creada?.rol)
        assertNull(fuente.creada?.idCliente)
    }

    @Test
    fun form_adminCreaClienteConEmpresa() {
        val fuente = UsuariosFalsos(admin, base())
        val vm = UsuarioFormViewModel(fuente, vibrador, null)
        llenarFormulario(vm, RolCodigo.CLIENTE)
        vm.onCliente(Datos.empresa)
        vm.guardar()
        assertEquals(7L, fuente.creada?.idCliente)
        assertEquals("Minera Andes SpA", fuente.creada?.nombreCliente)
    }

    @Test
    fun form_correoDuplicado_muestraMensaje() {
        val fuente = UsuariosFalsos(admin, base())
        val vm = UsuarioFormViewModel(fuente, vibrador, null)
        llenarFormulario(vm, RolCodigo.TECNICO, correo = "p3@induztek.cl")
        vm.guardar()
        assertNull(vm.ui.value.guardadoId)
        assertEquals("Ya existe una cuenta con ese correo", vm.mensaje.value)
        assertFalse(vm.ui.value.guardando)
    }

    @Test
    fun form_edicion_cargaDatosYNoExigeContrasena() {
        val fuente = UsuariosFalsos(admin, base())
        val vm = UsuarioFormViewModel(fuente, vibrador, 3)
        assertTrue(vm.ui.value.editando)
        assertEquals("Persona 3", vm.ui.value.nombre.valor)
        vm.onNombre("Persona Tres")
        vm.guardar()
        assertEquals(3L, vm.ui.value.guardadoId)
        assertEquals("Persona Tres", fuente.usuarios.first { it.id == 3L }.nombre)
    }

    @Test
    fun detalle_adminPuedeTodoSobreUnTecnico() {
        val vm = UsuarioDetalleViewModel(UsuariosFalsos(admin, base()), vibrador, 3)
        val ui = vm.ui.value
        assertTrue(ui.puedeEditar)
        assertTrue(ui.puedeCambiarEstado)
        assertTrue(ui.puedeEliminar)
    }

    @Test
    fun detalle_ingenieroNoEliminaYNoAdministraAdmins() {
        val sobreTecnico = UsuarioDetalleViewModel(UsuariosFalsos(ingeniero, base()), vibrador, 3).ui.value
        assertTrue(sobreTecnico.puedeCambiarEstado)
        assertFalse(sobreTecnico.puedeEliminar)
        val sobreAdmin = UsuarioDetalleViewModel(UsuariosFalsos(ingeniero, base()), vibrador, 1).ui.value
        assertFalse(sobreAdmin.puedeCambiarEstado)
        assertFalse(sobreAdmin.puedeEditar)
    }

    @Test
    fun detalle_nadieSeDesactivaNiEliminaASiMismo() {
        val ui = UsuarioDetalleViewModel(UsuariosFalsos(admin, base()), vibrador, 1).ui.value
        assertTrue(ui.puedeEditar)
        assertFalse(ui.puedeCambiarEstado)
        assertFalse(ui.puedeEliminar)
    }

    @Test
    fun detalle_desactivarYActivar() {
        val fuente = UsuariosFalsos(admin, base())
        val vm = UsuarioDetalleViewModel(fuente, vibrador, 3)
        vm.alternarActivo()
        assertFalse(vm.ui.value.usuario!!.activo)
        assertFalse(fuente.usuarios.first { it.id == 3L }.activo)
        vm.alternarActivo()
        assertTrue(vm.ui.value.usuario!!.activo)
        assertEquals("Cuenta activada", vm.mensaje.value)
    }

    @Test
    fun detalle_eliminarSoloAdmin() {
        val fuenteIng = UsuariosFalsos(ingeniero, base())
        val vmIng = UsuarioDetalleViewModel(fuenteIng, vibrador, 3)
        vmIng.eliminar()
        assertFalse(vmIng.ui.value.eliminado)
        assertEquals("Solo un administrador puede eliminar cuentas", vmIng.mensaje.value)

        val fuente = UsuariosFalsos(admin, base())
        val vm = UsuarioDetalleViewModel(fuente, vibrador, 3)
        vm.eliminar()
        assertTrue(vm.ui.value.eliminado)
        assertTrue(fuente.usuarios.none { it.id == 3L })
    }

    @Test
    fun detalle_cuentaInexistente_muestraError() {
        val ui = UsuarioDetalleViewModel(UsuariosFalsos(admin, base()), vibrador, 999).ui.value
        assertNull(ui.usuario)
        assertFalse(ui.cargando)
        assertNotNull(ui.error)
    }
}
