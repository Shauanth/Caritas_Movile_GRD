package pucp.edu.caritas_movile_grd.Incidencias

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(tableName = "afectado_local")
data class AfectadoLocal(
    @PrimaryKey val uuidAfectado: String,
    val uuidIncidencia: String,
    val idCatalogoDoc: Int,           // Tipo de documento
    val idAfectadoRemoto: String? = null,
    val documentoIdentidad: String,
    val nombres: String,
    // Datos extendidos
    val apellidoPaterno: String? = null,
    val apellidoMaterno: String? = null,
    val edad: Int? = null,
    val genero: String? = null,
    val celular: String? = null,
    val parentesco: String? = null,
    val situacionActual: String? = null,
    val familiaId: String? = null,
    val familiaNombre: String? = null,
    val estadoSync: EstadoSync = EstadoSync.NUEVO
)