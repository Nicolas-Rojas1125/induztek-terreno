package cl.duocuc.induztek.domain.reglas

import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.Instrumento
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.util.Formato
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object Plazos {
    const val DIAS_HABILES_META = 2

    fun diasHabilesEntre(desde: LocalDate, hasta: LocalDate): Int {
        if (!hasta.isAfter(desde)) return 0
        var dias = 0
        var d = desde.plusDays(1)
        while (!d.isAfter(hasta)) {
            if (d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY) dias++
            d = d.plusDays(1)
        }
        return dias
    }

    fun estaAtrasado(estado: EstadoProtocolo, fechaPrueba: LocalDate, hoy: LocalDate): Boolean =
        estado != EstadoProtocolo.FORMALIZADO && estado != EstadoProtocolo.ANULADO &&
            diasHabilesEntre(fechaPrueba, hoy) > DIAS_HABILES_META
}

enum class VigenciaInstrumento(val etiqueta: String) {
    VIGENTE("Vigente"),
    POR_VENCER("Vence en 30 días o menos"),
    VENCIDO("Calibración vencida"),
    SIN_CALIBRACION("Sin calibración registrada")
}

object ReglasInstrumento {
    const val DIAS_AVISO = 30L

    fun vigencia(vencimiento: LocalDate?, fecha: LocalDate): VigenciaInstrumento = when {
        vencimiento == null -> VigenciaInstrumento.SIN_CALIBRACION
        vencimiento.isBefore(fecha) -> VigenciaInstrumento.VENCIDO
        ChronoUnit.DAYS.between(fecha, vencimiento) <= DIAS_AVISO -> VigenciaInstrumento.POR_VENCER
        else -> VigenciaInstrumento.VIGENTE
    }

    fun validarUso(instrumento: Instrumento, tipoPrueba: TipoPrueba, fechaPrueba: LocalDate): String? {
        if (tipoPrueba !in instrumento.categoria.pruebas) {
            return "${instrumento.categoria.etiqueta} no sirve para ${tipoPrueba.etiqueta.lowercase()}"
        }
        val venc = instrumento.vencimiento
            ?: return "El instrumento no tiene calibración registrada (RN-11)"
        if (venc.isBefore(fechaPrueba)) {
            return "Calibración vencida el ${Formato.fecha(venc)} (RN-11)"
        }
        return null
    }
}

object VisibilidadCliente {
    const val PENDIENTE = "Pendiente de formalización"

    fun etiquetaPara(estado: EstadoProtocolo): String? = when (estado) {
        EstadoProtocolo.FORMALIZADO -> EstadoProtocolo.FORMALIZADO.etiqueta
        EstadoProtocolo.REGISTRADO_TERRENO, EstadoProtocolo.OBSERVADO -> PENDIENTE
        EstadoProtocolo.BORRADOR, EstadoProtocolo.ANULADO -> null
    }

    val estadosVisibles: List<EstadoProtocolo> =
        EstadoProtocolo.entries.filter { etiquetaPara(it) != null }
}

object NumeradorProtocolo {
    private val patron = Regex("""^PT-(\d{4})-(\d{4})(-[A-Z0-9]{4})?$""")

    fun siguiente(anio: Int, ultimoCorrelativo: Int, codigoDispositivo: String): String =
        "PT-$anio-${(ultimoCorrelativo + 1).toString().padStart(4, '0')}-${codigoDispositivo.uppercase()}"

    fun correlativoDe(numero: String): Int? = patron.matchEntire(numero)?.groupValues?.get(2)?.toIntOrNull()

    fun esValido(numero: String): Boolean = patron.matches(numero)
}
