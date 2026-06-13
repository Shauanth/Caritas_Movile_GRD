package pucp.edu.caritas_movile_grd.Masters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MasterViewModel(private val repository: MasterRepository) : ViewModel() {

    val parroquias: StateFlow<List<ParroquiaLocal>> = repository.allParroquias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val catalogos: StateFlow<List<CatalogoLocal>> = repository.allCatalogos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getCatalogosPorCategoria(categoria: String) = repository.getCatalogosPorCategoria(categoria)

    fun refrescarCatalogosDesdeBackend() {
        viewModelScope.launch {
            repository.refrescarCatalogosDesdeBackend()
        }
    }    
}
