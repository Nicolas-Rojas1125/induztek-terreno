package cl.duocuc.induztek.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Sesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class NotificadorCambios {
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()
    fun notificar() = _version.update { it + 1 }
}

abstract class RepositorioBase(
    protected val helper: InduztekDbHelper,
    protected val cambios: NotificadorCambios
) {
    protected suspend fun <T> leer(bloque: SQLiteDatabase.() -> T): T =
        withContext(Dispatchers.IO) { helper.readableDatabase.bloque() }

    protected suspend fun <T> escribir(
        mensajeDuplicado: String = "El registro ya existe",
        bloque: SQLiteDatabase.() -> Operacion<T>
    ): Operacion<T> = withContext(Dispatchers.IO) {
        try {
            val r = helper.writableDatabase.bloque()
            if (r is Operacion.Exito) cambios.notificar()
            r
        } catch (e: SQLiteConstraintException) {
            val msg = e.message.orEmpty()
            Operacion.Fallo(
                when {
                    msg.contains("UNIQUE", ignoreCase = true) -> mensajeDuplicado
                    msg.contains("RN-", ignoreCase = true) -> msg.substringBefore(" (code").trim()
                    msg.contains("FOREIGN KEY", ignoreCase = true) -> "Referencia inválida a otro registro"
                    else -> "No se pudo guardar: datos fuera de rango"
                }
            )
        } catch (e: SQLiteException) {
            Operacion.Fallo("Error de base de datos: ${e.message?.substringBefore(" (code")}")
        }
    }

    protected fun filtroEmpresa(sesion: Sesion, columnaCliente: String, args: MutableList<Any?>): String =
        if (sesion.esCliente) {
            args += (sesion.idCliente ?: -1L)
            " AND $columnaCliente = ? "
        } else ""
}
