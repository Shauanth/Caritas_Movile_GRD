package pucp.edu.caritas_movile_grd.Kits

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KitDao {
    // ─────────────────────────────────────────────
    // Entregas registradas
    // ─────────────────────────────────────────────

    @Query("SELECT * FROM entrega_kit_local")
    fun getAllEntregas(): Flow<List<EntregaKitLocal>>

    @Query("SELECT * FROM entrega_kit_local WHERE uuidAfectado = :uuidAfectado")
    fun getEntregasByAfectado(uuidAfectado: String): Flow<List<EntregaKitLocal>>

    @Query("SELECT * FROM entrega_kit_local WHERE uuidIncidencia = :uuidIncidencia ORDER BY fechaEntrega DESC")
    fun getEntregasByIncidencia(uuidIncidencia: String): Flow<List<EntregaKitLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntrega(entrega: EntregaKitLocal)

    @Update
    suspend fun updateEntrega(entrega: EntregaKitLocal)

    @Delete
    suspend fun deleteEntrega(entrega: EntregaKitLocal)

    // ─────────────────────────────────────────────
    // Kits asignados por especialista
    // ─────────────────────────────────────────────

    @Query("""
        SELECT * FROM kit_asignado_local
        WHERE uuidIncidencia = :uuidIncidencia
        ORDER BY nombreFamilia ASC, tipoKit ASC
    """)
    fun getKitsAsignadosPorIncidencia(uuidIncidencia: String): Flow<List<KitAsignadoLocal>>

    @Query("""
        SELECT * FROM kit_articulo_asignado_local
        WHERE uuidKitAsignado = :uuidKitAsignado
        ORDER BY descripcion ASC
    """)
    fun getArticulosPorKit(uuidKitAsignado: String): Flow<List<KitArticuloAsignadoLocal>>

    @Query("""
        SELECT * FROM kit_articulo_asignado_local
        WHERE uuidKitAsignado IN (:uuidsKitAsignado)
        ORDER BY descripcion ASC
    """)
    suspend fun getArticulosPorKitsSync(uuidsKitAsignado: List<String>): List<KitArticuloAsignadoLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKitAsignado(kit: KitAsignadoLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKitsAsignados(kits: List<KitAsignadoLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticulosAsignados(articulos: List<KitArticuloAsignadoLocal>)

    @Update
    suspend fun updateKitAsignado(kit: KitAsignadoLocal)

    @Update
    suspend fun updateArticuloAsignado(articulo: KitArticuloAsignadoLocal)

    @Query("""
        UPDATE kit_articulo_asignado_local
        SET confirmado = :confirmado,
            cantidadEntregada = :cantidadEntregada
        WHERE uuidArticuloAsignado = :uuidArticuloAsignado
    """)
    suspend fun actualizarConfirmacionArticulo(
        uuidArticuloAsignado: String,
        confirmado: Boolean,
        cantidadEntregada: Int
    )

    @Query("""
        UPDATE kit_asignado_local
        SET estadoEntrega = :estadoEntrega,
            fechaEntrega = :fechaEntrega,
            descripcionEntrega = :descripcionEntrega,
            evidenciaLocalUri = :evidenciaLocalUri,
            estadoSync = :estadoSync
        WHERE uuidKitAsignado = :uuidKitAsignado
    """)
    suspend fun marcarKitEntregado(
        uuidKitAsignado: String,
        estadoEntrega: String,
        fechaEntrega: Long,
        descripcionEntrega: String?,
        evidenciaLocalUri: String?,
        estadoSync: pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
    )
}
