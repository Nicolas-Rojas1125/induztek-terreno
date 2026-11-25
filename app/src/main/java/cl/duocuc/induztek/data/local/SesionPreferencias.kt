package cl.duocuc.induztek.data.local

import android.content.Context
import androidx.core.content.edit
import cl.duocuc.induztek.data.remote.ServiciosRemotos
import java.util.UUID

class SesionPreferencias(context: Context) {
    private val prefs = context.getSharedPreferences("induztek_sesion", Context.MODE_PRIVATE)

    companion object {
        private const val CLAVE_SESION = "sesion_json"
        private const val CLAVE_DISPOSITIVO = "codigo_dispositivo"
        private const val CLAVE_TEMA = "tema"
        private const val CLAVE_ULTIMO_CORREO = "ultimo_correo"
        private const val CLAVE_SERVIDOR = "url_servidor"
        private const val CLAVE_ULTIMA_DESCARGA = "ultima_descarga_protocolos"
        private const val CLAVE_ULTIMA_SINCRONIZACION = "ultima_sincronizacion"
    }

    fun guardarSesion(json: String, correo: String) {
        prefs.edit {
            putString(CLAVE_SESION, json)
            putString(CLAVE_ULTIMO_CORREO, correo)
        }
    }

    fun sesionJson(): String? = prefs.getString(CLAVE_SESION, null)

    fun borrarSesion() {
        prefs.edit {
            remove(CLAVE_SESION)
            remove(CLAVE_ULTIMA_DESCARGA)
        }
    }

    fun ultimoCorreo(): String = prefs.getString(CLAVE_ULTIMO_CORREO, "") ?: ""

    fun codigoDispositivo(): String {
        val guardado = prefs.getString(CLAVE_DISPOSITIVO, null)
        if (guardado != null) return guardado
        val nuevo = UUID.randomUUID().toString().replace("-", "").take(4).uppercase()
        prefs.edit { putString(CLAVE_DISPOSITIVO, nuevo) }
        return nuevo
    }

    fun tema(): String = prefs.getString(CLAVE_TEMA, "sistema") ?: "sistema"

    fun guardarTema(valor: String) {
        prefs.edit { putString(CLAVE_TEMA, valor) }
    }

    fun servidor(): String = prefs.getString(CLAVE_SERVIDOR, ServiciosRemotos.URL_POR_DEFECTO) ?: ServiciosRemotos.URL_POR_DEFECTO

    fun guardarServidor(url: String) {
        prefs.edit { putString(CLAVE_SERVIDOR, url) }
    }

    fun ultimaDescarga(): Long = prefs.getLong(CLAVE_ULTIMA_DESCARGA, 0L)

    fun guardarUltimaDescarga(millis: Long) {
        prefs.edit { putLong(CLAVE_ULTIMA_DESCARGA, millis) }
    }

    fun ultimaSincronizacion(): Long? = prefs.getLong(CLAVE_ULTIMA_SINCRONIZACION, 0L).takeIf { it > 0 }

    fun guardarUltimaSincronizacion(millis: Long) {
        prefs.edit { putLong(CLAVE_ULTIMA_SINCRONIZACION, millis) }
    }
}
