package cl.duocuc.induztek.data.repository

import cl.duocuc.induztek.data.remote.NuevaCuenta
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Clima
import cl.duocuc.induztek.domain.model.CodigoRecuperacion
import cl.duocuc.induztek.domain.model.EstadoSincronizacion
import cl.duocuc.induztek.domain.model.Operacion
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.ResumenSincronizacion
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.Usuario
import cl.duocuc.induztek.nativo.TipoVibracion
import kotlinx.coroutines.flow.StateFlow

fun interface Vibrador {
    fun vibrar(tipo: TipoVibracion)
}

interface Autenticacion {
    val ultimoCorreo: String
    suspend fun iniciarSesion(correo: String, contrasena: String): Operacion<Sesion>
}

interface CuentaFuente {
    val sesion: StateFlow<Sesion?>
    suspend fun recuperar(correo: String): Operacion<CodigoRecuperacion>
    suspend fun restablecer(correo: String, codigo: String, nueva: String): Operacion<String>
    suspend fun cambiarContrasena(actual: String, nueva: String): Operacion<Unit>
    suspend fun editarPerfil(nombre: String, correo: String, telefono: String, cargo: String): Operacion<Sesion>
}

interface UsuariosFuente {
    val sesion: StateFlow<Sesion?>
    suspend fun listar(buscar: String, rol: RolCodigo?, activo: Boolean?, pagina: Int): Operacion<Pagina<Usuario>>
    suspend fun obtener(id: Long): Operacion<Usuario>
    suspend fun crear(cuenta: NuevaCuenta): Operacion<Usuario>
    suspend fun editar(id: Long, nombre: String, correo: String, telefono: String, cargo: String): Operacion<Usuario>
    suspend fun cambiarEstado(id: Long, activo: Boolean): Operacion<Usuario>
    suspend fun eliminar(id: Long): Operacion<Unit>
    suspend fun empresas(): List<Cliente>
}

fun interface ClimaFuente {
    suspend fun actual(latitud: Double, longitud: Double): Operacion<Clima>
}

interface Sincronizador {
    val estado: StateFlow<EstadoSincronizacion>
    suspend fun sincronizar(): ResumenSincronizacion
}
