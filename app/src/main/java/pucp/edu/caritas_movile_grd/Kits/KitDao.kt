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

    @Query("SELECT * FROM entrega_kit_local WHERE uuidEntrega = :uuidEntrega LIMIT 1")
    suspend fun getEntregaByUuid(uuidEntrega: String): EntregaKitLocal?

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
        SELECT * FROM kit_asignado_local
        WHERE uuidIncidencia = :uuidIncidencia
        ORDER BY nombreFamilia ASC, tipoKit ASC
    """)
    suspend fun getKitsAsignadosPorIncidenciaSync(uuidIncidencia: String): List<KitAsignadoLocal>

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
    SELECT * FROM kit_asignado_local
    WHERE estadoSync != 'SINCRONIZADO'
    ORDER BY uuidIncidencia ASC, tipoKit ASC
""")
    suspend fun getKitsAsignadosPendientesSync(): List<KitAsignadoLocal>

    @Query("""
    SELECT * FROM kit_articulo_asignado_local
    WHERE uuidKitAsignado IN (:uuidsKitAsignado)
    ORDER BY uuidKitAsignado ASC, descripcion ASC
""")
    suspend fun getArticulosAsignadosPorKitsSync(
        uuidsKitAsignado: List<String>
    ): List<KitArticuloAsignadoLocal>

    @Query("""
    UPDATE kit_asignado_local
    SET estadoSync = 'SINCRONIZADO'
    WHERE uuidKitAsignado IN (:uuidsKitAsignado)
""")
    suspend fun marcarKitsAsignadosSincronizados(
        uuidsKitAsignado: List<String>
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
    @Query("""
    UPDATE kit_articulo_asignado_local
    SET confirmado = :confirmado,
        cantidadEntregada = :cantidadEntregada
    WHERE uuidKitAsignado = :uuidKitAsignado
      AND (
        (:codigo != '' AND codigo = :codigo)
        OR (:descripcion != '' AND descripcion = :descripcion)
      )
""")
    suspend fun actualizarConfirmacionArticuloPorKitCodigo(
        uuidKitAsignado: String,
        codigo: String,
        descripcion: String,
        confirmado: Boolean,
        cantidadEntregada: Int
    )
    @Query("""
    UPDATE kit_articulo_asignado_local
    SET confirmado = 1,
        cantidadEntregada = cantidadAsignada
    WHERE uuidKitAsignado = :uuidKitAsignado
""")
    suspend fun marcarTodosArticulosDeKitEntregados(
        uuidKitAsignado: String
    )

    @Transaction
    suspend fun confirmarEntregaKitAsignado(
        kit: KitAsignadoLocal,
        entrega: EntregaKitLocal,
        estadoEntrega: String
    ) {
        insertEntrega(entrega)
        marcarKitEntregado(
            uuidKitAsignado = kit.uuidKitAsignado,
            estadoEntrega = estadoEntrega,
            fechaEntrega = entrega.fechaEntrega,
            descripcionEntrega = entrega.descripcionAyuda,
            evidenciaLocalUri = entrega.uuidEntrega,
            estadoSync = pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync.NUEVO
        )
    }
}
