package pucp.edu.caritas_movile_grd.Incidencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal
import pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal
import pucp.edu.caritas_movile_grd.Seguimientos.SeguimientoLocal

class IncidenciaViewModel(private val repository: IncidenciaRepository) : ViewModel() {

    val incidencias: StateFlow<List<IncidenciaLocal>> = repository.allIncidencias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun guardarIncidencia(incidencia: IncidenciaLocal) {
        viewModelScope.launch {
            repository.guardarIncidencia(incidencia)
        }
    }

    fun guardarIncidenciaYLuego(incidencia: IncidenciaLocal, despues: () -> Unit) {
        viewModelScope.launch {
            repository.guardarIncidencia(incidencia)
            withContext(Dispatchers.Main) { despues() }
        }
    }

    fun getAfectados(uuidIncidencia: String) = repository.getAfectados(uuidIncidencia)

    fun guardarAfectado(afectado: AfectadoLocal) {
        viewModelScope.launch { repository.guardarAfectado(afectado) }
    }

    fun editarAfectado(afectado: AfectadoLocal) {
        viewModelScope.launch { repository.editarAfectado(afectado) }
    }

    fun eliminarAfectado(afectado: AfectadoLocal) {
        viewModelScope.launch { repository.eliminarAfectado(afectado) }
    }

    fun eliminarAfectado(uuidAfectado: String) {
        viewModelScope.launch {
            repository.eliminarAfectado(uuidAfectado)
        }
    }

    fun getGruposFamiliares(uuidIncidencia: String) =
        repository.getGruposFamiliares(uuidIncidencia)

    fun guardarGrupoFamiliar(grupo: GrupoFamiliarLocal) {
        viewModelScope.launch {
            repository.guardarGrupoFamiliar(grupo)
        }
    }

    fun eliminarGrupoFamiliar(uuidGrupo: String) {
        viewModelScope.launch {
            repository.eliminarGrupoFamiliar(uuidGrupo)
        }
    }
    fun guardarObservacion(observacion: ObservacionLocal) {
        viewModelScope.launch {
            repository.guardarObservacion(observacion)
        }
    } 
    fun guardarSeguimiento(seguimiento: SeguimientoLocal) {
        viewModelScope.launch {
            repository.guardarSeguimiento(seguimiento)
        }
    } 
    fun getObservaciones(uuidIncidencia: String) =
        repository.getObservaciones(uuidIncidencia)

    fun getSeguimientos(uuidIncidencia: String) =
        repository.getSeguimientos(uuidIncidencia)

    fun getEvidencias(uuidIncidencia: String) =
        repository.getEvidencias(uuidIncidencia)

    fun guardarEvidencia(evidencia: EvidenciaLocal) {
        viewModelScope.launch {
            repository.guardarEvidencia(evidencia)
        }
    }    
         
    fun refrescarIncidenciasAsignadas() {
        viewModelScope.launch {
            repository.refrescarIncidenciasAsignadas()
        }
    }

    fun finalizarRecopilacion(
        incidencia: IncidenciaLocal,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val ok = repository.finalizarRecopilacion(incidencia)

                if (ok) {
                    onResult(true, null)
                } else {
                    onResult(false, "No se pudo finalizar la recopilación.")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error al finalizar la recopilación.")
            }
        }
    }

}
