package pucp.edu.caritas_movile_grd.Observaciones

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(tableName = "observacion_local")
data class ObservacionLocal(
    @PrimaryKey val uuidObservacion: String,
    val uuidIncidencia: String,
    val textoObservacion: String,
    val fechaRegistro: String,
    val estadoSync: EstadoSync = EstadoSync.NUEVO,
    val idObservacionRemota: String? = null
)