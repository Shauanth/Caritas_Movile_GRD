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

    fun getEntregasPorIncidencia(uuidIncidencia: String) =
        repository.getEntregasPorIncidencia(uuidIncidencia)

    fun getKitsAsignadosPorIncidencia(uuidIncidencia: String) =
        repository.getKitsAsignadosPorIncidencia(uuidIncidencia)

    fun getArticulosPorKit(uuidKitAsignado: String) =
        repository.getArticulosPorKit(uuidKitAsignado)

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

    fun actualizarConfirmacionArticulo(
        articulo: KitArticuloAsignadoLocal,
        confirmado: Boolean
    ) {
        viewModelScope.launch {
            repository.actualizarConfirmacionArticulo(articulo, confirmado)
        }
    }

    fun marcarKitEntregado(
        kit: KitAsignadoLocal,
        descripcionEntrega: String?,
        evidenciaLocalUri: String?,
        estadoEntrega: String
    ) {
        viewModelScope.launch {
            repository.marcarKitEntregado(
                kit = kit,
                descripcionEntrega = descripcionEntrega,
                evidenciaLocalUri = evidenciaLocalUri,
                estadoEntrega = estadoEntrega
            )
        }
    }
    fun confirmarKitCompleto(
        kit: KitAsignadoLocal,
        descripcionEntrega: String?
    ) {
        viewModelScope.launch {
            repository.confirmarKitCompleto(
                kit = kit,
                descripcionEntrega = descripcionEntrega
            )
        }
    }

}
