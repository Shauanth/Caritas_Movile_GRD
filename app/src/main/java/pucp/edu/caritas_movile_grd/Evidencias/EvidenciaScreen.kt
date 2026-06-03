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
import androidx.compose.ui.text.style.TextAlign
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
    // URI pendiente de confirmación antes de guardar en BD
    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    fun confirmarYGuardar(uris: List<Uri>) {
        uris.forEach { uri ->
            viewModel.guardarEvidencia(
                EvidenciaLocal(UUID.randomUUID().toString(), uuidReferencia, uri.toString(), estadoSync = EstadoSync.PENDIENTE_SUBIDA)
            )
        }
        pendingUris = emptyList()
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        showEvidenciaSheet = false
        if (success) cameraImageUri?.let { uri -> pendingUris = listOf(uri) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        showEvidenciaSheet = false
        uris.forEach { uri ->
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
        }
        if (uris.isNotEmpty()) pendingUris = uris
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        showEvidenciaSheet = false
        uri?.let {
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            pendingUris = listOf(it)
        }
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        showEvidenciaSheet = false
        uri?.let {
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            pendingUris = listOf(it)
        }
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

    if (pendingUris.isNotEmpty()) {
        PreviewConfirmDialog(
            uris = pendingUris,
            context = context,
            onConfirm = { confirmarYGuardar(pendingUris) },
            onDiscard = { pendingUris = emptyList() }
        )
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
        onClick = {
            when {
                tipo == TipoEvidencia.IMAGEN -> mostrarPreview = true
                tipo == TipoEvidencia.DOCUMENTO && esPdf -> {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) { }
                }
            }
        },
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
                when {
                    tipo == TipoEvidencia.IMAGEN -> Text("Toca para ver", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    tipo == TipoEvidencia.DOCUMENTO && esPdf -> Text("Toca para abrir", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
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

@Composable
private fun PreviewConfirmDialog(
    uris: List<Uri>,
    context: Context,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit
) {
    Dialog(onDismissRequest = onDiscard, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (uris.size > 1) "Confirmar ${uris.size} archivos" else "Confirmar archivo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Grilla de previsualizaciones (máx 4 visibles)
                val visible = uris.take(4)
                val columns = if (visible.size == 1) 1 else 2
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visible.chunked(columns).forEach { rowUris ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowUris.forEach { uri ->
                                PreviewTile(
                                    uri = uri,
                                    context = context,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(if (visible.size == 1) 16f / 9f else 1f)
                                )
                            }
                            // Relleno si la fila tiene solo un elemento en grid de 2
                            if (rowUris.size < columns) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (uris.size > 4) {
                    Text(
                        text = "+ ${uris.size - 4} archivo(s) más",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDiscard,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Descartar")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009850))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTile(uri: Uri, context: Context, modifier: Modifier = Modifier) {
    val tipo = remember(uri) { detectarTipo(context, uri.toString()) }
    val esPdf = remember(uri) {
        val mime = try { context.contentResolver.getType(uri) } catch (e: Exception) { null }
        val ext  = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase() ?: ""
        mime == "application/pdf" || ext == "pdf"
    }
    val nombre = remember(uri) {
        uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString().substringAfterLast('/')
    }

    val thumbnail by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                when {
                    tipo == TipoEvidencia.IMAGEN -> {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                            BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                        }
                    }
                    tipo == TipoEvidencia.DOCUMENTO && esPdf -> {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                            val renderer = PdfRenderer(fd)
                            val page = renderer.openPage(0)
                            val bmp = android.graphics.Bitmap.createBitmap(
                                page.width, page.height, android.graphics.Bitmap.Config.ARGB_8888
                            )
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close(); renderer.close()
                            bmp.asImageBitmap()
                        }
                    }
                    else -> null
                }
            } catch (e: Exception) { null }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            thumbnail != null -> Image(
                bitmap = thumbnail!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            tipo == TipoEvidencia.IMAGEN -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                Text("Cargando…", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            tipo == TipoEvidencia.AUDIO -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.tertiary)
                Text(nombre, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
                Text(nombre, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
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
