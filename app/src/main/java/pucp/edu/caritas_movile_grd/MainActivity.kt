package pucp.edu.caritas_movile_grd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import pucp.edu.caritas_movile_grd.login.LoginScreen
import pucp.edu.caritas_movile_grd.login.LoginViewModel
import pucp.edu.caritas_movile_grd.login.LoginRepository
import pucp.edu.caritas_movile_grd.home.HomeScreen
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaScreen
import pucp.edu.caritas_movile_grd.Incidencias.HistorialScreen
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaViewModel
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaRepository
import pucp.edu.caritas_movile_grd.Cursos.AcademiaScreen
import pucp.edu.caritas_movile_grd.Cursos.CursoViewModel
import pucp.edu.caritas_movile_grd.Cursos.CursoRepository
import pucp.edu.caritas_movile_grd.Kits.EntregaKitScreen
import pucp.edu.caritas_movile_grd.Kits.KitViewModel
import pucp.edu.caritas_movile_grd.Kits.KitRepository
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

    // Inicialización de Repositorios
    val loginRepository = LoginRepository(database.loginDao())
    val incidenciaRepository = IncidenciaRepository(database.incidenciaDao())
    val cursoRepository = CursoRepository(database.cursoDao())
    val kitRepository = KitRepository(database.kitDao())

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = { username ->
                navController.navigate("home")
            })
        }
        composable("home") {
            HomeScreen(
                userName = "Brigadista", 
                onNavigate = { route -> navController.navigate(route) },
                onLogout = { navController.navigate("login") }
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
        composable("historial") {
            val viewModel: IncidenciaViewModel = viewModel(
                factory = GenericViewModelFactory { IncidenciaViewModel(incidenciaRepository) }
            )
            HistorialScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("cursos") {
            val viewModel: CursoViewModel = viewModel(
                factory = GenericViewModelFactory { CursoViewModel(cursoRepository) }
            )
            AcademiaScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToCurso = { /* Navegar a detalle */ }
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
