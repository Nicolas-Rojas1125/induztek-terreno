package cl.duocuc.induztek.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.util.Formato
import cl.duocuc.induztek.ui.theme.Dimens
import cl.duocuc.induztek.ui.theme.InduztekTheme
import cl.duocuc.induztek.ui.theme.VerdeAprobado
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class EstadoCampo { NEUTRO, ERROR, VALIDO }

@Composable
private fun coloresCampo(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorLeadingIconColor = MaterialTheme.colorScheme.error,
    errorSupportingTextColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.error
)

@Composable
private fun IconoEstado(error: String?, valido: Boolean) {
    val estado = when {
        error != null -> EstadoCampo.ERROR
        valido -> EstadoCampo.VALIDO
        else -> EstadoCampo.NEUTRO
    }
    Crossfade(targetState = estado, label = "iconoEstadoCampo") { e ->
        when (e) {
            EstadoCampo.ERROR -> Icon(
                painterResource(R.drawable.ic_error), contentDescription = "Campo con error",
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(Dimens.tamanoIcono)
            )
            EstadoCampo.VALIDO -> Icon(
                painterResource(R.drawable.ic_check), contentDescription = "Campo válido",
                tint = VerdeAprobado, modifier = Modifier.size(Dimens.tamanoIcono)
            )
            EstadoCampo.NEUTRO -> Unit
        }
    }
}

@Composable
fun CampoTexto(
    valor: String,
    onValorCambia: (String) -> Unit,
    etiqueta: String,
    @DrawableRes icono: Int,
    modifier: Modifier = Modifier,
    error: String? = null,
    valido: Boolean = false,
    ayuda: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    capitalizacion: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    accionTeclado: ImeAction = ImeAction.Next,
    sufijo: String? = null,
    lineas: Int = 1,
    habilitado: Boolean = true,
    transformacion: VisualTransformation = VisualTransformation.None,
    iconoFinal: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambia,
        modifier = modifier.fillMaxWidth(),
        enabled = habilitado,
        label = { Text(etiqueta) },
        leadingIcon = {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(Dimens.tamanoIcono))
        },
        trailingIcon = {
            if (iconoFinal != null) iconoFinal() else IconoEstado(error, valido)
        },
        suffix = if (sufijo != null) {
            { Text(sufijo, style = MaterialTheme.typography.labelLarge) }
        } else null,
        supportingText = if (error != null || ayuda != null) {
            { Text(error ?: ayuda.orEmpty()) }
        } else null,
        isError = error != null,
        singleLine = lineas == 1,
        minLines = if (lineas > 1) lineas else 1,
        maxLines = if (lineas > 1) lineas + 3 else 1,
        visualTransformation = transformacion,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalizacion,
            keyboardType = tipoTeclado,
            imeAction = if (lineas > 1) ImeAction.Default else accionTeclado
        ),
        shape = MaterialTheme.shapes.medium,
        colors = coloresCampo()
    )
}

@Composable
fun CampoContrasena(
    valor: String,
    onValorCambia: (String) -> Unit,
    etiqueta: String,
    visible: Boolean,
    onAlternarVisible: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    accionTeclado: ImeAction = ImeAction.Done
) {
    CampoTexto(
        valor = valor,
        onValorCambia = onValorCambia,
        etiqueta = etiqueta,
        icono = R.drawable.ic_candado,
        modifier = modifier,
        error = error,
        tipoTeclado = KeyboardType.Password,
        capitalizacion = KeyboardCapitalization.None,
        accionTeclado = accionTeclado,
        transformacion = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        iconoFinal = {
            IconButton(onClick = onAlternarVisible) {
                Icon(
                    painterResource(if (visible) R.drawable.ic_ojo_tachado else R.drawable.ic_ojo),
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                    modifier = Modifier.size(Dimens.tamanoIcono)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CampoSeleccion(
    etiqueta: String,
    opciones: List<T>,
    seleccion: T?,
    onSeleccionar: (T) -> Unit,
    texto: (T) -> String,
    @DrawableRes icono: Int,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    habilitado: Boolean = true,
    detalle: ((T) -> String?)? = null
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido && habilitado,
        onExpandedChange = { if (habilitado) expandido = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = seleccion?.let(texto) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text(etiqueta) },
            leadingIcon = {
                Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(Dimens.tamanoIcono))
            },
            trailingIcon = {
                if (error != null) IconoEstado(error, false)
                else ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido)
            },
            supportingText = if (error != null || ayuda != null) {
                { Text(error ?: ayuda.orEmpty()) }
            } else null,
            isError = error != null,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = coloresCampo(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, habilitado)
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            if (opciones.isEmpty()) {
                DropdownMenuItem(text = { Text("Sin opciones disponibles") }, onClick = { expandido = false }, enabled = false)
            }
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(texto(opcion), style = MaterialTheme.typography.bodyLarge)
                            detalle?.invoke(opcion)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    onClick = {
                        onSeleccionar(opcion)
                        expandido = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    etiqueta: String,
    fecha: LocalDate?,
    onFecha: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    permitirFuturas: Boolean = true,
    permitirPasadas: Boolean = true,
    habilitado: Boolean = true
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = fecha?.let { Formato.fecha(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text(etiqueta) },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_calendario), contentDescription = null, modifier = Modifier.size(Dimens.tamanoIcono))
            },
            trailingIcon = { IconoEstado(error, fecha != null && error == null) },
            supportingText = if (error != null) {
                { Text(error) }
            } else null,
            isError = error != null,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = if (error != null) 20.dp else 0.dp)
                .clickable(enabled = habilitado) { abierto = true }
        )
    }
    if (abierto) {
        val hoyUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    (permitirFuturas || utcTimeMillis <= hoyUtc) && (permitirPasadas || utcTimeMillis >= hoyUtc)
            }
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        onFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CamposPreview() {
    InduztekTheme {
        Column(Modifier.padding(16.dp)) {
            CampoTexto("14.226.791-7", {}, "RUT", R.drawable.ic_credencial, error = "RUT inválido: revise el dígito verificador (RN-03)")
            CampoTexto("OT-2026-1187", {}, "Número de OT", R.drawable.ic_numero, valido = true)
            CampoTexto("6100", {}, "Lectura a 1 min", R.drawable.ic_medidor, sufijo = "MΩ", valido = true)
        }
    }
}
