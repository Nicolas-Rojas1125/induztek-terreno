package cl.duocuc.induztek.domain

import cl.duocuc.induztek.domain.model.Parametro
import cl.duocuc.induztek.domain.validacion.HashContrasena
import cl.duocuc.induztek.domain.validacion.ValidadorRut
import cl.duocuc.induztek.domain.validacion.Validadores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidadoresTest {
    @Test
    fun rn03_rutsDelAnexo9SonValidos() {
        listOf(
            "76.482.915-8", "96.731.540-0", "77.105.362-9", "17.284.519-3",
            "15.906.334-8", "13.871.905-7", "14.226.791-8", "18.390.452-3"
        ).forEach { assertTrue(it, ValidadorRut.esValido(it)) }
    }

    @Test
    fun hu08_rutConDvIncorrecto_seRechaza() {
        assertFalse(ValidadorRut.esValido("14.226.791-7"))
        assertNotNull(Validadores.rut("14.226.791-7"))
        assertNull(Validadores.rut("14226791-8"))
    }

    @Test
    fun rut_formateoNormalizado() {
        assertEquals("14.226.791-8", ValidadorRut.formatear("142267918"))
        assertEquals("6.543.210-K", ValidadorRut.formatear("6543210k"))
    }

    @Test
    fun correo_y_telefono() {
        assertNull(Validadores.correo("mfuentes@envpacifico.ejemplo"))
        assertNotNull(Validadores.correo("ana@"))
        assertNull(Validadores.telefono("+56 9 0000 0101"))
        assertNull(Validadores.telefono("912345678"))
        assertNotNull(Validadores.telefono("1234"))
    }

    @Test
    fun ot_y_tag() {
        assertNull(Validadores.numeroOt("OT-2026-1187"))
        assertNotNull(Validadores.numeroOt("2026-1187"))
        assertNull(Validadores.tag("REL-50/51-A"))
        assertNotNull(Validadores.tag(" "))
    }

    @Test
    fun aDecimal_aceptaComaYPunto() {
        assertEquals(5.8, Validadores.aDecimal("5,8")!!, 1e-9)
        assertEquals(5.8, Validadores.aDecimal("5.8")!!, 1e-9)
        assertNull(Validadores.aDecimal("5,8,1"))
        assertNull(Validadores.aDecimal("abc"))
    }

    @Test
    fun hu03_medicionNegativa_oFueraDeRango_seRechaza() {
        assertTrue(Validadores.medicion("-5", Parametro.LECTURA_1MIN)!!.contains("negativo"))
        assertNotNull(Validadores.medicion("20", Parametro.TENSION_PRUEBA))
        assertNull(Validadores.medicion("5", Parametro.TENSION_PRUEBA))
        assertNull(Validadores.medicion("5,8", Parametro.LECTURA_1MIN, factor = 1000.0))
        assertNotNull(Validadores.medicion("3,5", Parametro.POSICION_TAP))
    }

    @Test
    fun condicionesAmbientales() {
        assertNull(Validadores.temperatura("19"))
        assertNull(Validadores.temperatura(""))
        assertNotNull(Validadores.temperatura("75"))
        assertNotNull(Validadores.humedad("120"))
    }

    @Test
    fun calibracion_vencimientoPosterior() {
        val cal = LocalDate.of(2026, 3, 15)
        assertNull(Validadores.fechaVencimiento(cal, LocalDate.of(2027, 3, 15)))
        assertNotNull(Validadores.fechaVencimiento(cal, LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun contrasena_reglas() {
        assertNull(Validadores.contrasena("Induztek2026"))
        assertNotNull(Validadores.contrasena("corta1"))
        assertNotNull(Validadores.contrasena("solamenteletras"))
    }

    @Test
    fun nombrePersona_requiereApellido() {
        assertNull(Validadores.nombrePersona("Marcela Fuentes Rojas"))
        assertNotNull(Validadores.nombrePersona("Marcela"))
        assertNotNull(Validadores.nombrePersona("Juan 2"))
    }

    @Test
    fun hash_nuncaTextoPlano_yVerifica() {
        val sal = HashContrasena.nuevaSal()
        val hash = HashContrasena.hash("Induztek2026", sal)
        assertFalse(hash.contains("Induztek2026"))
        assertTrue(HashContrasena.verificar("Induztek2026", sal, hash))
        assertFalse(HashContrasena.verificar("otra", sal, hash))
    }
}
