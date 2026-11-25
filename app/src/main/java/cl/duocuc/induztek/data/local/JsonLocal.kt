package cl.duocuc.induztek.data.local

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

object JsonLocal {
    private val gson = Gson()
    private val tipoMapa = object : TypeToken<Map<String, String>>() {}.type

    fun mapaATexto(mapa: Map<String, String>): String = gson.toJson(mapa)

    fun textoAMapa(texto: String?): Map<String, String> {
        if (texto.isNullOrBlank()) return emptyMap()
        return try {
            gson.fromJson<Map<String, String>>(texto, tipoMapa) ?: emptyMap()
        } catch (e: JsonParseException) {
            emptyMap()
        }
    }

    fun <T> aTexto(valor: T): String = gson.toJson(valor)

    fun <T> desdeTexto(texto: String?, clase: Class<T>): T? {
        if (texto.isNullOrBlank()) return null
        return try {
            gson.fromJson(texto, clase)
        } catch (e: JsonParseException) {
            null
        }
    }
}
