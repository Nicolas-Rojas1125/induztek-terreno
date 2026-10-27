package cl.duocuc.induztek.domain.model

sealed class Operacion<out T> {
    data class Exito<T>(val valor: T) : Operacion<T>()
    data class Fallo(val mensaje: String, val detalles: List<String> = emptyList()) : Operacion<Nothing>()
}
