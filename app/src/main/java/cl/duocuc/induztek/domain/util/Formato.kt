package cl.duocuc.induztek.domain.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formato {
    private val localeCl: Locale = Locale.forLanguageTag("es-CL")
    private val simbolos = DecimalFormatSymbols(localeCl).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    }
    private val fmtFecha = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val fmtFechaHora = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    private val fmtHora = DateTimeFormatter.ofPattern("HH:mm")

    val zona: ZoneId = ZoneId.of("America/Santiago")

    fun numero(valor: Double, decimales: Int): String {
        val patron = if (decimales <= 0) "#,##0" else "#,##0." + "0".repeat(decimales)
        return DecimalFormat(patron, simbolos).format(valor)
    }

    fun numeroCompacto(valor: Double): String = DecimalFormat("#,##0.####", simbolos).format(valor)

    fun numeroEditable(valor: Double): String = DecimalFormat("0.####", simbolos).format(valor)

    fun porcentajeConSigno(valor: Double): String = (if (valor > 0) "+" else "") + numero(valor, 1) + " %"

    fun fecha(fecha: LocalDate): String = fecha.format(fmtFecha)

    fun fechaDesdeMillis(millis: Long): String = aFecha(millis).format(fmtFecha)

    fun fechaHora(millis: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zona).format(fmtFechaHora)

    fun hora(millis: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zona).format(fmtHora)

    fun aFecha(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()

    fun millisDe(fecha: LocalDate, hora: Int = 12, minuto: Int = 0): Long =
        fecha.atTime(hora, minuto).atZone(zona).toInstant().toEpochMilli()

    fun hoy(): LocalDate = LocalDate.now(zona)

    fun ahora(): Long = System.currentTimeMillis()

    fun coordenadas(lat: Double, lon: Double): String =
        String.format(Locale.US, "%.5f, %.5f", lat, lon)
}
