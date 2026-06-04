package pucp.edu.caritas_movile_grd.Cursos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CursoViewModel(private val repository: CursoRepository) : ViewModel() {

    val cursos: StateFlow<List<CursoCapacitacionLocal>> = repository.allCursos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun registrarAsistencia(participante: ParticipanteLocal) {
        viewModelScope.launch {
            repository.registrarAsistencia(participante)
        }
    }

    fun getEvaluaciones(idCurso: Int) = repository.getEvaluaciones(idCurso)

    fun guardarResultadoEvaluacion(idCurso: Int, uuidUsuario: String, nota: Int) {
        viewModelScope.launch {
            repository.guardarProgreso(
                ProgresoCursoLocal(
                    idCursoRemoto = idCurso,
                    uuidUsuario = uuidUsuario,
                    notaObtenida = nota,
                    completado = true
                )
            )
        }
    }
}
