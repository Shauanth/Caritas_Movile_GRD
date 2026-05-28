package pucp.edu.caritas_movile_grd.LocalBDConector

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaLocal
import pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal

@Dao
interface SyncDao {

    // 1. Obtener todas las incidencias que se crearon offline y necesitan enviarse al backend
    @Query("SELECT * FROM incidencia_local WHERE estadoSync = 'NUEVO'")
    suspend fun getIncidenciasNuevasParaSincronizar(): List<IncidenciaLocal>

    // 2. Obtener incidencias que fueron modificadas offline
    @Query("SELECT * FROM incidencia_local WHERE estadoSync = 'EDITADO'")
    suspend fun getIncidenciasEditadasParaSincronizar(): List<IncidenciaLocal>

    // 3. Obtener fotos pendientes de subir
    @Query("SELECT * FROM evidencia_local WHERE estadoSync = 'PENDIENTE_SUBIDA'")
    suspend fun getEvidenciasPendientes(): List<EvidenciaLocal>

    // Cuando el backend responde, actualizamos el ID real y el estado a SINCRONIZADO
    @Query("UPDATE incidencia_local SET idIncidenciaRemota = :idRemoto, estadoSync = 'SINCRONIZADO' WHERE uuidIncidencia = :uuid")
    suspend fun marcarIncidenciaComoSincronizada(uuid: String, idRemoto: Int)

    @Insert
    suspend fun insertarIncidenciaOffline(incidencia: IncidenciaLocal)
}