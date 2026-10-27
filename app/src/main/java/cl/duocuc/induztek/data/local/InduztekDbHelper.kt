package cl.duocuc.induztek.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.duocuc.induztek.domain.model.CategoriaInstrumento
import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.Momento
import cl.duocuc.induztek.domain.model.Resultado
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.model.TipoPrueba
import cl.duocuc.induztek.domain.model.TipoVisita

class InduztekDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NOMBRE_BD, null, VERSION_BD) {
    companion object {
        const val NOMBRE_BD = "induztek.db"
        const val VERSION_BD = 1

        @Volatile
        private var instancia: InduztekDbHelper? = null

        fun obtener(context: Context): InduztekDbHelper =
            instancia ?: synchronized(this) {
                instancia ?: InduztekDbHelper(context).also { instancia = it }
            }

        private fun valoresEnum(valores: List<String>) = valores.joinToString(",") { "'$it'" }

        private val ESTADOS = valoresEnum(EstadoProtocolo.entries.map { it.name })
        private val RESULTADOS = valoresEnum(Resultado.entries.map { it.name })
        private val TIPOS_EQUIPO = valoresEnum(TipoEquipo.entries.map { it.name })
        private val TIPOS_PRUEBA = valoresEnum(TipoPrueba.entries.map { it.name })
        private val MOMENTOS = valoresEnum(Momento.entries.map { it.name })
        private val TIPOS_VISITA = valoresEnum(TipoVisita.entries.map { it.name })
        private val CATEGORIAS = valoresEnum(CategoriaInstrumento.entries.map { it.name })

        val SQL_CREAR: List<String> = listOf(
            """CREATE TABLE region (
                id_region INTEGER PRIMARY KEY,
                nombre TEXT NOT NULL UNIQUE)""",
            """CREATE TABLE comuna (
                id_comuna INTEGER PRIMARY KEY,
                nombre TEXT NOT NULL,
                id_region INTEGER NOT NULL REFERENCES region(id_region),
                UNIQUE(nombre, id_region))""",

            """CREATE TABLE rol (
                id_rol INTEGER PRIMARY KEY,
                codigo TEXT NOT NULL UNIQUE,
                nombre TEXT NOT NULL)""",
            """CREATE TABLE permiso (
                id_permiso INTEGER PRIMARY KEY,
                codigo TEXT NOT NULL UNIQUE,
                descripcion TEXT NOT NULL)""",
            """CREATE TABLE rol_permiso (
                id_rol INTEGER NOT NULL REFERENCES rol(id_rol),
                id_permiso INTEGER NOT NULL REFERENCES permiso(id_permiso),
                PRIMARY KEY(id_rol, id_permiso))""",
            """CREATE TABLE cliente (
                id_cliente INTEGER PRIMARY KEY AUTOINCREMENT,
                rut TEXT NOT NULL UNIQUE,
                razon_social TEXT NOT NULL,
                giro TEXT NOT NULL,
                contacto_nombre TEXT NOT NULL,
                telefono TEXT NOT NULL,
                correo TEXT NOT NULL)""",
            """CREATE TABLE usuario (
                id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
                id_rol INTEGER NOT NULL REFERENCES rol(id_rol),
                id_cliente INTEGER REFERENCES cliente(id_cliente),
                rut TEXT NOT NULL UNIQUE,
                nombre TEXT NOT NULL,
                correo TEXT NOT NULL UNIQUE COLLATE NOCASE,
                telefono TEXT NOT NULL DEFAULT '',
                cargo TEXT NOT NULL DEFAULT '',
                password_hash TEXT NOT NULL,
                sal TEXT NOT NULL,
                activo INTEGER NOT NULL DEFAULT 1 CHECK(activo IN (0,1)))""",
            """CREATE TRIGGER tg_usuario_cliente_con_empresa
                BEFORE INSERT ON usuario
                WHEN (SELECT codigo FROM rol WHERE id_rol = NEW.id_rol) = 'CLIENTE' AND NEW.id_cliente IS NULL
                BEGIN SELECT RAISE(ABORT, 'Un usuario cliente debe pertenecer a una empresa (RN-18)'); END""",

            """CREATE TABLE instalacion (
                id_instalacion INTEGER PRIMARY KEY AUTOINCREMENT,
                id_cliente INTEGER NOT NULL REFERENCES cliente(id_cliente),
                nombre TEXT NOT NULL,
                ubicacion TEXT NOT NULL,
                id_comuna INTEGER NOT NULL REFERENCES comuna(id_comuna),
                observaciones_acceso TEXT NOT NULL DEFAULT '',
                latitud REAL,
                longitud REAL,
                UNIQUE(id_cliente, nombre))""",
            """CREATE TABLE equipo (
                id_equipo INTEGER PRIMARY KEY AUTOINCREMENT,
                id_instalacion INTEGER NOT NULL REFERENCES instalacion(id_instalacion),
                tag TEXT NOT NULL,
                tipo_equipo TEXT NOT NULL CHECK(tipo_equipo IN ($TIPOS_EQUIPO)),
                marca TEXT NOT NULL,
                modelo TEXT NOT NULL DEFAULT '',
                numero_serie TEXT NOT NULL,
                anio_fabricacion INTEGER,
                potencia_kva REAL,
                tension_primaria_kv REAL,
                tension_secundaria_kv REAL,
                corriente_nominal_a REAL,
                max_contactos_uohm REAL,
                observaciones TEXT NOT NULL DEFAULT '',
                foto_placa TEXT,
                id_usuario_crea INTEGER REFERENCES usuario(id_usuario),
                UNIQUE(id_instalacion, tag))""",
            """CREATE TABLE instrumento (
                id_instrumento INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                categoria TEXT NOT NULL CHECK(categoria IN ($CATEGORIAS)),
                marca TEXT NOT NULL,
                modelo TEXT NOT NULL,
                numero_serie TEXT NOT NULL UNIQUE)""",
            """CREATE TABLE calibracion (
                id_calibracion INTEGER PRIMARY KEY AUTOINCREMENT,
                id_instrumento INTEGER NOT NULL REFERENCES instrumento(id_instrumento),
                fecha_calibracion TEXT NOT NULL,
                fecha_vencimiento TEXT NOT NULL,
                id_usuario_registra INTEGER REFERENCES usuario(id_usuario),
                CHECK(fecha_vencimiento > fecha_calibracion))""",

            """CREATE TABLE visita (
                id_visita INTEGER PRIMARY KEY AUTOINCREMENT,
                numero_ot TEXT NOT NULL UNIQUE,
                id_instalacion INTEGER NOT NULL REFERENCES instalacion(id_instalacion),
                fecha TEXT NOT NULL,
                tipo_visita TEXT NOT NULL CHECK(tipo_visita IN ($TIPOS_VISITA)),
                id_ingeniero_cargo INTEGER NOT NULL REFERENCES usuario(id_usuario),
                fecha_hora_cierre INTEGER,
                latitud_cierre REAL,
                longitud_cierre REAL,
                id_usuario_cierra INTEGER REFERENCES usuario(id_usuario))""",
            """CREATE TABLE visita_participante (
                id_visita INTEGER NOT NULL REFERENCES visita(id_visita) ON DELETE CASCADE,
                id_usuario INTEGER NOT NULL REFERENCES usuario(id_usuario),
                PRIMARY KEY(id_visita, id_usuario))""",
            """CREATE TABLE recepcion_acta (
                id_visita INTEGER PRIMARY KEY REFERENCES visita(id_visita),
                receptor_nombre TEXT NOT NULL,
                receptor_rut TEXT NOT NULL,
                receptor_cargo TEXT NOT NULL,
                fecha_hora_recepcion INTEGER NOT NULL)""",
            """CREATE TABLE protocolo (
                id_protocolo INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL UNIQUE,
                numero TEXT NOT NULL UNIQUE,
                id_visita INTEGER NOT NULL REFERENCES visita(id_visita),
                id_equipo INTEGER NOT NULL REFERENCES equipo(id_equipo),
                id_usuario_registra INTEGER NOT NULL REFERENCES usuario(id_usuario),
                fecha_hora_prueba INTEGER NOT NULL,
                temperatura_c REAL CHECK(temperatura_c IS NULL OR temperatura_c BETWEEN -30 AND 60),
                humedad_pct REAL CHECK(humedad_pct IS NULL OR humedad_pct BETWEEN 0 AND 100),
                descripcion_mantenimiento TEXT NOT NULL DEFAULT '',
                observaciones TEXT NOT NULL DEFAULT '',
                comentario_variacion TEXT NOT NULL DEFAULT '',
                estado TEXT NOT NULL CHECK(estado IN ($ESTADOS)),
                resultado_global TEXT CHECK(resultado_global IS NULL OR resultado_global IN ($RESULTADOS)),
                id_protocolo_reemplazado INTEGER REFERENCES protocolo(id_protocolo),
                sincronizado INTEGER NOT NULL DEFAULT 0)""",
            """CREATE UNIQUE INDEX ux_protocolo_visita_equipo
                ON protocolo(id_visita, id_equipo) WHERE id_protocolo_reemplazado IS NULL""",
            """CREATE TRIGGER tg_protocolo_formalizado_inmutable
                BEFORE UPDATE ON protocolo
                WHEN OLD.estado = 'FORMALIZADO'
                BEGIN SELECT RAISE(ABORT, 'Un protocolo Formalizado no se puede modificar (RN-04)'); END""",
            """CREATE TABLE historial_estado (
                id_historial INTEGER PRIMARY KEY AUTOINCREMENT,
                id_protocolo INTEGER NOT NULL REFERENCES protocolo(id_protocolo),
                estado_anterior TEXT CHECK(estado_anterior IS NULL OR estado_anterior IN ($ESTADOS)),
                estado_nuevo TEXT NOT NULL CHECK(estado_nuevo IN ($ESTADOS)),
                id_usuario INTEGER NOT NULL REFERENCES usuario(id_usuario),
                fecha_hora INTEGER NOT NULL,
                motivo TEXT NOT NULL DEFAULT '')""",
            """CREATE TABLE prueba (
                id_prueba INTEGER PRIMARY KEY AUTOINCREMENT,
                id_protocolo INTEGER NOT NULL REFERENCES protocolo(id_protocolo),
                tipo_prueba TEXT NOT NULL CHECK(tipo_prueba IN ($TIPOS_PRUEBA)),
                elemento TEXT NOT NULL,
                momento TEXT NOT NULL CHECK(momento IN ($MOMENTOS)),
                realizada INTEGER NOT NULL CHECK(realizada IN (0,1)),
                motivo_no_realizada TEXT NOT NULL DEFAULT '',
                id_instrumento INTEGER REFERENCES instrumento(id_instrumento),
                resultado TEXT CHECK(resultado IS NULL OR resultado IN ($RESULTADOS)),
                no_opera INTEGER NOT NULL DEFAULT 0,
                comentario TEXT NOT NULL DEFAULT '',
                UNIQUE(id_protocolo, tipo_prueba, elemento, momento))""",
            """CREATE TABLE medicion (
                id_medicion INTEGER PRIMARY KEY AUTOINCREMENT,
                id_prueba INTEGER NOT NULL REFERENCES prueba(id_prueba) ON DELETE CASCADE,
                parametro TEXT NOT NULL,
                valor REAL NOT NULL,
                UNIQUE(id_prueba, parametro))""",
            """CREATE TRIGGER tg_prueba_formalizada_ins
                BEFORE INSERT ON prueba
                WHEN (SELECT estado FROM protocolo WHERE id_protocolo = NEW.id_protocolo) = 'FORMALIZADO'
                BEGIN SELECT RAISE(ABORT, 'Protocolo formalizado: no se agregan pruebas'); END""",
            """CREATE TRIGGER tg_prueba_formalizada_del
                BEFORE DELETE ON prueba
                WHEN (SELECT estado FROM protocolo WHERE id_protocolo = OLD.id_protocolo) = 'FORMALIZADO'
                BEGIN SELECT RAISE(ABORT, 'Protocolo formalizado: no se eliminan pruebas'); END""",

            "CREATE INDEX ix_protocolo_equipo ON protocolo(id_equipo, fecha_hora_prueba)",
            "CREATE INDEX ix_protocolo_estado ON protocolo(estado)",
            "CREATE INDEX ix_equipo_serie ON equipo(numero_serie)",
            "CREATE INDEX ix_prueba_protocolo ON prueba(id_protocolo)"
        )

        private val TABLAS_EN_ORDEN_INVERSO = listOf(
            "medicion", "prueba", "historial_estado", "protocolo", "recepcion_acta",
            "visita_participante", "visita", "calibracion", "instrumento", "equipo",
            "instalacion", "usuario", "cliente", "rol_permiso", "permiso", "rol", "comuna", "region"
        )
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            SQL_CREAR.forEach { db.execSQL(it) }
            DatosSemilla.cargar(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TRIGGER IF EXISTS tg_protocolo_formalizado_inmutable")
        db.execSQL("DROP TRIGGER IF EXISTS tg_prueba_formalizada_ins")
        db.execSQL("DROP TRIGGER IF EXISTS tg_prueba_formalizada_del")
        db.execSQL("DROP TRIGGER IF EXISTS tg_usuario_cliente_con_empresa")
        TABLAS_EN_ORDEN_INVERSO.forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }
}
