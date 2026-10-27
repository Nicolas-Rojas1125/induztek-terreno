package cl.duocuc.induztek.nativo

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import cl.duocuc.induztek.domain.model.ActaTerreno
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.reglas.EvaluadorPruebas
import cl.duocuc.induztek.domain.util.Formato
import java.io.File
import java.io.FileOutputStream

object GeneradorActaPdf {
    private const val ANCHO = 595
    private const val ALTO = 842
    private const val MARGEN = 40f

    private val azul = Color.rgb(13, 59, 102)
    private val ambar = Color.rgb(196, 122, 0)

    private class Lienzo(val doc: PdfDocument) {
        var numeroPagina = 0
        lateinit var pagina: PdfDocument.Page
        lateinit var canvas: Canvas
        var y = 0f

        fun nuevaPagina(encabezado: (Canvas) -> Float) {
            if (numeroPagina > 0) doc.finishPage(pagina)
            numeroPagina++
            pagina = doc.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numeroPagina).create())
            canvas = pagina.canvas
            y = encabezado(canvas)
        }
    }

    private fun pintura(tam: Float, negrita: Boolean = false, color: Int = Color.BLACK) = Paint().apply {
        isAntiAlias = true
        textSize = tam
        this.color = color
        typeface = if (negrita) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
    }

    fun generar(context: Context, acta: ActaTerreno): File {
        val doc = PdfDocument()
        val l = Lienzo(doc)
        val titulo = pintura(16f, true, azul)
        val sub = pintura(10f, true)
        val normal = pintura(9f)
        val chico = pintura(8f, color = Color.DKGRAY)
        val linea = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f }

        val encabezado: (Canvas) -> Float = { c ->
            val banda = Paint().apply { color = azul }
            c.drawRect(0f, 0f, ANCHO.toFloat(), 70f, banda)
            c.drawText("INDUZTEK INGENIERÍA SpA", MARGEN, 30f, pintura(14f, true, Color.WHITE))
            c.drawText("Acta de prueba en terreno · OT ${acta.visita.numeroOt}", MARGEN, 50f, pintura(10f, false, Color.WHITE))
            val sello = Paint().apply { color = ambar }
            c.drawRect(ANCHO - 230f, 18f, ANCHO - MARGEN, 52f, sello)
            c.drawText("PENDIENTE DE FORMALIZACIÓN", ANCHO - 222f, 40f, pintura(10f, true, Color.WHITE))
            c.drawText("Página ${l.numeroPagina}", ANCHO - 90f, ALTO - 20f, chico)
            c.drawText(
                "Documento preliminar emitido en terreno. El protocolo oficial se formaliza por un ingeniero de pruebas.",
                MARGEN, ALTO - 20f, chico
            )
            95f
        }

        fun escribir(texto: String, p: Paint, salto: Float = 14f, x: Float = MARGEN) {
            val anchoDisponible = ANCHO - MARGEN - x
            val lineas = mutableListOf<String>()
            var actual = ""
            texto.split(" ").forEach { palabra ->
                val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
                if (p.measureText(prueba) > anchoDisponible && actual.isNotEmpty()) {
                    lineas += actual
                    actual = palabra
                } else {
                    actual = prueba
                }
            }
            if (actual.isNotEmpty()) lineas += actual
            lineas.forEach { renglon ->
                if (l.y > ALTO - 60f) l.nuevaPagina(encabezado)
                l.canvas.drawText(renglon, x, l.y, p)
                l.y += salto
            }
        }

        fun separador() {
            l.canvas.drawLine(MARGEN, l.y - 6f, ANCHO - MARGEN, l.y - 6f, linea)
            l.y += 6f
        }

        l.nuevaPagina(encabezado)

        val v = acta.visita
        escribir("Cliente: ${acta.cliente.razonSocial}  ·  RUT ${acta.cliente.rut}", sub)
        escribir("Instalación: ${acta.instalacion.nombre} · ${acta.instalacion.nombreComuna}, ${acta.instalacion.nombreRegion}", normal)
        escribir("Fecha de visita: ${Formato.fecha(v.fecha)}  ·  Tipo: ${v.tipo.etiqueta}", normal)
        escribir("Ingeniero a cargo: ${v.nombreIngeniero}", normal)
        escribir("Técnicos: ${v.participantes.joinToString { it.nombre }.ifBlank { "—" }}", normal)
        v.fechaHoraCierre?.let { escribir("Cierre de visita: ${Formato.fechaHora(it)}", normal) }
        if (v.latitudCierre != null && v.longitudCierre != null) {
            escribir("Coordenadas del cierre (GPS): ${Formato.coordenadas(v.latitudCierre, v.longitudCierre)}", normal)
        }
        val totales = acta.protocolos.map { it.first.resultadoGlobal }
        escribir(
            "Resumen: ${acta.protocolos.size} equipo(s) · " +
                "${totales.count { it == Resultado.APROBADO }} aprobado(s) · " +
                "${totales.count { it == Resultado.OBSERVADO }} observado(s) · " +
                "${totales.count { it == Resultado.RECHAZADO }} rechazado(s)", sub, 20f
        )
        separador()

        acta.protocolos.forEach { (prot, pruebas) ->
            if (l.y > ALTO - 160f) l.nuevaPagina(encabezado)
            escribir("${prot.numero}  ·  ${prot.tagEquipo} (${prot.tipoEquipo.etiqueta})", pintura(11f, true, azul), 15f)
            escribir(
                "Técnico: ${prot.nombreTecnico}  ·  Prueba: ${Formato.fechaHora(prot.fechaHoraPrueba)}  ·  " +
                    "T° ${prot.temperaturaC?.let { Formato.numero(it, 1) } ?: "—"} °C  ·  " +
                    "Humedad ${prot.humedadPct?.let { Formato.numero(it, 0) } ?: "—"} %", normal
            )
            escribir(
                "Resultado global: ${prot.resultadoGlobal?.etiqueta ?: "Sin resultado"}  ·  Estado: ${prot.estado.etiqueta}",
                sub
            )
            pruebas.forEach { p ->
                if (!p.realizada) {
                    escribir("• ${p.tipo.etiqueta}: NO REALIZADA — ${p.motivoNoRealizada}", normal, 13f, MARGEN + 8f)
                } else {
                    val valores = p.mediciones.joinToString("  ") {
                        "${it.parametro.etiqueta}: ${Formato.numeroCompacto(it.valor)} ${it.parametro.unidad.simbolo}".trimEnd()
                    }
                    val eval = EvaluadorPruebas.evaluar(
                        p.tipo, prot.tipoEquipo, p.valores, p.noOpera, prot.maxContactosUohm, p.resultado
                    )
                    val momento = if (p.momento.name == "UNICA") "" else " [${p.momento.etiqueta}]"
                    escribir(
                        "• ${p.tipo.etiqueta} · ${p.elemento}$momento → ${p.resultado?.etiqueta ?: "—"}" +
                            (eval.calculo?.let { " ($it)" } ?: ""),
                        normal, 12f, MARGEN + 8f
                    )
                    escribir(valores, chico, 12f, MARGEN + 20f)
                    if (p.nombreInstrumento.isNotBlank()) escribir("Instrumento: ${p.nombreInstrumento}", chico, 12f, MARGEN + 20f)
                }
            }
            if (prot.descripcionMantenimiento.isNotBlank()) escribir("Mantenimiento: ${prot.descripcionMantenimiento}", normal)
            if (prot.observaciones.isNotBlank()) escribir("Observaciones: ${prot.observaciones.take(140)}", normal)
            l.y += 6f
            separador()
        }

        if (l.y > ALTO - 120f) l.nuevaPagina(encabezado)
        escribir("RECEPCIÓN DEL ACTA (RF-08)", sub, 16f)
        val r = acta.recepcion
        if (r == null) {
            escribir("Pendiente de registrar quién recibe por parte del cliente.", normal)
        } else {
            escribir("Recibido por: ${r.receptorNombre}  ·  RUT ${r.receptorRut}", normal)
            escribir("Cargo: ${r.receptorCargo}  ·  Fecha y hora: ${Formato.fechaHora(r.fechaHoraRecepcion)}", normal)
        }

        doc.finishPage(l.pagina)
        val dir = File(context.cacheDir, "actas").apply { mkdirs() }
        val archivo = File(dir, "Acta_${v.numeroOt}.pdf")
        FileOutputStream(archivo).use { doc.writeTo(it) }
        doc.close()
        return archivo
    }

    fun compartir(context: Context, archivo: File, numeroOt: String) {
        val uri = FileProvider.getUriForFile(context, ArchivosFoto.autoridad(context), archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Acta de prueba en terreno $numeroOt · Induztek")
            putExtra(
                Intent.EXTRA_TEXT,
                "Adjuntamos el acta de prueba en terreno de la OT $numeroOt (pendiente de formalización)."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Compartir acta").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
