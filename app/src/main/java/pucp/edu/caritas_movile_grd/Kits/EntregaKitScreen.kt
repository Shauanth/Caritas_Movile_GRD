package pucp.edu.caritas_movile_grd.Kits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import java.text.SimpleDateFormat
import java.util.*

private val GREEN = Color(0xFF009850)

private val TIPOS_KIT = listOf(
    "Kit de Alimentos",
    "Kit de Higiene",
    "Kit de Abrigo",
    "Kit de Cocina",
    "Kit de Herramientas",
    "Kit Escolar",
    "Colchoneta",
    "Frazada",
    "Otro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntregaKitScreen(
    viewModel: KitViewModel,
    onBack: () -> Unit,
    onConfirmarEntrega: (EntregaKitLocal) -> Unit
) {
    val entregas by viewModel.entregas.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var filtro by remember { mutableStateOf("Todas") }

    val filtradas = when (filtro) {
        "Pendientes"    -> entregas.filter { it.estadoSync != EstadoSync.SINCRONIZADO }
        "Sincronizadas" -> entregas.filter { it.estadoSync == EstadoSync.SINCRONIZADO }
        else            -> entregas
    }

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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = GREEN,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva entrega")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // ── Chips filtro ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todas", "Pendientes", "Sincronizadas").forEach { f ->
                    FilterChip(
                        selected = filtro == f,
                        onClick = { filtro = f },
                        label = { Text(f, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GREEN,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // ── Resumen ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ResumenChip(
                    label = "Total",
                    count = entregas.size,
                    color = Color(0xFF1565C0)
                )
                ResumenChip(
                    label = "Pendientes",
                    count = entregas.count { it.estadoSync != EstadoSync.SINCRONIZADO },
                    color = Color(0xFFE65100)
                )
                ResumenChip(
                    label = "Sync",
                    count = entregas.count { it.estadoSync == EstadoSync.SINCRONIZADO },
                    color = GREEN
                )
            }

            // ── Lista ────────────────────────────────────────────────────────
            if (filtradas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.LightGray
                        )
                        Text("No hay entregas registradas", color = Color.Gray, fontSize = 14.sp)
                        TextButton(onClick = { showDialog = true }) {
                            Text("Registrar primera entrega", color = GREEN)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtradas) { entrega ->
                        EntregaKitCard(entrega = entrega)
                    }
                }
            }
        }
    }

    if (showDialog) {
        NuevaEntregaDialog(
            onDismiss = { showDialog = false },
            onConfirmar = { entrega ->
                onConfirmarEntrega(entrega)
                showDialog = false
            }
        )
    }
}

// ── Card de entrega ──────────────────────────────────────────────────────────

@Composable
private fun EntregaKitCard(entrega: EntregaKitLocal) {
    val sincronizado = entrega.estadoSync == EstadoSync.SINCRONIZADO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GREEN.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = GREEN, modifier = Modifier.size(22.dp))
                        }
                    }
                    Column {
                        Text(entrega.kitEntregado, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Cantidad: ${entrega.cantidad}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (sincronizado) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        if (sincronizado) "Sincronizado" else "Pendiente",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (sincronizado) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Text(
                        entrega.uuidAfectado.takeLast(8),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Text(
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(entrega.fechaEntrega)),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ── Dialog nueva entrega ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaEntregaDialog(
    onDismiss: () -> Unit,
    onConfirmar: (EntregaKitLocal) -> Unit
) {
    var uuidAfectado    by remember { mutableStateOf("") }
    var kitSeleccionado by remember { mutableStateOf(TIPOS_KIT[0]) }
    var cantidad        by remember { mutableIntStateOf(1) }
    var tipoEntrega     by remember { mutableStateOf("COMPLETA") }
    var kitExpanded     by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Entrega de Kit", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ID Afectado
                OutlinedTextField(
                    value = uuidAfectado,
                    onValueChange = { uuidAfectado = it; errorMsg = "" },
                    label = { Text("UUID del Afectado") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    isError = errorMsg.isNotEmpty(),
                    singleLine = true
                )

                // Tipo de kit
                ExposedDropdownMenuBox(
                    expanded = kitExpanded,
                    onExpandedChange = { kitExpanded = !kitExpanded }
                ) {
                    OutlinedTextField(
                        value = kitSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Kit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = kitExpanded,
                        onDismissRequest = { kitExpanded = false }
                    ) {
                        TIPOS_KIT.forEach { kit ->
                            DropdownMenuItem(
                                text = { Text(kit) },
                                onClick = { kitSeleccionado = kit; kitExpanded = false }
                            )
                        }
                    }
                }

                // Cantidad
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Cantidad:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    IconButton(
                        onClick = { if (cantidad > 1) cantidad-- },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = GREEN)
                    }
                    Text(
                        cantidad.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 32.dp)
                    )
                    IconButton(
                        onClick = { cantidad++ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = GREEN)
                    }
                }

                // Tipo entrega: Completa / Parcial
                Text("Tipo de entrega:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("COMPLETA", "PARCIAL").forEach { tipo ->
                        FilterChip(
                            selected = tipoEntrega == tipo,
                            onClick = { tipoEntrega = tipo },
                            label = { Text(tipo, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (tipo == "COMPLETA") GREEN else Color(0xFFE65100),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (uuidAfectado.isBlank()) {
                        errorMsg = "Ingresa el UUID del afectado"
                        return@Button
                    }
                    onConfirmar(
                        EntregaKitLocal(
                            uuidEntrega     = UUID.randomUUID().toString(),
                            uuidAfectado    = uuidAfectado.trim(),
                            kitEntregado    = "$kitSeleccionado ($tipoEntrega)",
                            cantidad        = cantidad,
                            fechaEntrega    = System.currentTimeMillis(),
                            estadoSync      = EstadoSync.NUEVO
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GREEN)
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Chip resumen ─────────────────────────────────────────────────────────────

@Composable
private fun ResumenChip(label: String, count: Int, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(label, fontSize = 12.sp, color = color)
        }
    }
}
