package cl.duocuc.induztek.ui.screens.equipos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.Clasificacion
import cl.duocuc.induztek.domain.model.TipoEquipo
import cl.duocuc.induztek.domain.reglas.ComparadorHistorico
import cl.duocuc.induztek.domain.reglas.VisibilidadCliente
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.ui.components.AparicionEscalonada
import cl.duocuc.induztek.ui.components.AvisoInformativo
import cl.duocuc.induztek.ui.components.BotonPrincipal
import cl.duocuc.induztek.ui.components.BotonSecundario
import cl.duocuc.induztek.ui.components.CampoSeleccion
import cl.duocuc.induztek.ui.components.CampoTexto
import cl.duocuc.induztek.ui.components.Cargando
import cl.duocuc.induztek.ui.components.ChipEstado
import cl.duocuc.induztek.ui.components.ChipResultado
import cl.duocuc.induztek.ui.components.EstadoVacio
import cl.duocuc.induztek.ui.components.FilaDato
import cl.duocuc.induztek.ui.components.FotoPlaca
import cl.duocuc.induztek.ui.components.GraficoTendencia
import cl.duocuc.induztek.ui.components.MostrarMensaje
import cl.duocuc.induztek.ui.components.PantallaBase
import cl.duocuc.induztek.ui.components.TarjetaLista
import cl.duocuc.induztek.ui.components.TarjetaSeccion
import cl.duocuc.induztek.ui.components.rememberCapturaFoto
import cl.duocuc.induztek.ui.components.rememberSacudida
import cl.duocuc.induztek.ui.components.sacudir
import cl.duocuc.induztek.ui.theme.AmbarObservado
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.RojoRechazado
import cl.duocuc.induztek.viewmodel.EquipoDetalleViewModel
import cl.duocuc.induztek.viewmodel.EquipoFormViewModel
import cl.duocuc.induztek.viewmodel.EquiposViewModel

@Composable
fun EquiposScreen(
    barraInferior: @Composable () -> Unit,
    onEquipo: (Long) -> Unit,
    viewModel: EquiposViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    PantallaBase(
        titulo = if (ui.esCliente) "Equipos de mi empresa" else "Equipos",
        subtitulo = "${ui.equipos.size} resultado(s)",
        barraInferior = barraInferior
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CampoTexto(
                    valor = ui.busqueda, onValorCambia = viewModel::onBuscar,
                    etiqueta = "Buscar por TAG, Nº de serie o instalación", icono = R.drawable.ic_buscar,
                    capitalizacion = KeyboardCapitalization.Characters
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Clasificacion.entries.forEach { cl ->
                        FilterChip(
                            selected = ui.clasificacion == cl,
                            onClick = { viewModel.onClasificacion(cl) },
                            label = { Text(cl.etiqueta) },
                            leadingIcon = if (ui.clasificacion == cl) {
                                { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }
            if (!ui.cargando && ui.equipos.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_buscar, "Sin equipos", "No hay equipos que coincidan con la búsqueda.") }
            }
            items(ui.equipos, key = { it.id }) { e ->
                AparicionEscalonada(ui.equipos.indexOf(e), Modifier.animateItem()) {
                    TarjetaLista(
                        titulo = "${e.tag} · ${e.tipo.etiqueta}",
                        subtitulo = "${e.nombreInstalacion} · ${e.nombreCliente}",
                        detalle = "Nº ${e.numeroSerie} · " +
                            (e.ultimaPrueba?.let { "Última prueba ${Formato.fechaDesdeMillis(it)}" } ?: "Sin pruebas"),
                        icono = R.drawable.ic_rayo,
                        colorIcono = if (e.tipo.clasificacion == Clasificacion.SECUNDARIO) AmbarObservado else MaterialTheme.colorScheme.primary,
                        onClick = { onEquipo(e.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun EquipoFormScreen(
    onVolver: () -> Unit,
    onGuardado: (Long) -> Unit,
    onUsarExistente: (Long) -> Unit,
    viewModel: EquipoFormViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    LaunchedEffect(ui.guardadoId) { ui.guardadoId?.let(onGuardado) }
    val sacudida = rememberSacudida(ui.intentosFallidos)
    val tomarFoto = rememberCapturaFoto(onFoto = viewModel::onFotoTomada, onAviso = viewModel::onAvisoCamara)
    val t = ui.tipo

    PantallaBase(titulo = "Registrar equipo", subtitulo = ui.instalacion?.let { "${it.nombre} · ${it.nombreCliente}" }, onVolver = onVolver, snackbar = snackbar) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarjetaSeccion("Identificación", icono = R.drawable.ic_etiqueta, modifier = Modifier.sacudir(sacudida)) {
                CampoTexto(
                    ui.tag.valor, viewModel::onTag, "TAG del cliente (ej.: TR-01)", R.drawable.ic_etiqueta,
                    modifier = Modifier.onFocusChanged { if (!it.isFocused && ui.tag.tocado) viewModel.verificarTag() },
                    error = ui.tag.errorVisible, valido = ui.tag.valido && ui.duplicado == null,
                    capitalizacion = KeyboardCapitalization.Characters,
                    ayuda = "Único dentro de la instalación (RN-01)"
                )
                AnimatedVisibility(visible = ui.duplicado != null) {
                    val dup = ui.duplicado
                    if (dup != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AvisoInformativo(
                                "El equipo ${dup.tag} (${dup.tipo.etiqueta}, Nº ${dup.numeroSerie}) ya existe en esta instalación.",
                                advertencia = true
                            )
                            BotonSecundario("Usar el equipo registrado", { onUsarExistente(dup.id) }, Modifier.fillMaxWidth(), icono = R.drawable.ic_check)
                        }
                    }
                }
                CampoSeleccion(
                    etiqueta = "Tipo de equipo", opciones = TipoEquipo.entries, seleccion = t, onSeleccionar = viewModel::onTipo,
                    texto = { it.etiqueta }, icono = R.drawable.ic_rayo, error = ui.errorTipo,
                    detalle = { "${it.clasificacion.etiqueta} · Pruebas: ${it.pruebas.joinToString { p -> p.etiqueta }}" }
                )
            }

            TarjetaSeccion("Placa de características", icono = R.drawable.ic_documento) {
                CampoTexto(ui.marca.valor, viewModel::onMarca, "Marca", R.drawable.ic_empresa,
                    error = ui.marca.errorVisible, valido = ui.marca.valido, capitalizacion = KeyboardCapitalization.Words)
                CampoTexto(ui.modelo.valor, viewModel::onModelo, "Modelo (opcional)", R.drawable.ic_etiqueta, error = ui.modelo.errorVisible)
                CampoTexto(ui.serie.valor, viewModel::onSerie, "Número de serie", R.drawable.ic_numero,
                    error = ui.serie.errorVisible, valido = ui.serie.valido, capitalizacion = KeyboardCapitalization.Characters)
                CampoTexto(ui.anio.valor, viewModel::onAnio, "Año de fabricación", R.drawable.ic_calendario,
                    error = ui.anio.errorVisible, valido = ui.anio.valido, tipoTeclado = KeyboardType.Number)
                AnimatedVisibility(visible = t != null && t != TipoEquipo.RELE) {
                    CampoTexto(ui.tensionPrimaria.valor, viewModel::onTensionPrimaria,
                        if (t?.usaTensionSecundaria == true) "Tensión primaria" else "Tensión nominal", R.drawable.ic_rayo,
                        error = ui.tensionPrimaria.errorVisible, valido = ui.tensionPrimaria.valido,
                        tipoTeclado = KeyboardType.Decimal, sufijo = "kV")
                }
                AnimatedVisibility(visible = t?.usaTensionSecundaria == true) {
                    CampoTexto(ui.tensionSecundaria.valor, viewModel::onTensionSecundaria, "Tensión secundaria", R.drawable.ic_rayo,
                        error = ui.tensionSecundaria.errorVisible, valido = ui.tensionSecundaria.valido,
                        tipoTeclado = KeyboardType.Decimal, sufijo = "kV")
                }
                AnimatedVisibility(visible = t?.usaPotencia == true) {
                    CampoTexto(ui.potencia.valor, viewModel::onPotencia, "Potencia", R.drawable.ic_medidor,
                        error = ui.potencia.errorVisible, valido = ui.potencia.valido, tipoTeclado = KeyboardType.Decimal, sufijo = "kVA")
                }
                AnimatedVisibility(visible = t?.usaCorriente == true) {
                    CampoTexto(ui.corriente.valor, viewModel::onCorriente,
                        if (t == TipoEquipo.RELE) "Corriente de ajuste" else "Corriente nominal", R.drawable.ic_medidor,
                        error = ui.corriente.errorVisible, valido = ui.corriente.valido, tipoTeclado = KeyboardType.Decimal, sufijo = "A")
                }
                AnimatedVisibility(visible = t?.usaMaxContactos == true) {
                    CampoTexto(ui.maxContactos.valor, viewModel::onMaxContactos, "Máx. resistencia de contactos", R.drawable.ic_medidor,
                        error = ui.maxContactos.errorVisible, valido = ui.maxContactos.valido, tipoTeclado = KeyboardType.Decimal,
                        sufijo = "µΩ", ayuda = "Valor de placa del fabricante (referencia RN-08)")
                }
                CampoTexto(ui.observaciones.valor, viewModel::onObservaciones, "Observaciones (opcional)", R.drawable.ic_info,
                    error = ui.observaciones.errorVisible, lineas = 2)
            }

            TarjetaSeccion("Foto de la placa (cámara)", icono = R.drawable.ic_camara) {
                Text(
                    "Evita errores al copiar la placa a mano (PB-03): el ingeniero puede verificar los datos en oficina.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ui.fotoPlaca?.let { FotoPlaca(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonSecundario(
                        if (ui.fotoPlaca == null) "Tomar foto" else "Repetir foto", tomarFoto,
                        Modifier.weight(1f), icono = R.drawable.ic_camara
                    )
                    if (ui.fotoPlaca != null) {
                        BotonSecundario("Quitar", viewModel::quitarFoto, Modifier.weight(0.6f), icono = R.drawable.ic_eliminar, color = RojoRechazado)
                    }
                }
            }
            BotonPrincipal("Guardar equipo", viewModel::guardar, Modifier.fillMaxWidth(), icono = R.drawable.ic_check, cargando = ui.guardando)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun EquipoDetalleScreen(
    onVolver: () -> Unit,
    onProtocolo: (Long) -> Unit,
    viewModel: EquipoDetalleViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(mensaje, snackbar, viewModel::onMensajeMostrado)
    val tomarFoto = rememberCapturaFoto(onFoto = viewModel::onFotoTomada, onAviso = viewModel::onAvisoCamara)
    val e = ui.equipo
    val s = ui.sesion

    PantallaBase(
        titulo = e?.let { "${it.tag} · ${it.tipo.etiqueta}" } ?: "Equipo",
        subtitulo = e?.let { "${it.nombreInstalacion} · ${it.nombreCliente}" },
        onVolver = onVolver,
        snackbar = snackbar,
        acciones = {
            if (s != null && !s.esCliente && e != null) {
                IconButton(onClick = tomarFoto) { Icon(painterResource(R.drawable.ic_camara), contentDescription = "Foto de la placa") }
            }
        }
    ) { padding ->
        if (ui.cargando) { Cargando(Modifier.padding(padding)); return@PantallaBase }
        if (e == null || s == null) {
            EstadoVacio(R.drawable.ic_bloqueo, "Equipo no disponible", "No existe o no tiene acceso (RN-18).", Modifier.padding(padding))
            return@PantallaBase
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                TarjetaSeccion("Placa de características", icono = R.drawable.ic_documento) {
                    e.fotoPlaca?.let { FotoPlaca(it) }
                    FilaDato("Clasificación", e.tipo.clasificacion.etiqueta)
                    FilaDato("Marca / modelo", "${e.marca} ${e.modelo}".trim())
                    FilaDato("Nº de serie", e.numeroSerie)
                    FilaDato("Año", e.anioFabricacion?.toString() ?: "")
                    e.potenciaKva?.let { FilaDato("Potencia", "${Formato.numeroCompacto(it)} kVA") }
                    e.tensionPrimariaKv?.let { p ->
                        FilaDato("Tensiones", "${Formato.numeroCompacto(p)}" + (e.tensionSecundariaKv?.let { " / ${Formato.numeroCompacto(it)}" } ?: "") + " kV")
                    }
                    e.corrienteNominalA?.let { FilaDato("Corriente", "${Formato.numeroCompacto(it)} A") }
                    e.maxContactosUohm?.let { FilaDato("Máx. contactos", "${Formato.numeroCompacto(it)} µΩ") }
                    if (e.observaciones.isNotBlank()) FilaDato("Observaciones", e.observaciones)
                    FilaDato("Última prueba", e.ultimaPrueba?.let { Formato.fechaDesdeMillis(it) } ?: "Sin pruebas")
                }
            }

            if (ui.series.isNotEmpty()) {
                item {
                    TarjetaSeccion("Evolución de mediciones", icono = R.drawable.ic_tendencia) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ui.series) { (tipo, elemento) ->
                                FilterChip(
                                    selected = ui.serie?.tipo == tipo && ui.serie?.elemento == elemento,
                                    onClick = { viewModel.seleccionarSerie(tipo, elemento) },
                                    label = { Text("${tipo.name.lowercase().replaceFirstChar { it.uppercase() }} · $elemento") }
                                )
                            }
                        }
                        val sel = ui.serie
                        if (sel != null && sel.tipo.parametrosPrincipales.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                sel.tipo.parametrosPrincipales.forEach { p ->
                                    FilterChip(
                                        selected = sel.parametro == p,
                                        onClick = { viewModel.seleccionarSerie(sel.tipo, sel.elemento, p) },
                                        label = { Text(p.etiqueta) }
                                    )
                                }
                            }
                        }
                        if (sel != null) {
                            Text(
                                "${sel.parametro.etiquetaConUnidad} · ${sel.tipo.etiqueta} ${sel.elemento}",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        GraficoTendencia(ui.puntos)
                        ui.puntos.forEachIndexed { i, p ->
                            val anterior = ui.puntos.getOrNull(i - 1)
                            val variacion = anterior?.let { ComparadorHistorico.variacion(p.valor, it.valor) }
                            val significativa = variacion?.let { ComparadorHistorico.esSignificativa(it) } == true
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("${p.numeroProtocolo} · ${Formato.fechaDesdeMillis(p.fechaHora)}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${Formato.numeroCompacto(p.valor)} ${sel?.parametro?.unidad?.simbolo.orEmpty()}  ·  " +
                                            (variacion?.let { Formato.porcentajeConSigno(it) } ?: "Sin historial previo"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (significativa) RojoRechazado else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (significativa) Text("Variación significativa", style = MaterialTheme.typography.labelSmall, color = RojoRechazado)
                                Spacer(Modifier.size(6.dp))
                                ChipResultado(p.resultado)
                            }
                        }
                    }
                }
            }

            item { Text("Historial de protocolos (${ui.historial.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp)) }
            if (ui.historial.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_historial, "Sin protocolos", "Este equipo aún no tiene pruebas registradas (sin historial previo).") }
            }
            items(ui.historial, key = { it.id }) { p ->
                TarjetaLista(
                    titulo = p.numero,
                    subtitulo = "${Formato.fechaHora(p.fechaHoraPrueba)} · ${p.nombreTecnico}",
                    detalle = "OT ${p.numeroOt}" + (p.reemplazadoPor?.let { " · Reemplazado por nº $it" } ?: ""),
                    icono = R.drawable.ic_documento,
                    onClick = { onProtocolo(p.id) },
                    pie = {
                        ChipEstado(p.estado, etiquetaCliente = if (s.esCliente) VisibilidadCliente.etiquetaPara(p.estado) else null)
                        ChipResultado(p.resultadoGlobal)
                    }
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
