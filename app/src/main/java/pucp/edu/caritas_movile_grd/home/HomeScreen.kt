package pucp.edu.caritas_movile_grd.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import pucp.edu.caritas_movile_grd.Incidencias.GRDScreen
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaViewModel
import pucp.edu.caritas_movile_grd.Simulacros.SimulacroViewModel
import pucp.edu.caritas_movile_grd.Simulacros.SimulacrosScreen
import pucp.edu.caritas_movile_grd.LocalBDConector.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    incidenciaViewModel: IncidenciaViewModel,
    simulacroViewModel: SimulacroViewModel,
    syncViewModel: SyncViewModel,
    onReportarIncidencia: () -> Unit,
    onRealizarActividad: (String) -> Unit,
    onSubirEvidencia: (String) -> Unit,
    onEntregaKits: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val syncState by syncViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        syncViewModel.sincronizarPendientes()
    }

    LaunchedEffect(syncState.lastMessage, syncState.lastError) {
        val mensaje = syncState.lastError ?: syncState.lastMessage

        if (!mensaje.isNullOrBlank()) {
            snackbarHostState.showSnackbar(mensaje)
            syncViewModel.limpiarMensajes()
        }
    }
    val tabTitles = listOf("Incidencias GRD", "Simulacros")

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },        
        topBar = {
            TopAppBar(
                title = { Text(tabTitles[selectedTab]) },
                actions = {
                    TextButton(
                        onClick = { syncViewModel.sincronizarPendientes() },
                        enabled = !syncState.isSyncing
                    ) {
                        Text(
                            text = if (syncState.isSyncing) "Sincronizando..." else "Sincronizar"
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Report, contentDescription = null) },
                    label = { Text("GRD") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                    label = { Text("Simulacros") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> GRDScreen(
                    viewModel = incidenciaViewModel,
                    onReportarIncidencia = onReportarIncidencia,
                    onRealizarActividad = { inc -> onRealizarActividad(inc.uuidIncidencia) },
                    onSubirEvidencia = { inc -> onSubirEvidencia(inc.uuidIncidencia) }
                )
                1 -> SimulacrosScreen(viewModel = simulacroViewModel)
            }
        }
    }
}
