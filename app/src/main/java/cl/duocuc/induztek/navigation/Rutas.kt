package cl.duocuc.induztek.navigation

import cl.duocuc.induztek.domain.model.TipoPrueba

sealed class Ruta(val ruta: String) {
    data object Login : Ruta("login")
    data object RecuperarClave : Ruta("recuperar-clave")
    data object Servidor : Ruta("servidor")
    data object Perfil : Ruta("perfil")
    data object CambiarClave : Ruta("cambiar-clave")
    data object Sincronizacion : Ruta("sincronizacion")

    data object Inicio : Ruta("inicio")
    data object Visitas : Ruta("visitas")
    data object Protocolos : Ruta("protocolos")
    data object Equipos : Ruta("equipos")
    data object Actas : Ruta("actas")
    data object Mas : Ruta("mas")

    data object Clientes : Ruta("clientes")
    data object ClienteNuevo : Ruta("nuevo-cliente")
    data object ClienteDetalle : Ruta("cliente/{id}") { fun crear(id: Long) = "cliente/$id" }
    data object ClienteEditar : Ruta("cliente/{id}/editar") { fun crear(id: Long) = "cliente/$id/editar" }
    data object InstalacionNueva : Ruta("nueva-instalacion/{clienteId}") { fun crear(clienteId: Long) = "nueva-instalacion/$clienteId" }
    data object InstalacionDetalle : Ruta("instalacion/{id}") { fun crear(id: Long) = "instalacion/$id" }
    data object InstalacionEditar : Ruta("instalacion/{id}/editar") { fun crear(id: Long) = "instalacion/$id/editar" }

    data object EquipoNuevo : Ruta("nuevo-equipo/{instalacionId}") { fun crear(instalacionId: Long) = "nuevo-equipo/$instalacionId" }
    data object EquipoDetalle : Ruta("equipo/{id}") { fun crear(id: Long) = "equipo/$id" }
    data object EquipoEditar : Ruta("equipo/{id}/editar") { fun crear(id: Long) = "equipo/$id/editar" }

    data object VisitaNueva : Ruta("nueva-visita?instalacionId={instalacionId}") {
        fun crear(instalacionId: Long? = null) = if (instalacionId == null) "nueva-visita" else "nueva-visita?instalacionId=$instalacionId"
    }
    data object VisitaDetalle : Ruta("visita/{id}") { fun crear(id: Long) = "visita/$id" }
    data object Recepcion : Ruta("visita/{id}/recepcion") { fun crear(id: Long) = "visita/$id/recepcion" }
    data object Acta : Ruta("visita/{id}/acta") { fun crear(id: Long) = "visita/$id/acta" }

    data object ProtocoloDetalle : Ruta("protocolo/{id}") { fun crear(id: Long) = "protocolo/$id" }
    data object PruebaForm : Ruta("protocolo/{protocoloId}/prueba/{tipo}?pruebaId={pruebaId}") {
        fun crear(protocoloId: Long, tipo: TipoPrueba, pruebaId: Long? = null) =
            "protocolo/$protocoloId/prueba/${tipo.name}" + (pruebaId?.let { "?pruebaId=$it" } ?: "")
    }

    data object Instrumentos : Ruta("instrumentos")
    data object InstrumentoNuevo : Ruta("nuevo-instrumento")
    data object InstrumentoDetalle : Ruta("instrumento/{id}") { fun crear(id: Long) = "instrumento/$id" }
    data object InstrumentoEditar : Ruta("instrumento/{id}/editar") { fun crear(id: Long) = "instrumento/$id/editar" }
    data object Usuarios : Ruta("usuarios")
    data object UsuarioNuevo : Ruta("nueva-cuenta")
    data object UsuarioDetalle : Ruta("usuario/{id}") { fun crear(id: Long) = "usuario/$id" }
    data object UsuarioEditar : Ruta("usuario/{id}/editar") { fun crear(id: Long) = "usuario/$id/editar" }
}
