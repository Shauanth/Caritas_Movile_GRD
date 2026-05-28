package pucp.edu.caritas_movile_grd.Simulacros

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(tableName = "simulacro_local")
data class SimulacroLocal(
    @PrimaryKey val uuidSimulacro: String,
    val idSimulacroRemoto: Int? = null,
    val tipo: String,              // "Sismo", "Incendio", "Inundación", "Evacuación"
    val parroquia: String,
    val fecha: Long,               // timestamp
    val descripcion: String = "",
    val estado: String,            // "Programada", "Asignada", "Ejecutada", "Observada", "Validada"
    val indicaciones: String = "",
    val notasBrigadista: String = "",
    val comentarioObservacion: String = "",
    val brigadistaAsignado: String = "",
    val estadoSync: EstadoSync = EstadoSync.NUEVO
)
