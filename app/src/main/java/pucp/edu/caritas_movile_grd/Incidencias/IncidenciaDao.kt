package pucp.edu.caritas_movile_grd.Incidencias

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidenciaDao {
    @Query("SELECT * FROM incidencia_local ORDER BY fechaUltimaModificacion DESC")
    fun getAllIncidencias(): Flow<List<IncidenciaLocal>>

    @Query("SELECT * FROM incidencia_local WHERE uuidIncidencia = :uuid")
    suspend fun getIncidenciaById(uuid: String): IncidenciaLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncidencia(incidencia: IncidenciaLocal)

    @Update
    suspend fun updateIncidencia(incidencia: IncidenciaLocal)

    @Query("SELECT * FROM afectado_local WHERE uuidIncidencia = :uuidIncidencia")
    fun getAfectadosByIncidencia(uuidIncidencia: String): Flow<List<AfectadoLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAfectado(afectado: AfectadoLocal)
}
