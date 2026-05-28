package pucp.edu.caritas_movile_grd.Incidencias

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    viewModel: IncidenciaViewModel,
    onBack: () -> Unit
) {
    val incidencias by viewModel.incidencias.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Incidencias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (incidencias.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay incidencias registradas", color = androidx.compose.ui.graphics.Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(incidencias) { incidencia ->
                    IncidenciaItem(incidencia)
                }
            }
        }
    }
}

@Composable
fun IncidenciaItem(incidencia: IncidenciaLocal) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val fecha = sdf.format(Date(incidencia.fechaUltimaModificacion))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Tipo ID: ${incidencia.idCatalogoTipo}", fontWeight = FontWeight.Bold)
                Text(text = incidencia.descripcion, maxLines = 1, fontSize = 14.sp)
                Text(text = fecha, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            Badge {
                Text(incidencia.estado)
            }
        }
    }
}
