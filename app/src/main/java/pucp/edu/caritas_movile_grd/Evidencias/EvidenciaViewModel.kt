package pucp.edu.caritas_movile_grd.Evidencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pucp.edu.caritas_movile_grd.LocalBDConector.SyncUiState
import pucp.edu.caritas_movile_grd.LocalBDConector.SyncViewModel

class EvidenciaViewModel(
    private val repository: EvidenciaRepository,
    private val syncViewModel: SyncViewModel
) : ViewModel() {

    val syncState: StateFlow<SyncUiState> = syncViewModel.uiState

    fun getEvidencias(uuidReferencia: String): Flow<List<EvidenciaLocal>> =
        repository.getEvidencias(uuidReferencia)

    fun guardarEvidencia(evidencia: EvidenciaLocal) {
        viewModelScope.launch {
            repository.guardarEvidencia(evidencia)
            syncViewModel.sincronizarPendientes()
        }
    }

    fun eliminarEvidencia(evidencia: EvidenciaLocal) {
        viewModelScope.launch {
            repository.eliminarEvidencia(evidencia)
            syncViewModel.sincronizarPendientes()
        }
    }

    fun sincronizar() {
        syncViewModel.sincronizarPendientes()
    }
}
