package cl.duocuc.induztek.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Equipo
import cl.duocuc.induztek.domain.model.Instalacion
import cl.duocuc.induztek.domain.model.Instrumento
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Usuario

object EspejoLocal {
    private fun SQLiteDatabase.upsert(tabla: String, columnaId: String, id: Long, cv: ContentValues) {
        val filas = update(tabla, cv, "$columnaId = ?", arrayOf(id.toString()))
        if (filas == 0) {
            cv.put(columnaId, id)
            insertOrThrow(tabla, null, cv)
        }
    }

    fun cliente(db: SQLiteDatabase, c: Cliente) = db.upsert(
        "cliente", "id_cliente", c.id, valores(
            "rut" to c.rut,
            "razon_social" to c.razonSocial,
            "giro" to c.giro,
            "contacto_nombre" to c.contactoNombre,
            "telefono" to c.telefono,
            "correo" to c.correo,
            "direccion" to c.direccion
        )
    )

    fun instalacion(db: SQLiteDatabase, i: Instalacion) = db.upsert(
        "instalacion", "id_instalacion", i.id, valores(
            "id_cliente" to i.idCliente,
            "nombre" to i.nombre,
            "ubicacion" to i.ubicacion,
            "id_comuna" to i.idComuna,
            "observaciones_acceso" to i.observacionesAcceso,
            "latitud" to i.latitud,
            "longitud" to i.longitud
        )
    )

    fun equipo(db: SQLiteDatabase, e: Equipo) {
        val cv = valores(
            "id_instalacion" to e.idInstalacion,
            "tag" to e.tag,
            "tipo_equipo" to e.tipo.name,
            "marca" to e.marca,
            "modelo" to e.modelo,
            "numero_serie" to e.numeroSerie,
            "anio_fabricacion" to e.anioFabricacion,
            "potencia_kva" to e.potenciaKva,
            "tension_primaria_kv" to e.tensionPrimariaKv,
            "tension_secundaria_kv" to e.tensionSecundariaKv,
            "corriente_nominal_a" to e.corrienteNominalA,
            "max_contactos_uohm" to e.maxContactosUohm,
            "observaciones" to e.observaciones,
            "datos_tecnicos" to JsonLocal.mapaATexto(e.datosTecnicos)
        )
        if (e.fotoPlaca != null) cv.put("foto_placa", e.fotoPlaca)
        db.upsert("equipo", "id_equipo", e.id, cv)
    }

    fun instrumento(db: SQLiteDatabase, i: Instrumento) {
        db.upsert(
            "instrumento", "id_instrumento", i.id, valores(
                "nombre" to i.nombre,
                "categoria" to i.categoria.name,
                "marca" to i.marca,
                "modelo" to i.modelo,
                "numero_serie" to i.numeroSerie
            )
        )
        if (i.calibraciones.isNotEmpty()) {
            db.delete("calibracion", "id_instrumento = ?", arrayOf(i.id.toString()))
            i.calibraciones.forEach { c ->
                db.insertOrThrow(
                    "calibracion", null, valores(
                        "id_calibracion" to c.id,
                        "id_instrumento" to i.id,
                        "fecha_calibracion" to c.fechaCalibracion.toString(),
                        "fecha_vencimiento" to c.fechaVencimiento.toString(),
                        "registrada_por" to c.registradaPor
                    )
                )
            }
        }
    }

    fun usuario(db: SQLiteDatabase, u: Usuario) = db.upsert(
        "usuario", "id_usuario", u.id, valores(
            "id_rol" to (u.rol.ordinal + 1).toLong(),
            "id_cliente" to (if (u.rol == RolCodigo.CLIENTE) u.idCliente else null),
            "rut" to u.rut.ifBlank { "SIN-RUT-${u.id}" },
            "nombre" to u.nombre,
            "correo" to u.correo,
            "telefono" to u.telefono,
            "cargo" to u.cargo,
            "activo" to u.activo
        )
    )

    fun eliminarSiNoSeUsa(db: SQLiteDatabase, tabla: String, columnaId: String, idsVigentes: Set<Long>): Int {
        val locales = db.consultar("SELECT $columnaId AS id FROM $tabla") { it.largo("id") }
        var eliminados = 0
        locales.filter { it !in idsVigentes }.forEach { id ->
            try {
                eliminados += db.delete(tabla, "$columnaId = ?", arrayOf(id.toString()))
            } catch (e: SQLiteException) {
                Unit
            }
        }
        return eliminados
    }
}
