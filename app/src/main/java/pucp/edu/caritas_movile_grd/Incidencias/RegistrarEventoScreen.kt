package pucp.edu.caritas_movile_grd.Incidencias

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import pucp.edu.caritas_movile_grd.Masters.MasterViewModel
import java.util.*

// ── Catálogos ────────────────────────────────────────────────────────────────

private val ROLES_INSTITUCION = listOf(
    "Párroco", "Agente Pastoral", "Líder Comunitario", "Brigadista parroquial",
    "Comunidad / Vecinos", "Defensa Civil", "Municipalidad", "Bomberos",
    "INDECI", "Policía Nacional", "Otro"
)

private val DISTRITOS_LIMA = listOf(
    "Ancón", "Ate", "Barranco", "Breña", "Carabayllo", "Chaclacayo", "Chorrillos",
    "Cieneguilla", "Comas", "El Agustino", "Independencia", "Jesús María",
    "La Molina", "La Victoria", "Lince", "Los Olivos", "Lurigancho", "Lurín",
    "Magdalena del Mar", "Miraflores", "Pachacámac", "Pucusana", "Pueblo Libre",
    "Puente Piedra", "Punta Hermosa", "Punta Negra", "Rímac", "San Bartolo",
    "San Borja", "San Isidro", "San Juan de Lurigancho", "San Juan de Miraflores",
    "San Luis", "San Martín de Porres", "San Miguel", "Santa Anita", "Santa María del Mar",
    "Santa Rosa", "Santiago de Surco", "Surquillo", "Villa El Salvador",
    "Villa María del Triunfo"
)

private val CATEGORIAS_EVENTO = mapOf(
    1 to "Incendios",
    2 to "Inundaciones",
    3 to "Derrumbes",
    4 to "Deslizamientos",
    5 to "Sismos",
    6 to "Tsunamis",
    7 to "Colapso de infraestructura",
    8 to "Pérdida de vivienda",
    9 to "Vendaval / Vientos fuertes",
    10 to "Lluvias intensas",
    11 to "Otros"
)

private val PARROQUIAS_LIMA = listOf(
    "Parroquia San Juan Bautista", "Parroquia Nuestra Señora del Carmen",
    "Parroquia Santa Rosa", "Parroquia San Pedro", "Parroquia San José Obrero",
    "Parroquia Cristo Salvador", "Parroquia Sagrado Corazón de Jesús",
    "Parroquia Santa María de Fátima", "Parroquia Nuestra Señora del Pilar",
    "Parroquia San Francisco de Asís"
)

private val NECESIDADES_CHIPS = listOf(
    "Alimentos", "Ropa", "Atención médica", "Materiales de construcción", "Otros"
)

private val SITUACIONES_ESPECIALES = listOf(
    "Gestante", "Discapacitado", "Con Lactancia", "Enfermo",
    "Herido", "Enfermo crónico", "Adulto mayor"
)

private val PARENTESCOS = listOf(
    "Jefe(a) de Hogar", "Padre", "Madre", "Hijo(a)",
    "Nieto(a)", "Abuelo(a)", "Tío(a)", "Cónyuge", "Otro"
)

private val GENEROS = listOf("Femenino", "Masculino", "Otro", "Prefiere no decir")

private val NIVELES_AFECTACION = listOf("Leve", "Moderado", "Severo")

// Estructura temporal de persona durante el llenado del formulario
private data class PendingPersona(
    val uuid: String = UUID.randomUUID().toString(),
    val idCatalogoDoc: Int = 1,
    val documentoIdentidad: String = "",
    val nombres: String = "",
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "",
    val edad: String = "",
    val genero: String = "Femenino",
    val celular: String = "",
    val parentesco: String = "",
    val situacionActual: String = "",
    val familiaId: String? = null,
    val familiaNombre: String? = null
)

private data class PendingFamilia(
    val id: String = UUID.randomUUID().toString(),
    val numero: Int
) { val nombre: String get() = "Grupo Familiar $numero" }

private data class ResumenAfectados(
    val ninos: Int, val adolescentes: Int, val adultos: Int, val adultosMayores: Int,
    val situaciones: Map<String, Int>, val totalFamilias: Int
)

private fun calcularResumen(personas: List<PendingPersona>, familias: List<PendingFamilia>): ResumenAfectados {
    fun edad(p: PendingPersona) = p.edad.toIntOrNull() ?: -1
    return ResumenAfectados(
        ninos          = personas.count { edad(it) in 0..12 },
        adolescentes   = personas.count { edad(it) in 13..17 },
        adultos        = personas.count { edad(it) in 18..59 },
        adultosMayores = personas.count { edad(it) >= 60 },
        situaciones    = personas.filter { it.situacionActual.isNotBlank() }
                                 .groupingBy { it.situacionActual }.eachCount(),
        totalFamilias  = familias.size + personas.count { it.familiaId == null }
    )
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarEventoScreen(
    masterViewModel: MasterViewModel,
    onBack: () -> Unit,
    onSave: (IncidenciaLocal, List<AfectadoLocal>) -> Unit,
    idUsuarioActual: String = ""
){
    // Datos generales
    var dniReporte       by remember { mutableStateOf("") }
    var nombreReporte    by remember { mutableStateOf("") }
    var celularReporte   by remember { mutableStateOf("") }
    var rolReporte       by remember { mutableStateOf("") }

    // Datos del evento
    var fechaMillis      by remember { mutableStateOf<Long?>(null) }
    var horaHora         by remember { mutableIntStateOf(-1) }
    var horaMinuto       by remember { mutableIntStateOf(0) }

    val catalogos by masterViewModel.catalogos.collectAsState()

    LaunchedEffect(Unit) {
        masterViewModel.refrescarCatalogosDesdeBackend()
    }

    val categoriasEvento = remember(catalogos) {
        val desdeBackend = catalogos
            .filter { it.categoria == "Tipos de Evento" }
            .sortedBy { it.valor }

        if (desdeBackend.isNotEmpty()) {
            desdeBackend.map { it.idCatalogo to it.valor }
        } else {
            CATEGORIAS_EVENTO.toList()
        }
    }

    var selectedCatId by remember { mutableIntStateOf(categoriasEvento.firstOrNull()?.first ?: 1) }

    LaunchedEffect(categoriasEvento) {
        if (categoriasEvento.none { it.first == selectedCatId }) {
            selectedCatId = categoriasEvento.firstOrNull()?.first ?: 1
        }
    }    

    val tipoEventoSeleccionado = remember(categoriasEvento, selectedCatId) {
        categoriasEvento
            .firstOrNull { it.first == selectedCatId }
            ?.second
            ?: "Otros"
    }

    var distrito         by remember { mutableStateOf("") }
    var direccion        by remember { mutableStateOf("") }
    var referencia       by remember { mutableStateOf("") }

    // Descripción
    var descripcion      by remember { mutableStateOf("") }
    var causa            by remember { mutableStateOf("") }

    // Sección 4: Personas afectadas
    var pendingPersonas  by remember { mutableStateOf<List<PendingPersona>>(emptyList()) }
    var pendingFamilias  by remember { mutableStateOf<List<PendingFamilia>>(emptyList()) }
    var showPersonaSheet by remember { mutableStateOf(false) }
    var activeFamiliaId  by remember { mutableStateOf<String?>(null) }  // null = sin familia

    // Sección 5: Necesidades
    var necesidades      by remember { mutableStateOf<List<String>>(emptyList()) }
    var necesidadOtra    by remember { mutableStateOf("") }
    var necesidadesObs   by remember { mutableStateOf("") }

    // Sección 7: Estimación
    var nivelAfectacion  by remember { mutableStateOf("Moderado") }

    // Errores
    var errDni           by remember { mutableStateOf<String?>(null) }
    var errNombre        by remember { mutableStateOf<String?>(null) }
    var errCelular       by remember { mutableStateOf<String?>(null) }
    var errRol           by remember { mutableStateOf<String?>(null) }
    var errFecha         by remember { mutableStateOf<String?>(null) }
    var errDistrito      by remember { mutableStateOf<String?>(null) }
    var errDescripcion   by remember { mutableStateOf<String?>(null) }

    // Diálogos
    var showDatePicker   by remember { mutableStateOf(false) }
    var showTimePicker   by remember { mutableStateOf(false) }

    val datePickerState  = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val timePickerState  = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
        is24Hour = false
    )

    // Texto formateado para mostrar
    val fechaTexto = fechaMillis?.let {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
    } ?: ""
    val horaTexto = if (horaHora >= 0) {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, horaHora); set(Calendar.MINUTE, horaMinuto) }
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    } else ""

    fun validar(): Boolean {
        var ok = true
        errDni       = if (dniReporte.isBlank()) "Requerido"
                       else if (!dniReporte.all { it.isDigit() }) "Solo números"
                       else if (dniReporte.length != 8) "Debe tener 8 dígitos"
                       else null
        errNombre    = if (nombreReporte.isBlank()) "Requerido" else null
        errCelular   = if (celularReporte.isBlank()) "Requerido"
                       else if (!celularReporte.all { it.isDigit() }) "Solo números"
                       else if (celularReporte.length != 9) "Debe tener 9 dígitos"
                       else null
        errRol       = if (rolReporte.isBlank()) "Selecciona un rol" else null
        errFecha     = if (fechaMillis == null) "Selecciona la fecha del suceso" else null
        errDistrito  = if (distrito.isBlank()) "Selecciona un distrito" else null
        errDescripcion = if (descripcion.isBlank()) "Describe brevemente el evento" else null

        listOf(errDni, errNombre, errCelular, errRol, errFecha, errDistrito, errDescripcion)
            .forEach { if (it != null) ok = false }
        return ok
    }

    // ── DatePicker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    fechaMillis = datePickerState.selectedDateMillis
                    errFecha = null
                    showDatePicker = false
                    if (horaHora < 0) showTimePicker = true   // pide hora justo después
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Fecha del suceso", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                headline = null,
                showModeToggle = false
            )
        }
    }

    // ── TimePicker dialog ────────────────────────────────────────────────────
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Hora del suceso") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                    Text("Puede ser aproximada", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    horaHora   = timePickerState.hour
                    horaMinuto = timePickerState.minute
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Omitir") }
            }
        )
    }

    // ── Scaffold ─────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Registrar Nuevo Evento", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Completa los datos del evento reportado", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                        Text(
                            "ETAPA 1 — REGISTRO",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider()
                    Button(
                        onClick = {
                            if (!validar()) return@Button
                            val uuidNuevo = UUID.randomUUID().toString()
                            val fechaFinal = combinarFechaHora(fechaMillis!!, horaHora.takeIf { it >= 0 }, horaMinuto)
                            val necesidadesStr = buildList {
                                addAll(necesidades.filter { it != "Otros" })
                                if (necesidades.contains("Otros") && necesidadOtra.isNotBlank()) add(necesidadOtra.trim())
                            }.joinToString(", ")                         
                            val incidencia = IncidenciaLocal(
                                uuidIncidencia          = uuidNuevo,
                                uuidUsuario = idUsuarioActual,
                                idParroquia             = 1,
                                idCatalogoTipo          = selectedCatId,
                                tipoEventoNombre        = tipoEventoSeleccionado,
                                descripcion             = descripcion.trim(),
                                nombre                  = "$tipoEventoSeleccionado - $distrito",
                                numAfectados            = pendingPersonas.size,
                                responsable             = "Brigadista",
                                estado                  = "ABIERTO",
                                fechaUltimaModificacion = System.currentTimeMillis(),
                                causa                   = causa.trim().ifBlank { null },
                                reportadoPorNombre      = nombreReporte.trim(),
                                reportadoPorCelular     = celularReporte.trim(),
                                reportadoPorDni         = dniReporte.trim(),
                                reportadoPorRol         = rolReporte,
                                distrito                = distrito,
                                direccion               = direccion.trim().ifBlank { null },
                                referencia              = referencia.trim().ifBlank { null },
                                fechaSuceso             = fechaFinal,
                                necesidades             = necesidadesStr.ifBlank { null },
                                necesidadesObs          = necesidadesObs.trim().ifBlank { null },
                                nivelAfectacion         = nivelAfectacion
                            )
                            val afectados = pendingPersonas.map { p ->
                                AfectadoLocal(
                                    uuidAfectado       = p.uuid,
                                    uuidIncidencia     = uuidNuevo,
                                    idCatalogoDoc      = p.idCatalogoDoc,
                                    documentoIdentidad = p.documentoIdentidad,
                                    nombres            = p.nombres,
                                    apellidoPaterno    = p.apellidoPaterno.ifBlank { null },
                                    apellidoMaterno    = p.apellidoMaterno.ifBlank { null },
                                    edad               = p.edad.toIntOrNull(),
                                    genero             = p.genero,
                                    celular            = p.celular.ifBlank { null },
                                    parentesco         = p.parentesco.ifBlank { null },
                                    situacionActual    = p.situacionActual.ifBlank { null },
                                    familiaId          = p.familiaId,
                                    familiaNombre      = p.familiaNombre,
                                    estadoSync         = EstadoSync.NUEVO
                                )
                            }
                            onSave(incidencia, afectados)
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Alias del evento (autogenerado)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "$tipoEventoSeleccionado - ${distrito.ifBlank { "…" }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ── 1. DATOS GENERALES ────────────────────────────────────────────
            FormSection(1, "DATOS GENERALES") {
                Text("Fecha y hora del reporte", fontSize = 13.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault()).format(Date()), fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Persona que reportó el evento", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dniReporte,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) { dniReporte = it; errDni = null } },
                        label = { Text("DNI *", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.4f),
                        shape = RoundedCornerShape(12.dp),
                        isError = errDni != null,
                        supportingText = errDni?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nombreReporte,
                        onValueChange = { nombreReporte = it; errNombre = null },
                        label = { Text("Nombre y apellidos *", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.6f),
                        shape = RoundedCornerShape(12.dp),
                        isError = errNombre != null,
                        supportingText = errNombre?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) } },
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = celularReporte,
                        onValueChange = { if (it.length <= 9 && it.all { c -> c.isDigit() }) { celularReporte = it; errCelular = null } },
                        label = { Text("Celular *", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.5f),
                        shape = RoundedCornerShape(12.dp),
                        isError = errCelular != null,
                        supportingText = errCelular?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    DropdownField(
                        label = "Rol / Institución *",
                        selected = rolReporte,
                        options = ROLES_INSTITUCION,
                        onSelect = { rolReporte = it; errRol = null },
                        isError = errRol != null,
                        errorText = errRol,
                        modifier = Modifier.weight(0.5f)
                    )
                }
            }

            // ── 2. DATOS DEL EVENTO ───────────────────────────────────────────
            FormSection(2, "DATOS DEL EVENTO") {

                // Fecha y hora con pickers — todo el recuadro es clickeable
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(0.5f)) {
                        OutlinedTextField(
                            value = fechaTexto,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha del suceso *", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("dd/mm/aaaa") },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            isError = errFecha != null,
                            supportingText = errFecha?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) } }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                    }
                    Box(modifier = Modifier.weight(0.5f)) {
                        OutlinedTextField(
                            value = horaTexto,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Hora (aprox.)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("--:-- --") },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showTimePicker = true })
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Categoría del evento *", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                    categoriasEvento.forEach { (id, nombre) ->
                        FilterChip(
                            selected = selectedCatId == id,
                            onClick = { selectedCatId = id },
                            label = { Text(nombre, fontSize = 12.sp) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Ubicación del suceso", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = "Perú", onValueChange = {},
                        label = { Text("País *", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.5f), enabled = false, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )
                    OutlinedTextField(
                        value = "Lima Metropolitana", onValueChange = {},
                        label = { Text("Región *", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.5f), enabled = false, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    )
                }

                DropdownField(
                    label = "Distrito *",
                    selected = distrito,
                    options = DISTRITOS_LIMA,
                    onSelect = { distrito = it; errDistrito = null },
                    isError = errDistrito != null,
                    errorText = errDistrito,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = direccion, onValueChange = { direccion = it },
                    label = { Text("Dirección / calle", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                )
                OutlinedTextField(
                    value = referencia, onValueChange = { referencia = it },
                    label = { Text("Referencia", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                )
            }

            // ── 3. DESCRIPCIÓN ────────────────────────────────────────────────
            FormSection(3, "DESCRIPCIÓN DEL EVENTO") {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it; errDescripcion = null },
                    label = { Text("Descripción breve del evento *", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Describe lo que se reportó…", fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    isError = errDescripcion != null,
                    supportingText = errDescripcion?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) } }
                )
                OutlinedTextField(
                    value = causa, onValueChange = { causa = it },
                    label = { Text("Causa o posible causa", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("¿Qué originó el evento?", fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── 4. PERSONAS AFECTADAS ─────────────────────────────────────────
            FormSection(4, "PERSONAS AFECTADAS",
                subtitle = "${pendingPersonas.size} persona(s) · ${pendingFamilias.size} grupo(s) familiar(es)"
            ) {
                // Grupos familiares
                pendingFamilias.forEach { familia ->
                    val miembros = pendingPersonas.filter { it.familiaId == familia.id }
                    FamiliaCard(
                        familia = familia,
                        miembros = miembros,
                        onAddMember = { activeFamiliaId = familia.id; showPersonaSheet = true },
                        onDelete = {
                            pendingFamilias = pendingFamilias.filter { it.id != familia.id }
                            pendingPersonas = pendingPersonas.filter { it.familiaId != familia.id }
                        },
                        onDeleteMember = { uuid -> pendingPersonas = pendingPersonas.filter { it.uuid != uuid } }
                    )
                }

                // Personas sin familia
                pendingPersonas.filter { it.familiaId == null }.forEach { persona ->
                    PersonaCard(persona) { pendingPersonas = pendingPersonas.filter { it.uuid != persona.uuid } }
                }

                if (pendingPersonas.isEmpty() && pendingFamilias.isEmpty()) {
                    Text("Sin personas registradas aún.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedButton(
                        onClick = {
                            val nuevo = PendingFamilia(numero = pendingFamilias.size + 1)
                            pendingFamilias = pendingFamilias + nuevo
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Group, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nuevo grupo", fontSize = 13.sp)
                    }
                    Button(
                        onClick = { activeFamiliaId = null; showPersonaSheet = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009850))
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar persona", fontSize = 13.sp)
                    }
                }
            }

            // ── 5. NECESIDADES IDENTIFICADAS ─────────────────────────────────
            FormSection(5, "NECESIDADES IDENTIFICADAS") {
                FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                    NECESIDADES_CHIPS.forEach { chip ->
                        FilterChip(
                            selected = chip in necesidades,
                            onClick = {
                                necesidades = if (chip in necesidades) necesidades - chip
                                             else necesidades + chip
                            },
                            label = { Text(chip, fontSize = 12.sp) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF009850),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                if ("Otros" in necesidades) {
                    OutlinedTextField(
                        value = necesidadOtra,
                        onValueChange = { necesidadOtra = it },
                        label = { Text("Especifica la necesidad", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = necesidadesObs,
                    onValueChange = { necesidadesObs = it },
                    label = { Text("Observaciones adicionales (opcional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── 6. ESTIMACIÓN INICIAL DE AFECTACIÓN ──────────────────────────
            FormSection(6, "ESTIMACIÓN INICIAL DE AFECTACIÓN") {
                // Selector de nivel
                Text("Nivel de afectación *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NIVELES_AFECTACION.forEach { nivel ->
                        val selected = nivelAfectacion == nivel
                        val color = when (nivel) {
                            "Leve"     -> Color(0xFF2E7D32)
                            "Moderado" -> Color(0xFFF57C00)
                            else       -> Color(0xFFDC2626)
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { nivelAfectacion = nivel },
                            label = { Text(nivel, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Resumen calculado
                if (pendingPersonas.isNotEmpty()) {
                    val r = calcularResumen(pendingPersonas, pendingFamilias)
                    Spacer(Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Resumen de personas registradas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            ResumenRow("Niños (0-12)", r.ninos)
                            ResumenRow("Adolescentes (13-17)", r.adolescentes)
                            ResumenRow("Adultos (18-59)", r.adultos)
                            ResumenRow("Adultos mayores (60+)", r.adultosMayores)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            ResumenRow("Total personas", pendingPersonas.size, bold = true)
                            ResumenRow("Núcleos familiares / individuales", r.totalFamilias, bold = true)
                            if (r.situaciones.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text("Situaciones especiales:", fontSize = 11.sp, color = Color.Gray)
                                r.situaciones.forEach { (sit, cnt) ->
                                    Text("  • $sit: $cnt", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text("Agrega personas en la sección anterior para ver el resumen.", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Bottom sheet: agregar persona ─────────────────────────────────────────
    if (showPersonaSheet) {
        AgregarPersonaSheet(
            familiaActiva = pendingFamilias.find { it.id == activeFamiliaId },
            familias = pendingFamilias,
            onDismiss = { showPersonaSheet = false },
            onSave = { persona ->
                pendingPersonas = pendingPersonas + persona
                showPersonaSheet = false
            }
        )
    }
}

// ── Componentes de sección 4 ──────────────────────────────────────────────────

@Composable
private fun FamiliaCard(
    familia: PendingFamilia,
    miembros: List<PendingPersona>,
    onAddMember: () -> Unit,
    onDelete: () -> Unit,
    onDeleteMember: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9F4)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF009850).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Group, null, modifier = Modifier.size(18.dp), tint = Color(0xFF009850))
                    Text(familia.nombre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("(${miembros.size} integrante${if (miembros.size != 1) "s" else ""})", fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
            miembros.forEach { m ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${m.nombres} ${m.apellidoPaterno}".trim(), fontSize = 13.sp)
                        if (m.edad.isNotBlank()) Text("${m.edad} años · ${m.genero}", fontSize = 11.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { onDeleteMember(m.uuid) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            TextButton(onClick = onAddMember, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(14.dp), tint = Color(0xFF009850))
                Spacer(Modifier.width(4.dp))
                Text("Agregar integrante", fontSize = 12.sp, color = Color(0xFF009850))
            }
        }
    }
}

@Composable
private fun PersonaCard(persona: PendingPersona, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                Column {
                    Text("${persona.nombres} ${persona.apellidoPaterno}".trim(), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    val info = listOfNotNull(
                        if (persona.edad.isNotBlank()) "${persona.edad} años" else null,
                        persona.genero.ifBlank { null },
                        persona.situacionActual.ifBlank { null }
                    ).joinToString(" · ")
                    if (info.isNotBlank()) Text(info, fontSize = 11.sp, color = Color.Gray)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ResumenRow(label: String, valor: Int, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(valor.toString(), fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = Color(0xFF009850))
    }
}

// ── Bottom sheet de persona ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgregarPersonaSheet(
    familiaActiva: PendingFamilia?,
    familias: List<PendingFamilia>,
    onDismiss: () -> Unit,
    onSave: (PendingPersona) -> Unit
) {
    var idCatalogoDoc      by remember { mutableIntStateOf(1) }
    var documentoIdentidad by remember { mutableStateOf("") }
    var nombres            by remember { mutableStateOf("") }
    var apellidoPaterno    by remember { mutableStateOf("") }
    var apellidoMaterno    by remember { mutableStateOf("") }
    var edad               by remember { mutableStateOf("") }
    var genero             by remember { mutableStateOf("Femenino") }
    var celular            by remember { mutableStateOf("") }
    var parentesco         by remember { mutableStateOf("") }
    var situacionActual    by remember { mutableStateOf("") }
    var familiaSelId       by remember { mutableStateOf(familiaActiva?.id) }

    var errNombres         by remember { mutableStateOf<String?>(null) }
    var errEdad            by remember { mutableStateOf<String?>(null) }

    val tiposDoc = listOf(1 to "DNI", 2 to "CE", 3 to "Pasaporte", 4 to "Otro")
    val familiaActualNombre = familias.find { it.id == familiaSelId }?.nombre

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (familiaActiva != null) "Agregar integrante — ${familiaActiva.nombre}"
                else "Agregar persona afectada",
                fontWeight = FontWeight.Bold, fontSize = 16.sp
            )

            // Tipo doc + número
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownField(
                    label = "Tipo doc.", selected = tiposDoc.first { it.first == idCatalogoDoc }.second,
                    options = tiposDoc.map { it.second },
                    onSelect = { sel -> idCatalogoDoc = tiposDoc.first { it.second == sel }.first },
                    modifier = Modifier.weight(0.4f)
                )
                OutlinedTextField(
                    value = documentoIdentidad, onValueChange = { documentoIdentidad = it },
                    label = { Text("N° Documento", fontSize = 12.sp) },
                    modifier = Modifier.weight(0.6f), shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                )
            }

            // Nombres
            OutlinedTextField(
                value = nombres, onValueChange = { nombres = it; errNombres = null },
                label = { Text("Nombres *", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                isError = errNombres != null,
                supportingText = errNombres?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) } }
            )

            // Apellidos
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = apellidoPaterno, onValueChange = { apellidoPaterno = it },
                    label = { Text("Ap. Paterno", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true
                )
                OutlinedTextField(
                    value = apellidoMaterno, onValueChange = { apellidoMaterno = it },
                    label = { Text("Ap. Materno", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true
                )
            }

            // Edad + género
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = edad, onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) { edad = it; errEdad = null } },
                    label = { Text("Edad *", fontSize = 12.sp) },
                    modifier = Modifier.weight(0.3f), shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    isError = errEdad != null
                )
                DropdownField(
                    label = "Género", selected = genero, options = GENEROS,
                    onSelect = { genero = it }, modifier = Modifier.weight(0.7f)
                )
            }

            // Celular + parentesco
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = celular, onValueChange = { if (it.length <= 9 && it.all { c -> c.isDigit() }) celular = it },
                    label = { Text("Celular", fontSize = 12.sp) },
                    modifier = Modifier.weight(0.5f), shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true
                )
                DropdownField(
                    label = "Parentesco", selected = parentesco, options = listOf("") + PARENTESCOS,
                    onSelect = { parentesco = it }, modifier = Modifier.weight(0.5f)
                )
            }

            // Situación especial
            DropdownField(
                label = "Situación especial (opcional)", selected = situacionActual,
                options = listOf("") + SITUACIONES_ESPECIALES,
                onSelect = { situacionActual = it }, modifier = Modifier.fillMaxWidth()
            )

            // Asignar a familia
            if (familias.isNotEmpty()) {
                DropdownField(
                    label = "Grupo familiar (opcional)",
                    selected = familiaActualNombre ?: "Sin grupo",
                    options = listOf("Sin grupo") + familias.map { it.nombre },
                    onSelect = { sel ->
                        familiaSelId = if (sel == "Sin grupo") null
                                       else familias.find { it.nombre == sel }?.id
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Botones
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = {
                        errNombres = if (nombres.isBlank()) "Requerido" else null
                        errEdad    = if (edad.isBlank()) "Requerido" else null
                        if (errNombres != null || errEdad != null) return@Button
                        onSave(
                            PendingPersona(
                                idCatalogoDoc = idCatalogoDoc, documentoIdentidad = documentoIdentidad,
                                nombres = nombres.trim(), apellidoPaterno = apellidoPaterno.trim(),
                                apellidoMaterno = apellidoMaterno.trim(), edad = edad, genero = genero,
                                celular = celular, parentesco = parentesco, situacionActual = situacionActual,
                                familiaId = familiaSelId,
                                familiaNombre = familias.find { it.id == familiaSelId }?.nombre
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009850))
                ) { Text("Guardar") }
            }
        }
    }
}

// ── Dropdown reutilizable ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null
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
            shape = RoundedCornerShape(12.dp),
            isError = isError,
            supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) } }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 14.sp) },
                    onClick = { onSelect(option); expanded = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun combinarFechaHora(fechaMillis: Long, hora: Int?, minuto: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = fechaMillis }
    if (hora != null) {
        cal.set(Calendar.HOUR_OF_DAY, hora)
        cal.set(Calendar.MINUTE, minuto)
        cal.set(Calendar.SECOND, 0)
    }
    return cal.timeInMillis
}
