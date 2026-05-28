package pucp.edu.caritas_movile_grd.Kits

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KitDao {
    @Query("SELECT * FROM entrega_kit_local")
    fun getAllEntregas(): Flow<List<EntregaKitLocal>>

    @Query("SELECT * FROM entrega_kit_local WHERE uuidAfectado = :uuidAfectado")
    fun getEntregasByAfectado(uuidAfectado: String): Flow<List<EntregaKitLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntrega(entrega: EntregaKitLocal)

    @Update
    suspend fun updateEntrega(entrega: EntregaKitLocal)

    @Delete
    suspend fun deleteEntrega(entrega: EntregaKitLocal)
}
