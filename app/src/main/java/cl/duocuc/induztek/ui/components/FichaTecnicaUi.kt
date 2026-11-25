package cl.duocuc.induztek.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duocuc.induztek.R
import cl.duocuc.induztek.domain.model.FichaTecnica
import cl.duocuc.induztek.domain.model.TipoCampoTecnico
import cl.duocuc.induztek.domain.model.TipoEquipo

@Composable
fun FormularioFichaTecnica(
    tipo: TipoEquipo,
    valores: Map<String, String>,
    errorDe: (String) -> String?,
    completitud: Int,
    onCambio: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Datos técnicos de ${tipo.etiqueta.lowercase()} · $completitud % completo",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(progress = { completitud / 100f }, modifier = Modifier.fillMaxWidth())
        FichaTecnica.campos(tipo).forEach { campo ->
            val valor = valores[campo.clave].orEmpty()
            val etiqueta = campo.etiqueta + if (campo.obligatorio) " *" else ""
            when (campo.tipo) {
                TipoCampoTecnico.OPCION -> CampoSeleccion(
                    etiqueta = etiqueta,
                    opciones = campo.opciones,
                    seleccion = valor.takeIf { it.isNotBlank() },
                    onSeleccionar = { onCambio(campo.clave, it) },
                    texto = { if (campo.unidad.isBlank()) it else "$it ${campo.unidad}" },
                    icono = R.drawable.ic_ficha,
                    error = errorDe(campo.clave),
                    ayuda = campo.ayuda
                )
                else -> CampoTexto(
                    valor = valor,
                    onValorCambia = { onCambio(campo.clave, it) },
                    etiqueta = etiqueta,
                    icono = if (campo.tipo == TipoCampoTecnico.TEXTO) R.drawable.ic_etiqueta else R.drawable.ic_medidor,
                    error = errorDe(campo.clave),
                    valido = valor.isNotBlank() && errorDe(campo.clave) == null,
                    ayuda = campo.ayuda,
                    tipoTeclado = when (campo.tipo) {
                        TipoCampoTecnico.ENTERO -> KeyboardType.Number
                        TipoCampoTecnico.NUMERO -> KeyboardType.Decimal
                        else -> KeyboardType.Text
                    },
                    capitalizacion = KeyboardCapitalization.Characters,
                    sufijo = campo.unidad.ifBlank { null }
                )
            }
        }
        Text(
            "* Obligatorio para este tipo de equipo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DetalleFichaTecnica(tipo: TipoEquipo, datos: Map<String, String>) {
    val campos = FichaTecnica.campos(tipo)
    val llenos = campos.filter { !datos[it.clave].isNullOrBlank() }
    if (llenos.isEmpty()) {
        Text(
            "Sin datos técnicos registrados. Edite el equipo para completarlos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        llenos.forEach { c -> FilaDato(c.etiqueta, FichaTecnica.textoVisible(c, datos[c.clave].orEmpty())) }
        val faltan = campos.size - llenos.size
        if (faltan > 0) {
            Text(
                "Faltan $faltan dato(s) de la ficha (${FichaTecnica.completitud(tipo, datos)} % completo)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
