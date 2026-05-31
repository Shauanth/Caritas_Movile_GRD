package pucp.edu.caritas_movile_grd.Evidencias

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
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
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenciaScreen(
    uuidReferencia: String,
    viewModel: EvidenciaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val evidencias by viewModel.getEvidencias(uuidReferencia).collectAsState(initial = emptyList())
    var showEvidenciaSheet by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- Activity Result Launchers ---
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraImageUri?.let { uri ->
            viewModel.guardarEvidencia(
                EvidenciaLocal(
                    uuidEvidencia = UUID.randomUUID().toString(),
                    uuidReferencia = uuidReferencia,
                    rutaLocal = uri.toString(),
                    estadoSync = EstadoSync.PENDIENTE_SUBIDA
                )
            )
        }
        showEvidenciaSheet = false
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            viewModel.guardarEvidencia(
                EvidenciaLocal(
                    uuidEvidencia = UUID.randomUUID().toString(),
                    uuidReferencia = uuidReferencia,
                    rutaLocal = uri.toString(),
                    estadoSync = EstadoSync.PENDIENTE_SUBIDA
                )
            )
        }
        showEvidenciaSheet = false
    }
    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.guardarEvidencia(
                EvidenciaLocal(
                    uuidEvidencia = UUID.randomUUID().toString(),
                    uuidReferencia = uuidReferencia,
                    rutaLocal = it.toString(),
                    estadoSync = EstadoSync.PENDIENTE_SUBIDA
                )
            )
        }
        showEvidenciaSheet = false
    }
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.guardarEvidencia(
                EvidenciaLocal(
                    uuidEvidencia = UUID.randomUUID().toString(),
                    uuidReferencia = uuidReferencia,
                    rutaLocal = it.toString(),
                    estadoSync = EstadoSync.PENDIENTE_SUBIDA
                )
            )
        }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evidencias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showEvidenciaSheet = true }) {
                Icon(Icons.Default.UploadFile, contentDescription = "Subir Evidencia")
            }
        }
    ) { padding ->
        if (evidencias.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay evidencias para esta incidencia", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(evidencias) { evidencia ->
                    EvidenciaItem(evidencia = evidencia, onDelete = { viewModel.eliminarEvidencia(evidencia) })
                }
            }
        }
    }

    if (showEvidenciaSheet) {
        ModalBottomSheet(onDismissRequest = { showEvidenciaSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                Text("Agregar Evidencia", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
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
private fun EvidenciaItem(evidencia: EvidenciaLocal, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(evidencia.rutaLocal.split("/").last(), fontWeight = FontWeight.Medium, maxLines = 1)
                Text(evidencia.estadoSync.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EvidenciaOptionCard(icon: @Composable () -> Unit, label: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun crearUriCamara(context: Context): Uri {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val photoFile = File.createTempFile("IMG_${timestamp}_", ".jpg", storageDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
}
