package pucp.edu.caritas_movile_grd.Incidencias

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// ── Mapeos de categoría ──────────────────────────────────────────────────────
val CATEGORIA_MAP = mapOf(
    1 to "Incendios",
    2 to "Inundaciones",
    3 to "Derrumbes",
    4 to "Deslizamientos",
    5 to "Sismos",
    6 to "Tsunamis",
    7 to "Colapso infra.",
    8 to "Pérd. vivienda"
)

val PARROQUIA_MAP = mapOf(
    1 to "Villa María del Triunfo",
    2 to "Independencia",
    3 to "Villa El Salvador",
    4 to "San Juan de Lurigancho",
    5 to "Comas",
    6 to "San Juan Bautista"
)

private val STATUS_LIST = listOf(
    "ABIERTO", "ASIGNADO", "DATA RECOPILADA", "EN EVALUACION",
    "APROBADO", "ATENDIDO", "SEGUIMIENTO ABIERTO", "CERRADO"
)

private val CATEGORIAS_FILTER = listOf(
    "Todas", "Incendios", "Inundaciones", "Derrumbes",
    "Deslizamientos", "Sismos", "Tsunamis"
)

fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))

// ── Color helpers ────────────────────────────────────────────────────────────

/** Color de fondo de la tarjeta de estado */
private fun statusBg(estado: String) = when (estado) {
    "ABIERTO"             -> Color(0xFFFFFDE7)
    "ASIGNADO"            -> Color(0xFFE3F2FD)
    "DATA RECOPILADA"     -> Color(0xFFFFF3E0)
    "EN EVALUACION"       -> Color(0xFFF3E5F5)
    "APROBADO"            -> Color(0xFFE8F5E9)
    "ATENDIDO"            -> Color(0xFFE0F7FA)
    "SEGUIMIENTO ABIERTO" -> Color(0xFFE0F2F1)
    "CERRADO"             -> Color(0xFFF5F5F5)
    else                  -> Color(0xFFF5F5F5)
}

/** Color del ícono/badge dentro de la tarjeta */
private fun statusIconBg(estado: String) = when (estado) {
    "ABIERTO"             -> Color(0xFFF9A825)
    "ASIGNADO"            -> Color(0xFF1565C0)
    "DATA RECOPILADA"     -> Color(0xFFE64A19)
    "EN EVALUACION"       -> Color(0xFF6A1B9A)
    "APROBADO"            -> Color(0xFF2E7D32)
    "ATENDIDO"            -> Color(0xFF00897B)
    "SEGUIMIENTO ABIERTO" -> Color(0xFF00695C)
    "CERRADO"             -> Color(0xFF546E7A)
    else                  -> Color(0xFF546E7A)
}

/** Color del texto de la tarjeta */
private fun statusTextColor(estado: String) = when (estado) {
    "ABIERTO"             -> Color(0xFFF57F17)
    "ASIGNADO"            -> Color(0xFF0D47A1)
    "DATA RECOPILADA"     -> Color(0xFFBF360C)
    "EN EVALUACION"       -> Color(0xFF4A148C)
    "APROBADO"            -> Color(0xFF1B5E20)
    "ATENDIDO"            -> Color(0xFF006064)
    "SEGUIMIENTO ABIERTO" -> Color(0xFF004D40)
    "CERRADO"             -> Color(0xFF37474F)
    else                  -> Color(0xFF37474F)
}

/** Color de borde de la tarjeta */
private fun statusBorderColor(estado: String) = when (estado) {
    "ABIERTO"             -> Color(0xFFFDD835)
    "ASIGNADO"            -> Color(0xFF90CAF9)
    "DATA RECOPILADA"     -> Color(0xFFFFAB91)
    "EN EVALUACION"       -> Color(0xFFCE93D8)
    "APROBADO"            -> Color(0xFFA5D6A7)
    "ATENDIDO"            -> Color(0xFF80DEEA)
    "SEGUIMIENTO ABIERTO" -> Color(0xFF80CBC4)
    "CERRADO"             -> Color(0xFFB0BEC5)
    else                  -> Color(0xFFB0BEC5)
}

/** Ícono asociado a cada estado */
private fun statusIcon(estado: String): ImageVector = when (estado) {
    "ABIERTO"             -> Icons.Default.Schedule
    "ASIGNADO"            -> Icons.Default.Assignment
    "DATA RECOPILADA"     -> Icons.Default.Assessment
    "EN EVALUACION"       -> Icons.Default.CheckCircle
    "APROBADO"            -> Icons.Default.CheckCircle
    "ATENDIDO"            -> Icons.Default.Favorite
    "SEGUIMIENTO ABIERTO" -> Icons.Default.TrackChanges
    "CERRADO"             -> Icons.Default.Cancel
    else                  -> Icons.Default.Cancel
}

/** Etiqueta corta para la tarjeta de estado */
private fun statusLabel(estado: String) = when (estado) {
    "ABIERTO"             -> "Abierto"
    "ASIGNADO"            -> "Asignado"
    "DATA RECOPILADA"     -> "Data"
    "EN EVALUACION"       -> "Evaluación"
    "APROBADO"            -> "Aprobado"
    "ATENDIDO"            -> "Atendido"
    "SEGUIMIENTO ABIERTO" -> "Seguimiento"
    "CERRADO"             -> "Cerrado"
    else                  -> estado
}

// ── Tarjeta de estado (estilo web) ──────────────────────────────────────────
@Composable
private fun StatusSummaryCard(
    estado: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg         = statusBg(estado)
    val iconBg     = statusIconBg(estado)
    val textColor  = statusTextColor(estado)
    val border     = if (isSelected) iconBg else statusBorderColor(estado)
    val icon       = statusIcon(estado)
    val label      = statusLabel(estado)

    Surface(
        modifier = Modifier
            .width(108.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = border
        ),
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        shadowElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ícono en badge cuadrado (igual que web)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconBg,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = count.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

// ── Pantalla principal GRD ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GRDScreen(
    viewModel: IncidenciaViewModel,
    onReportarIncidencia: () -> Unit,
    onRealizarActividad: (IncidenciaLocal) -> Unit,
    onSubirEvidencia: (IncidenciaLocal) -> Unit
) {
    val incidencias by viewModel.incidencias.collectAsState()

    var searchQuery       by remember { mutableStateOf("") }
    var statusFilter      by remember { mutableStateOf<String?>(null) }
    var categoriaExpanded by remember { mutableStateOf(false) }
    var selectedCategoria by remember { mutableStateOf("Todas") }

    val filtradas = incidencias.filter { inc ->
        val matchStatus   = statusFilter == null || inc.estado == statusFilter
        val matchSearch   = searchQuery.isBlank() ||
            inc.nombre.contains(searchQuery, ignoreCase = true) ||
            inc.descripcion.contains(searchQuery, ignoreCase = true)
        val matchCategoria = selectedCategoria == "Todas" ||
            CATEGORIA_MAP[inc.idCatalogoTipo] == selectedCategoria
        matchStatus && matchSearch && matchCategoria
    }

    val countMap = STATUS_LIST.associateWith { s -> incidencias.count { it.estado == s } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onReportarIncidencia,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Reportar incidencia", tint = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {

            // ── Tarjetas de estado ────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Estado de Incidencias",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(STATUS_LIST) { estado ->
                            StatusSummaryCard(
                                estado = estado,
                                count = countMap[estado] ?: 0,
                                isSelected = statusFilter == estado,
                                onClick = {
                                    statusFilter = if (statusFilter == estado) null else estado
                                }
                            )
                        }
                    }
                    if (statusFilter != null) {
                        TextButton(
                            onClick = { statusFilter = null },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                "Mostrar todos (${incidencias.size})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ── Búsqueda ─────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Búsqueda de incidentes...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // ── Filtro de categoría ───────────────────────────────────────
            item {
                ExposedDropdownMenuBox(
                    expanded = categoriaExpanded,
                    onExpandedChange = { categoriaExpanded = !categoriaExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCategoria,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría", fontSize = 13.sp) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = categoriaExpanded,
                        onDismissRequest = { categoriaExpanded = false }
                    ) {
                        CATEGORIAS_FILTER.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { selectedCategoria = cat; categoriaExpanded = false }
                            )
                        }
                    }
                }
            }

            // ── Contador de resultados ────────────────────────────────────
            item {
                Text(
                    text = "${filtradas.size} incidencia(s)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // ── Lista de incidencias ──────────────────────────────────────
            if (filtradas.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay incidencias que coincidan con los filtros",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filtradas) { inc ->
                    IncidenciaCard(
                        incidencia = inc,
                        onRealizarActividad = { onRealizarActividad(inc) },
                        onSubirEvidencia = { onSubirEvidencia(inc) }
                    )
                }
            }
        }
    }
}

// ── Tarjeta de incidencia ────────────────────────────────────────────────────
@Composable
fun IncidenciaCard(
    incidencia: IncidenciaLocal,
    onRealizarActividad: () -> Unit,
    onSubirEvidencia: () -> Unit
) {
    val textColor  = statusTextColor(incidencia.estado)
    val badgeBg    = statusBg(incidencia.estado)
    val categoria  = CATEGORIA_MAP[incidencia.idCatalogoTipo] ?: "Tipo ${incidencia.idCatalogoTipo}"
    val parroquia  = PARROQUIA_MAP[incidencia.idParroquia] ?: "Parroquia ${incidencia.idParroquia}"
    val grdCode    = "GRD-2026-${(incidencia.idIncidenciaRemota ?: incidencia.uuidIncidencia.takeLast(4)).toString().padStart(4, '0')}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ─ Título + código ─
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (incidencia.nombre.isNotBlank()) {
                        Text(
                            text = incidencia.nombre,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1A1A1A)
                        )
                    } else {
                        Text(
                            text = incidencia.descripcion,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1A1A1A),
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "$grdCode · ${formatDate(incidencia.fechaUltimaModificacion)}",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─ Badges: categoría + estado ─
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Categoría
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = categoria,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                // Estado
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusBorderColor(incidencia.estado))
                ) {
                    Text(
                        text = statusLabel(incidencia.estado),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─ Ubicación y afectados ─
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF888888))
                    Text(parroquia, fontSize = 12.sp, color = Color(0xFF555555))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF888888))
                    Text("${incidencia.numAfectados} afectados", fontSize = 12.sp, color = Color(0xFF555555))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─ Botones de acción (igual que web) ─
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRealizarActividad,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Realizar Actividad", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onSubirEvidencia,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF444444)
                    )
                ) {
                    Text("Subir Evidencia", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MetaChip(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
