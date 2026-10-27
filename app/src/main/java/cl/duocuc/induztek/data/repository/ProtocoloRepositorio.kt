package cl.duocuc.induztek.data.repository

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.booleano
import cl.duocuc.induztek.data.local.consultar
import cl.duocuc.induztek.data.local.consultarUno
import cl.duocuc.induztek.data.local.contar
import cl.duocuc.induztek.data.local.decimalONulo
import cl.duocuc.induztek.data.local.enTransaccion
import cl.duocuc.induztek.data.local.largo
import cl.duocuc.induztek.data.local.largoONulo
import cl.duocuc.induztek.data.local.texto
import cl.duocuc.induztek.data.local.textoONulo
import cl.duocuc.induztek.data.local.valores
import cl.duocuc.induztek.domain.model.ActaTerreno
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.HistorialEstado
import cl.duocuc.induztek.domain.model.Indicadores
import cl.duocuc.induztek.domain.model.Medicion
import cl.duocuc.induztek.domain.model.Momento
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Parametro
import cl.duocuc.induztek.domain.model.Protocolo
import cl.duocuc.induztek.domain.model.ProtocoloResumen
import cl.duocuc.induztek.domain.model.Prueba
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.model.TipoVisita
import cl.duocuc.induztek.domain.reglas.Comparacion
import cl.duocuc.induztek.domain.reglas.ComparadorHistorico
import cl.duocuc.induztek.domain.reglas.EvaluadorPruebas
import cl.duocuc.induztek.domain.reglas.MaquinaEstados
import cl.duocuc.induztek.domain.reglas.NumeradorProtocolo
import cl.duocuc.induztek.domain.reglas.Plazos
import cl.duocuc.induztek.domain.reglas.ReglasInstrumento
import cl.duocuc.induztek.domain.reglas.VigenciaInstrumento
import cl.duocuc.induztek.domain.reglas.VisibilidadCliente
import cl.duocuc.induztek.domain.util.Formato
import java.util.UUID

data class PruebaAnterior(
    val numeroProtocolo: String,
    val fechaHora: Long,
    val valores: Map<Parametro, Double>,
    val resultado: Resultado?
)

data class PuntoSerie(
    val numeroProtocolo: String,
    val fechaHora: Long,
    val valor: Double,
    val resultado: Resultado?
)

data class ComparacionPrueba(
    val prueba: Prueba,
    val anterior: PruebaAnterior?,
    val comparaciones: List<Comparacion>
) {
    val significativa: Boolean get() = comparaciones.any { it.significativa }
}

class ProtocoloRepositorio(
    helper: InduztekDbHelper,
    cambios: NotificadorCambios,
    private val instrumentos: InstrumentoRepositorio
) : RepositorioBase(helper, cambios) {
    private val sqlReemplazadoPor =
        "(SELECT r.numero FROM protocolo r WHERE r.id_protocolo_reemplazado = p.id_protocolo AND r.estado <> 'ANULADO' ORDER BY r.id_protocolo DESC LIMIT 1)"

    private val sqlResumen = """
        SELECT p.id_protocolo, p.numero, p.estado, p.fecha_hora_prueba, p.resultado_global,
               p.id_usuario_registra, e.id_equipo, e.tag, e.tipo_equipo, i.nombre AS instalacion,
               i.id_cliente, c.razon_social, u.nombre AS tecnico, v.numero_ot,
               $sqlReemplazadoPor AS reemplazado_por
        FROM protocolo p
        JOIN equipo e ON e.id_equipo = p.id_equipo
        JOIN visita v ON v.id_visita = p.id_visita
        JOIN instalacion i ON i.id_instalacion = v.id_instalacion
        JOIN cliente c ON c.id_cliente = i.id_cliente
        JOIN usuario u ON u.id_usuario = p.id_usuario_registra
        WHERE 1 = 1 """

    private fun leerResumen(c: Cursor) = ProtocoloResumen(
        id = c.largo("id_protocolo"),
        numero = c.texto("numero"),
        estado = EstadoProtocolo.desdeCodigo(c.texto("estado")),
        fechaHoraPrueba = c.largo("fecha_hora_prueba"),
        tagEquipo = c.texto("tag"),
        idEquipo = c.largo("id_equipo"),
        tipoEquipo = TipoEquipo.desdeCodigo(c.texto("tipo_equipo")),
        nombreInstalacion = c.texto("instalacion"),
        idCliente = c.largo("id_cliente"),
        nombreCliente = c.texto("razon_social"),
        idTecnico = c.largo("id_usuario_registra"),
        nombreTecnico = c.texto("tecnico"),
        numeroOt = c.texto("numero_ot"),
        resultadoGlobal = Resultado.desdeCodigo(c.textoONulo("resultado_global")),
        reemplazadoPor = c.textoONulo("reemplazado_por")
    )

    private fun filtroSesion(sesion: Sesion, args: MutableList<Any?>): String {
        var f = filtroEmpresa(sesion, "i.id_cliente", args)
        if (sesion.esCliente) {
            f += " AND p.estado IN (${VisibilidadCliente.estadosVisibles.joinToString(",") { "'${it.name}'" }}) "
        }
        return f
    }

    suspend fun listar(
        sesion: Sesion,
        estado: EstadoProtocolo? = null,
        idCliente: Long? = null,
        idTecnico: Long? = null,
        texto: String = ""
    ): List<ProtocoloResumen> = leer {
        val args = mutableListOf<Any?>()
        var sql = sqlResumen + filtroSesion(sesion, args)
        if (estado != null) { sql += " AND p.estado = ? "; args += estado.name }
        if (idCliente != null) { sql += " AND i.id_cliente = ? "; args += idCliente }
        if (idTecnico != null) { sql += " AND p.id_usuario_registra = ? "; args += idTecnico }
        if (texto.isNotBlank()) {
            sql += " AND (p.numero LIKE ? OR e.tag LIKE ? OR v.numero_ot LIKE ?) "
            repeat(3) { args += "%${texto.trim()}%" }
        }
        consultar("$sql ORDER BY p.fecha_hora_prueba DESC", args) { leerResumen(it) }
    }

    suspend fun historialEquipo(idEquipo: Long, sesion: Sesion): List<ProtocoloResumen> = leer {
        val args = mutableListOf<Any?>(idEquipo)
        consultar(
            sqlResumen + " AND p.id_equipo = ? " + filtroSesion(sesion, args) + " ORDER BY p.fecha_hora_prueba DESC",
            args
        ) { leerResumen(it) }
    }

    suspend fun deVisita(idVisita: Long, sesion: Sesion): List<ProtocoloResumen> = leer {
        val args = mutableListOf<Any?>(idVisita)
        consultar(
            sqlResumen + " AND p.id_visita = ? " + filtroSesion(sesion, args) + " ORDER BY e.tag", args
        ) { leerResumen(it) }
    }

    suspend fun obtener(id: Long, sesion: Sesion): Protocolo? = leer { obtenerSync(this, id, sesion) }

    private fun obtenerSync(db: SQLiteDatabase, id: Long, sesion: Sesion): Protocolo? {
        val args = mutableListOf<Any?>(id)
        val sql = """
            SELECT p.*, e.tag, e.tipo_equipo, e.max_contactos_uohm, i.nombre AS instalacion, i.id_cliente,
                   c.razon_social, u.nombre AS tecnico, v.numero_ot, v.tipo_visita,
                   v.fecha_hora_cierre,
                   (SELECT o.numero FROM protocolo o WHERE o.id_protocolo = p.id_protocolo_reemplazado) AS numero_reemplazado,
                   $sqlReemplazadoPor AS reemplazado_por,
                   (SELECT uf.nombre FROM historial_estado h JOIN usuario uf ON uf.id_usuario = h.id_usuario
                      WHERE h.id_protocolo = p.id_protocolo AND h.estado_nuevo = 'FORMALIZADO'
                      ORDER BY h.fecha_hora DESC LIMIT 1) AS formalizado_por,
                   (SELECT h.fecha_hora FROM historial_estado h
                      WHERE h.id_protocolo = p.id_protocolo AND h.estado_nuevo = 'FORMALIZADO'
                      ORDER BY h.fecha_hora DESC LIMIT 1) AS fecha_formalizacion
            FROM protocolo p
            JOIN equipo e ON e.id_equipo = p.id_equipo
            JOIN visita v ON v.id_visita = p.id_visita
            JOIN instalacion i ON i.id_instalacion = v.id_instalacion
            JOIN cliente c ON c.id_cliente = i.id_cliente
            JOIN usuario u ON u.id_usuario = p.id_usuario_registra
            WHERE p.id_protocolo = ? """ + filtroSesion(sesion, args)
        return db.consultarUno(sql, args) { c ->
            Protocolo(
                id = c.largo("id_protocolo"),
                uuid = c.texto("uuid"),
                numero = c.texto("numero"),
                idVisita = c.largo("id_visita"),
                idEquipo = c.largo("id_equipo"),
                idUsuarioRegistra = c.largo("id_usuario_registra"),
                fechaHoraPrueba = c.largo("fecha_hora_prueba"),
                temperaturaC = c.decimalONulo("temperatura_c"),
                humedadPct = c.decimalONulo("humedad_pct"),
                descripcionMantenimiento = c.texto("descripcion_mantenimiento"),
                observaciones = c.texto("observaciones"),
                comentarioVariacion = c.texto("comentario_variacion"),
                estado = EstadoProtocolo.desdeCodigo(c.texto("estado")),
                idProtocoloReemplazado = c.largoONulo("id_protocolo_reemplazado"),
                tagEquipo = c.texto("tag"),
                tipoEquipo = TipoEquipo.desdeCodigo(c.texto("tipo_equipo")),
                maxContactosUohm = c.decimalONulo("max_contactos_uohm"),
                nombreInstalacion = c.texto("instalacion"),
                idCliente = c.largo("id_cliente"),
                nombreCliente = c.texto("razon_social"),
                nombreTecnico = c.texto("tecnico"),
                numeroOt = c.texto("numero_ot"),
                tipoVisita = TipoVisita.desdeCodigo(c.texto("tipo_visita")),
                visitaCerrada = c.largoONulo("fecha_hora_cierre") != null,
                numeroReemplazado = c.textoONulo("numero_reemplazado"),
                reemplazadoPor = c.textoONulo("reemplazado_por"),
                resultadoGlobal = Resultado.desdeCodigo(c.textoONulo("resultado_global")),
                formalizadoPor = c.textoONulo("formalizado_por"),
                fechaFormalizacion = c.largoONulo("fecha_formalizacion")
            )
        }
    }

    suspend fun pruebas(idProtocolo: Long): List<Prueba> = leer { pruebasSync(this, idProtocolo) }

    private fun pruebasSync(db: SQLiteDatabase, idProtocolo: Long): List<Prueba> {
        val mediciones = db.consultar(
            """SELECT m.id_prueba, m.parametro, m.valor FROM medicion m
               JOIN prueba pr ON pr.id_prueba = m.id_prueba WHERE pr.id_protocolo = ?""",
            listOf(idProtocolo)
        ) { c ->
            val p = Parametro.entries.firstOrNull { it.name == c.texto("parametro") }
            if (p == null) null else c.largo("id_prueba") to Medicion(p, c.getDouble(c.getColumnIndexOrThrow("valor")))
        }.filterNotNull().groupBy({ it.first }, { it.second })

        return db.consultar(
            """SELECT pr.*, COALESCE(i.nombre || ' · Nº ' || i.numero_serie, '') AS instrumento
               FROM prueba pr LEFT JOIN instrumento i ON i.id_instrumento = pr.id_instrumento
               WHERE pr.id_protocolo = ?
               ORDER BY pr.tipo_prueba, pr.elemento, CASE pr.momento WHEN 'PREVIA' THEN 0 WHEN 'UNICA' THEN 1 ELSE 2 END""",
            listOf(idProtocolo)
        ) { c ->
            val id = c.largo("id_prueba")
            Prueba(
                id = id,
                idProtocolo = c.largo("id_protocolo"),
                tipo = TipoPrueba.entries.first { it.name == c.texto("tipo_prueba") },
                elemento = c.texto("elemento"),
                momento = Momento.desdeCodigo(c.texto("momento")),
                realizada = c.booleano("realizada"),
                motivoNoRealizada = c.texto("motivo_no_realizada"),
                idInstrumento = c.largoONulo("id_instrumento"),
                resultado = Resultado.desdeCodigo(c.textoONulo("resultado")),
                noOpera = c.booleano("no_opera"),
                comentario = c.texto("comentario"),
                mediciones = mediciones[id].orEmpty().sortedBy { it.parametro.ordinal },
                nombreInstrumento = c.texto("instrumento")
            )
        }.sortedBy { TipoPrueba.entries.indexOf(it.tipo) }
    }

    suspend fun obtenerPrueba(idPrueba: Long): Prueba? = leer {
        val idProt = consultarUno("SELECT id_protocolo FROM prueba WHERE id_prueba = ?", listOf(idPrueba)) {
            it.getLong(0)
        } ?: return@leer null
        pruebasSync(this, idProt).firstOrNull { it.id == idPrueba }
    }

    private fun recalcularGlobal(db: SQLiteDatabase, idProtocolo: Long) {
        val global = EvaluadorPruebas.resultadoGlobal(pruebasSync(db, idProtocolo))
        db.update(
            "protocolo", valores("resultado_global" to global?.name, "sincronizado" to 0),
            "id_protocolo = ?", arrayOf(idProtocolo.toString())
        )
    }

    suspend fun guardarPrueba(p: Prueba, sesion: Sesion): Operacion<Long> {
        val protocolo = obtener(p.idProtocolo, sesion) ?: return Operacion.Fallo("Protocolo no encontrado")
        if (!MaquinaEstados.puedeEditar(protocolo.estado, sesion, protocolo.idUsuarioRegistra)) {
            return Operacion.Fallo("No puede modificar este protocolo (RN-17)")
        }
        if (p.realizada) {
            val idInst = p.idInstrumento ?: return Operacion.Fallo("Seleccione el instrumento utilizado (RF-05)")
            val inst = instrumentos.obtener(idInst) ?: return Operacion.Fallo("Instrumento no encontrado")
            ReglasInstrumento.validarUso(inst, p.tipo, Formato.aFecha(protocolo.fechaHoraPrueba))?.let {
                return Operacion.Fallo(it)
            }
        }
        return escribir("Ya existe una prueba para ese elemento y momento") {
            enTransaccion {
                val cv = valores(
                    "id_protocolo" to p.idProtocolo,
                    "tipo_prueba" to p.tipo.name,
                    "elemento" to p.elemento,
                    "momento" to p.momento.name,
                    "realizada" to p.realizada,
                    "motivo_no_realizada" to p.motivoNoRealizada.trim(),
                    "id_instrumento" to (if (p.realizada) p.idInstrumento else null),
                    "resultado" to (if (p.realizada) p.resultado?.name else null),
                    "no_opera" to p.noOpera,
                    "comentario" to p.comentario.trim()
                )
                val id = if (p.id == 0L) {
                    insertOrThrow("prueba", null, cv)
                } else {
                    update("prueba", cv, "id_prueba = ?", arrayOf(p.id.toString()))
                    delete("medicion", "id_prueba = ?", arrayOf(p.id.toString()))
                    p.id
                }
                if (p.realizada) {
                    p.mediciones.forEach { m ->
                        insertOrThrow(
                            "medicion", null,
                            valores("id_prueba" to id, "parametro" to m.parametro.name, "valor" to m.valor)
                        )
                    }
                }
                if (!p.realizada) {
                    delete("prueba", "id_protocolo = ? AND tipo_prueba = ? AND id_prueba <> ?",
                        arrayOf(p.idProtocolo.toString(), p.tipo.name, id.toString()))
                } else {
                    delete("prueba", "id_protocolo = ? AND tipo_prueba = ? AND realizada = 0",
                        arrayOf(p.idProtocolo.toString(), p.tipo.name))
                }
                recalcularGlobal(this, p.idProtocolo)
                Operacion.Exito(id)
            }
        }
    }

    suspend fun eliminarPrueba(idPrueba: Long, idProtocolo: Long): Operacion<Unit> = escribir {
        enTransaccion {
            delete("prueba", "id_prueba = ?", arrayOf(idPrueba.toString()))
            recalcularGlobal(this, idProtocolo)
            Operacion.Exito(Unit)
        }
    }

    private fun siguienteNumero(db: SQLiteDatabase, codigoDispositivo: String): String {
        val anio = Formato.hoy().year
        val maximo = db.consultar("SELECT numero FROM protocolo WHERE numero LIKE ?", listOf("PT-$anio-%")) {
            NumeradorProtocolo.correlativoDe(it.texto("numero")) ?: 0
        }.maxOrNull() ?: 0
        return NumeradorProtocolo.siguiente(anio, maximo, codigoDispositivo)
    }

    private fun registrarHistorial(
        db: SQLiteDatabase, idProtocolo: Long, anterior: EstadoProtocolo?, nuevo: EstadoProtocolo, idUsuario: Long, motivo: String
    ) {
        db.insertOrThrow(
            "historial_estado", null, valores(
                "id_protocolo" to idProtocolo,
                "estado_anterior" to anterior?.name,
                "estado_nuevo" to nuevo.name,
                "id_usuario" to idUsuario,
                "fecha_hora" to System.currentTimeMillis(),
                "motivo" to motivo.trim()
            )
        )
    }

    suspend fun crear(idVisita: Long, idEquipo: Long, sesion: Sesion, codigoDispositivo: String): Operacion<Long> =
        escribir("Este equipo ya tiene un protocolo en esta visita (Aclaración nº 5)") {
            val abierta = contar(
                "SELECT COUNT(*) FROM visita WHERE id_visita = ? AND fecha_hora_cierre IS NULL", listOf(idVisita)
            ) > 0
            if (!abierta) return@escribir Operacion.Fallo("La visita ya está cerrada")
            val mismaInstalacion = contar(
                """SELECT COUNT(*) FROM visita v JOIN equipo e ON e.id_instalacion = v.id_instalacion
                   WHERE v.id_visita = ? AND e.id_equipo = ?""", listOf(idVisita, idEquipo)
            ) > 0
            if (!mismaInstalacion) return@escribir Operacion.Fallo("El equipo no pertenece a la instalación de la visita")
            val duplicado = contar(
                "SELECT COUNT(*) FROM protocolo WHERE id_visita = ? AND id_equipo = ? AND id_protocolo_reemplazado IS NULL",
                listOf(idVisita, idEquipo)
            ) > 0
            if (duplicado) return@escribir Operacion.Fallo("Este equipo ya tiene un protocolo en esta visita (Aclaración nº 5)")

            enTransaccion {
                val id = insertOrThrow(
                    "protocolo", null, valores(
                        "uuid" to UUID.randomUUID().toString(),
                        "numero" to siguienteNumero(this, codigoDispositivo),
                        "id_visita" to idVisita,
                        "id_equipo" to idEquipo,
                        "id_usuario_registra" to sesion.idUsuario,
                        "fecha_hora_prueba" to System.currentTimeMillis(),
                        "estado" to EstadoProtocolo.BORRADOR.name
                    )
                )
                registrarHistorial(this, id, null, EstadoProtocolo.BORRADOR, sesion.idUsuario, "Protocolo iniciado en terreno")
                Operacion.Exito(id)
            }
        }

    suspend fun actualizarCabecera(
        id: Long, temperatura: Double?, humedad: Double?, mantenimiento: String, observaciones: String,
        comentarioVariacion: String
    ): Operacion<Unit> = escribir {
        update(
            "protocolo", valores(
                "temperatura_c" to temperatura,
                "humedad_pct" to humedad,
                "descripcion_mantenimiento" to mantenimiento.trim(),
                "observaciones" to observaciones.trim(),
                "comentario_variacion" to comentarioVariacion.trim(),
                "sincronizado" to 0
            ),
            "id_protocolo = ?", arrayOf(id.toString())
        )
        Operacion.Exito(Unit)
    }

    suspend fun guardarComentarioVariacion(id: Long, comentario: String): Operacion<Unit> = escribir {
        update(
            "protocolo", valores("comentario_variacion" to comentario.trim()),
            "id_protocolo = ? AND estado <> 'FORMALIZADO'", arrayOf(id.toString())
        )
        Operacion.Exito(Unit)
    }

    suspend fun cambiarEstado(
        id: Long, desde: EstadoProtocolo, hacia: EstadoProtocolo, idUsuario: Long, motivo: String
    ): Operacion<Unit> = escribir {
        enTransaccion {
            val filas = update(
                "protocolo", valores("estado" to hacia.name, "sincronizado" to 0),
                "id_protocolo = ? AND estado = ?", arrayOf(id.toString(), desde.name)
            )
            if (filas == 0) {
                Operacion.Fallo("El protocolo cambió de estado en otro proceso. Recargue la pantalla.")
            } else {
                registrarHistorial(this, id, desde, hacia, idUsuario, motivo)
                Operacion.Exito(Unit)
            }
        }
    }

    suspend fun emitirReemplazo(idOriginal: Long, sesion: Sesion, codigoDispositivo: String): Operacion<Long> =
        escribir("Ya existe un reemplazo para este protocolo") {
            val original = obtenerSync(this, idOriginal, sesion)
                ?: return@escribir Operacion.Fallo("Protocolo no encontrado")
            if (original.estado != EstadoProtocolo.FORMALIZADO) {
                return@escribir Operacion.Fallo("Solo se reemplazan protocolos formalizados")
            }
            if (original.reemplazadoPor != null) {
                return@escribir Operacion.Fallo("Ya fue reemplazado por ${original.reemplazadoPor}")
            }
            val pruebasOriginales = pruebasSync(this, idOriginal)
            enTransaccion {
                val nuevo = insertOrThrow(
                    "protocolo", null, valores(
                        "uuid" to UUID.randomUUID().toString(),
                        "numero" to siguienteNumero(this, codigoDispositivo),
                        "id_visita" to original.idVisita,
                        "id_equipo" to original.idEquipo,
                        "id_usuario_registra" to sesion.idUsuario,
                        "fecha_hora_prueba" to original.fechaHoraPrueba,
                        "temperatura_c" to original.temperaturaC,
                        "humedad_pct" to original.humedadPct,
                        "descripcion_mantenimiento" to original.descripcionMantenimiento,
                        "observaciones" to original.observaciones,
                        "estado" to EstadoProtocolo.BORRADOR.name,
                        "resultado_global" to original.resultadoGlobal?.name,
                        "id_protocolo_reemplazado" to idOriginal
                    )
                )
                pruebasOriginales.forEach { p ->
                    val idP = insertOrThrow(
                        "prueba", null, valores(
                            "id_protocolo" to nuevo, "tipo_prueba" to p.tipo.name, "elemento" to p.elemento,
                            "momento" to p.momento.name, "realizada" to p.realizada,
                            "motivo_no_realizada" to p.motivoNoRealizada, "id_instrumento" to p.idInstrumento,
                            "resultado" to p.resultado?.name, "no_opera" to p.noOpera, "comentario" to p.comentario
                        )
                    )
                    p.mediciones.forEach { m ->
                        insertOrThrow("medicion", null, valores("id_prueba" to idP, "parametro" to m.parametro.name, "valor" to m.valor))
                    }
                }
                registrarHistorial(
                    this, nuevo, null, EstadoProtocolo.BORRADOR, sesion.idUsuario, "Reemplazo de ${original.numero}"
                )
                Operacion.Exito(nuevo)
            }
        }

    suspend fun historialEstados(idProtocolo: Long): List<HistorialEstado> = leer {
        consultar(
            """SELECT h.*, u.nombre FROM historial_estado h JOIN usuario u ON u.id_usuario = h.id_usuario
               WHERE h.id_protocolo = ? ORDER BY h.fecha_hora ASC, h.id_historial ASC""",
            listOf(idProtocolo)
        ) { c ->
            HistorialEstado(
                id = c.largo("id_historial"),
                estadoAnterior = c.textoONulo("estado_anterior")?.let { EstadoProtocolo.desdeCodigo(it) },
                estadoNuevo = EstadoProtocolo.desdeCodigo(c.texto("estado_nuevo")),
                nombreUsuario = c.texto("nombre"),
                fechaHora = c.largo("fecha_hora"),
                motivo = c.texto("motivo")
            )
        }
    }

    private fun anteriorSync(
        db: SQLiteDatabase, idEquipo: Long, tipo: TipoPrueba, elemento: String, antesDe: Long, excluir: Long,
        sesion: Sesion
    ): PruebaAnterior? {
        val args = mutableListOf<Any?>(idEquipo, tipo.name, elemento, antesDe, excluir)
        val estados = if (sesion.esCliente) VisibilidadCliente.estadosVisibles
        else EstadoProtocolo.entries.filter { it != EstadoProtocolo.ANULADO && it != EstadoProtocolo.BORRADOR }
        data class Fila(val idPrueba: Long, val numero: String, val fecha: Long, val resultado: Resultado?)
        val fila = db.consultarUno(
            """SELECT pr.id_prueba, p.numero, p.fecha_hora_prueba, pr.resultado
               FROM prueba pr JOIN protocolo p ON p.id_protocolo = pr.id_protocolo
               WHERE p.id_equipo = ? AND pr.tipo_prueba = ? AND pr.elemento = ? AND pr.realizada = 1
                 AND p.fecha_hora_prueba < ? AND p.id_protocolo <> ?
                 AND p.estado IN (${estados.joinToString(",") { "'${it.name}'" }})
               ORDER BY p.fecha_hora_prueba DESC, CASE pr.momento WHEN 'POSTERIOR' THEN 0 ELSE 1 END
               LIMIT 1""",
            args
        ) { Fila(it.largo("id_prueba"), it.texto("numero"), it.largo("fecha_hora_prueba"), Resultado.desdeCodigo(it.textoONulo("resultado"))) }
            ?: return null
        val vals = db.consultar("SELECT parametro, valor FROM medicion WHERE id_prueba = ?", listOf(fila.idPrueba)) { c ->
            Parametro.entries.firstOrNull { it.name == c.texto("parametro") }?.let { it to c.getDouble(c.getColumnIndexOrThrow("valor")) }
        }.filterNotNull().toMap()
        return PruebaAnterior(fila.numero, fila.fecha, vals, fila.resultado)
    }

    suspend fun anterior(
        idEquipo: Long, tipo: TipoPrueba, elemento: String, antesDe: Long, excluirProtocolo: Long, sesion: Sesion
    ): PruebaAnterior? = leer { anteriorSync(this, idEquipo, tipo, elemento, antesDe, excluirProtocolo, sesion) }

    suspend fun comparaciones(protocolo: Protocolo, sesion: Sesion): List<ComparacionPrueba> = leer {
        EvaluadorPruebas.pruebasEfectivas(pruebasSync(this, protocolo.id)).map { p ->
            val ant = anteriorSync(this, protocolo.idEquipo, p.tipo, p.elemento, protocolo.fechaHoraPrueba, protocolo.id, sesion)
            val comps = if (ant == null) emptyList() else ComparadorHistorico.comparar(
                p.tipo.parametrosPrincipales, p.valores, ant.valores, ant.numeroProtocolo
            )
            ComparacionPrueba(p, ant, comps)
        }
    }

    suspend fun serieHistorica(
        idEquipo: Long, tipo: TipoPrueba, elemento: String, parametro: Parametro, sesion: Sesion
    ): List<PuntoSerie> = leer {
        val args = mutableListOf<Any?>(idEquipo, tipo.name, elemento, parametro.name)
        val estados = if (sesion.esCliente) VisibilidadCliente.estadosVisibles
        else EstadoProtocolo.entries.filter { it != EstadoProtocolo.ANULADO }
        consultar(
            """SELECT p.numero, p.fecha_hora_prueba, m.valor, pr.resultado, pr.momento
               FROM medicion m JOIN prueba pr ON pr.id_prueba = m.id_prueba
               JOIN protocolo p ON p.id_protocolo = pr.id_protocolo
               WHERE p.id_equipo = ? AND pr.tipo_prueba = ? AND pr.elemento = ? AND m.parametro = ?
                 AND pr.realizada = 1 AND p.estado IN (${estados.joinToString(",") { "'${it.name}'" }})
               ORDER BY p.fecha_hora_prueba ASC, CASE pr.momento WHEN 'POSTERIOR' THEN 1 ELSE 0 END ASC""",
            args
        ) { c ->
            PuntoSerie(
                numeroProtocolo = c.texto("numero"),
                fechaHora = c.largo("fecha_hora_prueba"),
                valor = c.getDouble(c.getColumnIndexOrThrow("valor")),
                resultado = Resultado.desdeCodigo(c.textoONulo("resultado"))
            )
        }.groupBy { it.numeroProtocolo }.map { (_, puntos) -> puntos.last() }
            .sortedBy { it.fechaHora }
    }

    suspend fun seriesDisponibles(idEquipo: Long, sesion: Sesion): List<Pair<TipoPrueba, String>> = leer {
        val estados = if (sesion.esCliente) VisibilidadCliente.estadosVisibles
        else EstadoProtocolo.entries.filter { it != EstadoProtocolo.ANULADO }
        consultar(
            """SELECT DISTINCT pr.tipo_prueba, pr.elemento FROM prueba pr
               JOIN protocolo p ON p.id_protocolo = pr.id_protocolo
               WHERE p.id_equipo = ? AND pr.realizada = 1
                 AND p.estado IN (${estados.joinToString(",") { "'${it.name}'" }})
               ORDER BY pr.tipo_prueba, pr.elemento""",
            listOf(idEquipo)
        ) { c -> TipoPrueba.entries.first { it.name == c.texto("tipo_prueba") } to c.texto("elemento") }
    }

    suspend fun acta(
        idVisita: Long, sesion: Sesion, visitas: VisitaRepositorio, clientes: ClienteRepositorio
    ): ActaTerreno? {
        val visita = visitas.obtener(idVisita, sesion) ?: return null
        val instalacion = clientes.obtenerInstalacion(visita.idInstalacion, sesion) ?: return null
        val cliente = clientes.obtenerCliente(instalacion.idCliente, sesion) ?: return null
        val recepcion = visitas.recepcion(idVisita)
        val lista = deVisita(idVisita, sesion)
            .filter { it.estado != EstadoProtocolo.ANULADO }
            .mapNotNull { r -> obtener(r.id, sesion)?.let { it to pruebas(r.id) } }
        return ActaTerreno(visita, instalacion, cliente, lista, recepcion)
    }

    suspend fun indicadores(sesion: Sesion): Indicadores {
        val hoy = Formato.hoy()
        val todos = listar(sesion)
        if (sesion.esCliente) {
            return leer {
                val args = mutableListOf<Any?>()
                val equipos = contar(
                    "SELECT COUNT(*) FROM equipo e JOIN instalacion i ON i.id_instalacion = e.id_instalacion WHERE 1 = 1 " +
                        filtroEmpresa(sesion, "i.id_cliente", args), args
                )
                val args2 = mutableListOf<Any?>()
                val actas = contar(
                    "SELECT COUNT(*) FROM visita v JOIN instalacion i ON i.id_instalacion = v.id_instalacion " +
                        "WHERE v.fecha_hora_cierre IS NOT NULL " + filtroEmpresa(sesion, "i.id_cliente", args2), args2
                )
                Indicadores(
                    pendientesFormalizar = todos.count { it.estado != EstadoProtocolo.FORMALIZADO },
                    equiposCliente = equipos,
                    actasCliente = actas
                )
            }
        }
        val listaInstrumentos = instrumentos.listar()
        val promedio = leer {
            val dias = consultar(
                """SELECT p.fecha_hora_prueba, h.fecha_hora FROM protocolo p
                   JOIN historial_estado h ON h.id_protocolo = p.id_protocolo AND h.estado_nuevo = 'FORMALIZADO'"""
            ) { c ->
                Plazos.diasHabilesEntre(Formato.aFecha(c.getLong(0)), Formato.aFecha(c.getLong(1)))
            }
            if (dias.isEmpty()) null else dias.average()
        }
        val visitasAbiertas = leer {
            if (sesion.rol == RolCodigo.TECNICO) contar(
                """SELECT COUNT(*) FROM visita v WHERE v.fecha_hora_cierre IS NULL AND EXISTS
                   (SELECT 1 FROM visita_participante vp WHERE vp.id_visita = v.id_visita AND vp.id_usuario = ?)""",
                listOf(sesion.idUsuario)
            ) else contar("SELECT COUNT(*) FROM visita WHERE fecha_hora_cierre IS NULL")
        }
        return Indicadores(
            pendientesFormalizar = todos.count { it.estado == EstadoProtocolo.REGISTRADO_TERRENO },
            atrasados = todos.count { Plazos.estaAtrasado(it.estado, Formato.aFecha(it.fechaHoraPrueba), hoy) },
            borradoresPropios = todos.count { it.idTecnico == sesion.idUsuario && it.estado == EstadoProtocolo.BORRADOR },
            observadosPropios = todos.count { it.idTecnico == sesion.idUsuario && it.estado == EstadoProtocolo.OBSERVADO },
            visitasAbiertas = visitasAbiertas,
            instrumentosPorVencer = listaInstrumentos.count {
                ReglasInstrumento.vigencia(it.vencimiento, hoy) == VigenciaInstrumento.POR_VENCER
            },
            instrumentosVencidos = listaInstrumentos.count {
                ReglasInstrumento.vigencia(it.vencimiento, hoy) == VigenciaInstrumento.VENCIDO
            },
            promedioDiasFormalizacion = promedio
        )
    }
}
