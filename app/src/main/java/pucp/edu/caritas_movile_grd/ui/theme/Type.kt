package pucp.edu.caritas_movile_grd.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tipografías definidas en el UIKit
// Nota: Para usar Omnes y Asap, debes agregar los archivos .ttf en res/font
// y definirlos aquí. Por ahora usamos Default.
val AppFontFamily = FontFamily.Default // Cambiar a Asap cuando se agregue
val TitleFontFamily = FontFamily.Default // Cambiar a Omnes cuando se agregue

val Typography = Typography(
    // Display - Omnes 32px 700 1.2
    displayLarge = TextStyle(
        fontFamily = TitleFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = (32 * 1.2).sp
    ),
    // H1 - Asap 24px 700 1.3
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = (24 * 1.3).sp
    ),
    // H2 - Asap 20px 600 1.35
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = (20 * 1.35).sp
    ),
    // H3 - Asap 16px 600 1.4
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = (16 * 1.4).sp
    ),
    // Body - Asap 14px 400 1.6
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = (14 * 1.6).sp
    ),
    // Body small - Asap 13px 400 1.5
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = (13 * 1.5).sp
    ),
    // Label - Asap 13px 500 1.4
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = (13 * 1.4).sp
    ),
    // Caption - Asap 12px 400 1.4
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = (12 * 1.4).sp
    )
)
