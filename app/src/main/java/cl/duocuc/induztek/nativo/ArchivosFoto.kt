package cl.duocuc.induztek.nativo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ArchivosFoto {
    private const val CARPETA = "fotos_placa"

    fun autoridad(context: Context) = "${context.packageName}.archivos"

    fun nuevoArchivo(context: Context): Pair<File, Uri> {
        val dir = File(context.filesDir, CARPETA).apply { mkdirs() }
        val sello = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val archivo = File(dir, "placa_$sello.jpg")
        val uri = FileProvider.getUriForFile(context, autoridad(context), archivo)
        return archivo to uri
    }

    fun existe(ruta: String?): Boolean = ruta != null && File(ruta).let { it.exists() && it.length() > 0 }

    fun eliminar(ruta: String?) {
        if (ruta != null) runCatching { File(ruta).delete() }
    }

    fun miniatura(ruta: String, ladoMaximo: Int = 900): Bitmap? {
        if (!existe(ruta)) return null
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, opciones)
        var escala = 1
        while (opciones.outWidth / escala > ladoMaximo || opciones.outHeight / escala > ladoMaximo) escala *= 2
        return BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = escala })
    }
}
