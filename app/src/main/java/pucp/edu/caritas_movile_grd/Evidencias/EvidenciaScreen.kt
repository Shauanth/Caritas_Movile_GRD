package pucp.edu.caritas_movile_grd.Evidencias

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private enum class TipoEvidencia { IMAGEN, AUDIO, DOCUMENTO }

private val EXTENSIONES_IMAGEN = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
private val EXTENSIONES_AUDIO  = setOf("mp3", "wav", "ogg", "aac", "m4a", "flac")

private fun detectarTipo(context: Context, rutaLocal: String): TipoEvidencia {
    val mime = try { context.contentResolver.getType(Uri.parse(rutaLocal)) } catch (e: Exception) { null }
    val ext  = rutaLocal.substringAfterLast('.', "").lowercase().substringBefore('?')
    return when {
        mime?.startsWith("image") == true || ext in EXTENSIONES_IMAGEN -> TipoEvidencia.IMAGEN
        mime?.startsWith("audio") == true || ext in EXTENSIONES_AUDIO  -> TipoEvidencia.AUDIO
        else -> TipoEvidencia.DOCUMENTO
    }
}

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

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraImageUri?.let { uri ->
            viewModel.guardarEvidencia(EvidenciaLocal(UUID.randomUUID().toString(), uuidReferencia, uri.toString(), estadoSync = EstadoSync.PENDIENTE_SUBIDA))
        }
        showEvidenciaSheet = false
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            viewModel.guardarEvidencia(EvidenciaLocal(UUID.randomUUID().toString(), uuidReferencia, uri.toString(), estadoSync = EstadoSync.PENDIENTE_SUBIDA))
        }
        showEvidenciaSheet = false
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            viewModel.guardarEvidencia(EvidenciaLocal(UUID.randomUUID().toString(), uuidReferencia, it.toString(), estadoSync = EstadoSync.PENDIENTE_SUBIDA))
        }
        showEvidenciaSheet = false
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            viewModel.guardarEvidencia(EvidenciaLocal(UUID.randomUUID().toString(), uuidReferencia, it.toString(), estadoSync = EstadoSync.PENDIENTE_SUBIDA))
        }
        showEvidenciaSheet = false
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { val uri = crearUriCamara(context); cameraImageUri = uri; cameraLauncher.launch(uri) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evidencias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
                    icon = { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Tomar Foto",
                    description = "Usa la cámara del dispositivo"
                ) { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                EvidenciaOptionCard(
                    icon = { Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Galería de Imágenes",
                    description = "Selecciona imágenes o videos"
                ) { galleryLauncher.launch("image/*") }
                EvidenciaOptionCard(
                    icon = { Icon(Icons.Default.AudioFile, null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Archivo de Audio",
                    description = "Adjunta una grabación de voz"
                ) { audioLauncher.launch("audio/*") }
                EvidenciaOptionCard(
                    icon = { Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Documento / Archivo",
                    description = "PDF, Word u otros archivos"
                ) { documentLauncher.launch(arrayOf("application/pdf", "application/*", "text/*")) }
            }
        }
    }
}

@Composable
private fun EvidenciaItem(evidencia: EvidenciaLocal, onDelete: () -> Unit) {
    val context = LocalContext.current
    val uri = remember(evidencia.rutaLocal) { Uri.parse(evidencia.rutaLocal) }
    val tipo = remember(evidencia.rutaLocal) { detectarTipo(context, evidencia.rutaLocal) }
    val esPdf = remember(evidencia.rutaLocal) {
        val mime = try { context.contentResolver.getType(Uri.parse(evidencia.rutaLocal)) } catch (e: Exception) { null }
        val ext  = evidencia.rutaLocal.substringAfterLast('.', "").lowercase().substringBefore('?')
        mime == "application/pdf" || ext == "pdf"
    }
    var mostrarPreview by remember { mutableStateOf(false) }

    val thumbnail by produceState<ImageBitmap?>(initialValue = null, evidencia.rutaLocal) {
        value = withContext(Dispatchers.IO) {
            try {
                when {
                    tipo == TipoEvidencia.IMAGEN -> {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                            BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                        }
                    }
                    tipo == TipoEvidencia.DOCUMENTO && esPdf -> {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                            val renderer = PdfRenderer(fd)
                            val page = renderer.openPage(0)
                            val bmp = android.graphics.Bitmap.createBitmap(page.width, page.height, android.graphics.Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            renderer.close()
                            bmp.asImageBitmap()
                        }
                    }
                    else -> null
                }
            } catch (e: Exception) { null }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (tipo == TipoEvidencia.IMAGEN || (tipo == TipoEvidencia.DOCUMENTO && esPdf)) mostrarPreview = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                when {
                    thumbnail != null -> Image(bitmap = thumbnail!!, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    tipo == TipoEvidencia.IMAGEN    -> Icon(Icons.Default.Image,       null, tint = MaterialTheme.colorScheme.primary,   modifier = Modifier.size(32.dp))
                    tipo == TipoEvidencia.AUDIO     -> Icon(Icons.Default.AudioFile,   null, tint = MaterialTheme.colorScheme.tertiary,  modifier = Modifier.size(32.dp))
                    else                            -> Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(evidencia.rutaLocal.split("/").last(), fontWeight = FontWeight.Medium, maxLines = 1, fontSize = 14.sp)
                Text(
                    text = when (tipo) { TipoEvidencia.IMAGEN -> "Imagen"; TipoEvidencia.AUDIO -> "Audio"; TipoEvidencia.DOCUMENTO -> "Documento" },
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tipo == TipoEvidencia.IMAGEN || (tipo == TipoEvidencia.DOCUMENTO && esPdf)) {
                    Text("Toca para ver", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(2.dp))
                SyncBadge(estadoSync = evidencia.estadoSync)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (mostrarPreview && thumbnail != null) {
        ImagenPreviewDialog(bitmap = thumbnail!!, onDismiss = { mostrarPreview = false })
    }
}

@Composable
private fun SyncBadge(estadoSync: EstadoSync) {
    val (color, label) = when (estadoSync) {
        EstadoSync.SINCRONIZADO     -> MaterialTheme.colorScheme.primary to "Sincronizado"
        EstadoSync.PENDIENTE_SUBIDA -> Color(0xFFF59E0B) to "Pendiente de subida"
        EstadoSync.NUEVO            -> Color(0xFF6366F1) to "Nuevo"
        EstadoSync.EDITADO          -> Color(0xFFEC4899) to "Editado"
    }
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ImagenPreviewDialog(bitmap: ImageBitmap, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
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
