package pucp.edu.caritas_movile_grd.Incidencias

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Opciones de categorías (id → label, aligned with CATEGORIA_MAP in HistorialScreen)
private val CATEGORIAS = listOf(
    1 to "Incendios",
    2 to "Inundaciones",
    3 to "Derrumbes",
    4 to "Deslizamientos",
    5 to "Sismos",
    6 to "Tsunamis",
    7 to "Colapso de infraestructura",
    8 to "Pérdida parcial de vivienda"
)

// Parroquias de ejemplo (en producción vendrían del MasterDao)
private val PARROQUIAS = listOf(
    1 to "Villa María del Triunfo",
    2 to "Independencia",
    3 to "Villa El Salvador",
    4 to "San Juan de Lurigancho",
    5 to "Comas",
    6 to "San Juan Bautista"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidenciaScreen(
    onBack: () -> Unit,
    onSave: (IncidenciaLocal) -> Unit
) {
    val context = LocalContext.current

    var nombre        by remember { mutableStateOf("") }
    var descripcion   by remember { mutableStateOf("") }
    var numAfectados  by remember { mutableStateOf("0") }

    // Dropdowns
    var categoriaExpanded  by remember { mutableStateOf(false) }
    var selectedCategoria  by remember { mutableStateOf(CATEGORIAS[0]) }
    var parroquiaExpanded  by remember { mutableStateOf(false) }
    var selectedParroquia  by remember { mutableStateOf(PARROQUIAS[0]) }

    // Evidencias
    var showEvidenciaSheet by remember { mutableStateOf(false) }
    val evidenciasAgregadas = remember { mutableStateListOf<Uri>() }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- Activity Result Launchers ---
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraImageUri?.let { evidenciasAgregadas.add(it) }
        showEvidenciaSheet = false
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        evidenciasAgregadas.addAll(uris)
        showEvidenciaSheet = false
    }
    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { evidenciasAgregadas.add(it) }
        showEvidenciaSheet = false
    }
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { evidenciasAgregadas.add(it) }
        showEvidenciaSheet = false
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = crearUriCamara(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // --- Pantalla ---
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
                            uuidUsuario = "brigadista",
                            idParroquia = selectedParroquia.first,
                            idCatalogoTipo = selectedCategoria.first,
                            descripcion = descripcion,
                            nombre = nombre.ifBlank { selectedCategoria.second },
                            numAfectados = numAfectados.toIntOrNull() ?: 0,
                            responsable = "Brigadista",
                            estado = "ABIERTO",
                            estadoSync = pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.NUEVO,
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Detalles de la Emergencia",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            // Nombre del incidente
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del incidente") },
                placeholder = { Text("Ej: Incendio forestal en zona residencial") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Descripción
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción del suceso") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )

            // Número de afectados
            OutlinedTextField(
                value = numAfectados,
                onValueChange = { if (it.all { c -> c.isDigit() }) numAfectados = it },
                label = { Text("N° de afectados") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Categoría dropdown
            ExposedDropdownMenuBox(
                expanded = categoriaExpanded,
                onExpandedChange = { categoriaExpanded = !categoriaExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategoria.second,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Incidente") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = categoriaExpanded,
                    onDismissRequest = { categoriaExpanded = false }
                ) {
                    CATEGORIAS.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.second) },
                            onClick = { selectedCategoria = cat; categoriaExpanded = false }
                        )
                    }
                }
            }

            // Parroquia dropdown
            ExposedDropdownMenuBox(
                expanded = parroquiaExpanded,
                onExpandedChange = { parroquiaExpanded = !parroquiaExpanded }
            ) {
                OutlinedTextField(
                    value = selectedParroquia.second,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Parroquia / Ubicación") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parroquiaExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = parroquiaExpanded,
                    onDismissRequest = { parroquiaExpanded = false }
                ) {
                    PARROQUIAS.forEach { par ->
                        DropdownMenuItem(
                            text = { Text(par.second) },
                            onClick = { selectedParroquia = par; parroquiaExpanded = false }
                        )
                    }
                }
            }

            // Evidencias
            Text(
                "Evidencias",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedButton(
                onClick = { showEvidenciaSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subir Evidencia")
            }

            if (evidenciasAgregadas.isNotEmpty()) {
                Text(
                    "${evidenciasAgregadas.size} archivo(s) adjunto(s)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Button(
                onClick = { /* TODO: Gestionar afectados */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gestionar Afectados")
            }
        }
    }

    // --- BottomSheet de Evidencias ---
    if (showEvidenciaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEvidenciaSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Agregar Evidencia",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                EvidenciaOptionCard(
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Tomar Foto",
                    description = "Usa la cámara del dispositivo"
                ) { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }

                EvidenciaOptionCard(
                    icon = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Galería de Imágenes",
                    description = "Selecciona imágenes o videos"
                ) { galleryLauncher.launch("image/*") }

                EvidenciaOptionCard(
                    icon = { Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Archivo de Audio",
                    description = "Adjunta una grabación de voz"
                ) { audioLauncher.launch("audio/*") }

                EvidenciaOptionCard(
                    icon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Documento / Archivo",
                    description = "PDF, Word u otros archivos"
                ) { documentLauncher.launch(arrayOf("application/pdf", "application/*", "text/*")) }
            }
        }
    }
}

@Composable
private fun EvidenciaOptionCard(
    icon: @Composable () -> Unit,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Crea un archivo temporal y devuelve su URI a través del FileProvider */
private fun crearUriCamara(context: Context): Uri {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val photoFile = File.createTempFile("IMG_${timestamp}_", ".jpg", storageDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
}
