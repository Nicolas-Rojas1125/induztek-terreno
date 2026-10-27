package cl.duocuc.induztek.data.repository

import android.database.Cursor
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.consultar
import cl.duocuc.induztek.data.local.consultarUno
import cl.duocuc.induztek.data.local.decimalONulo
import cl.duocuc.induztek.data.local.enteroONulo
import cl.duocuc.induztek.data.local.largo
import cl.duocuc.induztek.data.local.largoONulo
import cl.duocuc.induztek.data.local.texto
import cl.duocuc.induztek.data.local.textoONulo
import cl.duocuc.induztek.data.local.valores
import cl.duocuc.induztek.domain.model.Clasificacion
import cl.duocuc.induztek.domain.model.Equipo
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.reglas.VisibilidadCliente

class EquipoRepositorio(helper: InduztekDbHelper, cambios: NotificadorCambios) :
    RepositorioBase(helper, cambios) {
    private fun sqlUltimaPrueba(sesion: Sesion): String {
        val estados = if (sesion.esCliente) VisibilidadCliente.estadosVisibles else
            EstadoProtocolo.entries.filter { it != EstadoProtocolo.ANULADO }
        val lista = estados.joinToString(",") { "'${it.name}'" }
        return "(SELECT MAX(p.fecha_hora_prueba) FROM protocolo p WHERE p.id_equipo = e.id_equipo AND p.estado IN ($lista))"
    }

    private fun sqlBase(sesion: Sesion) = """
        SELECT e.*, i.nombre AS instalacion, i.id_cliente, c.razon_social,
               ${sqlUltimaPrueba(sesion)} AS ultima_prueba
        FROM equipo e
        JOIN instalacion i ON i.id_instalacion = e.id_instalacion
        JOIN cliente c ON c.id_cliente = i.id_cliente
        WHERE 1 = 1 """

    private fun leerEquipo(c: Cursor) = Equipo(
        id = c.largo("id_equipo"),
        idInstalacion = c.largo("id_instalacion"),
        tag = c.texto("tag"),
        tipo = TipoEquipo.desdeCodigo(c.texto("tipo_equipo")),
        marca = c.texto("marca"),
        modelo = c.texto("modelo"),
        numeroSerie = c.texto("numero_serie"),
        anioFabricacion = c.enteroONulo("anio_fabricacion"),
        potenciaKva = c.decimalONulo("potencia_kva"),
        tensionPrimariaKv = c.decimalONulo("tension_primaria_kv"),
        tensionSecundariaKv = c.decimalONulo("tension_secundaria_kv"),
        corrienteNominalA = c.decimalONulo("corriente_nominal_a"),
        maxContactosUohm = c.decimalONulo("max_contactos_uohm"),
        observaciones = c.texto("observaciones"),
        fotoPlaca = c.textoONulo("foto_placa"),
        nombreInstalacion = c.texto("instalacion"),
        idCliente = c.largo("id_cliente"),
        nombreCliente = c.texto("razon_social"),
        ultimaPrueba = c.largoONulo("ultima_prueba")
    )

    suspend fun buscar(
        sesion: Sesion,
        texto: String = "",
        idInstalacion: Long? = null,
        clasificacion: Clasificacion? = null
    ): List<Equipo> = leer {
        val args = mutableListOf<Any?>()
        var sql = sqlBase(sesion) + filtroEmpresa(sesion, "i.id_cliente", args)
        if (texto.isNotBlank()) {
            sql += " AND (e.tag LIKE ? OR e.numero_serie LIKE ? OR i.nombre LIKE ? OR c.razon_social LIKE ?) "
            repeat(4) { args += "%${texto.trim()}%" }
        }
        if (idInstalacion != null) { sql += " AND e.id_instalacion = ? "; args += idInstalacion }
        if (clasificacion != null) {
            val tipos = TipoEquipo.entries.filter { it.clasificacion == clasificacion }
            sql += " AND e.tipo_equipo IN (${tipos.joinToString(",") { "'${it.name}'" }}) "
        }
        consultar("$sql ORDER BY c.razon_social, i.nombre, e.tag", args) { leerEquipo(it) }
    }

    suspend fun obtener(id: Long, sesion: Sesion): Equipo? = leer {
        val args = mutableListOf<Any?>(id)
        consultarUno(
            sqlBase(sesion) + " AND e.id_equipo = ? " + filtroEmpresa(sesion, "i.id_cliente", args), args
        ) { leerEquipo(it) }
    }

    suspend fun buscarPorTag(idInstalacion: Long, tag: String): Equipo? = leer {
        consultarUno(
            """SELECT e.*, i.nombre AS instalacion, i.id_cliente, c.razon_social, NULL AS ultima_prueba
               FROM equipo e JOIN instalacion i ON i.id_instalacion = e.id_instalacion
               JOIN cliente c ON c.id_cliente = i.id_cliente
               WHERE e.id_instalacion = ? AND UPPER(e.tag) = UPPER(?)""",
            listOf(idInstalacion, tag.trim())
        ) { leerEquipo(it) }
    }

    suspend fun crear(e: Equipo, idUsuario: Long): Operacion<Long> =
        escribir("El TAG ya existe en esta instalación (RN-01)") {
            val id = insertOrThrow(
                "equipo", null, valores(
                    "id_instalacion" to e.idInstalacion,
                    "tag" to e.tag.trim().uppercase(),
                    "tipo_equipo" to e.tipo.name,
                    "marca" to e.marca.trim(),
                    "modelo" to e.modelo.trim(),
                    "numero_serie" to e.numeroSerie.trim().uppercase(),
                    "anio_fabricacion" to e.anioFabricacion,
                    "potencia_kva" to e.potenciaKva,
                    "tension_primaria_kv" to e.tensionPrimariaKv,
                    "tension_secundaria_kv" to e.tensionSecundariaKv,
                    "corriente_nominal_a" to e.corrienteNominalA,
                    "max_contactos_uohm" to e.maxContactosUohm,
                    "observaciones" to e.observaciones.trim(),
                    "foto_placa" to e.fotoPlaca,
                    "id_usuario_crea" to idUsuario
                )
            )
            Operacion.Exito(id)
        }

    suspend fun actualizarFoto(idEquipo: Long, ruta: String): Operacion<Unit> = escribir {
        update("equipo", valores("foto_placa" to ruta), "id_equipo = ?", arrayOf(idEquipo.toString()))
        Operacion.Exito(Unit)
    }
}
