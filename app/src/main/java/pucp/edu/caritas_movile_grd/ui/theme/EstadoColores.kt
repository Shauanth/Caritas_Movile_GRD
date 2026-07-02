package pucp.edu.caritas_movile_grd.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

/**
 * Identidad visual de un estado: única fuente de verdad compartida por el flujo
 * GRD (incidencias) y por Simulacros, para que los colores estén alineados.
 *
 * @property bg      fondo suave (tarjetas, chips sin seleccionar)
 * @property solido  color fuerte (franjas, íconos en badge, chip seleccionado)
 * @property texto   color de texto sobre [bg]
 * @property borde   color de borde sobre [bg]
 */
data class EstadoVisual(
    val bg: Color,
    val solido: Color,
    val texto: Color,
    val borde: Color,
    val icono: ImageVector,
    val etiqueta: String
)

// ── Familias de color (compartidas entre estados equivalentes) ────────────────
private val AMARILLO = listOf(Color(0xFFFFFDE7), Color(0xFFF9A825), Color(0xFFF57F17), Color(0xFFFDD835))
private val AZUL     = listOf(Color(0xFFE3F2FD), Color(0xFF1565C0), Color(0xFF0D47A1), Color(0xFF90CAF9))
private val NARANJA  = listOf(Color(0xFFFFF3E0), Color(0xFFE64A19), Color(0xFFBF360C), Color(0xFFFFAB91))
private val MORADO   = listOf(Color(0xFFF3E5F5), Color(0xFF6A1B9A), Color(0xFF4A148C), Color(0xFFCE93D8))
private val VERDE    = listOf(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFFA5D6A7))
private val CYAN     = listOf(Color(0xFFE0F7FA), Color(0xFF00897B), Color(0xFF006064), Color(0xFF80DEEA))
private val TEAL     = listOf(Color(0xFFE0F2F1), Color(0xFF00695C), Color(0xFF004D40), Color(0xFF80CBC4))
private val GRIS     = listOf(Color(0xFFF5F5F5), Color(0xFF546E7A), Color(0xFF37474F), Color(0xFFB0BEC5))

private fun visual(
    familia: List<Color>,
    icono: ImageVector,
    etiqueta: String
) = EstadoVisual(familia[0], familia[1], familia[2], familia[3], icono, etiqueta)

/** Devuelve la identidad visual de un estado (GRD o Simulacros). */
fun estadoVisual(estadoRaw: String): EstadoVisual = when (estadoRaw.trim().uppercase(Locale.getDefault())) {
    // ── Flujo GRD (incidencias) ──
    "ABIERTO"             -> visual(AMARILLO, Icons.Default.Schedule, "Abierto")
    "ASIGNADO"            -> visual(AZUL, Icons.Default.Assignment, "Asignado")
    "DATA RECOPILADA"     -> visual(NARANJA, Icons.Default.Assessment, "Data")
    "EN EVALUACION"       -> visual(MORADO, Icons.Default.CheckCircle, "Evaluación")
    "APROBADO"            -> visual(VERDE, Icons.Default.CheckCircle, "Aprobado")
    "ATENDIDO"            -> visual(CYAN, Icons.Default.Favorite, "Atendido")
    "SEGUIMIENTO ABIERTO" -> visual(TEAL, Icons.Default.TrackChanges, "Seguimiento")
    "CERRADO"             -> visual(GRIS, Icons.Default.Cancel, "Cerrado")
    "RECHAZADO"           -> visual(GRIS, Icons.Default.Cancel, "Rechazado")

    // ── Flujo Simulacros (alineado a las familias de GRD) ──
    "PROGRAMADA"          -> visual(AMARILLO, Icons.Default.Event, "Programada")
    "ASIGNADA"            -> visual(AZUL, Icons.Default.Assignment, "Asignada")
    "EJECUTADA"           -> visual(NARANJA, Icons.Default.PlayCircle, "Ejecutada")
    "OBSERVADA"           -> visual(MORADO, Icons.Default.Visibility, "Observada")
    "VALIDADA"            -> visual(VERDE, Icons.Default.Verified, "Validada")
    "CANCELADA"           -> visual(GRIS, Icons.Default.Cancel, "Cancelada")

    else                  -> visual(GRIS, Icons.Default.Cancel, estadoRaw.trim())
}

/** Ícono representativo del tipo de evento / categoría (compartido GRD ↔ Simulacros). */
fun iconoTipoEvento(nombre: String?): ImageVector {
    val t = nombre?.lowercase(Locale.getDefault()).orEmpty()
    return when {
        "sismo" in t || "terremoto" in t -> Icons.Default.Bolt
        "incendio" in t || "fuego" in t -> Icons.Default.LocalFireDepartment
        "inunda" in t || "tsunami" in t || "lluvia" in t -> Icons.Default.Waves
        "derrumbe" in t || "desliz" in t || "colapso" in t -> Icons.Default.Warning
        else -> Icons.Default.Shield
    }
}
