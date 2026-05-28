package pucp.edu.caritas_movile_grd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary                = CaritasGreen,
    onPrimary              = CaritasOnGreen,
    primaryContainer       = CaritasGreenLight,
    onPrimaryContainer     = CaritasOnGreenDark,

    secondary              = CaritasTeal,
    onSecondary            = CaritasOnTeal,
    secondaryContainer     = CaritasTealLight,
    onSecondaryContainer   = CaritasOnTealDark,

    tertiary               = CaritasAmber,
    onTertiary             = CaritasOnAmber,
    tertiaryContainer      = CaritasAmberLight,
    onTertiaryContainer    = CaritasOnAmberDark,

    background             = CaritasBackground,
    onBackground           = CaritasOnGreenDark,

    surface                = CaritasSurface,
    onSurface              = CaritasOnGreenDark,
    surfaceVariant         = CaritasGreenLight,
    onSurfaceVariant       = CaritasGreenDark,

    outline                = CaritasOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary                = CaritasGreenDark80,
    onPrimary              = CaritasOnGreenDark,
    primaryContainer       = CaritasGreenDark,
    onPrimaryContainer     = CaritasGreenLight,

    secondary              = CaritasTealDark80,
    onSecondary            = CaritasOnTealDark,
    secondaryContainer     = CaritasTeal,
    onSecondaryContainer   = CaritasTealLight,

    tertiary               = CaritasAmberDark80,
    onTertiary             = CaritasOnAmberDark,
    tertiaryContainer      = CaritasAmber,
    onTertiaryContainer    = CaritasAmberLight,
)

@Composable
fun Caritas_Movile_GRDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // dynamicColor desactivado — queremos siempre los colores Cáritas
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
