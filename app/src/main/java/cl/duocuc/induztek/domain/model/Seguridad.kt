package cl.duocuc.induztek.domain.model

enum class RolCodigo(val etiqueta: String, val perfil: String) {
    TECNICO("Técnico de terreno", "P-01"),
    INGENIERO("Ingeniero de pruebas", "P-02"),
    ADMINISTRADOR("Administrador (Jefe técnico)", "P-03"),
    CLIENTE("Usuario cliente", "P-04");

    val esPersonalInduztek: Boolean get() = this != CLIENTE

    val rolesQuePuedeCrear: List<RolCodigo>
        get() = when (this) {
            ADMINISTRADOR -> listOf(ADMINISTRADOR, INGENIERO, TECNICO, CLIENTE)
            INGENIERO -> listOf(TECNICO, CLIENTE)
            TECNICO, CLIENTE -> emptyList()
        }

    fun puedeCrear(otro: RolCodigo): Boolean = otro in rolesQuePuedeCrear

    companion object {
        fun desdeCodigo(codigo: String): RolCodigo = when (codigo) {
            "JEFE_TECNICO" -> ADMINISTRADOR
            else -> entries.firstOrNull { it.name == codigo } ?: CLIENTE
        }
    }
}

enum class Permiso(val descripcion: String) {
    GESTIONAR_CLIENTES_EQUIPOS("Registrar y editar clientes, instalaciones y equipos"),
    ELIMINAR_REGISTROS("Eliminar clientes, instalaciones, equipos e instrumentos"),
    REGISTRAR_PROTOCOLO("Crear protocolos y registrar mediciones"),
    CERRAR_VISITA("Cerrar visita y registrar recepción del cliente"),
    FORMALIZAR_OBSERVAR("Formalizar u observar protocolos"),
    ANULAR_PROTOCOLO("Anular protocolos"),
    GESTIONAR_INSTRUMENTOS("Registrar instrumentos y calibraciones"),
    CREAR_CUENTAS("Crear y administrar cuentas según su rol"),
    ELIMINAR_CUENTAS("Eliminar cuentas de usuario"),
    CONSULTAR_HISTORIAL("Consultar historial de cualquier equipo"),
    CONSULTAR_PROPIA_EMPRESA("Consultar actas, protocolos e historial de su propia empresa")
}

object MatrizPermisos {
    private val matriz: Map<RolCodigo, Set<Permiso>> = mapOf(
        RolCodigo.TECNICO to setOf(
            Permiso.GESTIONAR_CLIENTES_EQUIPOS, Permiso.REGISTRAR_PROTOCOLO,
            Permiso.CERRAR_VISITA, Permiso.CONSULTAR_HISTORIAL
        ),
        RolCodigo.INGENIERO to setOf(
            Permiso.GESTIONAR_CLIENTES_EQUIPOS, Permiso.REGISTRAR_PROTOCOLO,
            Permiso.CERRAR_VISITA, Permiso.FORMALIZAR_OBSERVAR,
            Permiso.ANULAR_PROTOCOLO, Permiso.CREAR_CUENTAS, Permiso.CONSULTAR_HISTORIAL
        ),
        RolCodigo.ADMINISTRADOR to setOf(
            Permiso.GESTIONAR_CLIENTES_EQUIPOS, Permiso.ELIMINAR_REGISTROS, Permiso.REGISTRAR_PROTOCOLO,
            Permiso.FORMALIZAR_OBSERVAR, Permiso.ANULAR_PROTOCOLO,
            Permiso.GESTIONAR_INSTRUMENTOS, Permiso.CREAR_CUENTAS, Permiso.ELIMINAR_CUENTAS,
            Permiso.CONSULTAR_HISTORIAL
        ),
        RolCodigo.CLIENTE to setOf(Permiso.CONSULTAR_PROPIA_EMPRESA)
    )

    fun permisosDe(rol: RolCodigo): Set<Permiso> = matriz[rol].orEmpty()
}

data class Sesion(
    val idUsuario: Long,
    val nombre: String,
    val correo: String,
    val rol: RolCodigo,
    val permisos: Set<Permiso> = MatrizPermisos.permisosDe(rol),
    val idCliente: Long? = null,
    val nombreCliente: String? = null,
    val cargo: String? = null,
    val rut: String = "",
    val telefono: String = ""
) {
    fun puede(permiso: Permiso): Boolean = permiso in permisos
    val esCliente: Boolean get() = rol == RolCodigo.CLIENTE
    val iniciales: String
        get() = nombre.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }
}

object ReglasCuentas {
    fun puedeAdministrar(actor: Sesion, objetivo: Usuario): Boolean =
        actor.idUsuario != objetivo.id && actor.rol.puedeCrear(objetivo.rol)

    fun puedeEliminar(actor: Sesion, objetivo: Usuario): Boolean =
        actor.puede(Permiso.ELIMINAR_CUENTAS) && actor.idUsuario != objetivo.id

    fun puedeEditarPerfil(actor: Sesion, objetivo: Usuario): Boolean =
        actor.idUsuario == objetivo.id || actor.rol.puedeCrear(objetivo.rol)

    fun mensajeNoPuedeCrear(actor: RolCodigo): String = when (actor) {
        RolCodigo.INGENIERO -> "Un Ingeniero solo puede crear cuentas de Técnico y de Cliente"
        RolCodigo.TECNICO, RolCodigo.CLIENTE -> "Su rol no permite crear cuentas"
        RolCodigo.ADMINISTRADOR -> "Rol no permitido"
    }
}
