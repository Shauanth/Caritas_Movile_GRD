package pucp.edu.caritas_movile_grd.Evidencias

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenciaDao {
    @Query("SELECT * FROM evidencia_local WHERE uuidReferencia = :uuidReferencia")
    fun getEvidenciasByReferencia(uuidReferencia: String): Flow<List<EvidenciaLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidencia(evidencia: EvidenciaLocal)

    @Update
    suspend fun updateEvidencia(evidencia: EvidenciaLocal)

    @Delete
    suspend fun deleteEvidencia(evidencia: EvidenciaLocal)

    @Query("SELECT * FROM evidencia_local WHERE estadoSync = 'PENDIENTE_SUBIDA'")
    suspend fun getEvidenciasPendientes(): List<EvidenciaLocal>

    @Query("UPDATE evidencia_local SET estadoSync = 'PENDIENTE_ELIMINACION' WHERE uuidEvidencia = :uuidEvidencia")
    suspend fun marcarEvidenciaParaEliminar(uuidEvidencia: String)
}
