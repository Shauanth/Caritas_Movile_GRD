package pucp.edu.caritas_movile_grd.LocalBDConector

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal
import pucp.edu.caritas_movile_grd.Incidencias.AfectadoLocal
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaLocal
import pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal
import pucp.edu.caritas_movile_grd.Seguimientos.SeguimientoLocal
import pucp.edu.caritas_movile_grd.Kits.EntregaKitLocal

@Dao
interface SyncDao {

    // 1. Obtener todas las incidencias que se crearon offline y necesitan enviarse al backend
    @Query("SELECT * FROM incidencia_local WHERE estadoSync = 'NUEVO'")
    suspend fun getIncidenciasNuevasParaSincronizar(): List<IncidenciaLocal>

    // 2. Obtener incidencias que fueron modificadas offline
    @Query("SELECT * FROM incidencia_local WHERE estadoSync = 'EDITADO'")
    suspend fun getIncidenciasEditadasParaSincronizar(): List<IncidenciaLocal>

    // 3. Obtener afectados nuevos o editados asociados a una incidencia local
    @Query("""
        SELECT * FROM afectado_local
        WHERE uuidIncidencia = :uuidIncidencia
          AND estadoSync IN ('NUEVO', 'EDITADO')
    """)
    suspend fun getAfectadosPendientesPorIncidencia(
        uuidIncidencia: String
    ): List<AfectadoLocal>

    // 4. Obtener todos los afectados pendientes, sin filtrar por incidencia
    @Query("""
        SELECT * FROM afectado_local
        WHERE estadoSync IN ('NUEVO', 'EDITADO')
    """)
    suspend fun getAfectadosPendientesParaSincronizar(): List<AfectadoLocal>

    // 5. Obtener fotos pendientes de subir
    @Query("SELECT * FROM evidencia_local WHERE estadoSync IN ('PENDIENTE_SUBIDA', 'NUEVO')")
    suspend fun getEvidenciasPendientes(): List<EvidenciaLocal>

    // Cuando el backend responde, actualizamos el ID real y el estado a SINCRONIZADO
    @Query("""
        UPDATE incidencia_local
        SET idIncidenciaRemota = :idRemoto,
            codigoCasoRemoto = :codigoCaso,
            estadoSync = 'SINCRONIZADO'
        WHERE uuidIncidencia = :uuid
    """)
    suspend fun marcarIncidenciaComoSincronizada(
        uuid: String,
        idRemoto: String,
        codigoCaso: String?
    )

    // Cuando el backend responde, actualizamos el ID real del afectado y lo marcamos como SINCRONIZADO
    @Query("""
        UPDATE afectado_local
        SET idAfectadoRemoto = :idRemoto,
            estadoSync = 'SINCRONIZADO'
        WHERE uuidAfectado = :uuidAfectado
    """)
    suspend fun marcarAfectadoComoSincronizado(
        uuidAfectado: String,
        idRemoto: String
    )

    // Cuando el backend responde, marcamos la evidencia como SINCRONIZADA
    @Query("""
        UPDATE evidencia_local
        SET urlS3 = :urlArchivo,
            estadoSync = 'SINCRONIZADO'
        WHERE uuidEvidencia = :uuidEvidencia
    """)
    suspend fun marcarEvidenciaComoSincronizada(
        uuidEvidencia: String,
        urlArchivo: String?
    )    

    @Insert
    suspend fun insertarIncidenciaOffline(incidencia: IncidenciaLocal)

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun upsertIncidencias(incidencias: List<IncidenciaLocal>)

    @Query("""
        UPDATE incidencia_local
        SET idIncidenciaRemota = :idRemoto,
            codigoCasoRemoto = :codigoCaso,
            nombre = :nombre,
            descripcion = :descripcion,
            estado = CASE
                WHEN estado = 'DATA RECOPILADA'
                 AND :estado IN ('ASIGNADO', 'ABIERTO', 'EN CAMPO')
                THEN estado
                ELSE :estado
            END,
            idResponsableGRD = :idResponsableGRD,
            estadoSync = 'SINCRONIZADO',
            fechaUltimaModificacion = :fechaUltimaModificacion
        WHERE uuidIncidencia = :uuid
        AND estadoSync = 'SINCRONIZADO'
    """)
    suspend fun actualizarIncidenciaSincronizada(
        uuid: String,
        idRemoto: String?,
        codigoCaso: String?,
        nombre: String,
        descripcion: String,
        estado: String,
        idResponsableGRD: String?,
        fechaUltimaModificacion: Long
    )

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertEvidenciasIfNotExists(evidencias: List<pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal>)

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertAfectadosIfNotExists(afectados: List<pucp.edu.caritas_movile_grd.Incidencias.AfectadoLocal>)

    @Query("""
        UPDATE evidencia_local
        SET urlS3 = :urlFirmada, rutaLocal = :urlFirmada
        WHERE uuidEvidencia = :uuidEvidencia AND estadoSync = 'SINCRONIZADO'
    """)
    suspend fun refrescarUrlEvidencia(uuidEvidencia: String, urlFirmada: String)

    @Query("SELECT * FROM evidencia_local WHERE estadoSync = 'PENDIENTE_ELIMINACION'")
    suspend fun getEvidenciasPendientesEliminacion(): List<pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal>

    @Query("SELECT * FROM afectado_local WHERE estadoSync = 'PENDIENTE_ELIMINACION' AND idAfectadoRemoto IS NOT NULL")
    suspend fun getAfectadosPendientesEliminacion(): List<pucp.edu.caritas_movile_grd.Incidencias.AfectadoLocal>

    @Query("DELETE FROM afectado_local WHERE uuidAfectado = :uuidAfectado")
    suspend fun deleteAfectadoByUuid(uuidAfectado: String)

    @Query("DELETE FROM evidencia_local WHERE uuidEvidencia = :uuidEvidencia")
    suspend fun deleteEvidenciaByUuid(uuidEvidencia: String)

    @Query("UPDATE evidencia_local SET estadoSync = 'PENDIENTE_ELIMINACION' WHERE uuidEvidencia = :uuidEvidencia")
    suspend fun marcarEvidenciaParaEliminar(uuidEvidencia: String)

    @Query("""
        DELETE FROM evidencia_local
        WHERE uuidReferencia = :uuidIncidencia
        AND estadoSync IN ('SINCRONIZADO', 'PENDIENTE_ELIMINACION')
        AND uuidEvidencia NOT IN (:uuidsActivos)
    """)
    suspend fun eliminarEvidenciasRemotas(uuidIncidencia: String, uuidsActivos: List<String>)

    @Query("DELETE FROM evidencia_local WHERE uuidReferencia = :uuidIncidencia AND estadoSync IN ('SINCRONIZADO', 'PENDIENTE_ELIMINACION')")
    suspend fun eliminarTodasEvidenciasRemotas(uuidIncidencia: String)


    @Query("SELECT * FROM incidencia_local WHERE uuidIncidencia = :uuidIncidencia LIMIT 1")
    suspend fun getIncidenciaPorUuid(uuidIncidencia: String): IncidenciaLocal?

    @Query("""
        SELECT * FROM observacion_local
        WHERE estadoSync IN ('NUEVO', 'EDITADO')
    """)
    suspend fun getObservacionesPendientesParaSincronizar(): List<ObservacionLocal>

    @Query("""
        UPDATE observacion_local
        SET idObservacionRemota = :idRemoto,
            estadoSync = 'SINCRONIZADO'
        WHERE uuidObservacion = :uuidObservacion
    """)
    suspend fun marcarObservacionComoSincronizada(
        uuidObservacion: String,
        idRemoto: String
    )   

    @Query("""
        SELECT * FROM seguimiento_local
        WHERE estadoSync IN ('NUEVO', 'EDITADO')
    """)
    suspend fun getSeguimientosPendientesParaSincronizar(): List<SeguimientoLocal>

    @Query("""
        UPDATE seguimiento_local
        SET idSeguimientoRemoto = :idRemoto,
            estadoSync = 'SINCRONIZADO'
        WHERE uuidSeguimiento = :uuidSeguimiento
    """)
    suspend fun marcarSeguimientoComoSincronizado(
        uuidSeguimiento: String,
        idRemoto: String
    )    
    @Query("""
        SELECT * FROM entrega_kit_local
        WHERE estadoSync IN ('NUEVO', 'EDITADO')
    """)
    suspend fun getEntregasPendientesParaSincronizar(): List<EntregaKitLocal>

    @Query("SELECT * FROM entrega_kit_local WHERE uuidEntrega = :uuidEntrega LIMIT 1")
    suspend fun getEntregaPorUuid(uuidEntrega: String): EntregaKitLocal?

    @Query("SELECT * FROM evidencia_local WHERE uuidReferencia = :uuidReferencia AND estadoSync IN ('PENDIENTE_SUBIDA', 'NUEVO')")
    suspend fun getEvidenciasPendientesPorReferencia(uuidReferencia: String): List<EvidenciaLocal>

    @Query("""
        UPDATE entrega_kit_local
        SET idEntregaRemota = :idRemoto,
            estadoSync = 'SINCRONIZADO'
        WHERE uuidEntrega = :uuidEntrega
    """)
    suspend fun marcarEntregaComoSincronizada(
        uuidEntrega: String,
        idRemoto: String
    )

    @Query("""
        UPDATE incidencia_local
        SET estado = :estado,
            estadoSync = 'SINCRONIZADO',
            fechaUltimaModificacion = :fechaUltimaModificacion
        WHERE uuidIncidencia = :uuidIncidencia
    """)
    suspend fun actualizarEstadoIncidencia(
        uuidIncidencia: String,
        estado: String,
        fechaUltimaModificacion: Long
    )
}
