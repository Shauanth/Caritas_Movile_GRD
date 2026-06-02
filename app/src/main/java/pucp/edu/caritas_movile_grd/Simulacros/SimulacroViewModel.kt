package pucp.edu.caritas_movile_grd.Simulacros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SimulacroViewModel(private val repository: SimulacroRepository) : ViewModel() {

    val simulacros: StateFlow<List<SimulacroLocal>> = repository.allSimulacros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun guardar(simulacro: SimulacroLocal) {
        viewModelScope.launch { repository.guardarSimulacro(simulacro) }
    }

    fun actualizar(simulacro: SimulacroLocal) {
        viewModelScope.launch { repository.actualizarSimulacro(simulacro) }
    }

    fun enviarReporte(uuid: String, notas: String) {
        viewModelScope.launch {
            simulacros.value.find { it.uuidSimulacro == uuid }?.let {
                repository.actualizarSimulacro(it.copy(estado = "Ejecutada", notasBrigadista = notas))
            }
        }
    }
}
