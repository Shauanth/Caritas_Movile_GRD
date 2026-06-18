package pucp.edu.caritas_movile_grd.Simulacros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SimulacroUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class SimulacroViewModel(private val repository: SimulacroRepository) : ViewModel() {

    val simulacros: StateFlow<List<SimulacroLocal>> = repository.allSimulacros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(SimulacroUiState())
    val uiState: StateFlow<SimulacroUiState> = _uiState.asStateFlow()

    init {
        refrescar()
    }

    fun guardar(simulacro: SimulacroLocal) {
        viewModelScope.launch { repository.guardarSimulacro(simulacro) }
    }

    fun actualizar(simulacro: SimulacroLocal) {
        viewModelScope.launch { repository.actualizarSimulacro(simulacro) }
    }

    fun refrescar() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = SimulacroUiState(isLoading = true)
            try {
                val total = repository.descargarSimulacrosDesdeBackend()
                _uiState.value = SimulacroUiState(
                    message = "Simulacros actualizados: $total"
                )
            } catch (ex: Exception) {
                _uiState.value = SimulacroUiState(
                    error = "No se pudieron descargar simulacros: ${ex.message}"
                )
            }
        }
    }

    fun ejecutarSimulacro(
        uuid: String,
        resultadoGeneral: String,
        reporteBrigadista: String,
        numeroParticipantesReal: Int?,
        duracionSimulacro: Int?,
        recomendaciones: String?,
        observaciones: String?
    ) {
        viewModelScope.launch {
            try {
                repository.marcarSimulacroEjecutadoLocalmente(
                    uuidSimulacro = uuid,
                    resultadoGeneral = resultadoGeneral,
                    reporteBrigadista = reporteBrigadista,
                    numeroParticipantesReal = numeroParticipantesReal,
                    duracionSimulacro = duracionSimulacro,
                    recomendaciones = recomendaciones,
                    observaciones = observaciones
                )
                val sincronizados = repository.sincronizarSimulacrosPendientes()
                _uiState.value = SimulacroUiState(
                    message = if (sincronizados > 0) {
                        "Simulacro ejecutado y sincronizado."
                    } else {
                        "Simulacro ejecutado. Queda pendiente de sincronizar."
                    }
                )
            } catch (ex: Exception) {
                _uiState.value = SimulacroUiState(
                    error = "Simulacro guardado localmente. No se pudo sincronizar: ${ex.message}"
                )
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
