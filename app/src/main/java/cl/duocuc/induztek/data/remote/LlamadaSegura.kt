package cl.duocuc.induztek.data.remote

import cl.duocuc.induztek.domain.model.Operacion
import com.google.gson.Gson
import com.google.gson.JsonParseException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

object LlamadaSegura {
    const val SIN_CONEXION = "No se pudo conectar con el servidor. Revise su conexión o la dirección del servidor"
    private val gson = Gson()

    suspend fun <T> llamar(peticion: suspend () -> T): Operacion<T> = try {
        Operacion.Exito(peticion())
    } catch (e: HttpException) {
        Operacion.Fallo(mensajeHttp(e.code(), e.response()?.errorBody()?.string()))
    } catch (e: IOException) {
        Operacion.Fallo(SIN_CONEXION)
    } catch (e: JsonParseException) {
        Operacion.Fallo("El servidor respondió con datos inválidos")
    }

    suspend fun sinCuerpo(peticion: suspend () -> Response<Unit>): Operacion<Unit> = try {
        val r = peticion()
        if (r.isSuccessful) Operacion.Exito(Unit)
        else Operacion.Fallo(mensajeHttp(r.code(), r.errorBody()?.string()))
    } catch (e: IOException) {
        Operacion.Fallo(SIN_CONEXION)
    }

    fun mensajeHttp(codigo: Int, cuerpo: String?): String {
        val delServidor = try {
            if (cuerpo.isNullOrBlank()) null else gson.fromJson(cuerpo, ErrorDto::class.java)?.message
        } catch (e: JsonParseException) {
            null
        }
        if (!delServidor.isNullOrBlank()) return delServidor
        return when (codigo) {
            400 -> "Datos inválidos"
            401 -> "Sesión no válida: vuelva a iniciar sesión"
            403 -> "No tiene permiso para esta acción"
            404 -> "El registro no existe en el servidor"
            409 -> "El registro choca con otro existente"
            502, 503, 504 -> "El servidor está iniciando o no responde. Intente en unos segundos"
            else -> "Error del servidor ($codigo)"
        }
    }
}
