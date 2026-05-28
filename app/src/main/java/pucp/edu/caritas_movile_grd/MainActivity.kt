package pucp.edu.caritas_movile_grd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pucp.edu.caritas_movile_grd.login.LoginScreen
import pucp.edu.caritas_movile_grd.home.MainScreen
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaScreen
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaViewModel
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaRepository
import pucp.edu.caritas_movile_grd.Cursos.CursoRepository
import pucp.edu.caritas_movile_grd.Cursos.CursoViewModel
import pucp.edu.caritas_movile_grd.Kits.EntregaKitScreen
import pucp.edu.caritas_movile_grd.Kits.KitRepository
import pucp.edu.caritas_movile_grd.Kits.KitViewModel
import pucp.edu.caritas_movile_grd.Simulacros.SimulacroRepository
import pucp.edu.caritas_movile_grd.Simulacros.SimulacroViewModel
import pucp.edu.caritas_movile_grd.LocalBDConector.AppDatabase
import pucp.edu.caritas_movile_grd.ui.theme.Caritas_Movile_GRDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Caritas_Movile_GRDTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)

    val incidenciaRepository = IncidenciaRepository(database.incidenciaDao())
    val cursoRepository = CursoRepository(database.cursoDao())
    val kitRepository = KitRepository(database.kitDao())
    val simulacroRepository = SimulacroRepository(database.simulacroDao())

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = { _ ->
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("main") {
            val incidenciaViewModel: IncidenciaViewModel = viewModel(
                factory = GenericViewModelFactory { IncidenciaViewModel(incidenciaRepository) }
            )
            val cursoViewModel: CursoViewModel = viewModel(
                factory = GenericViewModelFactory { CursoViewModel(cursoRepository) }
            )
            val simulacroViewModel: SimulacroViewModel = viewModel(
                factory = GenericViewModelFactory { SimulacroViewModel(simulacroRepository) }
            )
            MainScreen(
                incidenciaViewModel = incidenciaViewModel,
                cursoViewModel = cursoViewModel,
                simulacroViewModel = simulacroViewModel,
                onReportarIncidencia = { navController.navigate("reportar_incidencia") },
                onEntregaKits = { navController.navigate("entrega_kits") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
        composable("reportar_incidencia") {
            val viewModel: IncidenciaViewModel = viewModel(
                factory = GenericViewModelFactory { IncidenciaViewModel(incidenciaRepository) }
            )
            IncidenciaScreen(
                onBack = { navController.popBackStack() },
                onSave = { incidencia ->
                    viewModel.guardarIncidencia(incidencia)
                    navController.popBackStack()
                }
            )
        }
        composable("entrega_kits") {
            val viewModel: KitViewModel = viewModel(
                factory = GenericViewModelFactory { KitViewModel(kitRepository) }
            )
            EntregaKitScreen(
                onBack = { navController.popBackStack() },
                onConfirmarEntrega = { entrega ->
                    viewModel.realizarEntrega(entrega)
                    navController.popBackStack()
                }
            )
        }
    }
}

// Factory genérica para simplificar la creación de ViewModels sin DI pesado
class GenericViewModelFactory<T : androidx.lifecycle.ViewModel>(
    private val creator: () -> T
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}
