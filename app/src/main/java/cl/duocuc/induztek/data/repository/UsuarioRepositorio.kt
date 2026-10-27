package cl.duocuc.induztek.data.repository

import android.database.Cursor
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.booleano
import cl.duocuc.induztek.data.local.consultar
import cl.duocuc.induztek.data.local.contar
import cl.duocuc.induztek.data.local.largo
import cl.duocuc.induztek.data.local.largoONulo
import cl.duocuc.induztek.data.local.texto
import cl.duocuc.induztek.data.local.valores
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Usuario
import cl.duocuc.induztek.domain.validacion.HashContrasena
import cl.duocuc.induztek.domain.validacion.ValidadorRut

class UsuarioRepositorio(helper: InduztekDbHelper, cambios: NotificadorCambios) :
    RepositorioBase(helper, cambios) {
    private val sqlBase = """
        SELECT u.id_usuario, u.rut, u.nombre, u.correo, u.telefono, u.cargo, u.activo, u.id_cliente,
               r.codigo AS rol, COALESCE(c.razon_social, '') AS razon_social
        FROM usuario u
        JOIN rol r ON r.id_rol = u.id_rol
        LEFT JOIN cliente c ON c.id_cliente = u.id_cliente
        WHERE 1 = 1 """

    private fun leerUsuario(c: Cursor) = Usuario(
        id = c.largo("id_usuario"),
        rol = RolCodigo.desdeCodigo(c.texto("rol")),
        rut = c.texto("rut"),
        nombre = c.texto("nombre"),
        correo = c.texto("correo"),
        telefono = c.texto("telefono"),
        activo = c.booleano("activo"),
        idCliente = c.largoONulo("id_cliente"),
        cargo = c.texto("cargo"),
        nombreCliente = c.texto("razon_social")
    )

    suspend fun listar(texto: String = "", rol: RolCodigo? = null): List<Usuario> = leer {
        val args = mutableListOf<Any?>()
        var sql = sqlBase
        if (rol != null) { sql += " AND r.codigo = ? "; args += rol.name }
        if (texto.isNotBlank()) {
            sql += " AND (u.nombre LIKE ? OR u.correo LIKE ? OR u.rut LIKE ?) "
            repeat(3) { args += "%${texto.trim()}%" }
        }
        consultar("$sql ORDER BY u.activo DESC, r.id_rol, u.nombre", args) { leerUsuario(it) }
    }

    suspend fun activosPorRol(vararg roles: RolCodigo): List<Usuario> = leer {
        val lista = roles.joinToString(",") { "'${it.name}'" }
        consultar("$sqlBase AND u.activo = 1 AND r.codigo IN ($lista) ORDER BY u.nombre") { leerUsuario(it) }
    }

    suspend fun existeRut(rut: String): Boolean = leer {
        contar("SELECT COUNT(*) FROM usuario WHERE rut = ?", listOf(ValidadorRut.formatear(rut))) > 0
    }

    suspend fun existeCorreo(correo: String): Boolean = leer {
        contar("SELECT COUNT(*) FROM usuario WHERE correo = ? COLLATE NOCASE", listOf(correo.trim())) > 0
    }

    suspend fun crear(u: Usuario, contrasena: String): Operacion<Long> =
        escribir("Ya existe un usuario con ese RUT o correo") {
            val sal = HashContrasena.nuevaSal()
            val idRol = (u.rol.ordinal + 1).toLong()
            val id = insertOrThrow(
                "usuario", null, valores(
                    "id_rol" to idRol,
                    "id_cliente" to (if (u.rol == RolCodigo.CLIENTE) u.idCliente else null),
                    "rut" to ValidadorRut.formatear(u.rut),
                    "nombre" to u.nombre.trim(),
                    "correo" to u.correo.trim().lowercase(),
                    "telefono" to u.telefono.trim(),
                    "cargo" to u.cargo.trim(),
                    "password_hash" to HashContrasena.hash(contrasena, sal),
                    "sal" to sal,
                    "activo" to true
                )
            )
            Operacion.Exito(id)
        }

    suspend fun cambiarActivo(idUsuario: Long, activo: Boolean): Operacion<Unit> = escribir {
        update("usuario", valores("activo" to activo), "id_usuario = ?", arrayOf(idUsuario.toString()))
        Operacion.Exito(Unit)
    }
}
