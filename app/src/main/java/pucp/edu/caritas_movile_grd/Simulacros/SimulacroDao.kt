package pucp.edu.caritas_movile_grd.Simulacros

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulacroDao {
    @Query("SELECT * FROM simulacro_local ORDER BY fechaProgramada DESC, horarioInicio DESC")
    fun getAllSimulacros(): Flow<List<SimulacroLocal>>

    @Query("SELECT * FROM simulacro_local WHERE estadoActividad = :estado ORDER BY fechaProgramada DESC, horarioInicio DESC")
    fun getSimulacrosByEstado(estado: String): Flow<List<SimulacroLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulacro(simulacro: SimulacroLocal)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSimulacrosIfNotExists(simulacros: List<SimulacroLocal>)

    @Update
    suspend fun updateSimulacro(simulacro: SimulacroLocal)

    @Query("SELECT * FROM simulacro_local WHERE uuidSimulacro = :uuid")
    suspend fun getSimulacroById(uuid: String): SimulacroLocal?

    @Query("""
        SELECT * FROM simulacro_local
        WHERE estadoSync IN ('NUEVO', 'EDITADO')
        ORDER BY fechaEjecucion ASC
    """)
    suspend fun getSimulacrosPendientesSincronizar(): List<SimulacroLocal>

    @Query("""
        UPDATE simulacro_local
        SET idActividadPreventivaRemota = :idActividadPreventivaRemota,
            codigoActividad = :codigoActividad,
            estadoActividad = :estadoActividad,
            idParroquia = :idParroquia,
            parroquiaNombre = :parroquiaNombre,
            tipoActividadPreventiva = :tipoActividadPreventiva,
            nombreActividad = :nombreActividad,
            fechaProgramada = :fechaProgramada,
            horarioInicio = :horarioInicio,
            horarioFin = :horarioFin,
            lugarActividad = :lugarActividad,
            publicoObjetivo = :publicoObjetivo,
            numeroParticipantesEstimado = :numeroParticipantesEstimado,
            numeroParticipantesReal = :numeroParticipantesReal,
            descripcionActividad = :descripcionActividad,
            resultadoGeneral = :resultadoGeneral,
            recomendaciones = :recomendaciones,
            observaciones = :observaciones,
            indicacionesEquipo = :indicacionesEquipo,
            reporteBrigadista = :reporteBrigadista,
            duracionSimulacro = :duracionSimulacro,
            fechaEjecucion = :fechaEjecucion,
            updatedAtRemoto = :updatedAtRemoto,
            idBrigadistaParroquialResponsable = :idBrigadistaParroquialResponsable,
            idUsuarioGRDResponsable = :idUsuarioGRDResponsable,
            nombreResponsable = :nombreResponsable,
            estadoSync = 'SINCRONIZADO'
        WHERE uuidSimulacro = :uuidSimulacro
          AND estadoSync = 'SINCRONIZADO'
    """)
    suspend fun actualizarSimulacroDescargadoSinPendientes(
        uuidSimulacro: String,
        idActividadPreventivaRemota: String?,
        codigoActividad: String?,
        estadoActividad: String,
        idParroquia: String?,
        parroquiaNombre: String?,
        tipoActividadPreventiva: String?,
        nombreActividad: String,
        fechaProgramada: String?,
        horarioInicio: String?,
        horarioFin: String?,
        lugarActividad: String?,
        publicoObjetivo: String?,
        numeroParticipantesEstimado: Int?,
        numeroParticipantesReal: Int?,
        descripcionActividad: String?,
        resultadoGeneral: String?,
        recomendaciones: String?,
        observaciones: String?,
        indicacionesEquipo: String?,
        reporteBrigadista: String?,
        duracionSimulacro: Int?,
        fechaEjecucion: String?,
        updatedAtRemoto: String?,
        idBrigadistaParroquialResponsable: String?,
        idUsuarioGRDResponsable: String?,
        nombreResponsable: String?
    )

    @Transaction
    suspend fun upsertSimulacrosDescargados(simulacros: List<SimulacroLocal>) {
        insertSimulacrosIfNotExists(simulacros)
        simulacros.forEach { s ->
            actualizarSimulacroDescargadoSinPendientes(
                uuidSimulacro = s.uuidSimulacro,
                idActividadPreventivaRemota = s.idActividadPreventivaRemota,
                codigoActividad = s.codigoActividad,
                estadoActividad = s.estadoActividad,
                idParroquia = s.idParroquia,
                parroquiaNombre = s.parroquiaNombre,
                tipoActividadPreventiva = s.tipoActividadPreventiva,
                nombreActividad = s.nombreActividad,
                fechaProgramada = s.fechaProgramada,
                horarioInicio = s.horarioInicio,
                horarioFin = s.horarioFin,
                lugarActividad = s.lugarActividad,
                publicoObjetivo = s.publicoObjetivo,
                numeroParticipantesEstimado = s.numeroParticipantesEstimado,
                numeroParticipantesReal = s.numeroParticipantesReal,
                descripcionActividad = s.descripcionActividad,
                resultadoGeneral = s.resultadoGeneral,
                recomendaciones = s.recomendaciones,
                observaciones = s.observaciones,
                indicacionesEquipo = s.indicacionesEquipo,
                reporteBrigadista = s.reporteBrigadista,
                duracionSimulacro = s.duracionSimulacro,
                fechaEjecucion = s.fechaEjecucion,
                updatedAtRemoto = s.updatedAtRemoto,
                idBrigadistaParroquialResponsable = s.idBrigadistaParroquialResponsable,
                idUsuarioGRDResponsable = s.idUsuarioGRDResponsable,
                nombreResponsable = s.nombreResponsable
            )
        }
    }

    @Query("""
        UPDATE simulacro_local
        SET estadoActividad = 'EJECUTADA',
            resultadoGeneral = :resultadoGeneral,
            reporteBrigadista = :reporteBrigadista,
            numeroParticipantesReal = :numeroParticipantesReal,
            duracionSimulacro = :duracionSimulacro,
            recomendaciones = :recomendaciones,
            observaciones = :observaciones,
            fechaEjecucion = :fechaEjecucion,
            estadoSync = 'EDITADO'
        WHERE uuidSimulacro = :uuidSimulacro
    """)
    suspend fun marcarEjecucionLocal(
        uuidSimulacro: String,
        resultadoGeneral: String,
        reporteBrigadista: String,
        numeroParticipantesReal: Int?,
        duracionSimulacro: Int?,
        recomendaciones: String?,
        observaciones: String?,
        fechaEjecucion: String
    )

    @Query("""
        UPDATE simulacro_local
        SET estadoActividad = :estadoActividad,
            estadoSync = 'SINCRONIZADO',
            fechaEjecucion = COALESCE(:fechaEjecucion, fechaEjecucion),
            updatedAtRemoto = COALESCE(:fechaSincronizacion, updatedAtRemoto)
        WHERE uuidSimulacro = :uuidSimulacro
    """)
    suspend fun marcarSincronizado(
        uuidSimulacro: String,
        estadoActividad: String,
        fechaEjecucion: String?,
        fechaSincronizacion: String?
    )
}
