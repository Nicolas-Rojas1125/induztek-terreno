package cl.duocuc.induztek.data.repository

import android.database.sqlite.SQLiteException
import cl.duocuc.induztek.data.local.EspejoLocal
import cl.duocuc.induztek.data.local.InduztekDbHelper
import cl.duocuc.induztek.data.local.JsonLocal
import cl.duocuc.induztek.data.local.SesionPreferencias
import cl.duocuc.induztek.data.remote.UsuariosRemoto
import cl.duocuc.induztek.domain.model.CodigoRecuperacion
import cl.duocuc.induztek.domain.model.MatrizPermisos
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SesionGuardada(
    val idUsuario: Long,
    val nombre: String?,
    val correo: String?,
    val rol: String?,
    val idCliente: Long?,
    val nombreCliente: String?,
    val cargo: String?,
    val rut: String?,
    val telefono: String?
) {
    fun aSesion(): Sesion {
        val r = RolCodigo.desdeCodigo(rol.orEmpty())
        return Sesion(
            idUsuario = idUsuario, nombre = nombre.orEmpty(), correo = correo.orEmpty(), rol = r,
            permisos = MatrizPermisos.permisosDe(r), idCliente = idCliente, nombreCliente = nombreCliente,
            cargo = cargo, rut = rut.orEmpty(), telefono = telefono.orEmpty()
        )
    }

    companion object {
        fun de(s: Sesion) = SesionGuardada(
            s.idUsuario, s.nombre, s.correo, s.rol.name, s.idCliente, s.nombreCliente, s.cargo, s.rut, s.telefono
        )
    }
}

class SesionRepositorio(
    helper: InduztekDbHelper,
    cambios: NotificadorCambios,
    private val prefs: SesionPreferencias,
    private val remoto: UsuariosRemoto
) : RepositorioBase(helper, cambios), Autenticacion, CuentaFuente {
    private val _sesion = MutableStateFlow(restaurar())
    override val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    private val _tema = MutableStateFlow(prefs.tema())
    val tema: StateFlow<String> = _tema.asStateFlow()

    private val _servidor = MutableStateFlow(prefs.servidor())
    val servidor: StateFlow<String> = _servidor.asStateFlow()

    val codigoDispositivo: String get() = prefs.codigoDispositivo()
    override val ultimoCorreo: String get() = prefs.ultimoCorreo()

    private fun restaurar(): Sesion? =
        JsonLocal.desdeTexto(prefs.sesionJson(), SesionGuardada::class.java)?.takeIf { it.idUsuario > 0 }?.aSesion()

    private fun guardar(s: Sesion) {
        prefs.guardarSesion(JsonLocal.aTexto(SesionGuardada.de(s)), s.correo)
        _sesion.value = s
    }

    private suspend fun reflejarUsuarioLocal(s: Sesion) = withContext(Dispatchers.IO) {
        if (s.rol == RolCodigo.CLIENTE) return@withContext
        try {
            EspejoLocal.usuario(
                helper.writableDatabase,
                Usuario(id = s.idUsuario, rol = s.rol, rut = s.rut, nombre = s.nombre, correo = s.correo,
                    telefono = s.telefono, cargo = s.cargo.orEmpty())
            )
        } catch (e: SQLiteException) {
            Unit
        }
    }

    override suspend fun iniciarSesion(correo: String, contrasena: String): Operacion<Sesion> {
        val r = remoto.login(correo, contrasena)
        if (r is Operacion.Exito) {
            guardar(r.valor)
            reflejarUsuarioLocal(r.valor)
        }
        return r
    }

    override suspend fun recuperar(correo: String): Operacion<CodigoRecuperacion> = remoto.recuperar(correo)

    override suspend fun restablecer(correo: String, codigo: String, nueva: String): Operacion<String> =
        remoto.restablecer(correo, codigo, nueva)

    override suspend fun cambiarContrasena(actual: String, nueva: String): Operacion<Unit> {
        val s = _sesion.value ?: return Operacion.Fallo("Sesión no iniciada")
        return remoto.cambiarContrasena(s.idUsuario, actual, nueva)
    }

    override suspend fun editarPerfil(nombre: String, correo: String, telefono: String, cargo: String): Operacion<Sesion> {
        val s = _sesion.value ?: return Operacion.Fallo("Sesión no iniciada")
        return when (val r = remoto.actualizar(s.idUsuario, s.idUsuario, nombre, correo, telefono, cargo)) {
            is Operacion.Fallo -> r
            is Operacion.Exito -> {
                val nueva = s.copy(nombre = r.valor.nombre, correo = r.valor.correo, telefono = r.valor.telefono, cargo = r.valor.cargo)
                guardar(nueva)
                withContext(Dispatchers.IO) {
                    try {
                        EspejoLocal.usuario(helper.writableDatabase, r.valor)
                    } catch (e: SQLiteException) {
                        Unit
                    }
                }
                cambios.notificar()
                Operacion.Exito(nueva)
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

    fun cambiarServidor(url: String) {
        prefs.guardarServidor(url.trim())
        _servidor.value = url.trim()
    }
}
