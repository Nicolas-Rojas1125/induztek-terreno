package cl.duocuc.induztek.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import cl.duocuc.induztek.domain.model.CategoriaInstrumento
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.MatrizPermisos
import cl.duocuc.induztek.domain.model.Momento
import cl.duocuc.induztek.domain.model.Parametro
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.model.TipoVisita
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.domain.validacion.HashContrasena
import java.time.LocalDate
import java.util.UUID

object DatosSemilla {
    const val CONTRASENA_DEMO = "Induztek2026"

    private const val C_ENVASADORA = 1L
    private const val C_SANITARIA = 2L
    private const val C_MINERA = 3L
    private const val C_CLINICA = 4L
    private const val C_CEMENTOS = 5L
    private const val C_DATACENTER = 6L

    private const val I_LAMPA = 1L
    private const val I_ELEVADORA = 2L
    private const val I_TILTIL = 3L
    private const val I_CLINICO = 4L
    private const val I_MOLIENDA = 5L
    private const val I_HUECHURABA = 6L

    private const val U_MATIAS = 1L
    private const val U_JAVIERA = 2L
    private const val U_CARLOS = 3L
    private const val U_DIEGO = 4L
    private const val U_ANDREA = 5L
    private const val U_SEBASTIAN = 6L
    private const val U_RICARDO = 7L

    private const val INS_MEGGER5 = 1L
    private const val INS_MEGGER10 = 2L
    private const val INS_TTR = 3L
    private const val INS_MICRO = 4L
    private const val INS_CMC = 5L

    private val estadosFinales = mutableListOf<Pair<Long, EstadoProtocolo>>()

    fun cargar(db: SQLiteDatabase) {
        estadosFinales.clear()
        cargarUbicacion(db)
        cargarSeguridad(db)
        cargarClientes(db)
        cargarUsuarios(db)
        cargarInstalacionesYEquipos(db)
        cargarInstrumentos(db)
        cargarVisitasYProtocolos(db)
    }

    private fun ins(db: SQLiteDatabase, tabla: String, vararg pares: Pair<String, Any?>): Long {
        val cv = ContentValues()
        pares.forEach { (col, v) ->
            when (v) {
                null -> cv.putNull(col)
                is String -> cv.put(col, v)
                is Int -> cv.put(col, v)
                is Long -> cv.put(col, v)
                is Double -> cv.put(col, v)
                is Boolean -> cv.put(col, if (v) 1 else 0)
                else -> cv.put(col, v.toString())
            }
        }
        return db.insertOrThrow(tabla, null, cv)
    }

    private fun millis(fecha: String, hora: Int, minuto: Int): Long =
        Formato.millisDe(LocalDate.parse(fecha), hora, minuto)

    private fun cargarUbicacion(db: SQLiteDatabase) {
        val regiones = listOf(
            13L to "Región Metropolitana de Santiago",
            5L to "Región de Valparaíso",
            6L to "Región del Libertador General Bernardo O'Higgins",
            2L to "Región de Antofagasta"
        )
        regiones.forEach { (id, nombre) -> ins(db, "region", "id_region" to id, "nombre" to nombre) }

        val comunas = mapOf(
            13L to listOf(
                "Lampa", "San Bernardo", "Til Til", "La Florida", "Puente Alto", "Huechuraba",
                "Santiago", "Quilicura", "Pudahuel", "Maipú", "Colina", "Buin", "Talagante",
                "Melipilla", "Las Condes", "Providencia", "Ñuñoa", "Renca", "Cerrillos"
            ),
            5L to listOf("Valparaíso", "Viña del Mar", "Quilpué", "San Antonio", "Los Andes", "Quillota"),
            6L to listOf("Rancagua", "Machalí", "San Fernando", "Rengo"),
            2L to listOf("Antofagasta", "Calama", "Mejillones")
        )
        var id = 1L
        comunas.forEach { (region, lista) ->
            lista.forEach { nombre ->
                ins(db, "comuna", "id_comuna" to id++, "nombre" to nombre, "id_region" to region)
            }
        }
    }

    private fun idComuna(db: SQLiteDatabase, nombre: String): Long =
        db.rawQuery("SELECT id_comuna FROM comuna WHERE nombre = ?", arrayOf(nombre)).use {
            it.moveToFirst(); it.getLong(0)
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

    private fun idRol(rol: RolCodigo): Long = (rol.ordinal + 1).toLong()

    private fun cargarClientes(db: SQLiteDatabase) {
        data class C(val id: Long, val razon: String, val rut: String, val giro: String, val contacto: String, val tel: String, val correo: String)
        listOf(
            C(C_ENVASADORA, "Envasadora del Pacífico S.A.", "76.482.915-8", "Agroindustria", "Marcela Fuentes Rojas", "+56 9 0000 0101", "mfuentes@envpacifico.ejemplo"),
            C(C_SANITARIA, "Sanitaria Valle Central S.A.", "96.731.540-0", "Servicios sanitarios", "Jorge Arancibia Leiva", "+56 9 0000 0102", "jarancibia@svcentral.ejemplo"),
            C(C_MINERA, "Minera Cerro Alto SpA", "77.105.362-9", "Minería", "Patricio Navarro Soto", "+56 9 0000 0103", "pnavarro@cerroalto.ejemplo"),
            C(C_CLINICA, "Clínica Los Aromos Ltda.", "76.903.218-5", "Salud", "Carolina Méndez Pizarro", "+56 9 0000 0104", "cmendez@losaromos.ejemplo"),
            C(C_CEMENTOS, "Cementos Lomas Blancas S.A.", "99.524.170-6", "Construcción", "Luis Bravo Castillo", "+56 9 0000 0105", "lbravo@lomasblancas.ejemplo"),
            C(C_DATACENTER, "Data Center Andes Norte SpA", "78.214.635-1", "Tecnología", "Felipe Órdenes Vera", "+56 9 0000 0106", "fordenes@andesnorte.ejemplo")
        ).forEach {
            ins(
                db, "cliente", "id_cliente" to it.id, "rut" to it.rut, "razon_social" to it.razon,
                "giro" to it.giro, "contacto_nombre" to it.contacto, "telefono" to it.tel, "correo" to it.correo
            )
        }
    }

    private fun cargarUsuarios(db: SQLiteDatabase) {
        data class U(val id: Long, val nombre: String, val rut: String, val rol: RolCodigo, val correo: String, val tel: String, val cliente: Long? = null, val cargo: String = "")
        listOf(
            U(U_MATIAS, "Matías Godoy Herrera", "17.284.519-3", RolCodigo.TECNICO, "mgodoy@induztek.ejemplo", "+56 9 0000 0201", cargo = "Técnico de terreno"),
            U(U_JAVIERA, "Javiera Tapia Muñoz", "19.412.087-7", RolCodigo.TECNICO, "jtapia@induztek.ejemplo", "+56 9 0000 0202", cargo = "Técnico de terreno"),
            U(U_CARLOS, "Carlos Rondón Pérez", "26.104.583-4", RolCodigo.TECNICO, "crondon@induztek.ejemplo", "+56 9 0000 0203", cargo = "Técnico de terreno"),
            U(U_DIEGO, "Diego Valenzuela Ríos", "20.137.648-3", RolCodigo.TECNICO, "dvalenzuela@induztek.ejemplo", "+56 9 0000 0204", cargo = "Técnico de terreno"),
            U(U_ANDREA, "Andrea Contreras Silva", "15.906.334-8", RolCodigo.INGENIERO, "acontreras@induztek.ejemplo", "+56 9 0000 0205", cargo = "Ingeniera de pruebas"),
            U(U_SEBASTIAN, "Sebastián Morales Gutiérrez", "16.553.270-8", RolCodigo.INGENIERO, "smorales@induztek.ejemplo", "+56 9 0000 0206", cargo = "Ingeniero de pruebas"),
            U(U_RICARDO, "Ricardo Espinoza Lagos", "13.871.905-7", RolCodigo.JEFE_TECNICO, "respinoza@induztek.ejemplo", "+56 9 0000 0207", cargo = "Jefe técnico"),
            U(8, "Marcela Fuentes Rojas", "14.226.791-8", RolCodigo.CLIENTE, "mfuentes@envpacifico.ejemplo", "+56 9 0000 0101", C_ENVASADORA, "Jefa de Mantenimiento"),
            U(9, "Tomás Leiva Araya", "18.390.452-3", RolCodigo.CLIENTE, "tleiva@losaromos.ejemplo", "+56 9 0000 0107", C_CLINICA, "Supervisor Eléctrico"),
            U(10, "Patricio Navarro Soto", "12.645.308-6", RolCodigo.CLIENTE, "pnavarro@cerroalto.ejemplo", "+56 9 0000 0103", C_MINERA, "Jefe de Mantenimiento Eléctrico")
        ).forEach {
            val sal = HashContrasena.nuevaSal()
            ins(
                db, "usuario", "id_usuario" to it.id, "id_rol" to idRol(it.rol), "id_cliente" to it.cliente,
                "rut" to it.rut, "nombre" to it.nombre, "correo" to it.correo, "telefono" to it.tel,
                "cargo" to it.cargo, "password_hash" to HashContrasena.hash(CONTRASENA_DEMO, sal),
                "sal" to sal, "activo" to true
            )
        }
    }

    private fun cargarInstalacionesYEquipos(db: SQLiteDatabase) {
        data class I(val id: Long, val cliente: Long, val nombre: String, val ubic: String, val comuna: String, val obs: String)
        listOf(
            I(I_LAMPA, C_ENVASADORA, "Planta Lampa", "Sector industrial", "Lampa", "Inducción obligatoria de 30 min"),
            I(I_ELEVADORA, C_SANITARIA, "Planta Elevadora Sur", "Sector rural poniente", "San Bernardo", "Sala eléctrica subterránea, sin señal"),
            I(I_TILTIL, C_MINERA, "Faena Til Til", "Acceso por Ruta 5 Norte", "Til Til", "Sin señal en toda la faena"),
            I(I_CLINICO, C_CLINICA, "Edificio Clínico", "Zona urbana", "La Florida", "Cortes solo domingos 06:00–10:00"),
            I(I_MOLIENDA, C_CEMENTOS, "Planta Molienda", "Sector precordillera", "Puente Alto", "Uso obligatorio de respirador"),
            I(I_HUECHURABA, C_DATACENTER, "Site Huechuraba", "Parque empresarial", "Huechuraba", "Acompañamiento permanente del cliente")
        ).forEach {
            ins(
                db, "instalacion", "id_instalacion" to it.id, "id_cliente" to it.cliente, "nombre" to it.nombre,
                "ubicacion" to it.ubic, "id_comuna" to idComuna(db, it.comuna), "observaciones_acceso" to it.obs
            )
        }

        data class E(
            val id: Long, val tag: String, val tipo: TipoEquipo, val inst: Long, val marca: String, val modelo: String,
            val serie: String, val anio: Int, val kva: Double? = null, val kv1: Double? = null, val kv2: Double? = null,
            val amp: Double? = null, val maxC: Double? = null, val obs: String = ""
        )
        listOf(
            E(1, "TR-01", TipoEquipo.TRANSFORMADOR, I_LAMPA, "Rhona", "", "RH-18733", 2012, 1500.0, 12.0, 0.4, obs = "Conexión Dyn11"),
            E(2, "TR-02", TipoEquipo.TRANSFORMADOR, I_LAMPA, "Rhona", "", "RH-21904", 2017, 1000.0, 12.0, 0.4),
            E(3, "TR-A", TipoEquipo.TRANSFORMADOR, I_ELEVADORA, "Tusan", "", "TS-5521", 2009, 2500.0, 23.0, 0.4),
            E(4, "CEL-MT-03", TipoEquipo.CELDA_MT, I_CLINICO, "Ormazabal", "", "OR-77812", 2018, kv1 = 12.0, amp = 630.0, maxC = 60.0),
            E(5, "INT-52-1", TipoEquipo.INTERRUPTOR, I_TILTIL, "Schneider", "", "SE-449021", 2015, kv1 = 23.0, amp = 1250.0, maxC = 45.0),
            E(6, "REL-50/51-A", TipoEquipo.RELE, I_MOLIENDA, "SEL", "SEL-751", "SEL-751-30219", 2020, amp = 5.0, obs = "Sobrecorriente, ajuste 5 A"),
            E(7, "CAB-MT-02", TipoEquipo.CABLE, I_HUECHURABA, "Nexans", "", "NX-2201-B", 2019, kv1 = 15.0, obs = "3×240 mm², 180 m"),
            E(8, "TR-SS-01", TipoEquipo.TRANSFORMADOR, I_TILTIL, "ABB", "", "ABB-1180442", 2011, 5000.0, 23.0, 6.6)
        ).forEach {
            ins(
                db, "equipo", "id_equipo" to it.id, "id_instalacion" to it.inst, "tag" to it.tag,
                "tipo_equipo" to it.tipo.name, "marca" to it.marca, "modelo" to it.modelo,
                "numero_serie" to it.serie, "anio_fabricacion" to it.anio, "potencia_kva" to it.kva,
                "tension_primaria_kv" to it.kv1, "tension_secundaria_kv" to it.kv2,
                "corriente_nominal_a" to it.amp, "max_contactos_uohm" to it.maxC, "observaciones" to it.obs
            )
        }
    }

    private fun cargarInstrumentos(db: SQLiteDatabase) {
        data class In(val id: Long, val nombre: String, val cat: CategoriaInstrumento, val marca: String, val modelo: String, val serie: String)
        listOf(
            In(INS_MEGGER5, "Megóhmetro 5 kV", CategoriaInstrumento.MEGOHMETRO, "Megger", "MIT525", "MIT-0045821"),
            In(INS_MEGGER10, "Megóhmetro 10 kV", CategoriaInstrumento.MEGOHMETRO, "Megger", "MIT1025", "MIT-0071233"),
            In(INS_TTR, "Medidor de relación (TTR)", CategoriaInstrumento.TTR, "Megger", "TTR300", "TTR-30044"),
            In(INS_MICRO, "Micro-ohmímetro", CategoriaInstrumento.MICROOHMIMETRO, "Megger", "DLRO10X", "DLR-10882"),
            In(INS_CMC, "Equipo de inyección secundaria", CategoriaInstrumento.INYECCION_SECUNDARIA, "Omicron", "CMC 356", "CMC-HB411")
        ).forEach {
            ins(
                db, "instrumento", "id_instrumento" to it.id, "nombre" to it.nombre, "categoria" to it.cat.name,
                "marca" to it.marca, "modelo" to it.modelo, "numero_serie" to it.serie
            )
        }
        listOf(
            Triple(INS_MEGGER5, "2024-03-14", "2025-03-14"),
            Triple(INS_MEGGER5, "2025-03-14", "2026-03-15"),
            Triple(INS_MEGGER5, "2026-03-15", "2027-03-15"),
            Triple(INS_MEGGER10, "2025-10-10", "2026-10-10"),
            Triple(INS_TTR, "2024-06-03", "2025-06-02"),
            Triple(INS_TTR, "2025-06-02", "2026-06-02"),
            Triple(INS_TTR, "2026-06-02", "2027-06-02"),
            Triple(INS_MICRO, "2024-08-20", "2025-08-20"),
            Triple(INS_MICRO, "2025-08-20", "2026-08-20"),
            Triple(INS_CMC, "2025-01-06", "2026-01-05"),
            Triple(INS_CMC, "2026-01-05", "2027-01-05")
        ).forEach { (inst, cal, venc) ->
            ins(
                db, "calibracion", "id_instrumento" to inst, "fecha_calibracion" to cal,
                "fecha_vencimiento" to venc, "id_usuario_registra" to U_RICARDO
            )
        }
    }

    private fun visita(
        db: SQLiteDatabase, ot: String, inst: Long, fecha: String, tipo: TipoVisita, ingeniero: Long,
        participantes: List<Long>, cierre: Long?, cierraPor: Long? = participantes.firstOrNull()
    ): Long {
        val id = ins(
            db, "visita", "numero_ot" to ot, "id_instalacion" to inst, "fecha" to fecha,
            "tipo_visita" to tipo.name, "id_ingeniero_cargo" to ingeniero, "fecha_hora_cierre" to cierre,
            "id_usuario_cierra" to (if (cierre != null) cierraPor else null)
        )
        participantes.forEach { ins(db, "visita_participante", "id_visita" to id, "id_usuario" to it) }
        return id
    }

    private class EstadoSemilla(val estado: EstadoProtocolo, val usuario: Long, val cuando: Long, val motivo: String = "")

    private fun protocolo(
        db: SQLiteDatabase, numero: String, visita: Long, equipo: Long, tecnico: Long, prueba: Long,
        temp: Double, hum: Double, resultado: Resultado, historial: List<EstadoSemilla>,
        mantenimiento: String = "", observaciones: String = "", comentarioVariacion: String = ""
    ): Long {
        val id = ins(
            db, "protocolo", "uuid" to UUID.randomUUID().toString(), "numero" to numero,
            "id_visita" to visita, "id_equipo" to equipo, "id_usuario_registra" to tecnico,
            "fecha_hora_prueba" to prueba, "temperatura_c" to temp, "humedad_pct" to hum,
            "descripcion_mantenimiento" to mantenimiento, "observaciones" to observaciones,
            "comentario_variacion" to comentarioVariacion, "estado" to EstadoProtocolo.BORRADOR.name,
            "resultado_global" to resultado.name, "sincronizado" to 1
        )
        var anterior: EstadoProtocolo? = null
        historial.forEach {
            ins(
                db, "historial_estado", "id_protocolo" to id, "estado_anterior" to anterior?.name,
                "estado_nuevo" to it.estado.name, "id_usuario" to it.usuario, "fecha_hora" to it.cuando,
                "motivo" to it.motivo
            )
            anterior = it.estado
        }
        estadosFinales += id to historial.last().estado
        return id
    }

    private fun prueba(
        db: SQLiteDatabase, protocolo: Long, tipo: TipoPrueba, elemento: String, instrumento: Long?,
        resultado: Resultado?, valores: Map<Parametro, Double>, comentario: String = ""
    ) {
        val id = ins(
            db, "prueba", "id_protocolo" to protocolo, "tipo_prueba" to tipo.name, "elemento" to elemento,
            "momento" to Momento.UNICA.name, "realizada" to true, "id_instrumento" to instrumento,
            "resultado" to resultado?.name, "comentario" to comentario
        )
        valores.forEach { (p, v) -> ins(db, "medicion", "id_prueba" to id, "parametro" to p.name, "valor" to v) }
    }

    private fun noRealizada(db: SQLiteDatabase, protocolo: Long, tipo: TipoPrueba, motivo: String) {
        ins(
            db, "prueba", "id_protocolo" to protocolo, "tipo_prueba" to tipo.name, "elemento" to "",
            "momento" to Momento.UNICA.name, "realizada" to false, "motivo_no_realizada" to motivo
        )
    }

    private fun aisl(kv: Double, l1: Double, l10: Double? = null) = buildMap {
        put(Parametro.TENSION_PRUEBA, kv); put(Parametro.LECTURA_1MIN, l1)
        if (l10 != null) put(Parametro.LECTURA_10MIN, l10)
    }

    private fun ttr(tap: Double, teo: Double, med: Double) =
        mapOf(Parametro.POSICION_TAP to tap, Parametro.RELACION_TEORICA to teo, Parametro.RELACION_MEDIDA to med)

    private fun rele(aj: Double, op: Double, esp: Double, med: Double) = mapOf(
        Parametro.CORRIENTE_AJUSTE to aj, Parametro.CORRIENTE_OPERACION to op,
        Parametro.TIEMPO_ESPERADO to esp, Parametro.TIEMPO_MEDIDO to med
    )

    private const val MOTIVO_MICRO = "Micro-ohmímetro DLR-10882 con calibración vencida (20-08-2026)"

    private fun cargarVisitasYProtocolos(db: SQLiteDatabase) {
        val A = Resultado.APROBADO
        val O = Resultado.OBSERVADO
        val R = Resultado.RECHAZADO
        val B = EstadoProtocolo.BORRADOR
        val RT = EstadoProtocolo.REGISTRADO_TERRENO
        val OB = EstadoProtocolo.OBSERVADO
        val F = EstadoProtocolo.FORMALIZADO

        val v1 = visita(db, "OT-2024-0951", I_LAMPA, "2024-09-14", TipoVisita.SOLO_PRUEBAS, U_ANDREA, listOf(U_MATIAS), millis("2024-09-14", 13, 5))
        val p1 = protocolo(
            db, "PT-2024-0312", v1, 1, U_MATIAS, millis("2024-09-14", 9, 50), 18.0, 55.0, A,
            listOf(
                EstadoSemilla(B, U_MATIAS, millis("2024-09-14", 9, 50)),
                EstadoSemilla(RT, U_MATIAS, millis("2024-09-14", 12, 40)),
                EstadoSemilla(F, U_ANDREA, millis("2024-09-16", 10, 15))
            )
        )
        prueba(db, p1, TipoPrueba.AISLACION, "AT-T", INS_MEGGER5, A, aisl(5.0, 12500.0, 31250.0))
        prueba(db, p1, TipoPrueba.TTR, "H1-X1", INS_TTR, A, ttr(3.0, 30.0, 30.02))
        prueba(db, p1, TipoPrueba.DEVANADOS, "AT U/H1", INS_MICRO, A, mapOf(Parametro.RESISTENCIA to 1835.0))

        val v2 = visita(db, "OT-2025-1032", I_LAMPA, "2025-09-13", TipoVisita.SOLO_PRUEBAS, U_ANDREA, listOf(U_JAVIERA), millis("2025-09-13", 12, 50))
        val p2 = protocolo(
            db, "PT-2025-0288", v2, 1, U_JAVIERA, millis("2025-09-13", 9, 30), 21.0, 48.0, A,
            listOf(
                EstadoSemilla(B, U_JAVIERA, millis("2025-09-13", 9, 30)),
                EstadoSemilla(RT, U_JAVIERA, millis("2025-09-13", 12, 20)),
                EstadoSemilla(F, U_ANDREA, millis("2025-09-15", 11, 0))
            )
        )
        prueba(db, p2, TipoPrueba.AISLACION, "AT-T", INS_MEGGER5, A, aisl(5.0, 9800.0, 21560.0))
        prueba(db, p2, TipoPrueba.TTR, "H1-X1", INS_TTR, A, ttr(3.0, 30.0, 30.05))
        prueba(db, p2, TipoPrueba.DEVANADOS, "AT U/H1", INS_MICRO, A, mapOf(Parametro.RESISTENCIA to 1838.0))

        val v3 = visita(
            db, "OT-2026-1187", I_LAMPA, "2026-09-26", TipoVisita.PRUEBAS_Y_MANT, U_ANDREA,
            listOf(U_MATIAS), millis("2026-09-26", 13, 35)
        )
        ins(
            db, "recepcion_acta", "id_visita" to v3, "receptor_nombre" to "Marcela Fuentes Rojas",
            "receptor_rut" to "14.226.791-8", "receptor_cargo" to "Jefa de Mantenimiento",
            "fecha_hora_recepcion" to millis("2026-09-26", 13, 42)
        )
        val p3 = protocolo(
            db, "PT-2026-0401", v3, 1, U_MATIAS, millis("2026-09-26", 9, 40), 19.0, 60.0, O,
            listOf(
                EstadoSemilla(B, U_MATIAS, millis("2026-09-26", 9, 40)),
                EstadoSemilla(RT, U_MATIAS, millis("2026-09-26", 12, 55))
            ),
            mantenimiento = "Limpieza de aisladores BT por suciedad leve",
            observaciones = "IP bajo en AT-T. No se repitió la medición posterior por término de la ventana de corte."
        )
        prueba(db, p3, TipoPrueba.AISLACION, "AT-BT", INS_MEGGER5, O, aisl(5.0, 5800.0, 11200.0))
        prueba(db, p3, TipoPrueba.AISLACION, "AT-T", INS_MEGGER5, O, aisl(5.0, 6100.0, 10980.0))
        prueba(db, p3, TipoPrueba.AISLACION, "BT-T", INS_MEGGER5, A, aisl(1.0, 3200.0, 6900.0))
        prueba(db, p3, TipoPrueba.TTR, "H1-X1", INS_TTR, A, ttr(3.0, 30.0, 30.04))
        prueba(db, p3, TipoPrueba.TTR, "H2-X2", INS_TTR, A, ttr(3.0, 30.0, 30.06))
        prueba(db, p3, TipoPrueba.TTR, "H3-X3", INS_TTR, A, ttr(3.0, 30.0, 29.98))
        noRealizada(db, p3, TipoPrueba.DEVANADOS, MOTIVO_MICRO)

        val p4 = protocolo(
            db, "PT-2026-0402", v3, 2, U_MATIAS, millis("2026-09-26", 11, 10), 19.0, 60.0, A,
            listOf(
                EstadoSemilla(B, U_MATIAS, millis("2026-09-26", 11, 10)),
                EstadoSemilla(RT, U_MATIAS, millis("2026-09-26", 13, 10))
            )
        )
        prueba(db, p4, TipoPrueba.AISLACION, "AT-T", INS_MEGGER5, A, aisl(5.0, 8400.0, 19800.0))
        prueba(db, p4, TipoPrueba.TTR, "H1-X1", INS_TTR, A, ttr(3.0, 30.0, 30.03))
        noRealizada(db, p4, TipoPrueba.DEVANADOS, MOTIVO_MICRO)

        val v4 = visita(
            db, "OT-2026-1154", I_CLINICO, "2026-09-20", TipoVisita.SOLO_PRUEBAS, U_SEBASTIAN,
            listOf(U_CARLOS), millis("2026-09-20", 9, 50)
        )
        ins(
            db, "recepcion_acta", "id_visita" to v4, "receptor_nombre" to "Tomás Leiva Araya",
            "receptor_rut" to "18.390.452-3", "receptor_cargo" to "Supervisor Eléctrico",
            "fecha_hora_recepcion" to millis("2026-09-20", 9, 55)
        )
        val p5 = protocolo(
            db, "PT-2026-0389", v4, 4, U_CARLOS, millis("2026-09-20", 7, 10), 22.0, 45.0, O,
            listOf(
                EstadoSemilla(B, U_CARLOS, millis("2026-09-20", 7, 10)),
                EstadoSemilla(RT, U_CARLOS, millis("2026-09-20", 9, 40)),
                EstadoSemilla(OB, U_SEBASTIAN, millis("2026-09-21", 10, 15), "Falta nº de serie del instrumento usado en resistencia de contactos")
            )
        )
        prueba(db, p5, TipoPrueba.AISLACION, "Polo 1", INS_MEGGER5, A, aisl(5.0, 4500.0))
        prueba(db, p5, TipoPrueba.AISLACION, "Polo 2", INS_MEGGER5, A, aisl(5.0, 4700.0))
        prueba(db, p5, TipoPrueba.AISLACION, "Polo 3", INS_MEGGER5, A, aisl(5.0, 4400.0))
        prueba(db, p5, TipoPrueba.CONTACTOS, "Polo 1", null, A, mapOf(Parametro.RESISTENCIA_CONTACTO to 58.0), "Instrumento no identificado")
        prueba(db, p5, TipoPrueba.CONTACTOS, "Polo 2", null, O, mapOf(Parametro.RESISTENCIA_CONTACTO to 66.0), "Instrumento no identificado")
        prueba(db, p5, TipoPrueba.CONTACTOS, "Polo 3", null, O, mapOf(Parametro.RESISTENCIA_CONTACTO to 61.0), "Instrumento no identificado")

        val v5 = visita(
            db, "OT-2026-1121", I_MOLIENDA, "2026-09-15", TipoVisita.SOLO_PRUEBAS, U_SEBASTIAN,
            listOf(U_DIEGO), millis("2026-09-15", 12, 30)
        )
        val p6 = protocolo(
            db, "PT-2026-0377", v5, 6, U_DIEGO, millis("2026-09-15", 10, 5), 24.0, 35.0, A,
            listOf(
                EstadoSemilla(B, U_DIEGO, millis("2026-09-15", 10, 5)),
                EstadoSemilla(RT, U_DIEGO, millis("2026-09-15", 11, 50)),
                EstadoSemilla(F, U_SEBASTIAN, millis("2026-09-16", 16, 20))
            )
        )
        prueba(db, p6, TipoPrueba.OPERACION_RELE, "Fase R", INS_CMC, A, rele(5.0, 5.02, 300.0, 309.0))
        prueba(db, p6, TipoPrueba.OPERACION_RELE, "Fase S", INS_CMC, A, rele(5.0, 5.01, 300.0, 306.0))
        prueba(db, p6, TipoPrueba.OPERACION_RELE, "Fase T", INS_CMC, A, rele(5.0, 5.03, 300.0, 311.0))

        val v6 = visita(
            db, "OT-2026-1098", I_TILTIL, "2026-09-08", TipoVisita.SOLO_PRUEBAS, U_ANDREA,
            listOf(U_JAVIERA), millis("2026-09-08", 15, 10)
        )
        val p7 = protocolo(
            db, "PT-2026-0360", v6, 5, U_JAVIERA, millis("2026-09-08", 10, 30), 27.0, 20.0, R,
            listOf(
                EstadoSemilla(B, U_JAVIERA, millis("2026-09-08", 10, 30)),
                EstadoSemilla(RT, U_JAVIERA, millis("2026-09-08", 14, 45)),
                EstadoSemilla(F, U_ANDREA, millis("2026-09-10", 9, 30))
            ),
            observaciones = "Polo 1 con aislación bajo el mínimo. Se recomienda revisión del interruptor."
        )
        prueba(db, p7, TipoPrueba.AISLACION, "Polo 1", INS_MEGGER5, R, aisl(5.0, 80.0))
        prueba(db, p7, TipoPrueba.AISLACION, "Polo 2", INS_MEGGER5, A, aisl(5.0, 2200.0))
        prueba(db, p7, TipoPrueba.AISLACION, "Polo 3", INS_MEGGER5, A, aisl(5.0, 2350.0))
        noRealizada(db, p7, TipoPrueba.CONTACTOS, MOTIVO_MICRO)

        estadosFinales.forEach { (id, estado) ->
            val cv = ContentValues().apply { put("estado", estado.name) }
            db.update("protocolo", cv, "id_protocolo = ?", arrayOf(id.toString()))
        }

        val hoy = Formato.hoy().toString()
        visita(db, "OT-2026-1240", I_TILTIL, hoy, TipoVisita.PRUEBAS_Y_MANT, U_ANDREA, listOf(U_MATIAS, U_JAVIERA), null)
        visita(db, "OT-2026-1238", I_ELEVADORA, hoy, TipoVisita.SOLO_PRUEBAS, U_SEBASTIAN, listOf(U_CARLOS, U_DIEGO), null)
    }
}
