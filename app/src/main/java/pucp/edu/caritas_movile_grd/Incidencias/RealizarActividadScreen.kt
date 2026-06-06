package pucp.edu.caritas_movile_grd.Incidencias

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import java.text.SimpleDateFormat
import java.util.*

private val TIPO_DOC_MAP = mapOf(1 to "DNI", 2 to "CE", 3 to "Pasaporte", 4 to "Otro")
private val GREEN = Color(0xFF009850)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealizarActividadScreen(
    uuidIncidencia: String,
    viewModel: IncidenciaViewModel,
    onBack: () -> Unit
) {
    val incidencias by viewModel.incidencias.collectAsState()
    val incidencia = incidencias.find { it.uuidIncidencia == uuidIncidencia }
    val afectados by viewModel.getAfectados(uuidIncidencia).collectAsState(initial = emptyList())

    if (incidencia == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Info", "Familias")
    var showMapSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            incidencia.nombre.ifBlank { "Incidencia" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            incidenciaCode(incidencia),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { showMapSheet = true }) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Ver ubicación",
                            tint = GREEN
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Tab row ──────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GREEN
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> InfoTab(incidencia, viewModel)
                1 -> FamiliasTab(incidencia, afectados, viewModel)
            }
        }
    }

    // ── Mapa de ubicación ─────────────────────────────────────────────────────
    if (showMapSheet) {
        UbicacionMapSheet(incidencia = incidencia, onDismiss = { showMapSheet = false })
    }
}

// ════════════════════════════════════════════════════════════════════════════
// TAB INFO
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun InfoTab(incidencia: IncidenciaLocal, viewModel: IncidenciaViewModel) {
    var showObsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Badges ───────────────────────────────────────────────────────────
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryBadge(CATEGORIA_MAP[incidencia.idCatalogoTipo] ?: "Otros")
                StatusBadge(incidencia.estado)
            }
        }

        // ── Título + ID + fecha ───────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    incidencia.nombre.ifBlank { "Sin nombre" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        incidenciaCode(incidencia),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Text(
                        formatDateLong(incidencia.fechaSuceso ?: incidencia.fechaUltimaModificacion),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // ── Brigadista responsable ────────────────────────────────────────────
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InitialsAvatar(name = incidencia.responsable, color = GREEN)
                Text(
                    "brigadistas",
                    fontSize = 13.sp,
                    color = Color(0xFF555555)
                )
            }
        }

        // ── Contacto ─────────────────────────────────────────────────────────
        item {
            InfoSection(label = "CONTACTO") {
                if (!incidencia.reportadoPorNombre.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            incidencia.reportadoPorNombre,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (!incidencia.reportadoPorCelular.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GREEN
                                )
                                Text(
                                    incidencia.reportadoPorCelular,
                                    fontSize = 14.sp,
                                    color = GREEN,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    if (!incidencia.reportadoPorDni.isNullOrBlank()) {
                        Text("DNI: ${incidencia.reportadoPorDni}", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    Text("Sin contacto registrado", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }

        // ── Descripción ───────────────────────────────────────────────────────
        item {
            InfoSection(label = "DESCRIPCIÓN DEL SUCESO") {
                Text(
                    incidencia.descripcion.ifBlank { "Sin descripción" },
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // ── Causa ─────────────────────────────────────────────────────────────
        item {
            InfoSection(label = "CAUSA DEL SUCESO") {
                Text(
                    incidencia.causa ?: "No especificada",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (incidencia.causa.isNullOrBlank()) Color.Gray else Color.Unspecified
                )
            }
        }

        // ── Observaciones ─────────────────────────────────────────────────────
        item {
            InfoSection(label = "OBSERVACIONES DEL EQUIPO") {
                val observaciones = incidencia.observacionesCampo
                    ?.split("\n")
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()

                if (observaciones.isEmpty()) {
                    Text("Sin observaciones registradas.", fontSize = 13.sp, color = Color.Gray)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        observaciones.forEach { obs ->
                            ObservacionItem(
                                autor = incidencia.responsable,
                                texto = obs
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showObsDialog = true },
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GREEN, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar observación", color = GREEN, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showObsDialog) {
        AgregarObservacionDialog(
            onDismiss = { showObsDialog = false },
            onConfirm = { texto ->
                val actual = incidencia.observacionesCampo ?: ""
                val nuevo = if (actual.isBlank()) texto else "$actual\n$texto"
                viewModel.guardarIncidencia(
                    incidencia.copy(
                        observacionesCampo = nuevo,
                        fechaUltimaModificacion = System.currentTimeMillis()
                    )
                )
                showObsDialog = false
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// TAB FAMILIAS
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamiliasTab(
    incidencia: IncidenciaLocal,
    afectados: List<AfectadoLocal>,
    viewModel: IncidenciaViewModel
) {
    var filtro by remember { mutableStateOf("Todos") }
    var showAddDialog by remember { mutableStateOf(false) }
    val filtros = listOf("Todos", "Pendientes", "Confirmados")

    val afectadosFiltrados = when (filtro) {
        "Pendientes"  -> afectados.filter { it.estadoSync == EstadoSync.NUEVO || it.estadoSync == EstadoSync.EDITADO }
        "Confirmados" -> afectados.filter { it.estadoSync == EstadoSync.SINCRONIZADO }
        else          -> afectados
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // ── Chips de filtro ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filtros.forEach { f ->
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Header conteo ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Personas (${afectadosFiltrados.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "${afectados.size} en total",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // ── Lista de afectados ───────────────────────────────────────────
            if (afectadosFiltrados.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sin personas registradas",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(afectadosFiltrados) { afectado ->
                    AfectadoCard(afectado)
                }
            }

            // ── Botón agregar ────────────────────────────────────────────────
            item {
                TextButton(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GREEN, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar persona", color = GREEN, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showAddDialog) {
        AgregarPersonaDialog(
            uuidIncidencia = incidencia.uuidIncidencia,
            onDismiss = { showAddDialog = false },
            onConfirm = { afectado ->
                viewModel.guardarAfectado(afectado)
                showAddDialog = false
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun InfoSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun ObservacionItem(autor: String, texto: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InitialsAvatar(name = autor, color = GREEN, size = 32)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(autor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Gray
                )
            }
            Text(texto, fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFF333333))
        }
    }
}

@Composable
private fun AfectadoCard(afectado: AfectadoLocal) {
    val esPendiente = afectado.estadoSync == EstadoSync.NUEVO || afectado.estadoSync == EstadoSync.EDITADO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
                Column {
                    Text(afectado.nombres, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        "${TIPO_DOC_MAP[afectado.idCatalogoDoc] ?: "Doc"}: ${afectado.documentoIdentidad}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (esPendiente) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
            ) {
                Text(
                    if (esPendiente) "Pendiente" else "Confirmado",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (esPendiente) Color(0xFFE65100) else Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
private fun InitialsAvatar(name: String, color: Color, size: Int = 36) {
    val initials = name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "?" }

    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = Color.White,
            fontSize = (size * 0.38f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgregarObservacionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    var texto by remember { mutableStateOf("") }
    var grabando by remember { mutableStateOf(false) }
    var textoParcial by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val reconocimientoDisponible = remember {
        SpeechRecognizer.isRecognitionAvailable(context)
    }

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) {
        onDispose { recognizer.stopListening(); recognizer.destroy() }
    }

    val listener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { errorMsg = null }
            override fun onBeginningOfSpeech() { }
            override fun onRmsChanged(rmsdB: Float) { }
            override fun onBufferReceived(buffer: ByteArray?) { }
            override fun onEndOfSpeech() { }
            override fun onError(error: Int) {
                grabando = false
                textoParcial = ""
                errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH       -> "No se entendió. Intenta de nuevo."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sin conexión para reconocimiento."
                    SpeechRecognizer.ERROR_AUDIO          -> "Error de micrófono."
                    else -> "Error al escuchar (código $error)."
                }
            }
            override fun onResults(results: Bundle?) {
                val transcripcion = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                if (transcripcion.isNotBlank()) {
                    texto = if (texto.isBlank()) transcripcion else "$texto $transcripcion"
                }
                textoParcial = ""
                grabando = false
            }
            override fun onPartialResults(partialResults: Bundle?) {
                textoParcial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
            }
            override fun onEvent(eventType: Int, params: Bundle?) { }
        }
    }
    recognizer.setRecognitionListener(listener)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) iniciarGrabacion(recognizer) { grabando = true; errorMsg = null }
        else errorMsg = "Se necesita permiso de micrófono."
    }

    fun toggleGrabacion() {
        if (grabando) { recognizer.stopListening(); grabando = false }
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "scale"
    )

    ModalBottomSheet(onDismissRequest = {
        if (grabando) recognizer.stopListening()
        onDismiss()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Nueva observación", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // Campo de texto
            OutlinedTextField(
                value = if (grabando && textoParcial.isNotBlank()) "$texto $textoParcial"
                        else texto,
                onValueChange = { if (!grabando) texto = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (grabando) "Escuchando…"
                        else "Describe la observación de campo…",
                        color = if (grabando) GREEN.copy(alpha = 0.6f) else Color.Gray
                    )
                },
                minLines = 4,
                maxLines = 7,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = if (grabando) GREEN else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (grabando) GREEN.copy(alpha = 0.4f)
                                          else MaterialTheme.colorScheme.outline
                )
            )

            // Botón micrófono + hint — solo si el reconocimiento está disponible
            if (reconocimientoDisponible) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FloatingActionButton(
                        onClick = { toggleGrabacion() },
                        modifier = Modifier
                            .size(52.dp)
                            .scale(if (grabando) pulseScale else 1f),
                        containerColor = if (grabando) Color(0xFFDC2626) else GREEN,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp)
                    ) {
                        Icon(
                            imageVector = if (grabando) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (grabando) "Detener" else "Dictar",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = when {
                            grabando          -> "Grabando… toca para detener"
                            texto.isNotBlank() -> "Toca para agregar más"
                            else              -> "Toca el micrófono para dictar"
                        },
                        fontSize = 11.sp,
                        color = if (grabando) GREEN else Color.Gray
                    )
                }
            } else {
                Text(
                    "⚠ Reconocimiento de voz no disponible en este dispositivo.",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Error
            errorMsg?.let { msg ->
                Text(msg, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { if (grabando) recognizer.stopListening(); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) { Text("Cancelar") }
                Button(
                    onClick = { if (texto.isNotBlank()) onConfirm(texto.trim()) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GREEN),
                    enabled = texto.isNotBlank() && !grabando
                ) { Text("Guardar") }
            }
        }
    }
}

private fun iniciarGrabacion(recognizer: SpeechRecognizer, onStarted: () -> Unit) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-PE")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    recognizer.startListening(intent)
    onStarted()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgregarPersonaDialog(
    uuidIncidencia: String,
    onDismiss: () -> Unit,
    onConfirm: (AfectadoLocal) -> Unit
) {
    var nombres by remember { mutableStateOf("") }
    var documento by remember { mutableStateOf("") }
    var tipoDocIdx by remember { mutableIntStateOf(1) } // 1=DNI
    var expanded by remember { mutableStateOf(false) }
    val tiposDoc = listOf(1 to "DNI", 2 to "CE", 3 to "Pasaporte", 4 to "Otro")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Agregar persona afectada", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            OutlinedTextField(
                value = nombres,
                onValueChange = { nombres = it },
                label = { Text("Nombres completos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = tiposDoc.first { it.first == tipoDocIdx }.second,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de documento") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    tiposDoc.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { tipoDocIdx = id; expanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = documento,
                onValueChange = { documento = it },
                label = { Text("Número de documento") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        if (nombres.isNotBlank() && documento.isNotBlank()) {
                            onConfirm(
                                AfectadoLocal(
                                    uuidAfectado = UUID.randomUUID().toString(),
                                    uuidIncidencia = uuidIncidencia,
                                    idCatalogoDoc = tipoDocIdx,
                                    documentoIdentidad = documento.trim(),
                                    nombres = nombres.trim(),
                                    estadoSync = EstadoSync.NUEVO
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GREEN),
                    enabled = nombres.isNotBlank() && documento.isNotBlank()
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// HELPERS
// ════════════════════════════════════════════════════════════════════════════

// ── Bottom sheet de mapa ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UbicacionMapSheet(incidencia: IncidenciaLocal, onDismiss: () -> Unit) {
    val context = LocalContext.current

    val queryParts = listOfNotNull(
        incidencia.direccion?.takeIf { it.isNotBlank() },
        incidencia.distrito?.takeIf { it.isNotBlank() },
        "Lima", "Perú"
    )
    val queryEncoded = Uri.encode(queryParts.joinToString(", "))

    fun abrirEnMaps() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$queryEncoded"))
        try { context.startActivity(intent) } catch (_: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://maps.google.com/maps?q=$queryEncoded")))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = GREEN, modifier = Modifier.size(22.dp))
                    Text("Ubicación del evento", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Filas de datos
            if (!incidencia.direccion.isNullOrBlank())
                UbicacionDetalle("Dirección", incidencia.direccion)

            Spacer(Modifier.height(12.dp))
            UbicacionDetalle(
                "Distrito / Región",
                listOfNotNull(incidencia.distrito, "Lima Metropolitana").joinToString(", ")
            )

            if (!incidencia.parroquiaNombre.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                UbicacionDetalle("Parroquia de referencia", incidencia.parroquiaNombre)
            }

            if (!incidencia.referencia.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                UbicacionDetalle("Referencias", incidencia.referencia)
            }

            Spacer(Modifier.height(24.dp))

            // Botón principal — abre Google Maps nativo
            Button(
                onClick = { abrirEnMaps() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GREEN)
            ) {
                Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ver en Google Maps", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun UbicacionDetalle(label: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1A1A1A))
        Text(valor, fontSize = 13.sp, color = Color(0xFF555555), lineHeight = 18.sp)
    }
}

private fun incidenciaCode(incidencia: IncidenciaLocal): String {
    return incidencia.codigoCasoRemoto?.let { "GRD · $it" }
        ?: "LOCAL-${incidencia.uuidIncidencia.takeLast(8)}"
}

private fun formatDateLong(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE d 'de' MMMM 'del' yyyy, hh:mm a", Locale("es", "PE"))
    return sdf.format(Date(timestamp))
}
