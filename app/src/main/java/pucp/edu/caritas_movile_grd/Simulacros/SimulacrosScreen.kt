package pucp.edu.caritas_movile_grd.Simulacros

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import pucp.edu.caritas_movile_grd.ui.theme.estadoVisual
import pucp.edu.caritas_movile_grd.ui.theme.iconoTipoEvento

private val ESTADO_CHIPS = listOf(
    "TODAS",
    "PROGRAMADA",
    "ASIGNADA",
    "EJECUTADA",
    "OBSERVADA",
    "VALIDADA",
    "CANCELADA"
)

// Colores, íconos y etiquetas por estado provienen de ui.theme.estadoVisual()
// (fuente única compartida con el flujo GRD).

private fun filtroLabel(estado: String): String =
    if (estado == "TODAS") "Todas" else estadoVisual(estado).etiqueta

private fun filtroIcono(estado: String): ImageVector =
    if (estado == "TODAS") Icons.Default.Apps else estadoVisual(estado).icono

@Composable
fun SimulacrosScreen(viewModel: SimulacroViewModel) {
    val simulacros by viewModel.simulacros.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedEstado by remember { mutableStateOf("TODAS") }

    LaunchedEffect(uiState.message, uiState.error) {
        val mensaje = uiState.error ?: uiState.message
        if (!mensaje.isNullOrBlank()) {
            snackbarHostState.showSnackbar(mensaje)
            viewModel.limpiarMensajes()
        }
    }

    val filtrados = if (selectedEstado == "TODAS") simulacros
    else simulacros.filter { it.estadoActividad.equals(selectedEstado, ignoreCase = true) }

    val countMap = ESTADO_CHIPS.drop(1).associateWith { estado ->
        simulacros.count { it.estadoActividad.equals(estado, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Encabezado ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 12.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Simulacros asignados",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = when (simulacros.size) {
                            0 -> "Sin actividades registradas"
                            1 -> "1 actividad en total"
                            else -> "${simulacros.size} actividades en total"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { viewModel.refrescar() },
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualizar simulacros",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            // ── Filtros por estado ────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ESTADO_CHIPS) { estado ->
                    val count = if (estado == "TODAS") simulacros.size else countMap[estado] ?: 0
                    val seleccionado = selectedEstado == estado
                    val label = if (estado != "TODAS" && count > 0)
                        "${filtroLabel(estado)} ($count)"
                    else filtroLabel(estado)
                    // Cada filtro lleva su propio ícono y color de estado
                    val acento = if (estado == "TODAS") MaterialTheme.colorScheme.primary
                    else estadoVisual(estado).solido
                    FilterChip(
                        selected = seleccionado,
                        onClick = { selectedEstado = estado },
                        label = {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = filtroIcono(estado),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = acento.copy(alpha = 0.10f),
                            labelColor = acento,
                            iconColor = acento,
                            selectedContainerColor = acento,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = seleccionado,
                            borderColor = acento.copy(alpha = 0.35f),
                            selectedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            if (filtrados.isEmpty()) {
                EmptySimulacros()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtrados, key = { it.uuidSimulacro }) { simulacro ->
                        SimulacroCard(
                            simulacro = simulacro,
                            onEjecutar = viewModel::ejecutarSimulacro
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun EmptySimulacros() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No hay simulacros en este estado",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Desliza los filtros o actualiza para ver más actividades.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SimulacroStatusChip(estado: String) {
    val visual = estadoVisual(estado)
    Surface(
        shape = RoundedCornerShape(50),
        color = visual.bg,
        border = BorderStroke(1.dp, visual.borde)
    ) {
        Text(
            visual.etiqueta,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            fontSize = 11.sp,
            color = visual.texto,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulacroCard(
    simulacro: SimulacroLocal,
    onEjecutar: (
        uuid: String,
        resultadoGeneral: String,
        reporteBrigadista: String,
        numeroParticipantesReal: Int?,
        duracionSimulacro: Int?,
        recomendaciones: String?,
        observaciones: String?
    ) -> Unit
) {
    var expanded by remember(simulacro.uuidSimulacro) { mutableStateOf(false) }
    var resultado by remember(simulacro.uuidSimulacro) { mutableStateOf(simulacro.resultadoGeneral.orEmpty()) }
    var reporte by remember(simulacro.uuidSimulacro) { mutableStateOf(simulacro.reporteBrigadista.orEmpty()) }
    var participantes by remember(simulacro.uuidSimulacro) {
        mutableStateOf(simulacro.numeroParticipantesReal?.toString().orEmpty())
    }
    var duracion by remember(simulacro.uuidSimulacro) {
        mutableStateOf(simulacro.duracionSimulacro?.toString().orEmpty())
    }
    var recomendaciones by remember(simulacro.uuidSimulacro) {
        mutableStateOf(simulacro.recomendaciones.orEmpty())
    }
    var observaciones by remember(simulacro.uuidSimulacro) {
        mutableStateOf(simulacro.observaciones.orEmpty())
    }

    val puedeEjecutar = simulacro.estadoActividad.uppercase() in setOf("ASIGNADA", "OBSERVADA")
    val codigo = simulacro.codigoActividad ?: simulacro.uuidSimulacro.takeLast(8).uppercase()
    val fechaHora = listOfNotNull(
        simulacro.fechaProgramada?.take(10),
        simulacro.horarioInicio
    ).joinToString(" · ")
    val visualEstado = estadoVisual(simulacro.estadoActividad)
    val contenedorEstado = visualEstado.bg
    val acentoEstado = visualEstado.solido

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Franja de acento por estado para escaneo visual rápido
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(acentoEstado)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Avatar de tipo: ancla visual coloreada por estado
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(contenedorEstado),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconoTipoEvento(simulacro.tipoActividadPreventiva),
                            contentDescription = null,
                            tint = acentoEstado,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = codigo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = simulacro.nombreActividad,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    SimulacroStatusChip(estado = simulacro.estadoActividad)
                }

                Spacer(modifier = Modifier.height(12.dp))

                MetaRow(
                    icon = Icons.Default.LocationOn,
                    text = simulacro.parroquiaNombre ?: "Parroquia no informada"
                )
                if (fechaHora.isNotBlank()) {
                    MetaRow(icon = Icons.Default.CalendarMonth, text = fechaHora)
                }

                if (simulacro.estadoSync != EstadoSync.SINCRONIZADO) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SmallInfoChip("Pendiente de sincronizar", Icons.Default.CloudOff)
                }

                val descripcion = simulacro.descripcionActividad.orEmpty()
                if (descripcion.isNotBlank() && expanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = descripcion,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(14.dp))

                    SimulacroDetail("Lugar", simulacro.lugarActividad)
                    SimulacroDetail("Público objetivo", simulacro.publicoObjetivo)
                    SimulacroDetail("Participantes estimados", simulacro.numeroParticipantesEstimado?.toString())
                    SimulacroDetail("Indicaciones", simulacro.indicacionesEquipo)

                    if (puedeEjecutar) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AssignmentTurnedIn,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Registrar ejecución",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = resultado,
                                    onValueChange = { resultado = it },
                                    label = { Text("Resultado general") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = reporte,
                                    onValueChange = { reporte = it },
                                    label = { Text("Reporte del brigadista") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = participantes,
                                        onValueChange = { participantes = it.filter(Char::isDigit) },
                                        label = { Text("Participantes") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = duracion,
                                        onValueChange = { duracion = it.filter(Char::isDigit) },
                                        label = { Text("Duración min.") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = recomendaciones,
                                    onValueChange = { recomendaciones = it },
                                    label = { Text("Recomendaciones") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = observaciones,
                                    onValueChange = { observaciones = it },
                                    label = { Text("Observaciones") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onEjecutar(
                                            simulacro.uuidSimulacro,
                                            resultado,
                                            reporte,
                                            participantes.toIntOrNull(),
                                            duracion.toIntOrNull(),
                                            recomendaciones.ifBlank { null },
                                            observaciones.ifBlank { null }
                                        )
                                        expanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = resultado.isNotBlank() || reporte.isNotBlank()
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Marcar ejecutado", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        SimulacroDetail("Resultado", simulacro.resultadoGeneral)
                        SimulacroDetail("Reporte", simulacro.reporteBrigadista)
                        SimulacroDetail("Participantes reales", simulacro.numeroParticipantesReal?.toString())
                        SimulacroDetail("Duración", simulacro.duracionSimulacro?.let { "$it min." })
                        SimulacroDetail("Recomendaciones", simulacro.recomendaciones)
                        SimulacroDetail("Observaciones", simulacro.observaciones)
                    }
                }

                // Pie: indicador de expandir / colapsar
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val footerLabel = when {
                        expanded -> "Ocultar detalle"
                        puedeEjecutar -> "Registrar ejecución"
                        else -> "Ver detalle"
                    }
                    Text(
                        text = footerLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SmallInfoChip(text: String, icon: ImageVector? = null) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SimulacroDetail(label: String, value: String?) {
    if (value.isNullOrBlank()) return

    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = value,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 19.sp
    )
}
