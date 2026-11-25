package cl.duocuc.induztek.data.repository

import android.database.Cursor
import android.database.sqlite.SQLiteException
import cl.duocuc.induztek.data.local.EspejoLocal
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.booleano
import cl.duocuc.induztek.data.local.consultar
import cl.duocuc.induztek.data.local.largo
import cl.duocuc.induztek.data.local.largoONulo
import cl.duocuc.induztek.data.local.texto
import cl.duocuc.induztek.data.remote.NuevaCuenta
import cl.duocuc.induztek.data.remote.UsuariosRemoto
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.ReglasCuentas
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.Usuario
import cl.duocuc.induztek.domain.util.Paginador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class UsuarioRepositorio(
    helper: InduztekDbHelper,
    cambios: NotificadorCambios,
    private val remoto: UsuariosRemoto,
    override val sesion: StateFlow<Sesion?>
) : RepositorioBase(helper, cambios), UsuariosFuente {
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

    suspend fun activosPorRol(vararg roles: RolCodigo): List<Usuario> = leer {
        val lista = roles.joinToString(",") { "'${it.name}'" }
        consultar("$sqlBase AND u.activo = 1 AND r.codigo IN ($lista) ORDER BY u.nombre") { leerUsuario(it) }
    }

    override suspend fun listar(buscar: String, rol: RolCodigo?, activo: Boolean?, pagina: Int): Operacion<Pagina<Usuario>> =
        remoto.listar(buscar, rol, activo, pagina, Paginador.TAMANO)

    override suspend fun obtener(id: Long): Operacion<Usuario> = remoto.obtener(id)

    override suspend fun crear(cuenta: NuevaCuenta): Operacion<Usuario> {
        val s = sesion.value ?: return Operacion.Fallo("Sesión no iniciada")
        if (!s.rol.puedeCrear(cuenta.rol)) return Operacion.Fallo(ReglasCuentas.mensajeNoPuedeCrear(s.rol))
        val r = remoto.crear(s.idUsuario, cuenta)
        if (r is Operacion.Exito) reflejar(r.valor)
        return r
    }

    override suspend fun editar(id: Long, nombre: String, correo: String, telefono: String, cargo: String): Operacion<Usuario> {
        val s = sesion.value ?: return Operacion.Fallo("Sesión no iniciada")
        val r = remoto.actualizar(s.idUsuario, id, nombre, correo, telefono, cargo)
        if (r is Operacion.Exito) reflejar(r.valor)
        return r
    }

    override suspend fun cambiarEstado(id: Long, activo: Boolean): Operacion<Usuario> {
        val s = sesion.value ?: return Operacion.Fallo("Sesión no iniciada")
        val r = remoto.cambiarEstado(s.idUsuario, id, activo)
        if (r is Operacion.Exito) reflejar(r.valor)
        return r
    }

    override suspend fun eliminar(id: Long): Operacion<Unit> {
        val s = sesion.value ?: return Operacion.Fallo("Sesión no iniciada")
        if (!s.puede(Permiso.ELIMINAR_CUENTAS)) return Operacion.Fallo("Solo un administrador puede eliminar cuentas")
        val r = remoto.eliminar(s.idUsuario, id)
        if (r is Operacion.Exito) {
            withContext(Dispatchers.IO) {
                try {
                    helper.writableDatabase.delete("usuario", "id_usuario = ?", arrayOf(id.toString()))
                } catch (e: SQLiteException) {
                    helper.writableDatabase.update(
                        "usuario", cl.duocuc.induztek.data.local.valores("activo" to false), "id_usuario = ?", arrayOf(id.toString())
                    )
                }
            }
            cambios.notificar()
        }
        return r
    }

    override suspend fun empresas(): List<Cliente> = leer {
        consultar("SELECT * FROM cliente ORDER BY razon_social") {
            Cliente(
                id = it.largo("id_cliente"), rut = it.texto("rut"), razonSocial = it.texto("razon_social"),
                giro = it.texto("giro"), contactoNombre = it.texto("contacto_nombre"),
                telefono = it.texto("telefono"), correo = it.texto("correo")
            )
        }
    }

    private suspend fun reflejar(u: Usuario) {
        withContext(Dispatchers.IO) {
            try {
                EspejoLocal.usuario(helper.writableDatabase, u)
            } catch (e: SQLiteException) {
                Unit
            }
        }
        cambios.notificar()
    }
}
