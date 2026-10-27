package cl.duocuc.induztek.data.repository

import android.database.Cursor
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.consultar
import cl.duocuc.induztek.data.local.consultarUno
import cl.duocuc.induztek.data.local.enTransaccion
import cl.duocuc.induztek.data.local.largo
import cl.duocuc.induztek.data.local.texto
import cl.duocuc.induztek.data.local.textoONulo
import cl.duocuc.induztek.data.local.valores
import cl.duocuc.induztek.domain.model.Calibracion
import cl.duocuc.induztek.domain.model.CategoriaInstrumento
import cl.duocuc.induztek.domain.model.Instrumento
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.TipoPrueba
import java.time.LocalDate

class InstrumentoRepositorio(helper: InduztekDbHelper, cambios: NotificadorCambios) :
    RepositorioBase(helper, cambios) {
    private val sqlBase = """
        SELECT i.*,
          (SELECT c.fecha_calibracion FROM calibracion c WHERE c.id_instrumento = i.id_instrumento
             ORDER BY c.fecha_calibracion DESC LIMIT 1) AS ultima,
          (SELECT c.fecha_vencimiento FROM calibracion c WHERE c.id_instrumento = i.id_instrumento
             ORDER BY c.fecha_calibracion DESC LIMIT 1) AS vence
        FROM instrumento i WHERE 1 = 1 """

    private fun leerInstrumento(c: Cursor) = Instrumento(
        id = c.largo("id_instrumento"),
        nombre = c.texto("nombre"),
        categoria = CategoriaInstrumento.desdeCodigo(c.texto("categoria")),
        marca = c.texto("marca"),
        modelo = c.texto("modelo"),
        numeroSerie = c.texto("numero_serie"),
        ultimaCalibracion = c.textoONulo("ultima")?.let { LocalDate.parse(it) },
        vencimiento = c.textoONulo("vence")?.let { LocalDate.parse(it) }
    )

    suspend fun listar(): List<Instrumento> = leer {
        consultar("$sqlBase ORDER BY vence ASC") { leerInstrumento(it) }
    }

    suspend fun obtener(id: Long): Instrumento? = leer {
        consultarUno("$sqlBase AND i.id_instrumento = ?", listOf(id)) { leerInstrumento(it) }
    }

    suspend fun paraPrueba(tipo: TipoPrueba): List<Instrumento> =
        listar().filter { tipo in it.categoria.pruebas }

    suspend fun calibraciones(idInstrumento: Long): List<Calibracion> = leer {
        consultar(
            """SELECT c.*, COALESCE(u.nombre, '') AS registra FROM calibracion c
               LEFT JOIN usuario u ON u.id_usuario = c.id_usuario_registra
               WHERE c.id_instrumento = ? ORDER BY c.fecha_calibracion DESC""",
            listOf(idInstrumento)
        ) {
            Calibracion(
                id = it.largo("id_calibracion"),
                idInstrumento = it.largo("id_instrumento"),
                fechaCalibracion = LocalDate.parse(it.texto("fecha_calibracion")),
                fechaVencimiento = LocalDate.parse(it.texto("fecha_vencimiento")),
                registradaPor = it.texto("registra")
            )
        }
    }

    suspend fun crear(i: Instrumento, calibracion: LocalDate, vencimiento: LocalDate, idUsuario: Long): Operacion<Long> =
        escribir("Ya existe un instrumento con ese número de serie") {
            enTransaccion {
                val id = insertOrThrow(
                    "instrumento", null, valores(
                        "nombre" to i.nombre.trim(),
                        "categoria" to i.categoria.name,
                        "marca" to i.marca.trim(),
                        "modelo" to i.modelo.trim(),
                        "numero_serie" to i.numeroSerie.trim().uppercase()
                    )
                )
                insertOrThrow(
                    "calibracion", null, valores(
                        "id_instrumento" to id,
                        "fecha_calibracion" to calibracion.toString(),
                        "fecha_vencimiento" to vencimiento.toString(),
                        "id_usuario_registra" to idUsuario
                    )
                )
                Operacion.Exito(id)
            }
        }

    suspend fun registrarCalibracion(c: Calibracion, idUsuario: Long): Operacion<Long> = escribir {
        val id = insertOrThrow(
            "calibracion", null, valores(
                "id_instrumento" to c.idInstrumento,
                "fecha_calibracion" to c.fechaCalibracion.toString(),
                "fecha_vencimiento" to c.fechaVencimiento.toString(),
                "id_usuario_registra" to idUsuario
            )
        )
        Operacion.Exito(id)
    }
}
