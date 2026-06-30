package pucp.edu.caritas_movile_grd.Incidencias

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pucp.edu.caritas_movile_grd.ui.theme.estadoVisual
import pucp.edu.caritas_movile_grd.ui.theme.iconoTipoEvento
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
    "APROBADO", "ATENDIDO", "SEGUIMIENTO ABIERTO", "CERRADO", "RECHAZADO"
)

private val VISIBILIDAD_FILTERS = listOf("Activas", "Cerradas", "Todas")
private val ESTADOS_NO_ACCIONABLES = setOf(
    "CERRADO", "CERRADA", "RECHAZADO", "CANCELADO", "CANCELADA", "ANULADO", "ANULADA"
)

private val CATEGORIAS_FILTER = listOf(
    "Todas", "Incendios", "Inundaciones", "Derrumbes",
    "Deslizamientos", "Sismos", "Tsunamis"
)

fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))


private fun categoriaIncidencia(incidencia: IncidenciaLocal): String {
    return incidencia.tipoEventoNombre
        ?.takeIf { it.isNotBlank() }
        ?: CATEGORIA_MAP[incidencia.idCatalogoTipo]
        ?: "Tipo ${incidencia.idCatalogoTipo}"
}

private fun estadoNormalizado(estado: String): String = estado.trim().uppercase(Locale.getDefault())

private fun esIncidenciaCerrada(incidencia: IncidenciaLocal): Boolean =
    estadoNormalizado(incidencia.estado) in ESTADOS_NO_ACCIONABLES

private fun textoBusqueda(incidencia: IncidenciaLocal): String = listOfNotNull(
    incidencia.codigoCasoRemoto,
    "LOCAL-${incidencia.uuidIncidencia.takeLast(8)}",
    incidencia.tipoEventoNombre,
    CATEGORIA_MAP[incidencia.idCatalogoTipo],
    incidencia.distrito,
    incidencia.parroquiaNombre,
    PARROQUIA_MAP[incidencia.idParroquia],
    incidencia.nombre,
    incidencia.descripcion
).joinToString(" ")

private fun emptyIncidenciasMessage(
    selectedVisibilidad: String,
    searchQuery: String,
    selectedCategoria: String,
    statusFilter: String?
): String {
    val hayFiltros = searchQuery.isNotBlank() || selectedCategoria != "Todas" || statusFilter != null
    return when {
        hayFiltros -> "No se encontraron incidencias con los filtros aplicados."
        selectedVisibilidad == "Cerradas" -> "No hay incidencias cerradas."
        selectedVisibilidad == "Activas" -> "No hay incidencias activas asignadas."
        else -> "No se encontraron incidencias con los filtros aplicados."
    }
}
// ── Color helpers ────────────────────────────────────────────────────────────
// Delegan en estadoVisual() (ui.theme) para compartir la paleta con Simulacros.

private fun statusBg(estado: String) = estadoVisual(estado).bg
private fun statusIconBg(estado: String) = estadoVisual(estado).solido
private fun statusTextColor(estado: String) = estadoVisual(estado).texto
private fun statusBorderColor(estado: String) = estadoVisual(estado).borde
private fun statusIcon(estado: String): ImageVector = estadoVisual(estado).icono
private fun statusLabel(estado: String) = estadoVisual(estado).etiqueta

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
                    shape = RoundedCornerShape(6.dp),
                    color = iconBg,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
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
    var selectedVisibilidad by remember { mutableStateOf("Activas") }

    val filtradasParaContadores = incidencias.filter { inc ->
        val matchVisibilidad = when (selectedVisibilidad) {
            "Activas" -> !esIncidenciaCerrada(inc)
            "Cerradas" -> esIncidenciaCerrada(inc)
            else -> true
        }
        val matchSearch   = searchQuery.isBlank() ||
            textoBusqueda(inc).contains(searchQuery, ignoreCase = true)
        val matchCategoria = selectedCategoria == "Todas" ||
            categoriaIncidencia(inc).equals(selectedCategoria, ignoreCase = true)
        matchVisibilidad && matchSearch && matchCategoria
    }

    val filtradas = filtradasParaContadores.filter { inc ->
        statusFilter == null || inc.estado.equals(statusFilter, ignoreCase = true)
    }

    val countMap = STATUS_LIST.associateWith { s ->
        filtradasParaContadores.count { it.estado.equals(s, ignoreCase = true) }
    }

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
                                "Mostrar todos (${filtradasParaContadores.size})",
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
                    placeholder = { Text("Buscar por código, distrito o tipo...", fontSize = 14.sp) },
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
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(VISIBILIDAD_FILTERS) { filtro ->
                        FilterChip(
                            selected = selectedVisibilidad == filtro,
                            onClick = {
                                selectedVisibilidad = filtro
                                statusFilter = null
                            },
                            label = { Text(filtro, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

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
                            emptyIncidenciasMessage(
                                selectedVisibilidad = selectedVisibilidad,
                                searchQuery = searchQuery,
                                selectedCategoria = selectedCategoria,
                                statusFilter = statusFilter
                            ),
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
    val visual     = estadoVisual(incidencia.estado)
    val textColor  = visual.texto
    val badgeBg    = visual.bg
    val categoria = categoriaIncidencia(incidencia)
    val ubicacion  = incidencia.distrito
        ?: incidencia.parroquiaNombre
        ?: PARROQUIA_MAP[incidencia.idParroquia]
        ?: "Sin ubicación"
    val grdCode = incidencia.codigoCasoRemoto
    ?: "LOCAL-${incidencia.uuidIncidencia.takeLast(8)}"
    val soloLectura = esIncidenciaCerrada(incidencia)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onRealizarActividad() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ─ Avatar de tipo + título + código ─
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(visual.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconoTipoEvento(categoria),
                        contentDescription = null,
                        tint = visual.solido,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$grdCode · ${formatDate(incidencia.fechaUltimaModificacion)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = incidencia.nombre.ifBlank { incidencia.descripcion },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ubicacion, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${incidencia.numAfectados} afectados", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─ Botones de acción ─
            if (soloLectura) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Text(
                        "Caso cerrado: solo lectura",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF546E7A),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRealizarActividad,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Realizar Actividad", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onSubirEvidencia,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF444444))
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Subir Evidencia", fontSize = 12.sp)
                    }
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
