package pucp.edu.caritas_movile_grd.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import pucp.edu.caritas_movile_grd.Cursos.CursoViewModel
import pucp.edu.caritas_movile_grd.Cursos.CapacitacionesScreen
import pucp.edu.caritas_movile_grd.Incidencias.GRDScreen
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaViewModel
import pucp.edu.caritas_movile_grd.Simulacros.SimulacroViewModel
import pucp.edu.caritas_movile_grd.Simulacros.SimulacrosScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    incidenciaViewModel: IncidenciaViewModel,
    cursoViewModel: CursoViewModel,
    simulacroViewModel: SimulacroViewModel,
    onReportarIncidencia: () -> Unit,
    onEntregaKits: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabTitles = listOf("Incidencias GRD", "Capacitaciones", "Simulacros")

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
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> GRDScreen(
                    viewModel = incidenciaViewModel,
                    onReportarIncidencia = onReportarIncidencia
                )
                1 -> CapacitacionesScreen(viewModel = cursoViewModel)
                2 -> SimulacrosScreen(viewModel = simulacroViewModel)
            }
        }
    }
}
