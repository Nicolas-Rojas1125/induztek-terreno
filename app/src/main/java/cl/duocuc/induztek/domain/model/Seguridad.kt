package cl.duocuc.induztek.domain.model

enum class RolCodigo(val etiqueta: String, val perfil: String) {
    TECNICO("Técnico de terreno", "P-01"),
    INGENIERO("Ingeniero de pruebas", "P-02"),
    JEFE_TECNICO("Jefe técnico", "P-03"),
    CLIENTE("Usuario cliente", "P-04");

    val esPersonalInduztek: Boolean get() = this != CLIENTE

    companion object {
        fun desdeCodigo(codigo: String): RolCodigo = entries.firstOrNull { it.name == codigo } ?: CLIENTE
    }
}

enum class Permiso(val descripcion: String) {
    GESTIONAR_CLIENTES_EQUIPOS("Registrar clientes, instalaciones y equipos"),
    REGISTRAR_PROTOCOLO("Crear protocolos y registrar mediciones"),
    CERRAR_VISITA("Cerrar visita y registrar recepción del cliente"),
    FORMALIZAR_OBSERVAR("Formalizar u observar protocolos"),
    ANULAR_PROTOCOLO("Anular protocolos"),
    GESTIONAR_INSTRUMENTOS("Registrar instrumentos y calibraciones"),
    GESTIONAR_PERSONAL("Agregar o desactivar personal (RF-22)"),
    OTORGAR_CUENTAS_CLIENTE("Otorgar cuentas a usuarios cliente"),
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
            Permiso.ANULAR_PROTOCOLO, Permiso.CONSULTAR_HISTORIAL
        ),
        RolCodigo.JEFE_TECNICO to setOf(
            Permiso.GESTIONAR_CLIENTES_EQUIPOS, Permiso.REGISTRAR_PROTOCOLO,
            Permiso.FORMALIZAR_OBSERVAR, Permiso.ANULAR_PROTOCOLO,
            Permiso.GESTIONAR_INSTRUMENTOS, Permiso.GESTIONAR_PERSONAL,
            Permiso.OTORGAR_CUENTAS_CLIENTE, Permiso.CONSULTAR_HISTORIAL
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
    val permisos: Set<Permiso>,
    val idCliente: Long? = null,
    val nombreCliente: String? = null,
    val cargo: String? = null
) {
    fun puede(permiso: Permiso): Boolean = permiso in permisos
    val esCliente: Boolean get() = rol == RolCodigo.CLIENTE
    val iniciales: String
        get() = nombre.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }
}
