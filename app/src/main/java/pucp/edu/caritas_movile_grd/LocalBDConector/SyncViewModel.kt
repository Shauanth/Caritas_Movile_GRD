package pucp.edu.caritas_movile_grd.LocalBDConector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SyncUiState(
    val isSyncing: Boolean = false,
    val lastMessage: String? = null,
    val lastError: String? = null
)

class SyncViewModel(
    private val repository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    fun sincronizarPendientes() {
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = SyncUiState(isSyncing = true)

            try {
                val result = repository.sincronizarPendientes()

                val mensaje = buildString {
                    append("Sincronización finalizada: ")
                    append("${result.incidenciasSincronizadas} incidencia(s), ")
                    append("${result.afectadosSincronizados} afectado(s), ")
                    append("${result.evidenciasSincronizadas} evidencia(s), ")
                    append("${result.observacionesSincronizadas} observación(es) y ")
                    append("${result.seguimientosSincronizados} seguimiento(s) sincronizado(s).")
                }

                val errores = if (result.errores.isNotEmpty()) {
                    result.errores.joinToString(separator = "\n")
                } else {
                    null
                }

                _uiState.value = SyncUiState(
                    isSyncing = false,
                    lastMessage = mensaje,
                    lastError = errores
                )
            } catch (ex: Exception) {
                _uiState.value = SyncUiState(
                    isSyncing = false,
                    lastError = "No se pudo sincronizar: ${ex.message}"
                )
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.value = _uiState.value.copy(
            lastMessage = null,
            lastError = null
        )
    }
}