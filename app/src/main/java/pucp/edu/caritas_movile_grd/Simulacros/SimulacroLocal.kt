package pucp.edu.caritas_movile_grd.Simulacros

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(tableName = "simulacro_local")
data class SimulacroLocal(
    @PrimaryKey val uuidSimulacro: String,
    val idActividadPreventivaRemota: String? = null,
    val codigoActividad: String? = null,
    val estadoActividad: String = "PROGRAMADA",
    val idParroquia: String? = null,
    val parroquiaNombre: String? = null,
    val tipoActividadPreventiva: String? = null,
    val nombreActividad: String = "",
    val fechaProgramada: String? = null,
    val horarioInicio: String? = null,
    val horarioFin: String? = null,
    val lugarActividad: String? = null,
    val publicoObjetivo: String? = null,
    val numeroParticipantesEstimado: Int? = null,
    val numeroParticipantesReal: Int? = null,
    val descripcionActividad: String? = null,
    val resultadoGeneral: String? = null,
    val recomendaciones: String? = null,
    val observaciones: String? = null,
    val indicacionesEquipo: String? = null,
    val reporteBrigadista: String? = null,
    val duracionSimulacro: Int? = null,
    val fechaEjecucion: String? = null,
    val updatedAtRemoto: String? = null,
    val idBrigadistaParroquialResponsable: String? = null,
    val idUsuarioGRDResponsable: String? = null,
    val nombreResponsable: String? = null,
    val estadoSync: EstadoSync = EstadoSync.SINCRONIZADO
)
