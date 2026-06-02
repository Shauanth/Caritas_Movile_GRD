package pucp.edu.caritas_movile_grd.Incidencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IncidenciaViewModel(private val repository: IncidenciaRepository) : ViewModel() {

    val incidencias: StateFlow<List<IncidenciaLocal>> = repository.allIncidencias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun guardarIncidencia(incidencia: IncidenciaLocal) {
        viewModelScope.launch {
            repository.guardarIncidencia(incidencia)
        }
    }

    fun getAfectados(uuidIncidencia: String) = repository.getAfectados(uuidIncidencia)

    fun guardarAfectado(afectado: AfectadoLocal) {
        viewModelScope.launch {
            repository.guardarAfectado(afectado)
        }
    }
}
