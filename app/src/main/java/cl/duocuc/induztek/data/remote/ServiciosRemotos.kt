package cl.duocuc.induztek.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ServiciosRemotos(private val urlServidor: () -> String) {
    companion object {
        const val URL_POR_DEFECTO = "http://10.0.2.2:8081/"
        const val URL_CLIMA = "https://api.open-meteo.com/"
    }

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private var urlActual: String = ""
    private var retrofit: Retrofit? = null

    private fun crear(url: String): Retrofit = Retrofit.Builder()
        .baseUrl(url)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Synchronized
    private fun servidor(): Retrofit {
        val url = normalizar(urlServidor())
        val actual = retrofit
        if (actual != null && url == urlActual) return actual
        val nuevo = crear(url)
        retrofit = nuevo
        urlActual = url
        return nuevo
    }

    val usuarios: UsuariosApi get() = servidor().create(UsuariosApi::class.java)
    val activos: ActivosApi get() = servidor().create(ActivosApi::class.java)
    val protocolos: ProtocolosApi get() = servidor().create(ProtocolosApi::class.java)
    val clima: ClimaApi by lazy { crear(URL_CLIMA).create(ClimaApi::class.java) }

    fun normalizar(url: String): String {
        val limpia = url.trim().ifBlank { URL_POR_DEFECTO }
        return if (limpia.endsWith("/")) limpia else "$limpia/"
    }
}
