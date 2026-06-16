package pucp.edu.caritas_movile_grd.Evidencias

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private fun detectarTipo(context: Context, rutaLocal: String): TipoEvidencia {
    val mime = try { context.contentResolver.getType(Uri.parse(rutaLocal)) } catch (e: Exception) { null }
    val ext  = rutaLocal.substringAfterLast('.', "").lowercase().substringBefore('?')
    return when {
        mime?.startsWith("image") == true || ext in EXTENSIONES_IMAGEN -> TipoEvidencia.IMAGEN
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
    val syncState by viewModel.syncState.collectAsState()
    var showEvidenciaSheet by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Sync al abrir la pantalla
    LaunchedEffect(uuidReferencia) { viewModel.sincronizar() }
    // Cuando el sync termina, Room Flow actualiza automáticamente la lista
    // (no llamamos sincronizar() aquí para evitar bucle)

    fun copiarAStoragePrivado(uri: Uri): String {
        val ext = context.contentResolver.getType(uri)
            ?.substringAfter('/')?.substringBefore(';') ?: "jpg"
        val nombre = "evidencia_${System.currentTimeMillis()}.$ext"
        val destino = java.io.File(context.filesDir, nombre)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destino.outputStream().use { output -> input.copyTo(output) }
        }
        return destino.absolutePath
    }

    fun confirmarYGuardar(uris: List<Uri>) {
        uris.forEach { uri ->
            val rutaLocal = try {
                copiarAStoragePrivado(uri)
            } catch (e: Exception) {
                uri.toString()
            }
            val mime = try { context.contentResolver.getType(uri) } catch (e: Exception) { null }
            val nombreArchivo = try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (it.moveToFirst() && idx >= 0) it.getString(idx) else null
                }
            } catch (e: Exception) { null }
            viewModel.guardarEvidencia(
                EvidenciaLocal(
                    uuidEvidencia  = UUID.randomUUID().toString(),
                    uuidReferencia = uuidReferencia,
                    rutaLocal      = rutaLocal,
                    nombreArchivo  = nombreArchivo,
                    contentType    = mime,
                    estadoSync     = EstadoSync.PENDIENTE_SUBIDA
                )
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
        if (uris.isNotEmpty()) pendingUris = uris
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        showEvidenciaSheet = false
        uri?.let { pendingUris = listOf(it) }
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
                    description = "JPG, PNG y otros formatos de imagen"
                ) { galleryLauncher.launch("image/*") }
                EvidenciaOptionCard(
                    icon = { Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary) },
                    label = "Documento / Archivo",
                    description = "PDF, Word u otros archivos"
                ) { documentLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/*")) }
            }
        }
    }
}

@Composable
private fun EvidenciaItem(evidencia: EvidenciaLocal, onDelete: () -> Unit) {
    val context = LocalContext.current
    val urlRemota = evidencia.urlS3?.takeIf { it.startsWith("http") }
    val esRutaAbsoluta = evidencia.rutaLocal.startsWith("/")
    val uri = remember(evidencia.rutaLocal) {
        if (esRutaAbsoluta) Uri.fromFile(java.io.File(evidencia.rutaLocal))
        else Uri.parse(evidencia.rutaLocal)
    }
    val tipo = remember(evidencia.rutaLocal, urlRemota) {
        when {
            urlRemota != null -> {
                val ext = urlRemota.substringBefore('?').substringAfterLast('.').lowercase()
                if (ext in EXTENSIONES_IMAGEN) TipoEvidencia.IMAGEN else TipoEvidencia.DOCUMENTO
            }
            else -> detectarTipo(context, evidencia.rutaLocal)
        }
    }
    val esPdf = remember(evidencia.rutaLocal, urlRemota) {
        if (urlRemota != null) {
            urlRemota.substringBefore('?').substringAfterLast('.').lowercase() == "pdf"
        } else {
            val mime = try { context.contentResolver.getType(uri) } catch (e: Exception) { null }
            val ext  = evidencia.rutaLocal.substringAfterLast('.', "").lowercase().substringBefore('?')
            mime == "application/pdf" || ext == "pdf"
        }
    }
    val nombreArchivo = remember(evidencia.rutaLocal) { obtenerNombreArchivo(context, uri) }
    var mostrarPreview by remember { mutableStateOf(false) }

    val thumbnail by produceState<ImageBitmap?>(initialValue = null, evidencia.rutaLocal, urlRemota) {
        value = withContext(Dispatchers.IO) {
            try {
                when {
                    tipo == TipoEvidencia.IMAGEN && urlRemota != null -> {
                        val conn = java.net.URL(urlRemota).openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 10_000
                        conn.readTimeout = 15_000
                        try {
                            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                            conn.inputStream.use { BitmapFactory.decodeStream(it, null, opts)?.asImageBitmap() }
                        } finally { conn.disconnect() }
                    }
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
                            val bmp = android.graphics.Bitmap.createBitmap(page.width, page.height, android.graphics.Bitmap.Config.ARGB_8888)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // ── Área de preview ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (tipo == TipoEvidencia.IMAGEN || esPdf) 180.dp else 96.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .then(
                    if (tipo == TipoEvidencia.IMAGEN || (tipo == TipoEvidencia.DOCUMENTO && esPdf))
                        Modifier.clickableNoRipple { mostrarPreview = true }
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                thumbnail != null -> {
                    Image(
                        bitmap = thumbnail!!,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Hint "toca para ampliar" solo en imágenes
                    if (tipo == TipoEvidencia.IMAGEN) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("Toca para ampliar", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
                tipo == TipoEvidencia.IMAGEN -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    Text("Cargando imagen…", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
                tipo == TipoEvidencia.AUDIO -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Text("Archivo de audio", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text("Vista previa no disponible", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        // ── Fila info + acciones ─────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombreArchivo,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    fontSize = 13.sp
                )
                Text(
                    text = when (tipo) {
                        TipoEvidencia.IMAGEN    -> "Imagen"
                        TipoEvidencia.AUDIO     -> "Audio"
                        TipoEvidencia.DOCUMENTO -> "Documento"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                SyncBadge(estadoSync = evidencia.estadoSync)
            }

            // Botón descargar
            IconButton(onClick = { descargarEvidencia(context, uri, nombreArchivo) }) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Descargar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // Botón eliminar
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (mostrarPreview && thumbnail != null) {
        ImagenPreviewDialog(bitmap = thumbnail!!, onDismiss = { mostrarPreview = false })
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(indication = null, interactionSource = null, onClick = onClick))

private fun obtenerNombreArchivo(context: Context, uri: Uri): String {
    // Intenta obtener el nombre real del archivo vía ContentResolver
    return try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return it.getString(idx)
            }
        }
        // Fallback: decodifica la URI y toma el último segmento
        Uri.decode(uri.lastPathSegment ?: uri.toString()).substringAfterLast('/')
    } catch (e: Exception) {
        Uri.decode(uri.toString()).substringAfterLast('/')
    }
}

private fun descargarEvidencia(context: Context, uri: Uri, nombreArchivo: String) {
    try {
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, nombreArchivo)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, mime)
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val destUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            destUri?.let { dest ->
                resolver.openOutputStream(dest)?.use { out ->
                    resolver.openInputStream(uri)?.use { inp -> inp.copyTo(out) }
                }
                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(dest, values, null, null)
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, nombreArchivo)
            context.contentResolver.openInputStream(uri)?.use { inp ->
                destFile.outputStream().use { out -> inp.copyTo(out) }
            }
        }
        android.widget.Toast.makeText(context, "Guardado en Descargas: $nombreArchivo", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error al descargar: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun SyncBadge(estadoSync: EstadoSync) {
    val (color, label) = when (estadoSync) {
        EstadoSync.SINCRONIZADO          -> MaterialTheme.colorScheme.primary to "Sincronizado"
        EstadoSync.PENDIENTE_SUBIDA      -> Color(0xFFF59E0B) to "Pendiente de subida"
        EstadoSync.NUEVO                 -> Color(0xFF6366F1) to "Nuevo"
        EstadoSync.EDITADO               -> Color(0xFFEC4899) to "Editado"
        EstadoSync.PENDIENTE_ELIMINACION -> Color(0xFFEF4444) to "Eliminando..."
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
