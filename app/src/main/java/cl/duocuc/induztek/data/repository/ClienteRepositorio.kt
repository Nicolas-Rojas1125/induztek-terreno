package cl.duocuc.induztek.data.repository

import android.database.Cursor
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.consultar
import cl.duocuc.induztek.data.local.consultarUno
import cl.duocuc.induztek.data.local.contar
import cl.duocuc.induztek.data.local.decimalONulo
import cl.duocuc.induztek.data.local.entero
import cl.duocuc.induztek.data.local.largo
import cl.duocuc.induztek.data.local.texto
import cl.duocuc.induztek.data.local.valores
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Comuna
import cl.duocuc.induztek.domain.model.Instalacion
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Region
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.validacion.ValidadorRut

class ClienteRepositorio(helper: InduztekDbHelper, cambios: NotificadorCambios) :
    RepositorioBase(helper, cambios) {
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

    suspend fun crearCliente(c: Cliente): Operacion<Long> =
        escribir("Ya existe un cliente con ese RUT") {
            val id = insertOrThrow(
                "cliente", null, valores(
                    "rut" to ValidadorRut.formatear(c.rut),
                    "razon_social" to c.razonSocial.trim(),
                    "giro" to c.giro.trim(),
                    "contacto_nombre" to c.contactoNombre.trim(),
                    "telefono" to c.telefono.trim(),
                    "correo" to c.correo.trim().lowercase()
                )
            )
            Operacion.Exito(id)
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

    suspend fun crearInstalacion(i: Instalacion): Operacion<Long> =
        escribir("Ese cliente ya tiene una instalación con ese nombre") {
            val id = insertOrThrow(
                "instalacion", null, valores(
                    "id_cliente" to i.idCliente,
                    "nombre" to i.nombre.trim(),
                    "ubicacion" to i.ubicacion.trim(),
                    "id_comuna" to i.idComuna,
                    "observaciones_acceso" to i.observacionesAcceso.trim(),
                    "latitud" to i.latitud,
                    "longitud" to i.longitud
                )
            )
            Operacion.Exito(id)
        }
}
