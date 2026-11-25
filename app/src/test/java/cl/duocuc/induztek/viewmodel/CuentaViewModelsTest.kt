package cl.duocuc.induztek.viewmodel

import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.fakes.CuentaFalsa
import cl.duocuc.induztek.fakes.Datos
import cl.duocuc.induztek.fakes.ReglaMain
import cl.duocuc.induztek.fakes.VibradorFalso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CuentaViewModelsTest {
    @get:Rule val main = ReglaMain()

    private val vibrador = VibradorFalso()

    @Test
    fun recuperar_correoInvalido_noAvanza() {
        val cuenta = CuentaFalsa()
        val vm = RecuperarClaveViewModel(cuenta, vibrador)
        vm.onCorreo("no-es-correo")
        vm.solicitarCodigo()
        assertEquals(PasoRecuperacion.SOLICITAR, vm.ui.value.paso)
        assertTrue(cuenta.llamadas.isEmpty())
    }

    @Test
    fun recuperar_flujoCompleto_llegaAListo() {
        val cuenta = CuentaFalsa()
        val vm = RecuperarClaveViewModel(cuenta, vibrador)
        vm.onCorreo("tecnico@induztek.cl")
        vm.solicitarCodigo()
        assertEquals(PasoRecuperacion.RESTABLECER, vm.ui.value.paso)
        assertEquals("123456", vm.ui.value.codigoDemo)
        vm.onCodigo("12a34-56")
        assertEquals("123456", vm.ui.value.codigo.valor)
        vm.onNueva("NuevaClave1")
        vm.onRepetida("NuevaClave1")
        vm.restablecer()
        assertEquals(PasoRecuperacion.LISTO, vm.ui.value.paso)
        assertEquals("restablecer:tecnico@induztek.cl:123456", cuenta.llamadas.last())
    }

    @Test
    fun recuperar_contrasenasDistintas_noRestablece() {
        val cuenta = CuentaFalsa()
        val vm = RecuperarClaveViewModel(cuenta, vibrador)
        vm.onCorreo("tecnico@induztek.cl")
        vm.solicitarCodigo()
        vm.onCodigo("123456")
        vm.onNueva("NuevaClave1")
        vm.onRepetida("NuevaClave2")
        vm.restablecer()
        assertEquals(PasoRecuperacion.RESTABLECER, vm.ui.value.paso)
        assertEquals("Las contraseñas no coinciden", vm.ui.value.repetida.error)
    }

    @Test
    fun recuperar_codigoRechazado_muestraErrorEnElCodigo() {
        val cuenta = CuentaFalsa().apply { respuestaRestablecer = Operacion.Fallo("Código incorrecto o vencido") }
        val vm = RecuperarClaveViewModel(cuenta, vibrador)
        vm.onCorreo("tecnico@induztek.cl")
        vm.solicitarCodigo()
        vm.onCodigo("000000")
        vm.onNueva("NuevaClave1")
        vm.onRepetida("NuevaClave1")
        vm.restablecer()
        assertEquals("Código incorrecto o vencido", vm.ui.value.codigo.error)
        vm.volverASolicitar()
        assertEquals(PasoRecuperacion.SOLICITAR, vm.ui.value.paso)
        assertNull(vm.ui.value.codigoDemo)
    }

    @Test
    fun recuperar_falloDelServidor_quedaEnSolicitarConMensaje() {
        val cuenta = CuentaFalsa().apply { respuestaRecuperar = Operacion.Fallo("Sin conexión") }
        val vm = RecuperarClaveViewModel(cuenta, vibrador)
        vm.onCorreo("tecnico@induztek.cl")
        vm.solicitarCodigo()
        assertEquals(PasoRecuperacion.SOLICITAR, vm.ui.value.paso)
        assertEquals("Sin conexión", vm.mensaje.value)
        vm.onMensajeMostrado()
        assertNull(vm.mensaje.value)
    }

    @Test
    fun cambiarClave_nuevaIgualALaActual_esRechazada() {
        val cuenta = CuentaFalsa(Datos.sesion(RolCodigo.TECNICO))
        val vm = CambiarClaveViewModel(cuenta, vibrador)
        vm.onActual("Clave2026")
        vm.onNueva("Clave2026")
        vm.onRepetida("Clave2026")
        vm.guardar()
        assertEquals("Debe ser distinta de la actual", vm.ui.value.nueva.error)
        assertFalse(vm.ui.value.listo)
        assertTrue(cuenta.llamadas.isEmpty())
    }

    @Test
    fun cambiarClave_datosValidos_quedaListo() {
        val cuenta = CuentaFalsa(Datos.sesion(RolCodigo.TECNICO))
        val vm = CambiarClaveViewModel(cuenta, vibrador)
        vm.onActual("Clave2026")
        vm.onNueva("Segura2027")
        vm.onRepetida("Segura2027")
        vm.guardar()
        assertTrue(vm.ui.value.listo)
        assertEquals("Contraseña actualizada", vm.mensaje.value)
    }

    @Test
    fun cambiarClave_actualIncorrecta_marcaElCampoActual() {
        val cuenta = CuentaFalsa(Datos.sesion(RolCodigo.TECNICO)).apply {
            respuestaCambio = Operacion.Fallo("La contraseña actual no es correcta")
        }
        val vm = CambiarClaveViewModel(cuenta, vibrador)
        vm.onActual("Mala2026")
        vm.onNueva("Segura2027")
        vm.onRepetida("Segura2027")
        vm.guardar()
        assertEquals("La contraseña actual no es correcta", vm.ui.value.actual.error)
        assertFalse(vm.ui.value.guardando)
    }

    @Test
    fun editarPerfil_cargaLosDatosDeLaSesion() {
        val cuenta = CuentaFalsa(Datos.sesion(RolCodigo.INGENIERO))
        val vm = EditarPerfilViewModel(cuenta, vibrador)
        assertEquals("Usuario ingeniero", vm.ui.value.nombre.valor)
        assertEquals("11.111.111-1", vm.ui.value.rut)
    }

    @Test
    fun editarPerfil_nombreInvalido_noGuarda() {
        val cuenta = CuentaFalsa(Datos.sesion(RolCodigo.INGENIERO))
        val vm = EditarPerfilViewModel(cuenta, vibrador)
        vm.onNombre("A")
        vm.guardar()
        assertNotNull(vm.ui.value.nombre.error)
        assertTrue(cuenta.llamadas.isEmpty())
    }

    @Test
    fun editarPerfil_datosValidos_actualizaLaSesion() {
        val cuenta = CuentaFalsa(Datos.sesion(RolCodigo.INGENIERO))
        val vm = EditarPerfilViewModel(cuenta, vibrador)
        vm.onNombre("Vicente Arancibia")
        vm.onTelefono("+56987654321")
        vm.guardar()
        assertTrue(vm.ui.value.guardado)
        assertEquals("Vicente Arancibia", cuenta.sesion.value?.nombre)
    }
}
