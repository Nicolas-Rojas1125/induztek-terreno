package cl.duocuc.induztek.domain

import cl.duocuc.induztek.domain.model.Momento
import cl.duocuc.induztek.domain.model.Parametro
import cl.duocuc.induztek.domain.model.Prueba
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.reglas.EvaluadorPruebas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EvaluadorPruebasTest {
    private fun aislacion(l1: Double, l10: Double?, tipo: TipoEquipo = TipoEquipo.TRANSFORMADOR) =
        EvaluadorPruebas.evaluar(
            TipoPrueba.AISLACION, tipo,
            buildMap {
                put(Parametro.TENSION_PRUEBA, 5.0)
                put(Parametro.LECTURA_1MIN, l1)
                if (l10 != null) put(Parametro.LECTURA_10MIN, l10)
            }
        )

    @Test
    fun hu05_indiceDePolarizacion16_quedaObservado() {
        assertEquals(Resultado.OBSERVADO, aislacion(1000.0, 1600.0).resultado)
    }

    @Test
    fun anexo9_historialTr01_resultadosCorrectos() {
        assertEquals(Resultado.APROBADO, aislacion(12500.0, 31250.0).resultado)
        assertEquals(Resultado.APROBADO, aislacion(9800.0, 21560.0).resultado)
        assertEquals(Resultado.OBSERVADO, aislacion(6100.0, 10980.0).resultado)
        assertEquals("IP = 1,80", aislacion(6100.0, 10980.0).calculo)
    }

    @Test
    fun ft03_ATBT_en_gigaohm_convertido_queda_observado() {
        assertEquals(Resultado.OBSERVADO, aislacion(5800.0, 11200.0).resultado)
        assertEquals(Resultado.APROBADO, aislacion(3200.0, 6900.0).resultado)
    }

    @Test
    fun rn06_lecturaMenorA100_rechaza_aunque_IP_sea_alto() {
        assertEquals(Resultado.RECHAZADO, aislacion(80.0, 400.0).resultado)
    }

    @Test
    fun rn06_ipMenorA1_rechaza() {
        assertEquals(Resultado.RECHAZADO, aislacion(500.0, 450.0).resultado)
    }

    @Test
    fun aislacionInterruptor_soloLectura1Min() {
        assertEquals(Resultado.APROBADO, aislacion(2500.0, null, TipoEquipo.INTERRUPTOR).resultado)
        assertEquals(Resultado.RECHAZADO, aislacion(80.0, null, TipoEquipo.CELDA_MT).resultado)
    }

    @Test
    fun aislacionTransformador_sinLectura10_quedaSinResultado() {
        assertNull(aislacion(6100.0, null).resultado)
    }

    @Test
    fun rn07_errorTtr_limites() {
        fun ttr(teo: Double, med: Double) = EvaluadorPruebas.evaluar(
            TipoPrueba.TTR, TipoEquipo.TRANSFORMADOR,
            mapOf(Parametro.POSICION_TAP to 3.0, Parametro.RELACION_TEORICA to teo, Parametro.RELACION_MEDIDA to med)
        ).resultado
        assertEquals(Resultado.APROBADO, ttr(30.0, 30.04))
        assertEquals(Resultado.APROBADO, ttr(30.0, 30.15))
        assertEquals(Resultado.OBSERVADO, ttr(30.0, 30.24))
        assertEquals(Resultado.RECHAZADO, ttr(30.0, 30.45))
    }

    @Test
    fun rn08_contactos_vsMaximoDePlaca() {
        fun c(v: Double) = EvaluadorPruebas.evaluar(
            TipoPrueba.CONTACTOS, TipoEquipo.CELDA_MT, mapOf(Parametro.RESISTENCIA_CONTACTO to v),
            maxContactosPlaca = 60.0
        ).resultado
        assertEquals(Resultado.APROBADO, c(60.0))
        assertEquals(Resultado.OBSERVADO, c(66.0))
        assertEquals(Resultado.RECHAZADO, c(75.0))
    }

    @Test
    fun rn08_contactos_sinMaximoDePlaca_noCalcula() {
        assertNull(
            EvaluadorPruebas.evaluar(
                TipoPrueba.CONTACTOS, TipoEquipo.INTERRUPTOR, mapOf(Parametro.RESISTENCIA_CONTACTO to 40.0)
            ).resultado
        )
    }

    @Test
    fun rn08b_rele_tiempos_y_noOpera() {
        fun r(esp: Double, med: Double, noOpera: Boolean = false) = EvaluadorPruebas.evaluar(
            TipoPrueba.OPERACION_RELE, TipoEquipo.RELE,
            mapOf(
                Parametro.CORRIENTE_AJUSTE to 5.0, Parametro.CORRIENTE_OPERACION to 5.02,
                Parametro.TIEMPO_ESPERADO to esp, Parametro.TIEMPO_MEDIDO to med
            ), noOpera = noOpera
        ).resultado
        assertEquals(Resultado.APROBADO, r(300.0, 309.0))
        assertEquals(Resultado.OBSERVADO, r(300.0, 324.0))
        assertEquals(Resultado.RECHAZADO, r(300.0, 340.0))
        assertEquals(Resultado.RECHAZADO, r(300.0, 300.0, noOpera = true))
    }

    @Test
    fun rn08c_devanados_resultadoManual() {
        val sinElegir = EvaluadorPruebas.evaluar(
            TipoPrueba.DEVANADOS, TipoEquipo.TRANSFORMADOR, mapOf(Parametro.RESISTENCIA to 1842.0)
        )
        assertNull(sinElegir.resultado)
        val elegido = EvaluadorPruebas.evaluar(
            TipoPrueba.DEVANADOS, TipoEquipo.TRANSFORMADOR, mapOf(Parametro.RESISTENCIA to 1842.0),
            resultadoManual = Resultado.APROBADO
        )
        assertEquals(Resultado.APROBADO, elegido.resultado)
    }

    private fun prueba(
        tipo: TipoPrueba, elemento: String, r: Resultado?, momento: Momento = Momento.UNICA, realizada: Boolean = true
    ) = Prueba(idProtocolo = 1, tipo = tipo, elemento = elemento, momento = momento, realizada = realizada, resultado = r)

    @Test
    fun hu05_rn10_unaRechazada_globalRechazado() {
        val pruebas = listOf(
            prueba(TipoPrueba.AISLACION, "AT-BT", Resultado.APROBADO),
            prueba(TipoPrueba.TTR, "H1-X1", Resultado.RECHAZADO),
            prueba(TipoPrueba.AISLACION, "AT-T", Resultado.OBSERVADO)
        )
        assertEquals(Resultado.RECHAZADO, EvaluadorPruebas.resultadoGlobal(pruebas))
    }

    @Test
    fun aclaracion2_noRealizadas_noCuentanParaElGlobal() {
        val pruebas = listOf(
            prueba(TipoPrueba.AISLACION, "AT-BT", Resultado.APROBADO),
            prueba(TipoPrueba.DEVANADOS, "", null, realizada = false)
        )
        assertEquals(Resultado.APROBADO, EvaluadorPruebas.resultadoGlobal(pruebas))
    }

    @Test
    fun aclaracion4_valeLaMedicionPosterior() {
        val pruebas = listOf(
            prueba(TipoPrueba.AISLACION, "AT-T", Resultado.OBSERVADO, Momento.PREVIA),
            prueba(TipoPrueba.AISLACION, "AT-T", Resultado.APROBADO, Momento.POSTERIOR)
        )
        assertEquals(Resultado.APROBADO, EvaluadorPruebas.resultadoGlobal(pruebas))
    }

    @Test
    fun sinPruebas_globalNulo() {
        assertNull(EvaluadorPruebas.resultadoGlobal(emptyList()))
    }
}
