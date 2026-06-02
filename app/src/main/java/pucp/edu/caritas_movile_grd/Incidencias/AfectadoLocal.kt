package pucp.edu.caritas_movile_grd.Incidencias

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(tableName = "afectado_local")
data class AfectadoLocal(
    @PrimaryKey val uuidAfectado: String,
    val uuidIncidencia: String,
    val idCatalogoDoc: Int, // Tipo de documento
    val idAfectadoRemoto: Int? = null,
    val documentoIdentidad: String,
    val nombres: String,
    val estadoSync: EstadoSync = EstadoSync.NUEVO
)