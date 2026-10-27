package cl.duocuc.induztek.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = AzulInduztek,
    onPrimary = Blanco,
    primaryContainer = AzulClaro,
    onPrimaryContainer = AzulOscuro,
    secondary = Ambar,
    onSecondary = TextoOscuro,
    secondaryContainer = AmbarClaro,
    onSecondaryContainer = AmbarOscuro,
    tertiary = VerdeAprobado,
    onTertiary = Blanco,
    background = FondoClaro,
    onBackground = TextoOscuro,
    surface = SuperficieClara,
    onSurface = TextoOscuro,
    surfaceVariant = SuperficieVariante,
    onSurfaceVariant = TextoSecundario,
    outline = GrisBorde,
    error = RojoRechazado,
    onError = Blanco,
    errorContainer = RojoRechazadoFondo,
    onErrorContainer = RojoRechazado
)

private val EsquemaOscuro = darkColorScheme(
    primary = AzulNoche,
    onPrimary = AzulOscuro,
    primaryContainer = AzulOscuro,
    onPrimaryContainer = AzulClaro,
    secondary = Ambar,
    onSecondary = TextoOscuro,
    secondaryContainer = AmbarOscuro,
    onSecondaryContainer = AmbarClaro,
    tertiary = VerdeAprobadoFondo,
    onTertiary = VerdeAprobado,
    background = FondoOscuro,
    onBackground = TextoClaro,
    surface = SuperficieOscura,
    onSurface = TextoClaro,
    surfaceVariant = SuperficieOscuraVariante,
    onSurfaceVariant = GrisBorde,
    outline = TextoSecundario,
    error = RojoRechazadoFondo,
    onError = RojoRechazado,
    errorContainer = RojoRechazado,
    onErrorContainer = RojoRechazadoFondo
)

private val Formas = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(Dimens.radioCampo),
    large = RoundedCornerShape(Dimens.radioTarjeta)
)

@Composable
fun InduztekTheme(
    preferencia: String = "sistema",
    content: @Composable () -> Unit
) {
    val oscuro = when (preferencia) {
        "claro" -> false
        "oscuro" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = Tipografia,
        shapes = Formas,
        content = content
    )
}
