package cl.duocuc.induztek.data.repository

import android.database.Cursor
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.booleano
import cl.duocuc.induztek.data.local.consultar
import cl.duocuc.induztek.data.local.consultarUno
import cl.duocuc.induztek.data.local.contar
import cl.duocuc.induztek.data.local.decimalONulo
import cl.duocuc.induztek.data.local.enTransaccion
import cl.duocuc.induztek.data.local.entero
import cl.duocuc.induztek.data.local.largo
import cl.duocuc.induztek.data.local.largoONulo
import cl.duocuc.induztek.data.local.texto
import cl.duocuc.induztek.data.local.valores
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.RecepcionActa
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.TipoVisita
import cl.duocuc.induztek.domain.model.Usuario
import cl.duocuc.induztek.domain.model.Visita
import cl.duocuc.induztek.domain.validacion.ValidadorRut
import java.time.LocalDate

class VisitaRepositorio(helper: InduztekDbHelper, cambios: NotificadorCambios) :
    RepositorioBase(helper, cambios) {
    private val sqlBase = """
        SELECT v.*, i.nombre AS instalacion, i.id_cliente, c.razon_social, u.nombre AS ingeniero,
               (SELECT COUNT(*) FROM protocolo p WHERE p.id_visita = v.id_visita AND p.estado <> 'ANULADO') AS n_protocolos
        FROM visita v
        JOIN instalacion i ON i.id_instalacion = v.id_instalacion
        JOIN cliente c ON c.id_cliente = i.id_cliente
        JOIN usuario u ON u.id_usuario = v.id_ingeniero_cargo
        WHERE 1 = 1 """

    private fun leerVisita(c: Cursor) = Visita(
        id = c.largo("id_visita"),
        numeroOt = c.texto("numero_ot"),
        idInstalacion = c.largo("id_instalacion"),
        fecha = LocalDate.parse(c.texto("fecha")),
        tipo = TipoVisita.desdeCodigo(c.texto("tipo_visita")),
        idIngenieroCargo = c.largo("id_ingeniero_cargo"),
        fechaHoraCierre = c.largoONulo("fecha_hora_cierre"),
        latitudCierre = c.decimalONulo("latitud_cierre"),
        longitudCierre = c.decimalONulo("longitud_cierre"),
        nombreInstalacion = c.texto("instalacion"),
        idCliente = c.largo("id_cliente"),
        nombreCliente = c.texto("razon_social"),
        nombreIngeniero = c.texto("ingeniero"),
        cantidadProtocolos = c.entero("n_protocolos")
    )

    suspend fun listar(sesion: Sesion, soloAbiertas: Boolean = false, soloMias: Boolean = false): List<Visita> = leer {
        val args = mutableListOf<Any?>()
        var sql = sqlBase + filtroEmpresa(sesion, "i.id_cliente", args)
        if (sesion.esCliente) sql += " AND v.fecha_hora_cierre IS NOT NULL "
        if (soloAbiertas) sql += " AND v.fecha_hora_cierre IS NULL "
        if (soloMias) {
            sql += " AND (v.id_ingeniero_cargo = ? OR EXISTS (SELECT 1 FROM visita_participante vp WHERE vp.id_visita = v.id_visita AND vp.id_usuario = ?)) "
            args += sesion.idUsuario; args += sesion.idUsuario
        }
        consultar("$sql ORDER BY v.fecha DESC, v.numero_ot DESC", args) { leerVisita(it) }
    }

    suspend fun obtener(id: Long, sesion: Sesion): Visita? = leer {
        val args = mutableListOf<Any?>(id)
        var sql = sqlBase + " AND v.id_visita = ? " + filtroEmpresa(sesion, "i.id_cliente", args)
        if (sesion.esCliente) sql += " AND v.fecha_hora_cierre IS NOT NULL "
        val visita = consultarUno(sql, args) { leerVisita(it) } ?: return@leer null
        val participantes = consultar(
            """SELECT u.id_usuario, u.nombre, u.rut, u.correo, u.activo, r.codigo AS rol
               FROM visita_participante vp JOIN usuario u ON u.id_usuario = vp.id_usuario
               JOIN rol r ON r.id_rol = u.id_rol WHERE vp.id_visita = ? ORDER BY u.nombre""",
            listOf(id)
        ) {
            Usuario(
                id = it.largo("id_usuario"), rol = RolCodigo.desdeCodigo(it.texto("rol")),
                rut = it.texto("rut"), nombre = it.texto("nombre"), correo = it.texto("correo"),
                activo = it.booleano("activo")
            )
        }
        visita.copy(participantes = participantes)
    }

    suspend fun existeOt(numeroOt: String): Boolean = leer {
        contar("SELECT COUNT(*) FROM visita WHERE numero_ot = ?", listOf(numeroOt.trim().uppercase())) > 0
    }

    suspend fun crear(v: Visita, participantes: List<Long>): Operacion<Long> =
        escribir("Ya existe una visita con ese número de OT") {
            enTransaccion {
                val id = insertOrThrow(
                    "visita", null, valores(
                        "numero_ot" to v.numeroOt.trim().uppercase(),
                        "id_instalacion" to v.idInstalacion,
                        "fecha" to v.fecha.toString(),
                        "tipo_visita" to v.tipo.name,
                        "id_ingeniero_cargo" to v.idIngenieroCargo
                    )
                )
                participantes.distinct().forEach { idU ->
                    insertOrThrow("visita_participante", null, valores("id_visita" to id, "id_usuario" to idU))
                }
                Operacion.Exito(id)
            }
        }

    suspend fun cerrar(idVisita: Long, idUsuario: Long, lat: Double?, lon: Double?): Operacion<Unit> = escribir {
        data class Pendiente(val numero: String, val tag: String, val estado: EstadoProtocolo)

        val pendientes = consultar(
            """SELECT p.numero, e.tag, p.estado FROM protocolo p JOIN equipo e ON e.id_equipo = p.id_equipo
               WHERE p.id_visita = ? AND p.estado NOT IN ('REGISTRADO_TERRENO', 'ANULADO', 'FORMALIZADO')""",
            listOf(idVisita)
        ) { Pendiente(it.texto("numero"), it.texto("tag"), EstadoProtocolo.desdeCodigo(it.texto("estado"))) }

        val total = contar(
            "SELECT COUNT(*) FROM protocolo WHERE id_visita = ? AND estado <> 'ANULADO'", listOf(idVisita)
        )
        when {
            total == 0 -> Operacion.Fallo("La visita no tiene protocolos: registre al menos un equipo")
            pendientes.isNotEmpty() -> Operacion.Fallo(
                "No se puede cerrar: hay protocolos sin registrar (RN-13)",
                pendientes.map { "${it.numero} · ${it.tag} · ${it.estado.etiqueta}" }
            )
            else -> {
                update(
                    "visita", valores(
                        "fecha_hora_cierre" to System.currentTimeMillis(),
                        "latitud_cierre" to lat,
                        "longitud_cierre" to lon,
                        "id_usuario_cierra" to idUsuario
                    ),
                    "id_visita = ? AND fecha_hora_cierre IS NULL", arrayOf(idVisita.toString())
                )
                Operacion.Exito(Unit)
            }
        }
    }

    suspend fun recepcion(idVisita: Long): RecepcionActa? = leer {
        consultarUno("SELECT * FROM recepcion_acta WHERE id_visita = ?", listOf(idVisita)) {
            RecepcionActa(
                idVisita = it.largo("id_visita"),
                receptorNombre = it.texto("receptor_nombre"),
                receptorRut = it.texto("receptor_rut"),
                receptorCargo = it.texto("receptor_cargo"),
                fechaHoraRecepcion = it.largo("fecha_hora_recepcion")
            )
        }
    }

    suspend fun registrarRecepcion(r: RecepcionActa): Operacion<Unit> =
        escribir("La recepción de esta acta ya fue registrada") {
            val cerrada = contar(
                "SELECT COUNT(*) FROM visita WHERE id_visita = ? AND fecha_hora_cierre IS NOT NULL",
                listOf(r.idVisita)
            ) > 0
            if (!cerrada) return@escribir Operacion.Fallo("Primero cierre la visita para generar el acta")
            insertOrThrow(
                "recepcion_acta", null, valores(
                    "id_visita" to r.idVisita,
                    "receptor_nombre" to r.receptorNombre.trim(),
                    "receptor_rut" to ValidadorRut.formatear(r.receptorRut),
                    "receptor_cargo" to r.receptorCargo.trim(),
                    "fecha_hora_recepcion" to r.fechaHoraRecepcion
                )
            )
            Operacion.Exito(Unit)
        }
}
