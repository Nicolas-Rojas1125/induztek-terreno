package cl.duocuc.induztek.domain.util

import cl.duocuc.induztek.domain.model.Pagina

object Paginador {
    const val TAMANO = 10

    fun <T> paginar(lista: List<T>, pagina: Int, tamano: Int = TAMANO): Pagina<T> {
        val t = if (tamano <= 0) TAMANO else tamano
        val totalPaginas = maxOf(1, (lista.size + t - 1) / t)
        val actual = pagina.coerceIn(0, totalPaginas - 1)
        val desde = actual * t
        val hasta = minOf(desde + t, lista.size)
        val items = if (desde < lista.size) lista.subList(desde, hasta).toList() else emptyList()
        return Pagina(items, actual, totalPaginas, lista.size.toLong())
    }

    fun coincide(texto: String, vararg campos: String?): Boolean {
        val buscado = Formato.sinTildes(texto.trim()).lowercase()
        if (buscado.isEmpty()) return true
        return campos.any { c -> c != null && Formato.sinTildes(c).lowercase().contains(buscado) }
    }
}
