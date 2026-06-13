package pucp.edu.caritas_movile_grd.Masters

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterDao {
    @Query("SELECT * FROM parroquia_local")
    fun getAllParroquias(): Flow<List<ParroquiaLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParroquias(parroquias: List<ParroquiaLocal>)

    @Query("SELECT * FROM catalogo_local")
    fun getAllCatalogos(): Flow<List<CatalogoLocal>>

    @Query("SELECT * FROM catalogo_local WHERE categoria = :categoria")
    fun getCatalogosByCategoria(categoria: String): Flow<List<CatalogoLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogos(catalogos: List<CatalogoLocal>)

    @Query("DELETE FROM catalogo_local")
    suspend fun clearCatalogos()

    @Transaction
    suspend fun reemplazarCatalogos(catalogos: List<CatalogoLocal>) {
        clearCatalogos()
        insertCatalogos(catalogos)
    }    
}
