package pucp.edu.caritas_movile_grd.Kits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID

private val TIPOS_KIT = listOf(
    "Kit de Víveres",
    "Kit de Higiene",
    "Kit de Dormitorio",
    "Kit de Complementos",
    "Otros"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntregaKitScreen(
    onBack: () -> Unit,
    onConfirmarEntrega: (EntregaKitLocal) -> Unit
) {
    var uuidAfectado by remember { mutableStateOf("") }
    var kitSeleccionado by remember { mutableStateOf(TIPOS_KIT[0]) }
    var cantidad by remember { mutableIntStateOf(1) }
    var notas by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    LaunchedEffect(mostrarConfirmacion) {
        if (mostrarConfirmacion) {
            snackbarHostState.showSnackbar("Entrega registrada correctamente")
            mostrarConfirmacion = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Entrega de Kit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ID del afectado
            OutlinedTextField(
                value = uuidAfectado,
                onValueChange = { uuidAfectado = it },
                label = { Text("ID del Afectado / Escanear DNI") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { /* TODO: iniciar escáner */ }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
                    }
                },
                singleLine = true
            )

            // Tipo de kit
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = kitSeleccionado,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de kit") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    TIPOS_KIT.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo) },
                            onClick = {
                                kitSeleccionado = tipo
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Cantidad con stepper
            Column {
                Text(
                    text = "Cantidad",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { if (cantidad > 1) cantidad-- },
                        enabled = cantidad > 1
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Reducir")
                    }
                    OutlinedTextField(
                        value = cantidad.toString(),
                        onValueChange = { v ->
                            val n = v.toIntOrNull()
                            if (n != null && n > 0) cantidad = n
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    FilledTonalIconButton(
                        onClick = { cantidad++ }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aumentar")
                    }
                }
            }

            // Observaciones
            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                label = { Text("Observaciones (opcional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4
            )

            // Botón confirmar
            Button(
                onClick = {
                    val entrega = EntregaKitLocal(
                        uuidEntrega = UUID.randomUUID().toString(),
                        uuidAfectado = uuidAfectado.trim(),
                        kitEntregado = kitSeleccionado,
                        cantidad = cantidad,
                        fechaEntrega = System.currentTimeMillis()
                    )
                    onConfirmarEntrega(entrega)
                    mostrarConfirmacion = true
                    uuidAfectado = ""
                    cantidad = 1
                    notas = ""
                    kitSeleccionado = TIPOS_KIT[0]
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = uuidAfectado.isNotBlank()
            ) {
                Text("CONFIRMAR ENTREGA", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
