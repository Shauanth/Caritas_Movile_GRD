package pucp.edu.caritas_movile_grd.Incidencias

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarEventoScreen(
    onBack: () -> Unit,
    onSave: (IncidenciaLocal) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    
    // --- Campos del Formulario ---
    // Datos Generales
    var dniReporte by remember { mutableStateOf("") }
    var nombreReporte by remember { mutableStateOf("") }
    var celularReporte by remember { mutableStateOf("") }
    var rolReporte by remember { mutableStateOf("Seleccionar...") }
    
    // Datos del Evento
    var fechaSuceso by remember { mutableStateOf("") }
    var horaSuceso by remember { mutableStateOf("") }
    var selectedCategoriaId by remember { mutableStateOf(1) }
    var distrito by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var referencia by remember { mutableStateOf("") }
    
    // Descripción
    var descripcion by remember { mutableStateOf("") }
    var causa by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Registrar Nuevo Evento", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Completa los datos del evento reportado", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Atrás",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "ETAPA 1 — REGISTRO",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Button(
                        onClick = {
                            val nuevaIncidencia = IncidenciaLocal(
                                uuidIncidencia = UUID.randomUUID().toString(),
                                uuidUsuario = "brigadista",
                                idParroquia = 1, // Placeholder
                                idCatalogoTipo = selectedCategoriaId,
                                descripcion = descripcion,
                                nombre = "Evento-${distrito.ifBlank { "Lima" }}",
                                responsable = "Brigadista",
                                estado = "ABIERTO",
                                fechaUltimaModificacion = System.currentTimeMillis(),
                                causa = causa,
                                reportadoPorNombre = nombreReporte,
                                reportadoPorCelular = celularReporte,
                                reportadoPorDni = dniReporte,
                                reportadoPorRol = rolReporte,
                                distrito = distrito,
                                direccion = direccion,
                                referencia = referencia
                            )
                            onSave(nuevaIncidencia)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Registrar Evento", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Alias autogenerado
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Alias del evento (autogenerado)", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Evento-${distrito.ifBlank { "Lima" }}", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 1. DATOS GENERALES
            FormSection(1, "DATOS GENERALES") {
                Text("Fecha y hora del reporte", fontSize = 13.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(SimpleDateFormat("dd/M/yyyy, HH:mm:ss a", Locale.getDefault()).format(Date()), fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Persona que reportó el evento", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dniReporte,
                        onValueChange = { dniReporte = it },
                        label = { Text("DNI *", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = nombreReporte,
                        onValueChange = { nombreReporte = it },
                        label = { Text("Nombre y apellidos completos *", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = celularReporte,
                        onValueChange = { celularReporte = it },
                        label = { Text("Número de celular *", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = rolReporte,
                        onValueChange = { rolReporte = it },
                        label = { Text("Rol/Institución *", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.5f),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.ExpandMore, null) }
                    )
                }
            }

            // 2. DATOS DEL EVENTO
            FormSection(2, "DATOS DEL EVENTO") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fechaSuceso,
                        onValueChange = { fechaSuceso = it },
                        label = { Text("Fecha del suceso *", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.5f),
                        placeholder = { Text("mm/dd/yyyy") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = horaSuceso,
                        onValueChange = { horaSuceso = it },
                        label = { Text("Hora del suceso", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.5f),
                        placeholder = { Text("--:-- --") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Text("La hora puede ser aproximada", fontSize = 11.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Categoría del evento *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                
                FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                    CATEGORIAS_FILTER.forEachIndexed { index, cat ->
                        if (cat == "Todas") return@forEachIndexed
                        FilterChip(
                            selected = selectedCategoriaId == index,
                            onClick = { selectedCategoriaId = index },
                            label = { Text(cat, fontSize = 12.sp) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ubicación del Suceso", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = "Perú", 
                        onValueChange = {}, 
                        label = { Text("País *", fontSize = 12.sp) }, 
                        modifier = Modifier.weight(0.5f), 
                        enabled = false, 
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    OutlinedTextField(
                        value = "Lima Metropolitana", 
                        onValueChange = {}, 
                        label = { Text("Región *", fontSize = 12.sp) }, 
                        modifier = Modifier.weight(0.5f), 
                        enabled = false, 
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
                OutlinedTextField(
                    value = distrito,
                    onValueChange = { distrito = it },
                    label = { Text("Distrito *", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.ExpandMore, null) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 3. DESCRIPCIÓN DEL EVENTO
            FormSection(3, "DESCRIPCIÓN DEL EVENTO") {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción breve del evento", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Describe lo que se reportó...", fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = causa,
                    onValueChange = { causa = it },
                    label = { Text("Causa o posible causa del suceso", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("¿Qué originó el evento?", fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private val CATEGORIAS_FILTER = listOf(
    "Todas", "Incendios", "Inundaciones", "Derrumbes",
    "Deslizamientos", "Sismos", "Tsunamis", "Vendaval / Vientos fuertes",
    "Colapso de infraestructura", "Pérdida parcial de la vivienda", "Lluvias intensas", "Otros"
)
