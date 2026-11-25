package cl.duocuc.induztek.viewmodel

import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.fakes.CuentaFalsa
import cl.duocuc.induztek.fakes.Datos
import cl.duocuc.induztek.fakes.ReglaMain
import cl.duocuc.induztek.fakes.VibradorFalso
import cl.duocuc.induztek.nativo.TipoVibracion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule val main = ReglaMain()

    private val cuenta = CuentaFalsa().apply { ultimoCorreo = "tecnico@induztek.cl" }
    private val vibrador = VibradorFalso()
    private val vm = LoginViewModel(cuenta, vibrador)

    @Test
    fun precargaElUltimoCorreoUsado() {
        assertEquals("tecnico@induztek.cl", vm.ui.value.correo.valor)
    }

    @Test
    fun camposVacios_noLlamaAlServidorYMarcaErrores() {
        vm.onCorreo("")
        vm.ingresar()
        val ui = vm.ui.value
        assertNotNull(ui.correo.errorVisible)
        assertNotNull(ui.contrasena.errorVisible)
        assertEquals(1, ui.intentosFallidos)
        assertTrue(cuenta.llamadas.isEmpty())
        assertEquals(TipoVibracion.ERROR, vibrador.ultima)
    }

    @Test
    fun correoMalFormado_esRechazado() {
        vm.onCorreo("tecnico@")
        vm.onContrasena("Clave2026")
        vm.ingresar()
        assertNotNull(vm.ui.value.correo.error)
        assertTrue(cuenta.llamadas.isEmpty())
    }

    @Test
    fun credencialesCorrectas_terminaConExito() {
        cuenta.respuestaLogin = Operacion.Exito(Datos.sesion(RolCodigo.TECNICO))
        vm.onContrasena("Clave2026")
        vm.ingresar()
        val ui = vm.ui.value
        assertTrue(ui.exito)
        assertFalse(ui.cargando)
        assertNull(ui.errorGeneral)
        assertEquals(listOf("login:tecnico@induztek.cl"), cuenta.llamadas)
        assertEquals(TipoVibracion.EXITO, vibrador.ultima)
    }

    @Test
    fun credencialesIncorrectas_muestraMensajeDelServidor() {
        vm.onContrasena("otraClave1")
        vm.ingresar()
        val ui = vm.ui.value
        assertFalse(ui.exito)
        assertEquals("Correo o contraseña incorrectos", ui.errorGeneral)
        assertEquals(1, ui.intentosFallidos)
    }

    @Test
    fun alEscribirDeNuevo_seLimpiaElErrorGeneral() {
        vm.onContrasena("otraClave1")
        vm.ingresar()
        vm.onContrasena("otraClave2")
        assertNull(vm.ui.value.errorGeneral)
    }

    @Test
    fun alternarVisibilidadDeContrasena() {
        vm.onAlternarVisible()
        assertTrue(vm.ui.value.contrasenaVisible)
        vm.onAlternarVisible()
        assertFalse(vm.ui.value.contrasenaVisible)
    }
}
