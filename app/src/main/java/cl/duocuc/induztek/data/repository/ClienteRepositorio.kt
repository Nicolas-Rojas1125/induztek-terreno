package cl.duocuc.induztek.data.repository

import android.database.Cursor
import cl.duocuc.induztek.data.local.EspejoLocal
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.consultar
import cl.duocuc.induztek.data.local.consultarUno
import cl.duocuc.induztek.data.local.contar
import cl.duocuc.induztek.data.local.decimalONulo
import cl.duocuc.induztek.data.local.entero
import cl.duocuc.induztek.data.local.largo
import cl.duocuc.induztek.data.local.texto
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Comuna
import cl.duocuc.induztek.domain.model.Instalacion
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.Region
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.util.Paginador
import cl.duocuc.induztek.domain.validacion.ValidadorRut
import cl.duocuc.induztek.data.remote.ActivosRemoto

class ClienteRepositorio(
    helper: InduztekDbHelper,
    cambios: NotificadorCambios,
    private val remoto: ActivosRemoto
) : RepositorioBase(helper, cambios) {
    suspend fun regiones(): List<Region> = leer {
        consultar("SELECT id_region, nombre FROM region ORDER BY id_region DESC") {
            Region(it.largo("id_region"), it.texto("nombre"))
        }
    }

    suspend fun comunas(idRegion: Long): List<Comuna> = leer {
        consultar(
            "SELECT c.id_comuna, c.nombre, c.id_region, r.nombre AS region FROM comuna c " +
                "JOIN region r ON r.id_region = c.id_region WHERE c.id_region = ? ORDER BY c.nombre",
            listOf(idRegion)
        ) { Comuna(it.largo("id_comuna"), it.texto("nombre"), it.largo("id_region"), it.texto("region")) }
    }

    private fun leerCliente(c: Cursor) = Cliente(
        id = c.largo("id_cliente"),
        rut = c.texto("rut"),
        razonSocial = c.texto("razon_social"),
        giro = c.texto("giro"),
        contactoNombre = c.texto("contacto_nombre"),
        telefono = c.texto("telefono"),
        correo = c.texto("correo"),
        direccion = c.texto("direccion"),
        cantidadInstalaciones = c.entero("n_instalaciones")
    )

    suspend fun listarClientes(sesion: Sesion, texto: String = ""): List<Cliente> = leer {
        val args = mutableListOf<Any?>()
        val filtro = filtroEmpresa(sesion, "c.id_cliente", args)
        var sql = """SELECT c.*, (SELECT COUNT(*) FROM instalacion i WHERE i.id_cliente = c.id_cliente) AS n_instalaciones
                     FROM cliente c WHERE 1 = 1 $filtro"""
        if (texto.isNotBlank()) {
            sql += " AND (c.razon_social LIKE ? OR c.rut LIKE ?) "
            args += "%${texto.trim()}%"; args += "%${texto.trim()}%"
        }
        consultar("$sql ORDER BY c.razon_social", args) { leerCliente(it) }
    }

    suspend fun obtenerCliente(id: Long, sesion: Sesion): Cliente? = leer {
        val args = mutableListOf<Any?>(id)
        val filtro = filtroEmpresa(sesion, "c.id_cliente", args)
        consultarUno(
            """SELECT c.*, (SELECT COUNT(*) FROM instalacion i WHERE i.id_cliente = c.id_cliente) AS n_instalaciones
               FROM cliente c WHERE c.id_cliente = ? $filtro""", args
        ) { leerCliente(it) }
    }

    suspend fun existeRutCliente(rut: String): Boolean = leer {
        contar("SELECT COUNT(*) FROM cliente WHERE rut = ?", listOf(ValidadorRut.formatear(rut))) > 0
    }

    suspend fun paginaClientes(sesion: Sesion, texto: String, pagina: Int): Pagina<Cliente> =
        Paginador.paginar(listarClientes(sesion, texto), pagina)

    suspend fun comuna(idComuna: Long): Comuna? = leer {
        consultarUno(
            "SELECT c.id_comuna, c.nombre, c.id_region, r.nombre AS region FROM comuna c " +
                "JOIN region r ON r.id_region = c.id_region WHERE c.id_comuna = ?",
            listOf(idComuna)
        ) { Comuna(it.largo("id_comuna"), it.texto("nombre"), it.largo("id_region"), it.texto("region")) }
    }

    suspend fun guardarCliente(actor: Long, c: Cliente): Operacion<Long> =
        when (val r = remoto.guardarCliente(actor, c)) {
            is Operacion.Fallo -> r
            is Operacion.Exito -> escribir { EspejoLocal.cliente(this, r.valor); Operacion.Exito(r.valor.id) }
        }

    suspend fun eliminarCliente(id: Long): Operacion<Unit> {
        val instalaciones = leer { contar("SELECT COUNT(*) FROM instalacion WHERE id_cliente = ?", listOf(id)) }
        if (instalaciones > 0) return Operacion.Fallo("No se puede eliminar: el cliente tiene $instalaciones instalación(es)")
        return when (val r = remoto.eliminarCliente(id)) {
            is Operacion.Fallo -> r
            is Operacion.Exito -> escribir("El cliente tiene registros asociados") {
                delete("cliente", "id_cliente = ?", arrayOf(id.toString()))
                Operacion.Exito(Unit)
            }
        }
    }

    private val sqlInstalacion = """
        SELECT i.*, c.razon_social, co.nombre AS comuna, r.nombre AS region,
               (SELECT COUNT(*) FROM equipo e WHERE e.id_instalacion = i.id_instalacion) AS n_equipos
        FROM instalacion i
        JOIN cliente c ON c.id_cliente = i.id_cliente
        JOIN comuna co ON co.id_comuna = i.id_comuna
        JOIN region r ON r.id_region = co.id_region
        WHERE 1 = 1 """

    private fun leerInstalacion(c: Cursor) = Instalacion(
        id = c.largo("id_instalacion"),
        idCliente = c.largo("id_cliente"),
        nombre = c.texto("nombre"),
        ubicacion = c.texto("ubicacion"),
        idComuna = c.largo("id_comuna"),
        observacionesAcceso = c.texto("observaciones_acceso"),
        latitud = c.decimalONulo("latitud"),
        longitud = c.decimalONulo("longitud"),
        nombreCliente = c.texto("razon_social"),
        nombreComuna = c.texto("comuna"),
        nombreRegion = c.texto("region"),
        cantidadEquipos = c.entero("n_equipos")
    )

    suspend fun listarInstalaciones(sesion: Sesion, idCliente: Long? = null): List<Instalacion> = leer {
        val args = mutableListOf<Any?>()
        var sql = sqlInstalacion + filtroEmpresa(sesion, "i.id_cliente", args)
        if (idCliente != null) { sql += " AND i.id_cliente = ? "; args += idCliente }
        consultar("$sql ORDER BY c.razon_social, i.nombre", args) { leerInstalacion(it) }
    }

    suspend fun obtenerInstalacion(id: Long, sesion: Sesion): Instalacion? = leer {
        val args = mutableListOf<Any?>(id)
        consultarUno(
            sqlInstalacion + " AND i.id_instalacion = ? " + filtroEmpresa(sesion, "i.id_cliente", args), args
        ) { leerInstalacion(it) }
    }

    suspend fun guardarInstalacion(actor: Long, i: Instalacion): Operacion<Long> {
        val comuna = comuna(i.idComuna) ?: return Operacion.Fallo("Seleccione una comuna válida")
        val completa = i.copy(nombreComuna = comuna.nombre, nombreRegion = comuna.nombreRegion)
        return when (val r = remoto.guardarInstalacion(actor, completa)) {
            is Operacion.Fallo -> r
            is Operacion.Exito -> escribir { EspejoLocal.instalacion(this, r.valor); Operacion.Exito(r.valor.id) }
        }
    }

    suspend fun eliminarInstalacion(id: Long): Operacion<Unit> {
        val usos = leer {
            contar("SELECT COUNT(*) FROM equipo WHERE id_instalacion = ?", listOf(id)) +
                contar("SELECT COUNT(*) FROM visita WHERE id_instalacion = ?", listOf(id))
        }
        if (usos > 0) return Operacion.Fallo("No se puede eliminar: la instalación tiene equipos o visitas")
        return when (val r = remoto.eliminarInstalacion(id)) {
            is Operacion.Fallo -> r
            is Operacion.Exito -> escribir("La instalación tiene registros asociados") {
                delete("instalacion", "id_instalacion = ?", arrayOf(id.toString()))
                Operacion.Exito(Unit)
            }
        }
    }
}
