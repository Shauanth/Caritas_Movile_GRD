package pucp.edu.caritas_movile_grd.Kits

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntregaKitScreen(
    onBack: () -> Unit,
    onConfirmarEntrega: (EntregaKitLocal) -> Unit
) {
    var uuidAfectado by remember { mutableStateOf("") }
    var kitSeleccionado by remember { mutableStateOf("Kit de Alimentos") }
    var cantidad by remember { mutableIntStateOf(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrega de Kits") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Registro de Ayuda Humanitaria",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uuidAfectado,
                onValueChange = { uuidAfectado = it },
                label = { Text("ID del Afectado / Escanear DNI") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { /* Iniciar Escáner */ }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Kit a Entregar", modifier = Modifier.fillMaxWidth())
            // Simulación de selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = true, onClick = {})
                Text("Kit de Alimentos")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val entrega = EntregaKitLocal(
                        uuidEntrega = UUID.randomUUID().toString(),
                        uuidAfectado = uuidAfectado,
                        kitEntregado = kitSeleccionado,
                        cantidad = cantidad,
                        fechaEntrega = System.currentTimeMillis()
                    )
                    onConfirmarEntrega(entrega)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                enabled = uuidAfectado.isNotEmpty()
            ) {
                Text("CONFIRMAR ENTREGA", fontWeight = FontWeight.Bold)
            }
        }
    }
}
