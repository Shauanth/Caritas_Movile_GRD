package pucp.edu.caritas_movile_grd.Evidencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class EvidenciaViewModel(private val repository: EvidenciaRepository) : ViewModel() {

    fun getEvidencias(uuidReferencia: String): Flow<List<EvidenciaLocal>> =
        repository.getEvidencias(uuidReferencia)

    fun guardarEvidencia(evidencia: EvidenciaLocal) {
        viewModelScope.launch {
            repository.guardarEvidencia(evidencia)
        }
    }

    fun eliminarEvidencia(evidencia: EvidenciaLocal) {
        viewModelScope.launch {
            repository.eliminarEvidencia(evidencia)
        }
    }
}
