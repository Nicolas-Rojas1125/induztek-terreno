package cl.duocuc.induztek.data.repository

import android.database.sqlite.SQLiteDatabase
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.SesionPreferencias
import cl.duocuc.induztek.data.local.booleano
import cl.duocuc.induztek.data.local.consultar
import cl.duocuc.induztek.data.local.consultarUno
import cl.duocuc.induztek.data.local.largo
import cl.duocuc.induztek.data.local.largoONulo
import cl.duocuc.induztek.data.local.texto
import cl.duocuc.induztek.data.local.textoONulo
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.validacion.HashContrasena
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SesionRepositorio(
    helper: InduztekDbHelper,
    cambios: NotificadorCambios,
    private val prefs: SesionPreferencias
) : RepositorioBase(helper, cambios) {
    private val _sesion = MutableStateFlow(restaurar())
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    private val _tema = MutableStateFlow(prefs.tema())
    val tema: StateFlow<String> = _tema.asStateFlow()

    val codigoDispositivo: String get() = prefs.codigoDispositivo()
    val ultimoCorreo: String get() = prefs.ultimoCorreo()

    private fun restaurar(): Sesion? {
        val id = prefs.idUsuario()
        if (id < 0) return null
        val s = cargarSesion(helper.readableDatabase, id)
        if (s == null) prefs.borrarSesion()
        return s
    }

    private fun cargarSesion(db: SQLiteDatabase, idUsuario: Long): Sesion? {
        val base = db.consultarUno(
            """SELECT u.id_usuario, u.nombre, u.correo, u.cargo, u.activo, u.id_cliente,
                      r.codigo AS rol, c.razon_social
               FROM usuario u
               JOIN rol r ON r.id_rol = u.id_rol
               LEFT JOIN cliente c ON c.id_cliente = u.id_cliente
               WHERE u.id_usuario = ?""",
            listOf(idUsuario)
        ) { c ->
            if (!c.booleano("activo")) null
            else Sesion(
                idUsuario = c.largo("id_usuario"),
                nombre = c.texto("nombre"),
                correo = c.texto("correo"),
                rol = RolCodigo.desdeCodigo(c.texto("rol")),
                permisos = emptySet(),
                idCliente = c.largoONulo("id_cliente"),
                nombreCliente = c.textoONulo("razon_social"),
                cargo = c.texto("cargo")
            )
        } ?: return null

        val permisos = db.consultar(
            """SELECT p.codigo FROM permiso p
               JOIN rol_permiso rp ON rp.id_permiso = p.id_permiso
               JOIN usuario u ON u.id_rol = rp.id_rol
               WHERE u.id_usuario = ?""",
            listOf(idUsuario)
        ) { c -> Permiso.entries.firstOrNull { it.name == c.texto("codigo") } }.filterNotNull().toSet()

        return base.copy(permisos = permisos)
    }

    suspend fun iniciarSesion(correo: String, contrasena: String): Operacion<Sesion> = withContext(Dispatchers.IO) {
        val db = helper.readableDatabase
        data class Credencial(val id: Long, val hash: String, val sal: String, val activo: Boolean)

        val cred = db.consultarUno(
            "SELECT id_usuario, password_hash, sal, activo FROM usuario WHERE correo = ? COLLATE NOCASE",
            listOf(correo.trim())
        ) { c -> Credencial(c.largo("id_usuario"), c.texto("password_hash"), c.texto("sal"), c.booleano("activo")) }

        when {
            cred == null || !HashContrasena.verificar(contrasena, cred.sal, cred.hash) ->
                Operacion.Fallo("Correo o contraseña incorrectos")
            !cred.activo ->
                Operacion.Fallo("Usuario desactivado. Contacte al Jefe técnico (Aclaración nº 8)")
            else -> {
                val s = cargarSesion(db, cred.id)
                if (s == null) Operacion.Fallo("No se pudo cargar el perfil")
                else {
                    prefs.guardarSesion(s.idUsuario, s.correo)
                    _sesion.value = s
                    Operacion.Exito(s)
                }
            }
        }
    }

    fun cerrarSesion() {
        prefs.borrarSesion()
        _sesion.value = null
    }

    fun cambiarTema(valor: String) {
        prefs.guardarTema(valor)
        _tema.value = valor
    }
}
