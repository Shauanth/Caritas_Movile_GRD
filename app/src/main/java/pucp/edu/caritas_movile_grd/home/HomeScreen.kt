package pucp.edu.caritas_movile_grd.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pucp.edu.caritas_movile_grd.Cursos.CursoViewModel
import pucp.edu.caritas_movile_grd.Cursos.CapacitacionesScreen
import pucp.edu.caritas_movile_grd.Incidencias.GRDScreen
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaViewModel
import pucp.edu.caritas_movile_grd.Kits.EntregaKitLocal
import pucp.edu.caritas_movile_grd.Kits.EntregaKitScreen
import pucp.edu.caritas_movile_grd.Kits.HistorialEntregasScreen
import pucp.edu.caritas_movile_grd.Kits.KitViewModel
import pucp.edu.caritas_movile_grd.Simulacros.SimulacroViewModel
import pucp.edu.caritas_movile_grd.Simulacros.SimulacrosScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    incidenciaViewModel: IncidenciaViewModel,
    cursoViewModel: CursoViewModel,
    simulacroViewModel: SimulacroViewModel,
    kitViewModel: KitViewModel,
    onReportarIncidencia: () -> Unit,
    onRealizarActividad: (String) -> Unit,
    onSubirEvidencia: (String) -> Unit,
    onEntregaKits: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var kitsSubTab by remember { mutableIntStateOf(0) }

    val entregas by kitViewModel.entregas.collectAsState()

    val tabTitles = listOf("GRD", "Capacitaciones", "Simulacros", "Kits")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tabTitles[selectedTab]) },
                actions = {
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
                    icon = { Icon(Icons.Default.School, contentDescription = null) },
                    label = { Text("Capacitaciones") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                    label = { Text("Simulacros") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    label = { Text("Kits") }
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
                1 -> CapacitacionesScreen(viewModel = cursoViewModel)
                2 -> SimulacrosScreen(viewModel = simulacroViewModel)
                3 -> KitsTabScreen(
                    subTab = kitsSubTab,
                    onSubTabChange = { kitsSubTab = it },
                    entregas = entregas,
                    onConfirmarEntrega = { entrega -> kitViewModel.realizarEntrega(entrega) }
                )
            }
        }
    }
}

@Composable
private fun KitsTabScreen(
    subTab: Int,
    onSubTabChange: (Int) -> Unit,
    entregas: List<EntregaKitLocal>,
    onConfirmarEntrega: (EntregaKitLocal) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTab) {
            Tab(
                selected = subTab == 0,
                onClick = { onSubTabChange(0) },
                text = { Text("Registrar") }
            )
            Tab(
                selected = subTab == 1,
                onClick = { onSubTabChange(1) },
                text = { Text("Historial") }
            )
        }
        when (subTab) {
            0 -> EntregaKitScreen(
                onBack = { onSubTabChange(1) },
                onConfirmarEntrega = { entrega ->
                    onConfirmarEntrega(entrega)
                    onSubTabChange(1)
                }
            )
            1 -> HistorialEntregasScreen(entregas = entregas)
        }
    }
}
