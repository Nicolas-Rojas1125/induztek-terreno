package cl.duocuc.induztek.domain

import cl.duocuc.induztek.domain.model.CategoriaInstrumento
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.Instrumento
import cl.duocuc.induztek.domain.model.MatrizPermisos
import cl.duocuc.induztek.domain.model.Parametro
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.Prueba
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.reglas.AccionProtocolo
import cl.duocuc.induztek.domain.reglas.ComparadorHistorico
import cl.duocuc.induztek.domain.reglas.ContextoTransicion
import cl.duocuc.induztek.domain.reglas.MaquinaEstados
import cl.duocuc.induztek.domain.reglas.NumeradorProtocolo
import cl.duocuc.induztek.domain.reglas.Plazos
import cl.duocuc.induztek.domain.reglas.ReglasInstrumento
import cl.duocuc.induztek.domain.reglas.VigenciaInstrumento
import cl.duocuc.induztek.domain.reglas.VisibilidadCliente
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReglasNegocioTest {
    private fun sesion(rol: RolCodigo, id: Long = 1) =
        Sesion(id, "Usuario Prueba", "u@induztek.ejemplo", rol, MatrizPermisos.permisosDe(rol))

    @Test
    fun rn09_variacionTr01_comoEnAnexo9() {
        val v2025 = ComparadorHistorico.variacion(9800.0, 12500.0)!!
        val v2026 = ComparadorHistorico.variacion(6100.0, 9800.0)!!
        assertEquals(-21.6, v2025, 0.05)
        assertEquals(-37.8, v2026, 0.05)
        assertFalse(ComparadorHistorico.esSignificativa(v2025))
        assertTrue(ComparadorHistorico.esSignificativa(v2026))
    }

    @Test
    fun rn09_comparar_soloParametrosPrincipalesConHistorial() {
        val r = ComparadorHistorico.comparar(
            TipoPrueba.AISLACION.parametrosPrincipales,
            mapOf(Parametro.LECTURA_1MIN to 6100.0, Parametro.LECTURA_10MIN to 10980.0, Parametro.TENSION_PRUEBA to 5.0),
            mapOf(Parametro.LECTURA_1MIN to 9800.0)
        )
        assertEquals(1, r.size)
        assertTrue(r.first().significativa)
    }

    private val pruebaCompleta = Prueba(
        idProtocolo = 1, tipo = TipoPrueba.AISLACION, elemento = "AT-T", resultado = Resultado.OBSERVADO
    )

    private fun ctx(
        estado: EstadoProtocolo, s: Sesion, autor: Long = 1, temp: Double? = 19.0, hum: Double? = 60.0,
        pruebas: List<Prueba> = listOf(pruebaCompleta),
        requeridos: List<TipoPrueba> = listOf(TipoPrueba.AISLACION),
        variacion: Boolean = false, comentario: String = "", motivo: String = ""
    ) = ContextoTransicion(estado, s, autor, temp, hum, pruebas, requeridos, variacion, comentario, motivo)

    @Test
    fun hu02_sinTemperaturaNiHumedad_noPasaARegistrado() {
        val errores = MaquinaEstados.validar(
            AccionProtocolo.REGISTRAR_TERRENO,
            ctx(EstadoProtocolo.BORRADOR, sesion(RolCodigo.TECNICO), temp = null, hum = null)
        )
        assertEquals(2, errores.size)
    }

    @Test
    fun aclaracion2_tiposSinRegistro_bloqueanRegistro() {
        val errores = MaquinaEstados.validar(
            AccionProtocolo.REGISTRAR_TERRENO,
            ctx(
                EstadoProtocolo.BORRADOR, sesion(RolCodigo.TECNICO),
                requeridos = TipoEquipo.TRANSFORMADOR.pruebas
            )
        )
        assertTrue(errores.any { it.contains("Relación de transformación") })
    }

    @Test
    fun registroValido_sinErrores() {
        assertTrue(
            MaquinaEstados.validar(
                AccionProtocolo.REGISTRAR_TERRENO, ctx(EstadoProtocolo.BORRADOR, sesion(RolCodigo.TECNICO))
            ).isEmpty()
        )
    }

    @Test
    fun hu10_tecnicoNoPuedeFormalizar() {
        val tecnico = sesion(RolCodigo.TECNICO)
        assertFalse(
            AccionProtocolo.FORMALIZAR in
                MaquinaEstados.accionesDisponibles(EstadoProtocolo.REGISTRADO_TERRENO, tecnico, 1)
        )
        assertFalse(
            MaquinaEstados.validar(AccionProtocolo.FORMALIZAR, ctx(EstadoProtocolo.REGISTRADO_TERRENO, tecnico))
                .isEmpty()
        )
    }

    @Test
    fun hu10_ingenieroFormaliza_yObservarExigeMotivo() {
        val ing = sesion(RolCodigo.INGENIERO, id = 5)
        assertTrue(
            MaquinaEstados.validar(AccionProtocolo.FORMALIZAR, ctx(EstadoProtocolo.REGISTRADO_TERRENO, ing)).isEmpty()
        )
        assertFalse(
            MaquinaEstados.validar(AccionProtocolo.OBSERVAR, ctx(EstadoProtocolo.REGISTRADO_TERRENO, ing)).isEmpty()
        )
        assertTrue(
            MaquinaEstados.validar(
                AccionProtocolo.OBSERVAR,
                ctx(EstadoProtocolo.REGISTRADO_TERRENO, ing, motivo = "Falta nº de serie del instrumento")
            ).isEmpty()
        )
    }

    @Test
    fun rn09_variacionSignificativa_exigeComentarioParaFormalizar() {
        val ing = sesion(RolCodigo.INGENIERO, id = 5)
        assertFalse(
            MaquinaEstados.validar(
                AccionProtocolo.FORMALIZAR, ctx(EstadoProtocolo.REGISTRADO_TERRENO, ing, variacion = true)
            ).isEmpty()
        )
        assertTrue(
            MaquinaEstados.validar(
                AccionProtocolo.FORMALIZAR,
                ctx(EstadoProtocolo.REGISTRADO_TERRENO, ing, variacion = true, comentario = "Programar secado")
            ).isEmpty()
        )
    }

    @Test
    fun rn04_formalizadoNoSePuedeAnular() {
        val jefe = sesion(RolCodigo.JEFE_TECNICO)
        assertFalse(AccionProtocolo.ANULAR in MaquinaEstados.accionesDisponibles(EstadoProtocolo.FORMALIZADO, jefe, 1))
        assertTrue(AccionProtocolo.ANULAR in MaquinaEstados.accionesDisponibles(EstadoProtocolo.BORRADOR, jefe, 1))
    }

    @Test
    fun rn17_tecnicoSoloEditaLoPropioEnBorradorUObservado() {
        val tecnico = sesion(RolCodigo.TECNICO, id = 1)
        assertTrue(MaquinaEstados.puedeEditar(EstadoProtocolo.BORRADOR, tecnico, idAutor = 1))
        assertTrue(MaquinaEstados.puedeEditar(EstadoProtocolo.OBSERVADO, tecnico, idAutor = 1))
        assertFalse(MaquinaEstados.puedeEditar(EstadoProtocolo.BORRADOR, tecnico, idAutor = 2))
        assertFalse(MaquinaEstados.puedeEditar(EstadoProtocolo.REGISTRADO_TERRENO, tecnico, idAutor = 1))
        assertFalse(MaquinaEstados.puedeEditar(EstadoProtocolo.BORRADOR, sesion(RolCodigo.CLIENTE), idAutor = 1))
    }

    @Test
    fun matrizPermisos_comoEnAnexo7() {
        assertTrue(Permiso.CERRAR_VISITA in MatrizPermisos.permisosDe(RolCodigo.TECNICO))
        assertFalse(Permiso.CERRAR_VISITA in MatrizPermisos.permisosDe(RolCodigo.JEFE_TECNICO))
        assertTrue(Permiso.GESTIONAR_INSTRUMENTOS in MatrizPermisos.permisosDe(RolCodigo.JEFE_TECNICO))
        assertFalse(Permiso.GESTIONAR_INSTRUMENTOS in MatrizPermisos.permisosDe(RolCodigo.INGENIERO))
        assertEquals(setOf(Permiso.CONSULTAR_PROPIA_EMPRESA), MatrizPermisos.permisosDe(RolCodigo.CLIENTE))
    }

    @Test
    fun rn16_diasHabiles_excluyenFinDeSemana() {
        assertEquals(2, Plazos.diasHabilesEntre(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 29)))
        assertFalse(Plazos.estaAtrasado(EstadoProtocolo.REGISTRADO_TERRENO, LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 29)))
        assertTrue(Plazos.estaAtrasado(EstadoProtocolo.REGISTRADO_TERRENO, LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 30)))
        assertFalse(Plazos.estaAtrasado(EstadoProtocolo.FORMALIZADO, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
    }

    private val micro = Instrumento(
        id = 4, nombre = "Micro-ohmímetro", categoria = CategoriaInstrumento.MICROOHMIMETRO,
        marca = "Megger", modelo = "DLRO10X", numeroSerie = "DLR-10882",
        ultimaCalibracion = LocalDate.of(2025, 8, 20), vencimiento = LocalDate.of(2026, 8, 20)
    )

    @Test
    fun hu04_instrumentoVencido_noSePermite_eInformaFecha() {
        val error = ReglasInstrumento.validarUso(micro, TipoPrueba.CONTACTOS, LocalDate.of(2026, 10, 26))
        assertNotNull(error)
        assertTrue(error!!.contains("20-08-2026"))
        assertNull(ReglasInstrumento.validarUso(micro, TipoPrueba.CONTACTOS, LocalDate.of(2026, 8, 1)))
    }

    @Test
    fun instrumento_categoriaIncorrecta_seRechaza() {
        assertNotNull(ReglasInstrumento.validarUso(micro, TipoPrueba.AISLACION, LocalDate.of(2026, 8, 1)))
    }

    @Test
    fun rf19_porVencerEn30Dias() {
        val hoy = LocalDate.of(2026, 10, 26)
        assertEquals(VigenciaInstrumento.POR_VENCER, ReglasInstrumento.vigencia(LocalDate.of(2026, 11, 20), hoy))
        assertEquals(VigenciaInstrumento.VIGENTE, ReglasInstrumento.vigencia(LocalDate.of(2027, 3, 15), hoy))
        assertEquals(VigenciaInstrumento.VENCIDO, ReglasInstrumento.vigencia(LocalDate.of(2026, 10, 10), hoy))
    }

    @Test
    fun rn19_clienteVePendienteYNoVeBorradorNiAnulado() {
        assertEquals(VisibilidadCliente.PENDIENTE, VisibilidadCliente.etiquetaPara(EstadoProtocolo.REGISTRADO_TERRENO))
        assertEquals(VisibilidadCliente.PENDIENTE, VisibilidadCliente.etiquetaPara(EstadoProtocolo.OBSERVADO))
        assertEquals("Formalizado", VisibilidadCliente.etiquetaPara(EstadoProtocolo.FORMALIZADO))
        assertNull(VisibilidadCliente.etiquetaPara(EstadoProtocolo.BORRADOR))
        assertNull(VisibilidadCliente.etiquetaPara(EstadoProtocolo.ANULADO))
    }

    @Test
    fun rn02_numeroConCodigoDeDispositivo() {
        val n = NumeradorProtocolo.siguiente(2026, 402, "a7f3")
        assertEquals("PT-2026-0403-A7F3", n)
        assertTrue(NumeradorProtocolo.esValido(n))
        assertTrue(NumeradorProtocolo.esValido("PT-2026-0401"))
        assertEquals(401, NumeradorProtocolo.correlativoDe("PT-2026-0401"))
    }

    @Test
    fun rn05_pruebasPorTipoDeEquipo() {
        assertEquals(3, TipoEquipo.TRANSFORMADOR.pruebas.size)
        assertEquals(listOf(TipoPrueba.OPERACION_RELE), TipoEquipo.RELE.pruebas)
        assertFalse(Parametro.LECTURA_10MIN in TipoPrueba.AISLACION.parametros(TipoEquipo.INTERRUPTOR))
        assertTrue(Parametro.LECTURA_10MIN in TipoPrueba.AISLACION.parametros(TipoEquipo.CABLE))
    }
}
