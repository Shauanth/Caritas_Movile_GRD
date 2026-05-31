package pucp.edu.caritas_movile_grd.Incidencias

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealizarActividadScreen(
    uuidIncidencia: String,
    viewModel: IncidenciaViewModel,
    onBack: () -> Unit
) {
    val incidenciaState = produceState<IncidenciaLocal?>(initialValue = null) {
        value = viewModel.incidencias.value.find { it.uuidIncidencia == uuidIncidencia }
    }

    val incidencia = incidenciaState.value

    if (incidencia == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(incidencia.nombre.ifBlank { "Incidencia sin nombre" }, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("ID: ${incidencia.uuidIncidencia.take(8).uppercase()}", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Notificaciones */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(end = 8.dp).size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("AM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con Estado y Categoría
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(incidencia.estado)
                CategoryBadge(CATEGORIA_MAP[incidencia.idCatalogoTipo] ?: "Otros")
            }

            // Banner Naranja "Etapa 3"
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF57C00)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Etapa 3 — Levantamiento de Campo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Verifica datos del evento, confirma empadronamiento y documenta desde campo.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                        Text("Responsable: ${incidencia.responsable}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("BRIGADISTA", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Secciones Desplegables
            ExpandableSection(index = 1, title = "Verificación del Evento") {
                VerificationContent(incidencia)
            }

            ExpandableSection(index = 2, title = "Verificación de Empadronamiento") {
                Text("Cargando padrón de afectados...", modifier = Modifier.padding(16.dp), fontSize = 14.sp)
            }

            ExpandableSection(index = 3, title = "Evidencias de Campo") {
                Text("0 evidencia(s) subida(s)", modifier = Modifier.padding(16.dp), fontSize = 14.sp)
            }

            // Botón Final
            Button(
                onClick = { /* Enviar */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar Levantamiento al Especialista GRD", fontWeight = FontWeight.Bold)
            }
            Text(
                "El Especialista revisará estos datos y elaborará el informe de evaluación social",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun VerificationContent(incidencia: IncidenciaLocal) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Datos del evento registrados
        InfoCard(title = "Datos del evento registrados:") {
            InfoRow("Categoría", CATEGORIA_MAP[incidencia.idCatalogoTipo] ?: "Otros")
            InfoRow("Ubicación", "${incidencia.direccion ?: ""}, ${PARROQUIA_MAP[incidencia.idParroquia] ?: ""}")
            InfoRow("Descripción inicial", incidencia.descripcion)
            InfoRow("Causa", incidencia.causa ?: "No especificada")
        }

        // Reportado por
        InfoCard(title = "Reportado por:") {
            InfoRow("Nombre", incidencia.reportadoPorNombre ?: "N/A")
            InfoRow("Celular", incidencia.reportadoPorCelular ?: "N/A")
            InfoRow("DNI", incidencia.reportadoPorDni ?: "N/A")
            InfoRow("Rol/Institución", incidencia.reportadoPorRol ?: "N/A")
        }

        // Necesidades identificadas
        InfoCard(title = "Necesidades identificadas:") {
            FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                val necesidades = incidencia.necesidades?.split(",") ?: emptyList()
                if (necesidades.isEmpty()) Text("Ninguna", fontSize = 13.sp)
                necesidades.forEach { nec ->
                    AssistChip(
                        onClick = {},
                        label = { Text(nec.trim(), fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFFF3E0))
                    )
                }
            }
        }
    }
}
