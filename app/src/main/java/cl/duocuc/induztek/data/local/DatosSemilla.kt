package cl.duocuc.induztek.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import cl.duocuc.induztek.domain.model.MatrizPermisos
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.RolCodigo

object DatosSemilla {
    val regiones: List<Pair<Long, String>> = listOf(
        13L to "Región Metropolitana de Santiago",
        5L to "Región de Valparaíso",
        6L to "Región del Libertador General Bernardo O'Higgins",
        2L to "Región de Antofagasta",
        8L to "Región del Biobío",
        4L to "Región de Coquimbo",
        7L to "Región del Maule",
        1L to "Región de Tarapacá",
        3L to "Región de Atacama",
        9L to "Región de La Araucanía",
        10L to "Región de Los Lagos",
        14L to "Región de Los Ríos",
        16L to "Región de Ñuble",
        15L to "Región de Arica y Parinacota",
        11L to "Región de Aysén",
        12L to "Región de Magallanes"
    )

    val comunas: List<Pair<Long, List<String>>> = listOf(
        13L to listOf(
            "Lampa", "San Bernardo", "Til Til", "La Florida", "Puente Alto", "Huechuraba",
            "Santiago", "Quilicura", "Pudahuel", "Maipú", "Colina", "Buin", "Talagante",
            "Melipilla", "Las Condes", "Providencia", "Ñuñoa", "Renca", "Cerrillos"
        ),
        5L to listOf("Valparaíso", "Viña del Mar", "Quilpué", "San Antonio", "Los Andes", "Quillota"),
        6L to listOf("Rancagua", "Machalí", "San Fernando", "Rengo"),
        2L to listOf("Antofagasta", "Calama", "Mejillones"),
        8L to listOf("Concepción", "Talcahuano", "Los Ángeles", "Coronel"),
        4L to listOf("La Serena", "Coquimbo", "Ovalle"),
        7L to listOf("Talca", "Curicó", "Linares"),
        1L to listOf("Iquique", "Alto Hospicio"),
        3L to listOf("Copiapó", "Vallenar"),
        9L to listOf("Temuco", "Angol"),
        10L to listOf("Puerto Montt", "Osorno"),
        14L to listOf("Valdivia"),
        16L to listOf("Chillán"),
        15L to listOf("Arica"),
        11L to listOf("Coyhaique"),
        12L to listOf("Punta Arenas")
    )

    fun cargar(db: SQLiteDatabase) {
        cargarUbicacion(db)
        cargarSeguridad(db)
    }

    private fun ins(db: SQLiteDatabase, tabla: String, vararg pares: Pair<String, Any?>): Long {
        val cv = ContentValues()
        pares.forEach { (col, v) ->
            when (v) {
                null -> cv.putNull(col)
                is String -> cv.put(col, v)
                is Long -> cv.put(col, v)
                else -> cv.put(col, v.toString())
            }
        }
        return db.insertOrThrow(tabla, null, cv)
    }

    private fun cargarUbicacion(db: SQLiteDatabase) {
        regiones.forEach { (id, nombre) -> ins(db, "region", "id_region" to id, "nombre" to nombre) }
        var id = 1L
        comunas.forEach { (region, lista) ->
            lista.forEach { nombre ->
                ins(db, "comuna", "id_comuna" to id++, "nombre" to nombre, "id_region" to region)
            }
        }
    }

    private fun cargarSeguridad(db: SQLiteDatabase) {
        RolCodigo.entries.forEachIndexed { i, rol ->
            ins(db, "rol", "id_rol" to (i + 1).toLong(), "codigo" to rol.name, "nombre" to rol.etiqueta)
        }
        Permiso.entries.forEachIndexed { i, p ->
            ins(db, "permiso", "id_permiso" to (i + 1).toLong(), "codigo" to p.name, "descripcion" to p.descripcion)
        }
        RolCodigo.entries.forEachIndexed { i, rol ->
            MatrizPermisos.permisosDe(rol).forEach { p ->
                ins(db, "rol_permiso", "id_rol" to (i + 1).toLong(), "id_permiso" to (p.ordinal + 1).toLong())
            }
        }
    }
}
