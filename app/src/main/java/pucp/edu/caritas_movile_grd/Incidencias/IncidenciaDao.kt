package pucp.edu.caritas_movile_grd.Incidencias
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

    @Query("DELETE FROM afectado_local WHERE uuidAfectado = :uuidAfectado")
    suspend fun eliminarAfectado(uuidAfectado: String)

    @Query("DELETE FROM afectado_local WHERE familiaId = :familiaId")
    suspend fun eliminarAfectadosDeFamilia(familiaId: String)

    // ── Grupos familiares ─────────────────────────────────────────────────────
    @Query("SELECT * FROM grupo_familiar_local WHERE uuidIncidencia = :uuidIncidencia")
    fun getGruposFamiliaresByIncidencia(uuidIncidencia: String): Flow<List<GrupoFamiliarLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrupoFamiliar(grupo: GrupoFamiliarLocal)

    @Query("DELETE FROM grupo_familiar_local WHERE uuidGrupo = :uuidGrupo")
    suspend fun eliminarGrupoFamiliar(uuidGrupo: String)

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
}
