package pucp.edu.caritas_movile_grd.Kits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        confirmado: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.actualizarConfirmacionArticulo(articulo, confirmado)
            onDone()
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

    fun confirmarEntregaKitAsignado(
        kit: KitAsignadoLocal,
        articulos: List<KitArticuloAsignadoLocal>,
        descripcionEntrega: String,
        onResult: (Result<EntregaKitLocal>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val entrega = repository.confirmarEntregaKitAsignado(
                    kit = kit,
                    articulos = articulos,
                    descripcionEntrega = descripcionEntrega
                )
                onResult(Result.success(entrega))
            } catch (ex: Exception) {
                onResult(Result.failure(ex))
            }
        }
    }

    fun validarTodosKitsEntregados(
        uuidIncidencia: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val completos = withContext(Dispatchers.IO) {
                repository.todosKitsEntregados(uuidIncidencia)
            }
            onResult(completos)
        }
    }

}
