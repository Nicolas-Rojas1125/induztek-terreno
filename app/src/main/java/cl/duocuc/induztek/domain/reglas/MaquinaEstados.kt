package cl.duocuc.induztek.domain.reglas

import cl.duocuc.induztek.domain.model.EstadoProtocolo
import cl.duocuc.induztek.domain.model.Permiso
import cl.duocuc.induztek.domain.model.Prueba
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.TipoPrueba

enum class AccionProtocolo(val etiqueta: String, val destino: EstadoProtocolo, val requiereMotivo: Boolean) {
    REGISTRAR_TERRENO("Pasar a Registrado en terreno", EstadoProtocolo.REGISTRADO_TERRENO, false),
    FORMALIZAR("Formalizar", EstadoProtocolo.FORMALIZADO, false),
    OBSERVAR("Devolver con observaciones", EstadoProtocolo.OBSERVADO, true),
    ANULAR("Anular protocolo", EstadoProtocolo.ANULADO, true)
}

data class ContextoTransicion(
    val estado: EstadoProtocolo,
    val sesion: Sesion,
    val idAutor: Long,
    val temperatura: Double?,
    val humedad: Double?,
    val pruebas: List<Prueba>,
    val tiposRequeridos: List<TipoPrueba>,
    val hayVariacionSignificativa: Boolean,
    val comentarioVariacion: String,
    val motivo: String = ""
)

object MaquinaEstados {
    const val LARGO_MINIMO_MOTIVO = 10

    fun puedeEditar(estado: EstadoProtocolo, sesion: Sesion, idAutor: Long): Boolean {
        if (!sesion.puede(Permiso.REGISTRAR_PROTOCOLO)) return false
        if (estado != EstadoProtocolo.BORRADOR && estado != EstadoProtocolo.OBSERVADO) return false
        return sesion.rol != RolCodigo.TECNICO || sesion.idUsuario == idAutor
    }

    fun accionesDisponibles(estado: EstadoProtocolo, sesion: Sesion, idAutor: Long): List<AccionProtocolo> =
        AccionProtocolo.entries.filter { permitidaPorRolYEstado(it, estado, sesion, idAutor) }

    private fun permitidaPorRolYEstado(
        accion: AccionProtocolo, estado: EstadoProtocolo, sesion: Sesion, idAutor: Long
    ): Boolean = when (accion) {
        AccionProtocolo.REGISTRAR_TERRENO ->
            (estado == EstadoProtocolo.BORRADOR || estado == EstadoProtocolo.OBSERVADO) &&
                puedeEditar(estado, sesion, idAutor)
        AccionProtocolo.FORMALIZAR, AccionProtocolo.OBSERVAR ->
            estado == EstadoProtocolo.REGISTRADO_TERRENO && sesion.puede(Permiso.FORMALIZAR_OBSERVAR)
        AccionProtocolo.ANULAR ->
            estado != EstadoProtocolo.FORMALIZADO && estado != EstadoProtocolo.ANULADO &&
                sesion.puede(Permiso.ANULAR_PROTOCOLO)
    }

    fun validar(accion: AccionProtocolo, ctx: ContextoTransicion): List<String> {
        val errores = mutableListOf<String>()
        if (!permitidaPorRolYEstado(accion, ctx.estado, ctx.sesion, ctx.idAutor)) {
            errores += when (accion) {
                AccionProtocolo.FORMALIZAR, AccionProtocolo.OBSERVAR ->
                    "Solo un Ingeniero de pruebas o el Jefe técnico puede ${accion.etiqueta.lowercase()} (HU-10)"
                AccionProtocolo.ANULAR ->
                    if (ctx.estado == EstadoProtocolo.FORMALIZADO)
                        "Un protocolo Formalizado no se puede anular: emita un reemplazo (RN-04)"
                    else "No tiene permiso para anular protocolos"
                AccionProtocolo.REGISTRAR_TERRENO ->
                    "Solo el técnico que creó el protocolo puede modificarlo, en Borrador u Observado (RN-17)"
            }
            return errores
        }
        if (accion.requiereMotivo && ctx.motivo.trim().length < LARGO_MINIMO_MOTIVO) {
            errores += "El motivo es obligatorio (mínimo $LARGO_MINIMO_MOTIVO caracteres)"
        }
        when (accion) {
            AccionProtocolo.REGISTRAR_TERRENO -> {
                if (ctx.temperatura == null) errores += "Falta la temperatura ambiente (HU-02)"
                if (ctx.humedad == null) errores += "Falta la humedad relativa (HU-02)"
                val completas = ctx.pruebas.filter { it.realizada && it.resultado != null }
                if (completas.isEmpty()) errores += "Debe registrar al menos una prueba completa (RN-04)"
                val faltantes = tiposSinRegistro(ctx.tiposRequeridos, ctx.pruebas)
                if (faltantes.isNotEmpty()) {
                    errores += "Marque como realizada o No realizada: " +
                        faltantes.joinToString { it.etiqueta } + " (Aclaración nº 2)"
                }
                val sinResultado = ctx.pruebas.filter { it.realizada && it.resultado == null }
                if (sinResultado.isNotEmpty()) {
                    errores += "Hay ${sinResultado.size} prueba(s) sin resultado"
                }
            }
            AccionProtocolo.FORMALIZAR -> {
                if (ctx.hayVariacionSignificativa && ctx.comentarioVariacion.isBlank()) {
                    errores += "Hay variaciones significativas: deje un comentario antes de formalizar (RN-09)"
                }
            }
            else -> Unit
        }
        return errores
    }

    fun tiposSinRegistro(requeridos: List<TipoPrueba>, pruebas: List<Prueba>): List<TipoPrueba> =
        requeridos.filter { tipo -> pruebas.none { it.tipo == tipo } }
}
