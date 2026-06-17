package pucp.edu.caritas_movile_grd.Incidencias
import pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal
import pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pucp.edu.caritas_movile_grd.Seguimientos.SeguimientoLocal
import androidx.room.Insert
import androidx.room.OnConflictStrategy

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

    @Update
    suspend fun updateAfectado(afectado: AfectadoLocal)

    @Delete
    suspend fun deleteAfectado(afectado: AfectadoLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservacion(observacion: ObservacionLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservaciones(observaciones: List<ObservacionLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeguimiento(seguimiento: SeguimientoLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncidencia(incidencia: IncidenciaLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncidencias(incidencias: List<IncidenciaLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAfectados(afectados: List<AfectadoLocal>)

    // ── Familias (grupos familiares) ───────────────────────────────────────────
    @Query("SELECT * FROM familia_local WHERE uuidIncidencia = :uuidIncidencia")
    fun getFamiliasByIncidencia(uuidIncidencia: String): Flow<List<FamiliaLocal>>

    @Query("SELECT * FROM familia_local WHERE familiaId = :familiaId LIMIT 1")
    suspend fun getFamiliaById(familiaId: String): FamiliaLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilia(familia: FamiliaLocal)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFamiliasIfNotExists(familias: List<FamiliaLocal>)

    @Query("""
        SELECT * FROM observacion_local
        WHERE uuidIncidencia = :uuidIncidencia
        ORDER BY fechaRegistro DESC
    """)
    fun getObservacionesByIncidencia(uuidIncidencia: String): Flow<List<ObservacionLocal>>

    @Query("""
        SELECT * FROM seguimiento_local
        WHERE uuidIncidencia = :uuidIncidencia
        ORDER BY fechaSeguimiento DESC
    """)
    fun getSeguimientosByIncidencia(uuidIncidencia: String): Flow<List<SeguimientoLocal>>

    @Query("""
        SELECT * FROM evidencia_local
        WHERE uuidReferencia = :uuidIncidencia
        ORDER BY rowid DESC
    """)
    fun getEvidenciasByIncidencia(uuidIncidencia: String): Flow<List<EvidenciaLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidencia(evidencia: EvidenciaLocal)
}
