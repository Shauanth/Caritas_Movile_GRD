package pucp.edu.caritas_movile_grd.Simulacros

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulacroDao {
    @Query("SELECT * FROM simulacro_local ORDER BY fecha DESC")
    fun getAllSimulacros(): Flow<List<SimulacroLocal>>

    @Query("SELECT * FROM simulacro_local WHERE estado = :estado ORDER BY fecha DESC")
    fun getSimulacrosByEstado(estado: String): Flow<List<SimulacroLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulacro(simulacro: SimulacroLocal)

    @Update
    suspend fun updateSimulacro(simulacro: SimulacroLocal)

    @Query("SELECT * FROM simulacro_local WHERE uuidSimulacro = :uuid")
    suspend fun getSimulacroById(uuid: String): SimulacroLocal?
}
