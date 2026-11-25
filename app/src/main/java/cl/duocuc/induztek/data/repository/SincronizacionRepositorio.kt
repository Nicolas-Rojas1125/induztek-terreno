package cl.duocuc.induztek.data.repository

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import cl.duocuc.induztek.data.local.EspejoLocal
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.SesionPreferencias
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
import cl.duocuc.induztek.data.remote.ActivosRemoto
import cl.duocuc.induztek.data.remote.DatosMaestros
import cl.duocuc.induztek.data.remote.HistorialDto
import cl.duocuc.induztek.data.remote.MedicionDto
import cl.duocuc.induztek.data.remote.ProtocoloDto
import cl.duocuc.induztek.data.remote.ProtocolosRemoto
import cl.duocuc.induztek.data.remote.PruebaDto
import cl.duocuc.induztek.data.remote.RecepcionDto
import cl.duocuc.induztek.data.remote.UsuariosRemoto
import cl.duocuc.induztek.data.remote.VisitaDto
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.EstadoSincronizacion
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.ResumenSincronizacion
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SincronizacionRepositorio(
    helper: InduztekDbHelper,
    cambios: NotificadorCambios,
    private val prefs: SesionPreferencias,
    private val sesion: StateFlow<Sesion?>,
    private val activos: ActivosRemoto,
    private val usuarios: UsuariosRemoto,
    private val protocolos: ProtocolosRemoto
) : RepositorioBase(helper, cambios), Sincronizador {
    private val candado = Mutex()
    private val _estado = MutableStateFlow(EstadoSincronizacion(ultimaSincronizacion = prefs.ultimaSincronizacion()))
    override val estado: StateFlow<EstadoSincronizacion> = _estado.asStateFlow()

    suspend fun contarPendientes(): Int = leer {
        contar("SELECT COUNT(*) FROM visita WHERE sincronizado = 0") +
            contar("SELECT COUNT(*) FROM protocolo WHERE sincronizado = 0")
    }

    suspend fun refrescarPendientes() {
        val n = contarPendientes()
        _estado.update { it.copy(pendientes = n) }
    }

    override suspend fun sincronizar(): ResumenSincronizacion = candado.withLock {
        val s = sesion.value ?: return ResumenSincronizacion(errores = listOf("Inicie sesión para sincronizar"))
        _estado.update { it.copy(sincronizando = true) }
        val errores = mutableListOf<String>()
        var recibidos = 0
        var enviados = 0
        try {
            when (val r = activos.exportar(if (s.esCliente) s.idCliente else null)) {
                is Operacion.Fallo -> errores += r.mensaje
                is Operacion.Exito -> recibidos += aplicarDatosMaestros(r.valor, s, errores)
            }
            if (errores.isEmpty()) {
                when (val r = usuarios.directorio()) {
                    is Operacion.Fallo -> errores += r.mensaje
                    is Operacion.Exito -> aplicarUsuarios(r.valor, s)
                }
            }
            if (errores.isEmpty() && !s.esCliente) {
                enviados = enviarPendientes(errores)
            }
            if (errores.none { it == cl.duocuc.induztek.data.remote.LlamadaSegura.SIN_CONEXION }) {
                when (val r = protocolos.cambios(prefs.ultimaDescarga(), if (s.esCliente) s.idCliente else null)) {
                    is Operacion.Fallo -> errores += r.mensaje
                    is Operacion.Exito -> {
                        recibidos += aplicarCambios(r.valor.visitas.orEmpty(), r.valor.protocolos.orEmpty(), errores)
                        prefs.guardarUltimaDescarga(r.valor.servidorMillis)
                    }
                }
            }
        } finally {
            val ahora = System.currentTimeMillis()
            val resumen = ResumenSincronizacion(enviados, recibidos, errores.distinct(), ahora)
            if (errores.isEmpty()) prefs.guardarUltimaSincronizacion(ahora)
            if (recibidos > 0 || enviados > 0) cambios.notificar()
            val pendientes = contarPendientes()
            _estado.update {
                it.copy(
                    sincronizando = false,
                    pendientes = pendientes,
                    ultimaSincronizacion = if (errores.isEmpty()) ahora else it.ultimaSincronizacion,
                    ultimoResultado = resumen
                )
            }
        }
        _estado.value.ultimoResultado ?: ResumenSincronizacion()
    }

    private suspend fun aplicarDatosMaestros(d: DatosMaestros, s: Sesion, errores: MutableList<String>): Int =
        withContext(Dispatchers.IO) {
            val db = helper.writableDatabase
            var n = 0
            fun intentar(descripcion: String, accion: () -> Unit) {
                try {
                    accion()
                    n++
                } catch (e: SQLiteException) {
                    errores += "No se pudo actualizar $descripcion en el teléfono"
                }
            }
            db.enTransaccion {
                d.clientes.forEach { c -> intentar("el cliente ${c.razonSocial}") { EspejoLocal.cliente(this, c) } }
                d.instalaciones.forEach { i -> intentar("la instalación ${i.nombre}") { EspejoLocal.instalacion(this, i) } }
                d.equipos.forEach { e -> intentar("el equipo ${e.tag}") { EspejoLocal.equipo(this, e.copy(fotoPlaca = null)) } }
                d.instrumentos.forEach { i -> intentar("el instrumento ${i.nombre}") { EspejoLocal.instrumento(this, i) } }
                if (!s.esCliente) {
                    EspejoLocal.eliminarSiNoSeUsa(this, "equipo", "id_equipo", d.equipos.map { it.id }.toSet())
                    EspejoLocal.eliminarSiNoSeUsa(this, "instalacion", "id_instalacion", d.instalaciones.map { it.id }.toSet())
                    EspejoLocal.eliminarSiNoSeUsa(this, "cliente", "id_cliente", d.clientes.map { it.id }.toSet())
                    val vigentes = d.instrumentos.map { it.id }.toSet()
                    consultar("SELECT id_instrumento AS id FROM instrumento") { it.largo("id") }
                        .filter { it !in vigentes && contar("SELECT COUNT(*) FROM prueba WHERE id_instrumento = ?", listOf(it)) == 0 }
                        .forEach { id ->
                            delete("calibracion", "id_instrumento = ?", arrayOf(id.toString()))
                            delete("instrumento", "id_instrumento = ?", arrayOf(id.toString()))
                        }
                }
            }
            n
        }

    private suspend fun aplicarUsuarios(lista: List<Usuario>, s: Sesion) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        val clientesLocales = db.consultar("SELECT id_cliente AS id FROM cliente") { it.largo("id") }.toSet()
        db.enTransaccion {
            lista.filter { u ->
                when {
                    u.rol != RolCodigo.CLIENTE -> true
                    s.esCliente -> u.idCliente == s.idCliente
                    else -> u.idCliente in clientesLocales
                }
            }.forEach { u ->
                try {
                    EspejoLocal.usuario(this, u)
                } catch (e: SQLiteException) {
                    Unit
                }
            }
        }
    }

    private suspend fun enviarPendientes(errores: MutableList<String>): Int {
        var enviados = 0
        val visitas = leer { visitasPendientes(this) }
        for ((idLocal, dto) in visitas) {
            when (val r = protocolos.enviarVisita(dto)) {
                is Operacion.Fallo -> { errores += "Visita ${dto.numeroOt}: ${r.mensaje}"; if (r.mensaje == cl.duocuc.induztek.data.remote.LlamadaSegura.SIN_CONEXION) return enviados }
                is Operacion.Exito -> { marcar("visita", "id_visita", idLocal); enviados++ }
            }
        }
        val pendientes = leer { protocolosPendientes(this) }
        for ((idLocal, dto) in pendientes) {
            when (val r = protocolos.enviarProtocolo(dto)) {
                is Operacion.Fallo -> { errores += "Protocolo ${dto.numero}: ${r.mensaje}"; if (r.mensaje == cl.duocuc.induztek.data.remote.LlamadaSegura.SIN_CONEXION) return enviados }
                is Operacion.Exito -> { marcar("protocolo", "id_protocolo", idLocal); enviados++ }
            }
        }
        return enviados
    }

    private suspend fun marcar(tabla: String, columna: String, id: Long) = withContext(Dispatchers.IO) {
        helper.writableDatabase.update(tabla, valores("sincronizado" to 1), "$columna = ?", arrayOf(id.toString()))
    }

    private fun visitasPendientes(db: SQLiteDatabase): List<Pair<Long, VisitaDto>> =
        db.consultar(
            """SELECT v.*, i.id_cliente FROM visita v JOIN instalacion i ON i.id_instalacion = v.id_instalacion
               WHERE v.sincronizado = 0 ORDER BY v.id_visita"""
        ) { c -> c.largo("id_visita") to visitaDesdeCursor(c) }.map { (id, v) ->
            val participantes = db.consultar("SELECT id_usuario FROM visita_participante WHERE id_visita = ?", listOf(id)) { it.largo("id_usuario") }
            val recepcion = db.consultarUno("SELECT * FROM recepcion_acta WHERE id_visita = ?", listOf(id)) {
                RecepcionDto(it.texto("receptor_nombre"), it.texto("receptor_rut"), it.texto("receptor_cargo"), it.largo("fecha_hora_recepcion"))
            }
            id to v.copy(participantes = participantes, recepcion = recepcion, origen = prefs.codigoDispositivo())
        }

    private fun visitaDesdeCursor(c: Cursor) = VisitaDto(
        uuid = c.texto("uuid"),
        numeroOt = c.texto("numero_ot"),
        idInstalacion = c.largo("id_instalacion"),
        idCliente = c.largo("id_cliente"),
        fecha = c.texto("fecha"),
        tipoVisita = c.texto("tipo_visita"),
        idIngenieroCargo = c.largo("id_ingeniero_cargo"),
        fechaHoraCierre = c.largoONulo("fecha_hora_cierre"),
        latitudCierre = c.decimalONulo("latitud_cierre"),
        longitudCierre = c.decimalONulo("longitud_cierre"),
        idUsuarioCierra = c.largoONulo("id_usuario_cierra"),
        participantes = emptyList(),
        recepcion = null,
        origen = null,
        actualizadoEn = null
    )

    private fun protocolosPendientes(db: SQLiteDatabase): List<Pair<Long, ProtocoloDto>> =
        db.consultar(
            """SELECT p.*, v.uuid AS visita_uuid, v.id_instalacion, i.id_cliente, r.uuid AS reemplaza_uuid
               FROM protocolo p
               JOIN visita v ON v.id_visita = p.id_visita
               JOIN instalacion i ON i.id_instalacion = v.id_instalacion
               LEFT JOIN protocolo r ON r.id_protocolo = p.id_protocolo_reemplazado
               WHERE p.sincronizado = 0
               ORDER BY p.id_protocolo"""
        ) { c ->
            c.largo("id_protocolo") to ProtocoloDto(
                uuid = c.texto("uuid"),
                numero = c.texto("numero"),
                visitaUuid = c.texto("visita_uuid"),
                idEquipo = c.largo("id_equipo"),
                idInstalacion = c.largo("id_instalacion"),
                idCliente = c.largo("id_cliente"),
                idUsuarioRegistra = c.largo("id_usuario_registra"),
                fechaHoraPrueba = c.largo("fecha_hora_prueba"),
                temperaturaC = c.decimalONulo("temperatura_c"),
                humedadPct = c.decimalONulo("humedad_pct"),
                descripcionMantenimiento = c.texto("descripcion_mantenimiento"),
                observaciones = c.texto("observaciones"),
                comentarioVariacion = c.texto("comentario_variacion"),
                estado = c.texto("estado"),
                resultadoGlobal = c.textoONulo("resultado_global"),
                reemplazaUuid = c.textoONulo("reemplaza_uuid"),
                pruebas = emptyList(),
                historial = emptyList(),
                actualizadoEn = null
            )
        }.map { (id, p) -> id to p.copy(pruebas = pruebasDe(db, id), historial = historialDe(db, id)) }

    private fun pruebasDe(db: SQLiteDatabase, idProtocolo: Long): List<PruebaDto> =
        db.consultar("SELECT * FROM prueba WHERE id_protocolo = ? ORDER BY id_prueba", listOf(idProtocolo)) { c ->
            c.largo("id_prueba") to PruebaDto(
                tipoPrueba = c.texto("tipo_prueba"),
                elemento = c.texto("elemento"),
                momento = c.texto("momento"),
                realizada = c.booleano("realizada"),
                motivoNoRealizada = c.texto("motivo_no_realizada"),
                idInstrumento = c.largoONulo("id_instrumento"),
                resultado = c.textoONulo("resultado"),
                noOpera = c.booleano("no_opera"),
                comentario = c.texto("comentario"),
                mediciones = emptyList()
            )
        }.map { (idPrueba, p) ->
            p.copy(mediciones = db.consultar("SELECT parametro, valor FROM medicion WHERE id_prueba = ?", listOf(idPrueba)) {
                MedicionDto(it.texto("parametro"), it.getDouble(it.getColumnIndexOrThrow("valor")))
            })
        }

    private fun historialDe(db: SQLiteDatabase, idProtocolo: Long): List<HistorialDto> =
        db.consultar("SELECT * FROM historial_estado WHERE id_protocolo = ? ORDER BY fecha_hora, id_historial", listOf(idProtocolo)) {
            HistorialDto(it.textoONulo("estado_anterior"), it.texto("estado_nuevo"), it.largo("id_usuario"), it.largo("fecha_hora"), it.texto("motivo"))
        }

    private suspend fun aplicarCambios(visitas: List<VisitaDto>, lista: List<ProtocoloDto>, errores: MutableList<String>): Int =
        withContext(Dispatchers.IO) {
            val db = helper.writableDatabase
            var n = 0
            visitas.forEach { v ->
                try {
                    if (db.enTransaccion { aplicarVisita(this, v) }) n++
                } catch (e: SQLiteException) {
                    errores += "No se pudo descargar la visita ${v.numeroOt}"
                }
            }
            lista.sortedBy { if (it.reemplazaUuid == null) 0 else 1 }.forEach { p ->
                try {
                    if (db.enTransaccion { aplicarProtocolo(this, p) }) n++
                } catch (e: SQLiteException) {
                    errores += "No se pudo descargar el protocolo ${p.numero}"
                }
            }
            n
        }

    private fun aplicarVisita(db: SQLiteDatabase, v: VisitaDto): Boolean {
        val local = db.consultarUno("SELECT id_visita, sincronizado FROM visita WHERE uuid = ?", listOf(v.uuid)) {
            it.largo("id_visita") to it.booleano("sincronizado")
        }
        if (local != null && !local.second) return false
        val cv = valores(
            "numero_ot" to v.numeroOt,
            "id_instalacion" to v.idInstalacion,
            "fecha" to v.fecha,
            "tipo_visita" to v.tipoVisita,
            "id_ingeniero_cargo" to v.idIngenieroCargo,
            "fecha_hora_cierre" to v.fechaHoraCierre,
            "latitud_cierre" to v.latitudCierre,
            "longitud_cierre" to v.longitudCierre,
            "id_usuario_cierra" to v.idUsuarioCierra
        )
        val id = if (local == null) {
            cv.put("uuid", v.uuid)
            db.insertOrThrow("visita", null, cv)
        } else {
            db.update("visita", cv, "id_visita = ?", arrayOf(local.first.toString()))
            local.first
        }
        val existentes = db.consultar("SELECT id_usuario FROM usuario") { it.largo("id_usuario") }.toSet()
        db.delete("visita_participante", "id_visita = ?", arrayOf(id.toString()))
        v.participantes.orEmpty().filter { it in existentes }.distinct().forEach { u ->
            db.insertOrThrow("visita_participante", null, valores("id_visita" to id, "id_usuario" to u))
        }
        val r = v.recepcion
        if (r?.fechaHoraRecepcion != null && db.contar("SELECT COUNT(*) FROM recepcion_acta WHERE id_visita = ?", listOf(id)) == 0) {
            db.insertOrThrow(
                "recepcion_acta", null, valores(
                    "id_visita" to id,
                    "receptor_nombre" to r.receptorNombre.orEmpty(),
                    "receptor_rut" to r.receptorRut.orEmpty(),
                    "receptor_cargo" to r.receptorCargo.orEmpty(),
                    "fecha_hora_recepcion" to r.fechaHoraRecepcion
                )
            )
        }
        db.update("visita", valores("sincronizado" to 1), "id_visita = ?", arrayOf(id.toString()))
        return true
    }

    private fun aplicarProtocolo(db: SQLiteDatabase, p: ProtocoloDto): Boolean {
        val idVisita = db.consultarUno("SELECT id_visita FROM visita WHERE uuid = ?", listOf(p.visitaUuid)) { it.largo("id_visita") }
            ?: return false
        val local = db.consultarUno("SELECT id_protocolo, estado, sincronizado FROM protocolo WHERE uuid = ?", listOf(p.uuid)) {
            Triple(it.largo("id_protocolo"), it.texto("estado"), it.booleano("sincronizado"))
        }
        if (local != null && (!local.third || local.second == EstadoProtocolo.FORMALIZADO.name)) return false
        val cabecera = valores(
            "numero" to p.numero,
            "id_visita" to idVisita,
            "id_equipo" to p.idEquipo,
            "id_usuario_registra" to p.idUsuarioRegistra,
            "fecha_hora_prueba" to p.fechaHoraPrueba,
            "temperatura_c" to p.temperaturaC,
            "humedad_pct" to p.humedadPct,
            "descripcion_mantenimiento" to p.descripcionMantenimiento.orEmpty(),
            "observaciones" to p.observaciones.orEmpty(),
            "comentario_variacion" to p.comentarioVariacion.orEmpty()
        )
        val id = if (local == null) {
            cabecera.put("uuid", p.uuid)
            cabecera.put("estado", EstadoProtocolo.BORRADOR.name)
            db.insertOrThrow("protocolo", null, cabecera)
        } else {
            db.update("protocolo", cabecera, "id_protocolo = ?", arrayOf(local.first.toString()))
            local.first
        }
        val instrumentos = db.consultar("SELECT id_instrumento FROM instrumento") { it.largo("id_instrumento") }.toSet()
        db.delete("prueba", "id_protocolo = ?", arrayOf(id.toString()))
        p.pruebas.orEmpty().forEach { pr ->
            val idPrueba = db.insertOrThrow(
                "prueba", null, valores(
                    "id_protocolo" to id,
                    "tipo_prueba" to pr.tipoPrueba,
                    "elemento" to pr.elemento,
                    "momento" to (pr.momento ?: "UNICA"),
                    "realizada" to pr.realizada,
                    "motivo_no_realizada" to pr.motivoNoRealizada.orEmpty(),
                    "id_instrumento" to pr.idInstrumento?.takeIf { it in instrumentos },
                    "resultado" to pr.resultado,
                    "no_opera" to pr.noOpera,
                    "comentario" to pr.comentario.orEmpty()
                )
            )
            pr.mediciones.orEmpty().forEach { m ->
                db.insertOrThrow("medicion", null, valores("id_prueba" to idPrueba, "parametro" to m.parametro, "valor" to m.valor))
            }
        }
        val usuariosLocales = db.consultar("SELECT id_usuario FROM usuario") { it.largo("id_usuario") }.toSet()
        db.delete("historial_estado", "id_protocolo = ?", arrayOf(id.toString()))
        p.historial.orEmpty().filter { it.idUsuario in usuariosLocales }.forEach { h ->
            db.insertOrThrow(
                "historial_estado", null, valores(
                    "id_protocolo" to id,
                    "estado_anterior" to h.estadoAnterior,
                    "estado_nuevo" to h.estadoNuevo,
                    "id_usuario" to h.idUsuario,
                    "fecha_hora" to h.fechaHora,
                    "motivo" to h.motivo.orEmpty()
                )
            )
        }
        val reemplazado = p.reemplazaUuid?.let { u ->
            db.consultarUno("SELECT id_protocolo FROM protocolo WHERE uuid = ?", listOf(u)) { it.largo("id_protocolo") }
        }
        db.update(
            "protocolo",
            valores("estado" to p.estado, "resultado_global" to p.resultadoGlobal, "id_protocolo_reemplazado" to reemplazado),
            "id_protocolo = ?", arrayOf(id.toString())
        )
        db.update("protocolo", valores("sincronizado" to 1), "id_protocolo = ?", arrayOf(id.toString()))
        return true
    }
}
