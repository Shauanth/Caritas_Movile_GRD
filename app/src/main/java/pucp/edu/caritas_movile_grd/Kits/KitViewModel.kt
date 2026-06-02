package pucp.edu.caritas_movile_grd.Kits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KitViewModel(private val repository: KitRepository) : ViewModel() {

    val entregas: StateFlow<List<EntregaKitLocal>> = repository.allEntregas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun realizarEntrega(entrega: EntregaKitLocal) {
        viewModelScope.launch {
            repository.insertarEntrega(entrega)
        }
    }

    fun eliminarEntrega(entrega: EntregaKitLocal) {
        viewModelScope.launch {
            repository.eliminarEntrega(entrega)
        }
    }
}
