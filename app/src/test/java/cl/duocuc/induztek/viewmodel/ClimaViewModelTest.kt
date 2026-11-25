package cl.duocuc.induztek.viewmodel

import cl.duocuc.induztek.domain.model.Clima
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.fakes.ClimaFalso
import cl.duocuc.induztek.fakes.ReglaMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ClimaViewModelTest {
    @get:Rule val main = ReglaMain()

    private fun clima(humedad: Double) = Clima(17.5, humedad, 10.0, 3, "Nublado", "10:00")

    @Test
    fun sinCoordenadas_noConsultaLaApi() {
        val fuente = ClimaFalso(Operacion.Exito(clima(50.0)))
        val vm = ClimaViewModel(fuente)
        vm.consultar(null, -70.6)
        assertEquals(ClimaViewModel.SIN_COORDENADAS, vm.ui.value.error)
        assertTrue(vm.ui.value.consultado)
        assertEquals(0, fuente.consultas)
    }

    @Test
    fun consultaExitosa_muestraElClima() {
        val vm = ClimaViewModel(ClimaFalso(Operacion.Exito(clima(55.0))))
        vm.consultar(-33.45, -70.66)
        val ui = vm.ui.value
        assertFalse(ui.cargando)
        assertNull(ui.error)
        assertEquals(17.5, ui.clima!!.temperaturaC, 0.0)
        assertFalse(ui.alertaHumedad)
    }

    @Test
    fun humedadSobre80_activaAlerta() {
        val vm = ClimaViewModel(ClimaFalso(Operacion.Exito(clima(86.0))))
        vm.consultar(-33.45, -70.66)
        assertTrue(vm.ui.value.alertaHumedad)
    }

    @Test
    fun errorDeRed_seMuestraSinBloquear() {
        val vm = ClimaViewModel(ClimaFalso(Operacion.Fallo("Sin conexión: el clima requiere internet")))
        vm.consultar(-33.45, -70.66)
        assertEquals("Sin conexión: el clima requiere internet", vm.ui.value.error)
        assertNull(vm.ui.value.clima)
        assertFalse(vm.ui.value.cargando)
    }
}
