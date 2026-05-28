package pucp.edu.caritas_movile_grd.Incidencias

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidenciaScreen(
    onBack: () -> Unit,
    onSave: (IncidenciaLocal) -> Unit
) {
    var descripcion by remember { mutableStateOf("") }
    var selectedTipo by remember { mutableIntStateOf(0) }
    var selectedParroquia by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Incidencia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val nuevaIncidencia = IncidenciaLocal(
                            uuidIncidencia = UUID.randomUUID().toString(),
                            uuidUsuario = "current_user_uuid", // Esto debería venir del ViewModel
                            idParroquia = selectedParroquia,
                            idCatalogoTipo = selectedTipo,
                            descripcion = descripcion,
                            estado = "Activa",
                            fechaUltimaModificacion = System.currentTimeMillis()
                        )
                        onSave(nuevaIncidencia)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Detalles de la Emergencia", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción del suceso") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Aquí irían los Dropdowns para Tipo y Parroquia usando los Masters
            Text("Tipo de Incidente (ID): $selectedTipo")
            Slider(value = selectedTipo.toFloat(), onValueChange = { selectedTipo = it.toInt() }, valueRange = 0f..10f)

            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Parroquia (ID): $selectedParroquia")
            Slider(value = selectedParroquia.toFloat(), onValueChange = { selectedParroquia = it.toInt() }, valueRange = 0f..10f)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* Lógica para agregar afectados */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gestionar Afectados")
            }
        }
    }
}
