package cl.duocuc.induztek.data.remote

import cl.duocuc.induztek.domain.model.Calibracion
import cl.duocuc.induztek.domain.model.CategoriaInstrumento
import cl.duocuc.induztek.domain.model.Cliente
import cl.duocuc.induztek.domain.model.Equipo
import cl.duocuc.induztek.domain.model.Instalacion
import cl.duocuc.induztek.domain.model.Instrumento
import cl.duocuc.induztek.domain.model.MatrizPermisos
import cl.duocuc.induztek.domain.model.Pagina
import cl.duocuc.induztek.domain.model.RolCodigo
import cl.duocuc.induztek.domain.model.Sesion
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.model.Usuario
import java.time.LocalDate

data class DatosMaestros(
    val clientes: List<Cliente>,
    val instalaciones: List<Instalacion>,
    val equipos: List<Equipo>,
    val instrumentos: List<Instrumento>
)

data class NuevaCuenta(
    val rut: String,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val cargo: String,
    val rol: RolCodigo,
    val idCliente: Long?,
    val nombreCliente: String?,
    val contrasena: String
)

fun <D, T> PaginaDto<D>.aPagina(mapeo: (D) -> T): Pagina<T> =
    Pagina(contenido.orEmpty().map(mapeo), pagina, maxOf(totalPaginas, 1), totalElementos)

fun UsuarioDto.aUsuario(): Usuario = Usuario(
    id = id,
    rol = RolCodigo.desdeCodigo(rol.orEmpty()),
    rut = rut.orEmpty(),
    nombre = nombre.orEmpty(),
    correo = correo.orEmpty(),
    telefono = telefono.orEmpty(),
    activo = activo ?: true,
    idCliente = idCliente,
    cargo = cargo.orEmpty(),
    nombreCliente = nombreCliente.orEmpty(),
    creadoEn = creadoEn,
    actualizadoEn = actualizadoEn
)

fun UsuarioDto.aSesion(): Sesion {
    val r = RolCodigo.desdeCodigo(rol.orEmpty())
    return Sesion(
        idUsuario = id,
        nombre = nombre.orEmpty(),
        correo = correo.orEmpty(),
        rol = r,
        permisos = MatrizPermisos.permisosDe(r),
        idCliente = idCliente,
        nombreCliente = nombreCliente,
        cargo = cargo,
        rut = rut.orEmpty(),
        telefono = telefono.orEmpty()
    )
}

fun ClienteDto.aCliente(): Cliente = Cliente(
    id = id,
    rut = rut.orEmpty(),
    razonSocial = razonSocial.orEmpty(),
    giro = giro.orEmpty(),
    contactoNombre = contactoNombre.orEmpty(),
    telefono = telefono.orEmpty(),
    correo = correo.orEmpty(),
    direccion = direccion.orEmpty(),
    cantidadInstalaciones = (cantidadInstalaciones ?: 0L).toInt()
)

fun Cliente.aRequest(): ClienteRequest = ClienteRequest(
    rut = rut.trim(),
    razonSocial = razonSocial.trim(),
    giro = giro.trim(),
    contactoNombre = contactoNombre.trim(),
    telefono = telefono.trim(),
    correo = correo.trim().lowercase(),
    direccion = direccion.trim()
)

fun InstalacionDto.aInstalacion(): Instalacion = Instalacion(
    id = id,
    idCliente = idCliente,
    nombre = nombre.orEmpty(),
    ubicacion = ubicacion.orEmpty(),
    idComuna = idComuna,
    observacionesAcceso = observacionesAcceso.orEmpty(),
    latitud = latitud,
    longitud = longitud,
    nombreCliente = nombreCliente.orEmpty(),
    nombreComuna = nombreComuna.orEmpty(),
    nombreRegion = nombreRegion.orEmpty(),
    cantidadEquipos = (cantidadEquipos ?: 0L).toInt()
)

fun Instalacion.aRequest(): InstalacionRequest = InstalacionRequest(
    idCliente = idCliente,
    nombre = nombre.trim(),
    ubicacion = ubicacion.trim(),
    idComuna = idComuna,
    nombreComuna = nombreComuna,
    nombreRegion = nombreRegion,
    observacionesAcceso = observacionesAcceso.trim(),
    latitud = latitud,
    longitud = longitud
)

fun EquipoDto.aEquipo(): Equipo = Equipo(
    id = id,
    idInstalacion = idInstalacion,
    tag = tag.orEmpty(),
    tipo = TipoEquipo.desdeCodigo(tipo.orEmpty()),
    marca = marca.orEmpty(),
    modelo = modelo.orEmpty(),
    numeroSerie = numeroSerie.orEmpty(),
    anioFabricacion = anioFabricacion,
    potenciaKva = potenciaKva,
    tensionPrimariaKv = tensionPrimariaKv,
    tensionSecundariaKv = tensionSecundariaKv,
    corrienteNominalA = corrienteNominalA,
    maxContactosUohm = maxContactosUohm,
    observaciones = observaciones.orEmpty(),
    datosTecnicos = datosTecnicos.orEmpty(),
    nombreInstalacion = nombreInstalacion.orEmpty(),
    idCliente = idCliente,
    nombreCliente = nombreCliente.orEmpty()
)

fun Equipo.aRequest(): EquipoRequest = EquipoRequest(
    idInstalacion = idInstalacion,
    tag = tag.trim().uppercase(),
    tipo = tipo.name,
    marca = marca.trim(),
    modelo = modelo.trim(),
    numeroSerie = numeroSerie.trim().uppercase(),
    anioFabricacion = anioFabricacion,
    potenciaKva = potenciaKva,
    tensionPrimariaKv = tensionPrimariaKv,
    tensionSecundariaKv = tensionSecundariaKv,
    corrienteNominalA = corrienteNominalA,
    maxContactosUohm = maxContactosUohm,
    observaciones = observaciones.trim(),
    datosTecnicos = datosTecnicos
)

private fun fecha(texto: String?): LocalDate? = texto?.takeIf { it.isNotBlank() }?.let {
    try { LocalDate.parse(it) } catch (e: java.time.format.DateTimeParseException) { null }
}

fun InstrumentoDto.aInstrumento(): Instrumento = Instrumento(
    id = id,
    nombre = nombre.orEmpty(),
    categoria = CategoriaInstrumento.desdeCodigo(categoria.orEmpty()),
    marca = marca.orEmpty(),
    modelo = modelo.orEmpty(),
    numeroSerie = numeroSerie.orEmpty(),
    ultimaCalibracion = fecha(ultimaCalibracion),
    vencimiento = fecha(vencimiento),
    calibraciones = calibraciones.orEmpty().mapNotNull { c ->
        val fc = fecha(c.fechaCalibracion)
        val fv = fecha(c.fechaVencimiento)
        if (fc == null || fv == null) null
        else Calibracion(id = c.id, idInstrumento = id, fechaCalibracion = fc, fechaVencimiento = fv, registradaPor = c.registradaPor.orEmpty())
    }
)

fun SyncActivosDto.aDatosMaestros(): DatosMaestros = DatosMaestros(
    clientes = clientes.orEmpty().map { it.aCliente() },
    instalaciones = instalaciones.orEmpty().map { it.aInstalacion() },
    equipos = equipos.orEmpty().map { it.aEquipo() },
    instrumentos = instrumentos.orEmpty().map { it.aInstrumento() }
)

fun descripcionClima(codigo: Int?): String = when (codigo) {
    null -> "Sin información"
    0 -> "Despejado"
    1, 2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Neblina"
    51, 53, 55, 56, 57 -> "Llovizna"
    61, 63, 65, 66, 67, 80, 81, 82 -> "Lluvia"
    71, 73, 75, 77, 85, 86 -> "Nieve"
    95, 96, 99 -> "Tormenta eléctrica"
    else -> "Condición $codigo"
}
