package pucp.edu.caritas_movile_grd.Cursos

import kotlinx.coroutines.flow.Flow

class CursoRepository(private val cursoDao: CursoDao) {
    val allCursos: Flow<List<CursoCapacitacionLocal>> = cursoDao.getAllCursos()

    fun getParticipantes(idCurso: Int): Flow<List<ParticipanteLocal>> = 
        cursoDao.getParticipantesByCurso(idCurso)

    fun getMateriales(idCurso: Int): Flow<List<MaterialCapacitacionLocal>> = 
        cursoDao.getMaterialesByCurso(idCurso)

    suspend fun insertarCurso(curso: CursoCapacitacionLocal) {
        cursoDao.insertCurso(curso)
    }

    suspend fun registrarAsistencia(participante: ParticipanteLocal) {
        cursoDao.insertParticipante(participante)
    }

    fun getEvaluaciones(idCurso: Int) = cursoDao.getEvaluacionesByCurso(idCurso)

    suspend fun getPreguntas(idEvaluacion: Int) = cursoDao.getPreguntasByEvaluacion(idEvaluacion)

    suspend fun guardarProgreso(progreso: ProgresoCursoLocal) {
        cursoDao.insertProgreso(progreso)
    }

    fun getProgreso(idCurso: Int, uuidUsuario: String) = cursoDao.getProgresoUsuario(idCurso, uuidUsuario)
}
