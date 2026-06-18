package pucp.edu.caritas_movile_grd.Simulacros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

private val ESTADO_CHIPS = listOf(
    "TODAS",
    "PROGRAMADA",
    "ASIGNADA",
    "EJECUTADA",
    "OBSERVADA",
    "VALIDADA",
    "CANCELADA"
)

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Simulacros asignados",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                IconButton(
                    onClick = { viewModel.refrescar() },
                    enabled = !uiState.isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar simulacros")
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ESTADO_CHIPS) { estado ->
                    val count = if (estado == "TODAS") simulacros.size else countMap[estado] ?: 0
                    val label = if (count > 0) "${estado.lowercase().replaceFirstChar { it.uppercase() }} ($count)"
                    else estado.lowercase().replaceFirstChar { it.uppercase() }
                    FilterChip(
                        selected = selectedEstado == estado,
                        onClick = { selectedEstado = estado },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            if (filtrados.isEmpty()) {
                EmptySimulacros()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtrados, key = { it.uuidSimulacro }) { simulacro ->
                        SimulacroCard(
                            simulacro = simulacro,
                            onEjecutar = viewModel::ejecutarSimulacro
                        )
                    }
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
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No hay simulacros en este estado",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SimulacroStatusChip(estado: String) {
    val estadoNormalizado = estado.uppercase()
    val (bg, fg) = when (estadoNormalizado) {
        "PROGRAMADA" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "ASIGNADA" -> Color(0xFFDEE1FF) to Color(0xFF1A237E)
        "EJECUTADA" -> Color(0xFFFFF8E1) to Color(0xFFE65100)
        "OBSERVADA" -> Color(0xFFFFE0B2) to Color(0xFFBF360C)
        "VALIDADA" -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
        "CANCELADA" -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            estadoNormalizado,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = fg,
            fontWeight = FontWeight.SemiBold
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
    ).joinToString(" ")

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = codigo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SimulacroStatusChip(estado = simulacro.estadoActividad)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = simulacro.nombreActividad,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = simulacro.parroquiaNombre ?: "Parroquia no informada",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (fechaHora.isNotBlank()) {
                Text(
                    text = fechaHora,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!simulacro.tipoActividadPreventiva.isNullOrBlank()) {
                    SmallInfoChip(simulacro.tipoActividadPreventiva)
                }
                if (simulacro.estadoSync != EstadoSync.SINCRONIZADO) {
                    SmallInfoChip("Pendiente sync")
                }
            }

            val descripcion = simulacro.descripcionActividad.orEmpty()
            if (descripcion.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = descripcion,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                SimulacroDetail("Lugar", simulacro.lugarActividad)
                SimulacroDetail("Publico objetivo", simulacro.publicoObjetivo)
                SimulacroDetail("Participantes estimados", simulacro.numeroParticipantesEstimado?.toString())
                SimulacroDetail("Indicaciones", simulacro.indicacionesEquipo)

                if (puedeEjecutar) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resultado,
                        onValueChange = { resultado = it },
                        label = { Text("Resultado general") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reporte,
                        onValueChange = { reporte = it },
                        label = { Text("Reporte del brigadista") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = participantes,
                            onValueChange = { participantes = it.filter(Char::isDigit) },
                            label = { Text("Participantes") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = duracion,
                            onValueChange = { duracion = it.filter(Char::isDigit) },
                            label = { Text("Duracion min.") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recomendaciones,
                        onValueChange = { recomendaciones = it },
                        label = { Text("Recomendaciones") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = observaciones,
                        onValueChange = { observaciones = it },
                        label = { Text("Observaciones") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
                        enabled = resultado.isNotBlank() || reporte.isNotBlank()
                    ) {
                        Text("Marcar ejecutado", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    SimulacroDetail("Resultado", simulacro.resultadoGeneral)
                    SimulacroDetail("Reporte", simulacro.reporteBrigadista)
                    SimulacroDetail("Participantes reales", simulacro.numeroParticipantesReal?.toString())
                    SimulacroDetail("Duracion", simulacro.duracionSimulacro?.let { "$it min." })
                    SimulacroDetail("Recomendaciones", simulacro.recomendaciones)
                    SimulacroDetail("Observaciones", simulacro.observaciones)
                }
            }
        }
    }
}

@Composable
private fun SmallInfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SimulacroDetail(label: String, value: String?) {
    if (value.isNullOrBlank()) return

    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = value,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
