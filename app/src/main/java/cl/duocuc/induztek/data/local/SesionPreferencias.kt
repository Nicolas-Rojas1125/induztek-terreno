package cl.duocuc.induztek.data.local

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

class SesionPreferencias(context: Context) {
    private val prefs = context.getSharedPreferences("induztek_sesion", Context.MODE_PRIVATE)

    companion object {
        private const val CLAVE_ID_USUARIO = "id_usuario"
        private const val CLAVE_DISPOSITIVO = "codigo_dispositivo"
        private const val CLAVE_TEMA = "tema"
        private const val CLAVE_ULTIMO_CORREO = "ultimo_correo"
        private const val SIN_SESION = -1L
    }

    fun guardarSesion(idUsuario: Long, correo: String) {
        prefs.edit {
            putLong(CLAVE_ID_USUARIO, idUsuario)
            putString(CLAVE_ULTIMO_CORREO, correo)
        }
    }

    fun idUsuario(): Long = prefs.getLong(CLAVE_ID_USUARIO, SIN_SESION)

    fun borrarSesion() {
        prefs.edit { remove(CLAVE_ID_USUARIO) }
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
}
