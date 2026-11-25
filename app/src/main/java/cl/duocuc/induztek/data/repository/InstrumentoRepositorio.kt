package cl.duocuc.induztek.data.repository

import android.database.Cursor
import cl.duocuc.induztek.data.local.EspejoLocal
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.contar
import cl.duocuc.induztek.data.remote.ActivosRemoto
import cl.duocuc.induztek.data.local.consultar
import cl.duocuc.induztek.data.local.consultarUno
import cl.duocuc.induztek.data.local.enTransaccion
import cl.duocuc.induztek.data.local.largo
import cl.duocuc.induztek.data.local.texto
import cl.duocuc.induztek.data.local.textoONulo
import cl.duocuc.induztek.domain.model.Calibracion
import cl.duocuc.induztek.domain.model.CategoriaInstrumento
import cl.duocuc.induztek.domain.model.Instrumento
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.reglas.ReglasInstrumento
import cl.duocuc.induztek.domain.reglas.VigenciaInstrumento
import cl.duocuc.induztek.domain.util.Paginador
import cl.duocuc.induztek.domain.model.TipoPrueba
import java.time.LocalDate

class InstrumentoRepositorio(
    helper: InduztekDbHelper,
    cambios: NotificadorCambios,
    private val remoto: ActivosRemoto
) : RepositorioBase(helper, cambios) {
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
            """SELECT c.*, COALESCE(NULLIF(c.registrada_por, ''), u.nombre, '') AS registra FROM calibracion c
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

    suspend fun filtrar(texto: String, categoria: CategoriaInstrumento?, vigencia: VigenciaInstrumento?, hoy: LocalDate): List<Instrumento> =
        listar().filter { ins ->
            Paginador.coincide(texto, ins.nombre, ins.marca, ins.modelo, ins.numeroSerie) &&
                (categoria == null || ins.categoria == categoria) &&
                (vigencia == null || ReglasInstrumento.vigencia(ins.vencimiento, hoy) == vigencia)
        }

    suspend fun pagina(texto: String, categoria: CategoriaInstrumento?, vigencia: VigenciaInstrumento?, hoy: LocalDate, pagina: Int): Pagina<Instrumento> =
        Paginador.paginar(filtrar(texto, categoria, vigencia, hoy), pagina)

    suspend fun guardar(
        actor: Long, nombreActor: String, i: Instrumento, calibracion: LocalDate?, vencimiento: LocalDate?
    ): Operacion<Long> = when (val r = remoto.guardarInstrumento(actor, i, calibracion, vencimiento, nombreActor)) {
        is Operacion.Fallo -> r
        is Operacion.Exito -> escribir("Ya existe un instrumento con ese número de serie") {
            enTransaccion { EspejoLocal.instrumento(this, r.valor) }
            Operacion.Exito(r.valor.id)
        }
    }

    suspend fun registrarCalibracion(c: Calibracion, actor: Long, nombreActor: String): Operacion<Long> =
        when (val r = remoto.calibrar(c.idInstrumento, c.fechaCalibracion, c.fechaVencimiento, actor, nombreActor)) {
            is Operacion.Fallo -> r
            is Operacion.Exito -> escribir {
                enTransaccion { EspejoLocal.instrumento(this, r.valor) }
                Operacion.Exito(r.valor.id)
            }
        }

    suspend fun eliminar(id: Long): Operacion<Unit> {
        val usos = leer { contar("SELECT COUNT(*) FROM prueba WHERE id_instrumento = ?", listOf(id)) }
        if (usos > 0) return Operacion.Fallo("No se puede eliminar: el instrumento se usó en $usos prueba(s)")
        return when (val r = remoto.eliminarInstrumento(id)) {
            is Operacion.Fallo -> r
            is Operacion.Exito -> escribir {
                enTransaccion {
                    delete("calibracion", "id_instrumento = ?", arrayOf(id.toString()))
                    delete("instrumento", "id_instrumento = ?", arrayOf(id.toString()))
                }
                Operacion.Exito(Unit)
            }
        }
    }
}
