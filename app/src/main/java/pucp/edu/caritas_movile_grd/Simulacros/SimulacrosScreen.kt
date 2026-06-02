package pucp.edu.caritas_movile_grd.Simulacros

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

private val ESTADO_CHIPS = listOf(
    "Todas",
    "Programada",
    "Asignada",
    "Ejecutada",
    "Observada",
    "Validada"
)

@Composable
fun SimulacrosScreen(viewModel: SimulacroViewModel) {
    val simulacros by viewModel.simulacros.collectAsState()
    var selectedEstado by remember { mutableStateOf("Todas") }

    val filtrados = if (selectedEstado == "Todas") simulacros
    else simulacros.filter { it.estado == selectedEstado }

    val countMap = ESTADO_CHIPS.drop(1).associateWith { e -> simulacros.count { it.estado == e } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ESTADO_CHIPS) { estado ->
                val count = if (estado == "Todas") simulacros.size else countMap[estado] ?: 0
                val label = if (count > 0) "$estado ($count)" else estado
                FilterChip(
                    selected = selectedEstado == estado,
                    onClick = { selectedEstado = estado },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        if (filtrados.isEmpty()) {
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtrados) { simulacro ->
                    SimulacroCard(
                        simulacro = simulacro,
                        onEnviarReporte = { uuid, notas ->
                            viewModel.enviarReporte(uuid, notas)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SimulacroStatusChip(estado: String) {
    val (bg, fg) = when (estado) {
        "Programada" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "Asignada"   -> Color(0xFFDEE1FF) to Color(0xFF1A237E)
        "Ejecutada"  -> Color(0xFFFFF8E1) to Color(0xFFE65100)
        "Observada"  -> Color(0xFFFFE0B2) to Color(0xFFBF360C)
        "Validada"   -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
        else         -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            estado,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SimulacroCard(
    simulacro: SimulacroLocal,
    onEnviarReporte: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var notas by remember { mutableStateOf(simulacro.notasBrigadista) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val fechaStr = sdf.format(Date(simulacro.fecha))
    val simCode = "SIM-2026-${simulacro.uuidSimulacro.takeLast(4).uppercase()}"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: code + type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = simCode,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = simulacro.tipo,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parroquia and date
            Text(
                text = simulacro.parroquia,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = fechaStr,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status badge
            SimulacroStatusChip(estado = simulacro.estado)

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            if (simulacro.descripcion.isNotBlank()) {
                Text(
                    text = simulacro.descripcion,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2
                )
            }

            // Expanded content
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                if (simulacro.indicaciones.isNotBlank()) {
                    Text(
                        text = "Indicaciones",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = simulacro.indicaciones,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Comentario field
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Comentario") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Camera and Gallery buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* placeholder: cámara */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cámara", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { /* placeholder: galería */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Galería", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Enviar Reporte button
                Button(
                    onClick = {
                        onEnviarReporte(simulacro.uuidSimulacro, notas)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = simulacro.estado != "Ejecutada" && simulacro.estado != "Validada"
                ) {
                    Text("Enviar Reporte", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
