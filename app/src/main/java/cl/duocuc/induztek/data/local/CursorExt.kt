package cl.duocuc.induztek.data.local

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

fun Cursor.texto(col: String): String = getString(getColumnIndexOrThrow(col)) ?: ""

fun Cursor.textoONulo(col: String): String? {
    val i = getColumnIndexOrThrow(col)
    return if (isNull(i)) null else getString(i)
}

fun Cursor.largo(col: String): Long = getLong(getColumnIndexOrThrow(col))

fun Cursor.largoONulo(col: String): Long? {
    val i = getColumnIndexOrThrow(col)
    return if (isNull(i)) null else getLong(i)
}

fun Cursor.entero(col: String): Int = getInt(getColumnIndexOrThrow(col))

fun Cursor.enteroONulo(col: String): Int? {
    val i = getColumnIndexOrThrow(col)
    return if (isNull(i)) null else getInt(i)
}

fun Cursor.decimalONulo(col: String): Double? {
    val i = getColumnIndexOrThrow(col)
    return if (isNull(i)) null else getDouble(i)
}

fun Cursor.booleano(col: String): Boolean = getInt(getColumnIndexOrThrow(col)) == 1

fun <T> SQLiteDatabase.consultar(sql: String, args: List<Any?> = emptyList(), mapear: (Cursor) -> T): List<T> {
    val lista = mutableListOf<T>()
    rawQuery(sql, args.map { it?.toString() }.toTypedArray()).use { c ->
        while (c.moveToNext()) lista.add(mapear(c))
    }
    return lista
}

fun <T> SQLiteDatabase.consultarUno(sql: String, args: List<Any?> = emptyList(), mapear: (Cursor) -> T): T? =
    consultar(sql, args, mapear).firstOrNull()

fun SQLiteDatabase.contar(sql: String, args: List<Any?> = emptyList()): Int =
    consultarUno(sql, args) { it.getInt(0) } ?: 0

fun valores(vararg pares: Pair<String, Any?>): ContentValues = ContentValues().apply {
    pares.forEach { (col, v) ->
        when (v) {
            null -> putNull(col)
            is String -> put(col, v)
            is Int -> put(col, v)
            is Long -> put(col, v)
            is Double -> put(col, v)
            is Boolean -> put(col, if (v) 1 else 0)
            else -> put(col, v.toString())
        }
    }
}

inline fun <T> SQLiteDatabase.enTransaccion(bloque: SQLiteDatabase.() -> T): T {
    beginTransaction()
    try {
        val r = bloque()
        setTransactionSuccessful()
        return r
    } finally {
        endTransaction()
    }
}
