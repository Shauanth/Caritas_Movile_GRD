package pucp.edu.caritas_movile_grd.Cursos

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CursoDao {
    @Query("SELECT * FROM curso_capacitacion_local")
    fun getAllCursos(): Flow<List<CursoCapacitacionLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurso(curso: CursoCapacitacionLocal)

    @Query("SELECT * FROM participante_local WHERE idCursoRemoto = :idCurso")
    fun getParticipantesByCurso(idCurso: Int): Flow<List<ParticipanteLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipante(participante: ParticipanteLocal)

    @Query("SELECT * FROM material_capacitacion_local WHERE idCursoRemoto = :idCurso")
    fun getMaterialesByCurso(idCurso: Int): Flow<List<MaterialCapacitacionLocal>>

    @Query("SELECT * FROM evaluacion_local WHERE idCursoRemoto = :idCurso")
    fun getEvaluacionesByCurso(idCurso: Int): Flow<List<EvaluacionLocal>>

    @Query("SELECT * FROM pregunta_local WHERE idEvaluacionRemota = :idEvaluacion")
    suspend fun getPreguntasByEvaluacion(idEvaluacion: Int): List<PreguntaLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgreso(progreso: ProgresoCursoLocal)

    @Query("SELECT * FROM progreso_curso_local WHERE idCursoRemoto = :idCurso AND uuidUsuario = :uuidUsuario")
    fun getProgresoUsuario(idCurso: Int, uuidUsuario: String): Flow<ProgresoCursoLocal?>
}
