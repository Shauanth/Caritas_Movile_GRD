package pucp.edu.caritas_movile_grd.Incidencias

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import pucp.edu.caritas_movile_grd.LocalBDConector.SyncViewModel
import pucp.edu.caritas_movile_grd.Network.MobileApiConfig
import java.text.SimpleDateFormat
import java.util.*
import pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal
import pucp.edu.caritas_movile_grd.Seguimientos.SeguimientoLocal
import pucp.edu.caritas_movile_grd.Evidencias.EvidenciaViewModel
import pucp.edu.caritas_movile_grd.Evidencias.EvidenciaContent
import pucp.edu.caritas_movile_grd.Kits.EntregaKitLocal
import pucp.edu.caritas_movile_grd.Kits.KitRepository
import pucp.edu.caritas_movile_grd.Kits.KitViewModel
import pucp.edu.caritas_movile_grd.Kits.KitAsignadoLocal
import pucp.edu.caritas_movile_grd.Kits.KitArticuloAsignadoLocal
private val TIPO_DOC_MAP = mapOf(1 to "DNI", 2 to "CE", 3 to "Pasaporte", 4 to "Otro")
private val GREEN = Color(0xFF009850)

// ── Catálogos del empadronamiento de campo (alineados con la web) ──────────────
private val TIPOS_DOC_RA = listOf(1 to "DNI", 2 to "CE", 3 to "Pasaporte", 4 to "Otro")
private val GENEROS_RA = listOf("Femenino", "Masculino", "Otro", "Prefiere no decir")
private val PARENTESCOS_RA = listOf(
    "Jefe(a) de Hogar", "Padre", "Madre", "Hijo(a)",
    "Nieto(a)", "Abuelo(a)", "Tío(a)", "Cónyuge", "Otro"
)
private val SITUACIONES_RA = listOf(
    "Gestante", "Discapacitado", "Con Lactancia", "Enfermo",
    "Herido", "Enfermo crónico", "Adulto mayor"
)

/** Normaliza el género almacenado (puede venir como "M"/"F" del backend) a etiqueta de UI. */
private fun generoDisplay(genero: String?): String = when (genero?.trim()?.lowercase()) {
    "m", "masculino" -> "Masculino"
    "f", "femenino" -> "Femenino"
    null, "" -> "Femenino"
    else -> genero!!
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealizarActividadScreen(
    uuidIncidencia: String,
    viewModel: IncidenciaViewModel,
    evidenciaViewModel: EvidenciaViewModel,
    syncViewModel: SyncViewModel,
    kitViewModel: KitViewModel,
    onBack: () -> Unit
) {
    val incidencias by viewModel.incidencias.collectAsState()
    val incidencia = incidencias.find { it.uuidIncidencia == uuidIncidencia }
    val afectados by viewModel.getAfectados(uuidIncidencia).collectAsState(initial = emptyList())
    val context = LocalContext.current

    if (incidencia == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Info", "Familias", "Observaciones", "Evidencias", "Kits")
    var showMapSheet by remember { mutableStateOf(false) }
    var showFinalizarDialog by remember { mutableStateOf(false) }

    val puedeFinalizarRecopilacion = incidencia.estado == "ASIGNADO" &&
        (incidencia.idResponsableGRD == MobileApiConfig.MOBILE_SYNC_USER_ID ||
         incidencia.uuidUsuario == MobileApiConfig.MOBILE_SYNC_USER_ID)

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
                    if (puedeFinalizarRecopilacion) {
                        TextButton(onClick = { showFinalizarDialog = true }) {
                            Text("Finalizar", color = GREEN, fontWeight = FontWeight.Bold)
                        }
                    }
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
                2 -> ObservacionesTab(incidencia, viewModel)
                3 -> EvidenciaContent(
                    uuidReferencia = incidencia.uuidIncidencia,
                    viewModel = evidenciaViewModel,
                    modifier = Modifier.fillMaxSize()
                )
                4 -> KitsTab(incidencia, afectados, kitViewModel)
            }
        }
    }

    // ── Mapa de ubicación ─────────────────────────────────────────────────────
    if (showMapSheet) {
        UbicacionMapSheet(incidencia = incidencia, onDismiss = { showMapSheet = false })
    }

    // ── Finalizar Recopilación ────────────────────────────────────────────────
    if (showFinalizarDialog) {
        AlertDialog(
            onDismissRequest = { showFinalizarDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GREEN, modifier = Modifier.size(32.dp)) },
            title = { Text("Finalizar Recopilación", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Confirmas que completaste la recopilación de datos?")
                    Text(
                        "El estado cambiará a DATA RECOPILADA y se sincronizará automáticamente.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinalizarDialog = false
                        viewModel.finalizarRecopilacion(incidencia) { ok, message ->
                            Toast.makeText(
                                context,
                                if (ok) {
                                    "Recopilación finalizada."
                                } else {
                                    message ?: "No se pudo finalizar la recopilación."
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GREEN)
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalizarDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// TAB INFO
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun InfoTab(incidencia: IncidenciaLocal, viewModel: IncidenciaViewModel) {
    val observacionesLocales by viewModel
    .getObservaciones(incidencia.uuidIncidencia)
    .collectAsState(initial = emptyList())

    val seguimientosLocales by viewModel
    .getSeguimientos(incidencia.uuidIncidencia)
    .collectAsState(initial = emptyList())
    var showObsDialog by remember { mutableStateOf(false) }
    var showSeguimientoDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showFinalizarDialog by remember { mutableStateOf(false) }
    var finalizandoRecopilacion by remember { mutableStateOf(false) }

    val puedeFinalizarRecopilacion = incidencia.estado.equals("ASIGNADO", ignoreCase = true)
    val recopilacionFinalizada = incidencia.estado.equals("DATA RECOPILADA", ignoreCase = true)

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
                val observacionesLegacy = incidencia.observacionesCampo
                    ?.split("\n")
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()

                val observaciones = if (observacionesLocales.isNotEmpty()) {
                    observacionesLocales.map { it.textoObservacion }
                } else {
                    observacionesLegacy
                }

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
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = GREEN,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar observación", color = GREEN, fontWeight = FontWeight.Medium)
                }
            }
        }
        item {
            if (puedeFinalizarRecopilacion) {
                Button(
                    onClick = { showFinalizarDialog = true },
                    enabled = !finalizandoRecopilacion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GREEN,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (finalizandoRecopilacion) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalizando...")
                    } else {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalizar recopilación")
                    }
                }
            } else if (recopilacionFinalizada) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GREEN
                        )
                        Column {
                            Text(
                                "Recopilación finalizada",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                "La información de campo ya fue enviada para revisión.",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }


        // ── Seguimiento ───────────────────────────────────────────────────────
        item {
            InfoSection(label = "SEGUIMIENTO") {
                Text(
                    "Registra una actualización de seguimiento sobre la situación del caso.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                if (seguimientosLocales.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sin seguimientos registrados.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        seguimientosLocales.forEach { seguimiento ->
                            SeguimientoItem(seguimiento)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { showSeguimientoDialog = true },
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(
                        Icons.Default.AddTask,
                        contentDescription = null,
                        tint = GREEN,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar seguimiento", color = GREEN, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showObsDialog) {
        AgregarObservacionDialog(
            onDismiss = { showObsDialog = false },
            onConfirm = { texto ->
                val textoLimpio = texto.trim()
                val ahora = System.currentTimeMillis()

                val actual = incidencia.observacionesCampo ?: ""
                val nuevo = if (actual.isBlank()) textoLimpio else "$actual\n$textoLimpio"

                // 1. Mantiene la observación visible en la pantalla actual
                viewModel.guardarIncidencia(
                    incidencia.copy(
                        observacionesCampo = nuevo,
                        fechaUltimaModificacion = ahora
                    )
                )

                // 2. Guarda la observación como entidad local pendiente de sincronización
                viewModel.guardarObservacion(
                    ObservacionLocal(
                        uuidObservacion = UUID.randomUUID().toString(),
                        uuidIncidencia = incidencia.uuidIncidencia,
                        textoObservacion = textoLimpio,
                        fechaRegistro = ahora.toString(),
                        estadoSync = EstadoSync.NUEVO
                    )
                )

                showObsDialog = false
            }
        )

        
    }
    if (showSeguimientoDialog) {
        AgregarSeguimientoDialog(
            onDismiss = { showSeguimientoDialog = false },
            onConfirm = { situacion, descripcion, necesidades, recomendaciones ->
                val ahora = System.currentTimeMillis()

                viewModel.guardarSeguimiento(
                    SeguimientoLocal(
                        uuidSeguimiento = UUID.randomUUID().toString(),
                        uuidIncidencia = incidencia.uuidIncidencia,
                        situacion = situacion.trim().ifBlank { null },
                        descripcion = descripcion.trim().ifBlank { null },
                        necesidadesPendientes = necesidades.trim().ifBlank { null },
                        recomendaciones = recomendaciones.trim().ifBlank { null },
                        estado = "REGISTRADO",
                        observaciones = null,
                        fechaSeguimiento = ahora.toString(),
                        estadoSync = EstadoSync.NUEVO
                    )
                )

                showSeguimientoDialog = false
            }
        )
    } 
    if (showFinalizarDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!finalizandoRecopilacion) {
                    showFinalizarDialog = false
                }
            },
            title = {
                Text("Finalizar recopilación")
            },
            text = {
                Text(
                    "¿Confirmas que ya terminaste de recopilar la información de campo? " +
                        "El caso pasará a DATA RECOPILADA y será revisado desde la web."
                )
            },
            confirmButton = {
                Button(
                    enabled = !finalizandoRecopilacion,
                    onClick = {
                        showFinalizarDialog = false
                        finalizandoRecopilacion = true

                        viewModel.finalizarRecopilacion(incidencia) { ok, message ->
                            finalizandoRecopilacion = false

                            Toast.makeText(
                                context,
                                if (ok) {
                                    "Recopilación finalizada."
                                } else {
                                    message ?: "No se pudo finalizar la recopilación."
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GREEN,
                        contentColor = Color.White
                    )
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !finalizandoRecopilacion,
                    onClick = { showFinalizarDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }


}

// ════════════════════════════════════════════════════════════════════════════
// TAB FAMILIAS
// ════════════════════════════════════════════════════════════════════════════

// Vista agregada de un grupo familiar (combina la fila local con sus integrantes).
private data class GrupoView(
    val id: String,
    val nombre: String,
    val observaciones: String?,
    val verificado: Boolean,
    val miembros: List<AfectadoLocal>,
    val row: GrupoFamiliarLocal?
)

// Estado del sheet de persona (alta o edición).
private data class PersonaSheetState(
    val editing: AfectadoLocal? = null,
    val familiaId: String? = editing?.familiaId,
    val familiaNombre: String? = editing?.familiaNombre
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamiliasTab(
    incidencia: IncidenciaLocal,
    afectados: List<AfectadoLocal>,
    viewModel: IncidenciaViewModel
) {
    val grupos by viewModel.getGruposFamiliares(incidencia.uuidIncidencia)
        .collectAsState(initial = emptyList())

    var personaSheet by remember { mutableStateOf<PersonaSheetState?>(null) }
    var showGrupoDialog by remember { mutableStateOf(false) }
    var obsGrupo by remember { mutableStateOf<GrupoFamiliarLocal?>(null) }
    var confirmarBorrarPersona by remember { mutableStateOf<AfectadoLocal?>(null) }
    var confirmarBorrarGrupo by remember { mutableStateOf<GrupoView?>(null) }

    // Agrupar afectados por familiaId; los que no tienen familia van como individuales.
    val personasIndividuales = afectados.filter { it.familiaId.isNullOrBlank() }
    val porFamilia = afectados
        .filter { !it.familiaId.isNullOrBlank() }
        .groupBy { it.familiaId!! }

    // La vista de grupos combina las filas locales con los familiaId presentes en afectados
    // (los afectados descargados del backend traen familiaId aunque aún no exista fila local).
    val gruposPorId = grupos.associateBy { it.uuidGrupo }
    val familiaIds = (grupos.map { it.uuidGrupo } + porFamilia.keys).distinct()
    val gruposVista = familiaIds.map { id ->
        val miembros = porFamilia[id].orEmpty()
        val row = gruposPorId[id]
        GrupoView(
            id = id,
            nombre = row?.nombre
                ?: miembros.firstOrNull()?.familiaNombre
                ?: "Grupo Familiar",
            observaciones = row?.observaciones,
            verificado = row?.verificado ?: false,
            miembros = miembros,
            row = row
        )
    }.sortedBy { it.nombre }

    // Filtro por estado de verificación de campo.
    var filtroVerif by remember { mutableStateOf("Todas") }
    val gruposMostrados = when (filtroVerif) {
        "Pendientes" -> gruposVista.filter { !it.verificado }
        "Verificadas" -> gruposVista.filter { it.verificado }
        else -> gruposVista
    }
    val verificadas = gruposVista.count { it.verificado }

    // Crea o actualiza la fila local del grupo y alterna su verificación.
    fun toggleVerificado(gv: GrupoView) {
        val base = gv.row ?: GrupoFamiliarLocal(
            uuidGrupo = gv.id,
            uuidIncidencia = incidencia.uuidIncidencia,
            nombre = gv.nombre,
            observaciones = gv.observaciones
        )
        viewModel.guardarGrupoFamiliar(base.copy(verificado = !gv.verificado))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Encabezado ───────────────────────────────────────────────────────
        item {
            Column {
                Text(
                    "PERSONAS AFECTADAS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Text(
                    "${gruposVista.size} grupo(s) familiar(es) · ${afectados.size} persona(s)",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                if (gruposVista.isNotEmpty()) {
                    Text(
                        "$verificadas de ${gruposVista.size} familia(s) verificada(s)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (verificadas == gruposVista.size) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }
            }
        }

        // ── Filtro de verificación ───────────────────────────────────────────
        if (gruposVista.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todas", "Pendientes", "Verificadas").forEach { f ->
                        FilterChip(
                            selected = filtroVerif == f,
                            onClick = { filtroVerif = f },
                            label = { Text(f, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GREEN,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // ── Grupos familiares ────────────────────────────────────────────────
        items(gruposMostrados, key = { it.id }) { gv ->
            FamiliaGrupoCard(
                grupo = gv,
                onToggleVerificado = { toggleVerificado(gv) },
                onAddMember = {
                    personaSheet = PersonaSheetState(familiaId = gv.id, familiaNombre = gv.nombre)
                },
                onComment = {
                    obsGrupo = gv.row ?: GrupoFamiliarLocal(
                        uuidGrupo = gv.id,
                        uuidIncidencia = incidencia.uuidIncidencia,
                        nombre = gv.nombre
                    )
                },
                onDeleteGrupo = { confirmarBorrarGrupo = gv },
                onEditPersona = { p ->
                    personaSheet = PersonaSheetState(editing = p, familiaId = gv.id, familiaNombre = gv.nombre)
                },
                onDeletePersona = { p -> confirmarBorrarPersona = p }
            )
        }

        // ── Personas individuales ────────────────────────────────────────────
        if (personasIndividuales.isNotEmpty()) {
            item {
                Text(
                    "Personas individuales",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
            }
            items(personasIndividuales, key = { it.uuidAfectado }) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    PersonaRow(
                        afectado = p,
                        onEdit = { personaSheet = PersonaSheetState(editing = p) },
                        onDelete = { confirmarBorrarPersona = p }
                    )
                }
            }
        }

        if (gruposVista.isEmpty() && personasIndividuales.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sin personas registradas aún.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ── Acciones ─────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showGrupoDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Registrar Grupo", fontSize = 13.sp)
                }
                Button(
                    onClick = { personaSheet = PersonaSheetState() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GREEN)
                ) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar Persona", fontSize = 13.sp)
                }
            }
        }
    }

    // ── Sheet de persona (alta / edición) ─────────────────────────────────────
    personaSheet?.let { st ->
        PersonaCampoSheet(
            uuidIncidencia = incidencia.uuidIncidencia,
            estado = st,
            grupos = gruposVista.map { it.id to it.nombre },
            onDismiss = { personaSheet = null },
            onSave = { afectado ->
                viewModel.guardarAfectado(afectado)
                personaSheet = null
            }
        )
    }

    // ── Diálogo: nuevo grupo familiar ─────────────────────────────────────────
    if (showGrupoDialog) {
        RegistrarGrupoDialog(
            sugerido = "Grupo Familiar ${gruposVista.size + 1}",
            onDismiss = { showGrupoDialog = false },
            onConfirm = { nombre ->
                viewModel.guardarGrupoFamiliar(
                    GrupoFamiliarLocal(
                        uuidGrupo = UUID.randomUUID().toString(),
                        uuidIncidencia = incidencia.uuidIncidencia,
                        nombre = nombre,
                        estadoSync = EstadoSync.NUEVO
                    )
                )
                showGrupoDialog = false
            }
        )
    }

    // ── Diálogo: observaciones de la familia ──────────────────────────────────
    obsGrupo?.let { g ->
        ObsFamiliaDialog(
            grupo = g,
            onDismiss = { obsGrupo = null },
            onSave = { texto ->
                viewModel.guardarGrupoFamiliar(g.copy(observaciones = texto.ifBlank { null }))
                obsGrupo = null
            }
        )
    }

    // ── Confirmaciones de borrado ─────────────────────────────────────────────
    confirmarBorrarPersona?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmarBorrarPersona = null },
            title = { Text("Eliminar persona") },
            text = { Text("¿Eliminar a ${p.nombres} del empadronamiento?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarAfectado(p.uuidAfectado)
                        confirmarBorrarPersona = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarBorrarPersona = null }) { Text("Cancelar") }
            }
        )
    }

    confirmarBorrarGrupo?.let { gv ->
        AlertDialog(
            onDismissRequest = { confirmarBorrarGrupo = null },
            title = { Text("Eliminar grupo familiar") },
            text = { Text("¿Eliminar \"${gv.nombre}\" y sus ${gv.miembros.size} integrante(s)?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarGrupoFamiliar(gv.id)
                        confirmarBorrarGrupo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarBorrarGrupo = null }) { Text("Cancelar") }
            }
        )
    }
}

// ── Tarjeta de grupo familiar ─────────────────────────────────────────────────
@Composable
private fun FamiliaGrupoCard(
    grupo: GrupoView,
    onToggleVerificado: () -> Unit,
    onAddMember: () -> Unit,
    onComment: () -> Unit,
    onDeleteGrupo: () -> Unit,
    onEditPersona: (AfectadoLocal) -> Unit,
    onDeletePersona: (AfectadoLocal) -> Unit
) {
    val tieneObs = !grupo.observaciones.isNullOrBlank()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Cabecera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F1FB))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Group, null, tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    grupo.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0D47A1),
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "(${grupo.miembros.size} integrante${if (grupo.miembros.size != 1) "s" else ""})",
                    fontSize = 12.sp,
                    color = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAddMember, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PersonAdd, "Agregar integrante", tint = GREEN, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onComment, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        "Comentario de familia",
                        tint = if (tieneObs) Color(0xFF1565C0) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDeleteGrupo, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Eliminar grupo", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                }
            }

            // Estado de verificación de empadronamiento
            val verifBg = if (grupo.verificado) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            val verifColor = if (grupo.verificado) Color(0xFF2E7D32) else Color(0xFFE65100)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(verifBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (grupo.verificado) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        null,
                        tint = verifColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (grupo.verificado) "Familia verificada" else "Pendiente de verificación",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = verifColor
                    )
                }
                TextButton(onClick = onToggleVerificado, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(
                        if (grupo.verificado) "Marcar pendiente" else "Marcar verificada",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = verifColor
                    )
                }
            }

            // Comentario de la familia
            if (tieneObs) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F9FF))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(grupo.observaciones!!, fontSize = 12.sp, color = Color(0xFF37474F), lineHeight = 16.sp)
                }
            }

            // Integrantes
            if (grupo.miembros.isEmpty()) {
                Text(
                    "Sin personas registradas en este grupo.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                grupo.miembros.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(color = Color(0xFFEEEEEE))
                    PersonaRow(
                        afectado = p,
                        onEdit = { onEditPersona(p) },
                        onDelete = { onDeletePersona(p) }
                    )
                }
            }
        }
    }
}

// ── Fila de persona afectada (editar / eliminar) ──────────────────────────────
@Composable
private fun PersonaRow(
    afectado: AfectadoLocal,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val nombreCompleto = listOfNotNull(
        afectado.nombres.takeIf { it.isNotBlank() },
        afectado.apellidoPaterno?.takeIf { it.isNotBlank() },
        afectado.apellidoMaterno?.takeIf { it.isNotBlank() }
    ).joinToString(" ").ifBlank { "Sin nombre" }

    val subtitulo = listOfNotNull(
        afectado.edad?.let { "$it años" },
        afectado.genero?.let { generoDisplay(it) },
        afectado.documentoIdentidad
            .takeIf { it.isNotBlank() }
            ?.let { "${TIPO_DOC_MAP[afectado.idCatalogoDoc] ?: "Doc"}: $it" }
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(nombreCompleto, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    afectado.parentesco?.takeIf { it.isNotBlank() }?.let { PersonaBadge(it, Color(0xFF1565C0), Color(0xFFE3F2FD)) }
                }
                if (subtitulo.isNotBlank()) {
                    Text(subtitulo, fontSize = 12.sp, color = Color.Gray)
                }
                afectado.situacionActual?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    PersonaBadge(it, Color(0xFFE65100), Color(0xFFFFF3E0))
                }
            }
        }
        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, "Editar", tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Eliminar", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PersonaBadge(texto: String, textColor: Color, bgColor: Color) {
    Surface(shape = RoundedCornerShape(50), color = bgColor) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
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
private fun SeguimientoItem(seguimiento: SeguimientoLocal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                seguimiento.estado,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = GREEN
            )

            seguimiento.situacion?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Situación: $it",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            seguimiento.descripcion?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Descripción: $it",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            seguimiento.necesidadesPendientes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Necesidades pendientes: $it",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            seguimiento.recomendaciones?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Recomendaciones: $it",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
// (AfectadoCard fue reemplazada por PersonaRow / FamiliaGrupoCard en el tab Familias.)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgregarSeguimientoDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        situacion: String,
        descripcion: String,
        necesidades: String,
        recomendaciones: String
    ) -> Unit
) {
    var situacion by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var necesidades by remember { mutableStateOf("") }
    var recomendaciones by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Nuevo seguimiento", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            OutlinedTextField(
                value = situacion,
                onValueChange = { situacion = it },
                label = { Text("Situación actual") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción del seguimiento") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = necesidades,
                onValueChange = { necesidades = it },
                label = { Text("Necesidades pendientes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = recomendaciones,
                onValueChange = { recomendaciones = it },
                label = { Text("Recomendaciones") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        onConfirm(
                            situacion,
                            descripcion,
                            necesidades,
                            recomendaciones
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GREEN),
                    enabled = situacion.isNotBlank() || descripcion.length >= 5
                ) {
                    Text("Guardar")
                }
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

// ── Sheet de persona afectada (alta / edición) ────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonaCampoSheet(
    uuidIncidencia: String,
    estado: PersonaSheetState,
    grupos: List<Pair<String, String>>, // id de grupo a nombre
    onDismiss: () -> Unit,
    onSave: (AfectadoLocal) -> Unit
) {
    val editing = estado.editing
    var tipoDocIdx by remember { mutableIntStateOf(editing?.idCatalogoDoc ?: 1) }
    var documento by remember { mutableStateOf(editing?.documentoIdentidad ?: "") }
    var nombres by remember { mutableStateOf(editing?.nombres ?: "") }
    var apPaterno by remember { mutableStateOf(editing?.apellidoPaterno ?: "") }
    var apMaterno by remember { mutableStateOf(editing?.apellidoMaterno ?: "") }
    var edad by remember { mutableStateOf(editing?.edad?.toString() ?: "") }
    var genero by remember { mutableStateOf(generoDisplay(editing?.genero)) }
    var celular by remember { mutableStateOf(editing?.celular ?: "") }
    var parentesco by remember { mutableStateOf(editing?.parentesco ?: "") }
    var situacion by remember { mutableStateOf(editing?.situacionActual ?: "") }
    var familiaSel by remember { mutableStateOf(estado.familiaId) }

    var errNombres by remember { mutableStateOf<String?>(null) }
    var errEdad by remember { mutableStateOf<String?>(null) }

    val titulo = when {
        editing != null -> "Editar persona"
        estado.familiaNombre != null -> "Agregar integrante — ${estado.familiaNombre}"
        else -> "Agregar persona afectada"
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // Tipo doc + número
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownFieldRA(
                    label = "Tipo doc.",
                    selected = TIPOS_DOC_RA.first { it.first == tipoDocIdx }.second,
                    options = TIPOS_DOC_RA.map { it.second },
                    onSelect = { sel -> tipoDocIdx = TIPOS_DOC_RA.first { it.second == sel }.first },
                    modifier = Modifier.weight(0.4f)
                )
                OutlinedTextField(
                    value = documento,
                    onValueChange = { documento = it },
                    label = { Text("N° Documento", fontSize = 12.sp) },
                    modifier = Modifier.weight(0.6f),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = nombres,
                onValueChange = { nombres = it; errNombres = null },
                label = { Text("Nombres *", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                isError = errNombres != null,
                supportingText = errNombres?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = apPaterno,
                    onValueChange = { apPaterno = it },
                    label = { Text("Ap. Paterno", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = apMaterno,
                    onValueChange = { apMaterno = it },
                    label = { Text("Ap. Materno", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = edad,
                    onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) { edad = it; errEdad = null } },
                    label = { Text("Edad *", fontSize = 12.sp) },
                    modifier = Modifier.weight(0.3f),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errEdad != null
                )
                DropdownFieldRA(
                    label = "Género",
                    selected = genero,
                    options = GENEROS_RA,
                    onSelect = { genero = it },
                    modifier = Modifier.weight(0.7f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = celular,
                    onValueChange = { if (it.length <= 9 && it.all { c -> c.isDigit() }) celular = it },
                    label = { Text("Celular", fontSize = 12.sp) },
                    modifier = Modifier.weight(0.5f),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                DropdownFieldRA(
                    label = "Parentesco",
                    selected = parentesco,
                    options = listOf("") + PARENTESCOS_RA,
                    onSelect = { parentesco = it },
                    modifier = Modifier.weight(0.5f)
                )
            }

            DropdownFieldRA(
                label = "Situación especial",
                selected = situacion,
                options = listOf("") + SITUACIONES_RA,
                onSelect = { situacion = it },
                modifier = Modifier.fillMaxWidth()
            )

            if (grupos.isNotEmpty()) {
                val nombreSel = grupos.firstOrNull { it.first == familiaSel }?.second ?: "Sin grupo"
                DropdownFieldRA(
                    label = "Grupo familiar",
                    selected = nombreSel,
                    options = listOf("Sin grupo") + grupos.map { it.second },
                    onSelect = { sel ->
                        familiaSel = if (sel == "Sin grupo") null else grupos.firstOrNull { it.second == sel }?.first
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = {
                        errNombres = if (nombres.isBlank()) "Requerido" else null
                        errEdad = if (edad.isBlank()) "Requerido" else null
                        if (errNombres != null || errEdad != null) return@Button

                        val familiaNombre = grupos.firstOrNull { it.first == familiaSel }?.second
                        // Si la persona ya existía en backend, marcarla EDITADO; si es nueva, NUEVO.
                        val nuevoEstadoSync =
                            if (editing?.idAfectadoRemoto != null) EstadoSync.EDITADO else EstadoSync.NUEVO

                        onSave(
                            AfectadoLocal(
                                uuidAfectado = editing?.uuidAfectado ?: UUID.randomUUID().toString(),
                                uuidIncidencia = uuidIncidencia,
                                idCatalogoDoc = tipoDocIdx,
                                idAfectadoRemoto = editing?.idAfectadoRemoto,
                                documentoIdentidad = documento.trim(),
                                nombres = nombres.trim(),
                                apellidoPaterno = apPaterno.trim().ifBlank { null },
                                apellidoMaterno = apMaterno.trim().ifBlank { null },
                                edad = edad.toIntOrNull(),
                                genero = genero,
                                celular = celular.trim().ifBlank { null },
                                parentesco = parentesco.ifBlank { null },
                                situacionActual = situacion.ifBlank { null },
                                familiaId = familiaSel,
                                familiaNombre = familiaNombre,
                                estadoSync = nuevoEstadoSync
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GREEN)
                ) { Text("Guardar") }
            }
        }
    }
}

// ── Diálogo: registrar grupo familiar ─────────────────────────────────────────
@Composable
private fun RegistrarGrupoDialog(
    sugerido: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nombre by remember { mutableStateOf(sugerido) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo grupo familiar") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre / apellido de referencia") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (nombre.isNotBlank()) onConfirm(nombre.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = GREEN),
                enabled = nombre.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Diálogo: observaciones del grupo familiar ─────────────────────────────────
@Composable
private fun ObsFamiliaDialog(
    grupo: GrupoFamiliarLocal,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var obs by remember { mutableStateOf(grupo.observaciones ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Observaciones — ${grupo.nombre}") },
        text = {
            OutlinedTextField(
                value = obs,
                onValueChange = { obs = it },
                placeholder = { Text("¿Qué necesita esta familia? Condiciones de la vivienda, apoyo urgente…") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(obs.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = GREEN)
            ) { Text("Guardar") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Dropdown reutilizable (tab Familias) ──────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownFieldRA(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(if (opt.isBlank()) "—" else opt, fontSize = 14.sp) },
                    onClick = { onSelect(opt); expanded = false }
                )
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

// ════════════════════════════════════════════════════════════════════════════
// TAB OBSERVACIONES
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun ObservacionesTab(incidencia: IncidenciaLocal, viewModel: IncidenciaViewModel) {
    val observaciones by viewModel.getObservaciones(incidencia.uuidIncidencia)
        .collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        if (observaciones.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                    Text("Sin observaciones registradas", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(observaciones) { obs ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = GREEN)
                                Text(incidencia.responsable, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GREEN)
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(4.dp), color = if (obs.estadoSync == pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.SINCRONIZADO) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)) {
                                    Text(
                                        if (obs.estadoSync == pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.SINCRONIZADO) "Sincronizado" else "Pendiente",
                                        fontSize = 10.sp,
                                        color = if (obs.estadoSync == pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.SINCRONIZADO) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(obs.textoObservacion, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = GREEN,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar observación")
        }
    }

    if (showDialog) {
        AgregarObservacionDialog(
            onDismiss = { showDialog = false },
            onConfirm = { texto ->
                val textoLimpio = texto.trim()
                val ahora = System.currentTimeMillis()
                val actual = incidencia.observacionesCampo ?: ""
                val nuevo = if (actual.isBlank()) textoLimpio else "$actual\n$textoLimpio"
                viewModel.guardarIncidencia(incidencia.copy(observacionesCampo = nuevo, fechaUltimaModificacion = ahora))
                viewModel.guardarObservacion(
                    pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal(
                        uuidObservacion = UUID.randomUUID().toString(),
                        uuidIncidencia = incidencia.uuidIncidencia,
                        textoObservacion = textoLimpio,
                        fechaRegistro = ahora.toString(),
                        estadoSync = pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.NUEVO
                    )
                )
                showDialog = false
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// TAB EVIDENCIAS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun EvidenciasTab(incidencia: IncidenciaLocal, viewModel: IncidenciaViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val evidencias by viewModel.getEvidencias(incidencia.uuidIncidencia)
        .collectAsState(initial = emptyList())
    var showSheet by remember { mutableStateOf(false) }

    val cameraImageUri = remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraImageUri.value?.let { uri ->
            viewModel.guardarEvidencia(
                pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal(
                    uuidEvidencia = UUID.randomUUID().toString(),
                    uuidReferencia = incidencia.uuidIncidencia,
                    nombreArchivo = "foto_${System.currentTimeMillis()}.jpg",
                    contentType = "image/jpeg",
                    rutaLocal = uri.toString(),
                    estadoSync = pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.NUEVO
                )
            )
        }
        showSheet = false
    }
    val galleryLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            viewModel.guardarEvidencia(
                pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal(
                    uuidEvidencia = UUID.randomUUID().toString(),
                    uuidReferencia = incidencia.uuidIncidencia,
                    nombreArchivo = "imagen_${System.currentTimeMillis()}.jpg",
                    contentType = "image/jpeg",
                    rutaLocal = uri.toString(),
                    estadoSync = pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.NUEVO
                )
            )
        }
        showSheet = false
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                java.io.File.createTempFile("IMG_", ".jpg", context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES))
            )
            cameraImageUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        if (evidencias.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                    Text("Sin evidencias registradas", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(evidencias) { ev ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                when {
                                    ev.contentType?.startsWith("image/") == true -> Icons.Default.Image
                                    ev.contentType?.startsWith("audio/") == true -> Icons.Default.AudioFile
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = GREEN
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.nombreArchivo ?: "Archivo", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(ev.contentType ?: "archivo", fontSize = 11.sp, color = Color.Gray)
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = if (ev.estadoSync == pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.SINCRONIZADO) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)) {
                                Text(
                                    if (ev.estadoSync == pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.SINCRONIZADO) "Sync" else "Pendiente",
                                    fontSize = 10.sp,
                                    color = if (ev.estadoSync == pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.SINCRONIZADO) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = GREEN,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar evidencia")
        }
    }

    if (showSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Agregar Evidencia", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                EvidenciaOptionCard(icon = { Icon(Icons.Default.CameraAlt, null, tint = GREEN) }, label = "Tomar Foto", description = "Usa la cámara del dispositivo") {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
                EvidenciaOptionCard(icon = { Icon(Icons.Default.Image, null, tint = GREEN) }, label = "Galería", description = "Selecciona imágenes") {
                    galleryLauncher.launch("image/*")
                }
            }
        }
    }
}

@Composable
private fun EvidenciaOptionCard(icon: @Composable () -> Unit, label: String, description: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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

// ── Kits Tab ─────────────────────────────────────────────────────────────────

private val TIPOS_KIT = listOf(
    "Kit de Alimentos", "Kit de Higiene", "Kit de Abrigo",
    "Kit de Cocina", "Kit Escolar", "Colchoneta", "Frazada", "Otro"
)

@Composable
private fun KitsTab(
    incidencia: IncidenciaLocal,
    afectados: List<AfectadoLocal>,
    kitViewModel: KitViewModel
) {
    val kitsAsignados by kitViewModel
        .getKitsAsignadosPorIncidencia(incidencia.uuidIncidencia)
        .collectAsState(initial = emptyList())

    if (kitsAsignados.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color.LightGray
                )
                Text(
                    "Aún no hay kits asignados",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    "La asignación debe realizarla el Especialista GRD desde la revisión del caso.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Kits asignados por el especialista",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "Confirma los artículos entregados y registra la evidencia de entrega.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        items(kitsAsignados, key = { it.uuidKitAsignado }) { kit ->
            KitAsignadoCard(
                kit = kit,
                kitViewModel = kitViewModel
            )
        }
    }
}
@Composable
private fun KitAsignadoCard(
    kit: KitAsignadoLocal,
    kitViewModel: KitViewModel
) {
    val articulos by kitViewModel
        .getArticulosPorKit(kit.uuidKitAsignado)
        .collectAsState(initial = emptyList())

    var descripcionEntrega by remember { mutableStateOf(kit.descripcionEntrega ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = GREEN
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        kit.tipoKit,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        kit.nombreFamilia ?: "Familia / persona asignada",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (kit.estadoEntrega) {
                        "ENTREGADO" -> Color(0xFFE8F5E9)
                        "PARCIAL" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFF3F4F6)
                    }
                ) {
                    Text(
                        kit.estadoEntrega,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (kit.estadoEntrega) {
                            "ENTREGADO" -> Color(0xFF2E7D32)
                            "PARCIAL" -> Color(0xFFE65100)
                            else -> Color.Gray
                        }
                    )
                }
            }

            if (articulos.isEmpty()) {
                Text(
                    "Este kit no tiene artículos cargados.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    articulos.forEach { articulo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (articulo.confirmado) Color(0xFFE8F5E9) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = articulo.confirmado,
                                onCheckedChange = { checked ->
                                    kitViewModel.actualizarConfirmacionArticulo(
                                        articulo = articulo,
                                        confirmado = checked
                                    )
                                }
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    articulo.descripcion,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    articulo.codigo,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            Text(
                                "x${articulo.cantidadAsignada}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = descripcionEntrega,
                onValueChange = { descripcionEntrega = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripción de entrega") },
                placeholder = {
                    Text("Describe observaciones de la entrega...")
                },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        kitViewModel.marcarKitEntregado(
                            kit = kit,
                            descripcionEntrega = descripcionEntrega,
                            evidenciaLocalUri = null,
                            estadoEntrega = "PARCIAL"
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar parcial")
                }

                Button(
                    onClick = {
                        kitViewModel.confirmarKitCompleto(
                            kit = kit,
                            descripcionEntrega = descripcionEntrega

                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GREEN)
                ) {
                    Text("Marcar entregado")
                }
            }
        }
    }
}
@Composable
private fun EntregaKitCard(entrega: EntregaKitLocal, nombreAfectado: String) {
    val sincronizado = entrega.estadoSync == EstadoSync.SINCRONIZADO
    val esParcial = entrega.kitEntregado.contains("PARCIAL", ignoreCase = true)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        Text(
                            entrega.kitEntregado.replace(" (COMPLETA)", "").replace(" (PARCIAL)", ""),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text("Cantidad: ${entrega.cantidad}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (esParcial) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                    ) {
                        Text(
                            if (esParcial) "Parcial" else "Completa",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (esParcial) Color(0xFFE65100) else Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (sincronizado) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                    ) {
                        Text(
                            if (sincronizado) "✓ Sync" else "Pendiente",
                            fontSize = 10.sp,
                            color = if (sincronizado) Color(0xFF2E7D32) else Color(0xFFF57F17),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Text(nombreAfectado, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Text(
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(entrega.fechaEntrega)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaEntregaDialog(
    afectados: List<AfectadoLocal>,
    uuidIncidencia: String,
    idIncidenciaRemota: String?,
    onDismiss: () -> Unit,
    onConfirmar: (EntregaKitLocal) -> Unit
) {
    var afectadoSeleccionado by remember { mutableStateOf(afectados.firstOrNull()) }
    var kitSeleccionado by remember { mutableStateOf(TIPOS_KIT[0]) }
    var cantidad by remember { mutableIntStateOf(1) }
    var tipoEntrega by remember { mutableStateOf("COMPLETA") }
    var afectadoExpanded by remember { mutableStateOf(false) }
    var kitExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Entrega de Kit", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Seleccionar afectado
                if (afectados.isEmpty()) {
                    Text(
                        "No hay afectados registrados en esta incidencia.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = afectadoExpanded,
                        onExpandedChange = { afectadoExpanded = !afectadoExpanded }
                    ) {
                        OutlinedTextField(
                            value = afectadoSeleccionado?.let {
                                "${it.nombres} ${it.apellidoPaterno ?: ""}".trim()
                            } ?: "Seleccionar afectado",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Afectado") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = afectadoExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = afectadoExpanded,
                            onDismissRequest = { afectadoExpanded = false }
                        ) {
                            afectados.forEach { afectado ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${afectado.nombres} ${afectado.apellidoPaterno ?: ""}".trim(), fontSize = 14.sp)
                                            Text(afectado.documentoIdentidad, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        afectadoSeleccionado = afectado
                                        afectadoExpanded = false
                                        errorMsg = ""
                                    }
                                )
                            }
                        }
                    }
                }

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
                    ExposedDropdownMenu(expanded = kitExpanded, onDismissRequest = { kitExpanded = false }) {
                        TIPOS_KIT.forEach { kit ->
                            DropdownMenuItem(
                                text = { Text(kit) },
                                onClick = { kitSeleccionado = kit; kitExpanded = false }
                            )
                        }
                    }
                }

                // Cantidad
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cantidad:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    IconButton(onClick = { if (cantidad > 1) cantidad-- }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = GREEN)
                    }
                    Text(cantidad.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 32.dp))
                    IconButton(onClick = { cantidad++ }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = GREEN)
                    }
                }

                // Completa / Parcial
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
                    if (afectadoSeleccionado == null) { errorMsg = "Selecciona un afectado"; return@Button }
                    onConfirmar(
                        EntregaKitLocal(
                            uuidEntrega        = UUID.randomUUID().toString(),
                            uuidAfectado       = afectadoSeleccionado!!.uuidAfectado,
                            uuidIncidencia     = uuidIncidencia,
                            idIncidenciaRemota = idIncidenciaRemota,
                            kitEntregado       = "$kitSeleccionado ($tipoEntrega)",
                            cantidad           = cantidad,
                            fechaEntrega       = System.currentTimeMillis(),
                            estadoSync         = EstadoSync.NUEVO
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GREEN),
                enabled = afectados.isNotEmpty()
            ) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun KitResumenChip(label: String, count: Int, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
            Text(label, fontSize = 12.sp, color = color)
        }
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
