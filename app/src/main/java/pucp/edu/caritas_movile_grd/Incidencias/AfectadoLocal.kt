package pucp.edu.caritas_movile_grd.Incidencias

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.Masters.CatalogoLocal
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(
    tableName = "afectado_local",
    foreignKeys = [
        ForeignKey(entity = IncidenciaLocal::class, parentColumns = ["uuidIncidencia"], childColumns = ["uuidIncidencia"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CatalogoLocal::class, parentColumns = ["idCatalogo"], childColumns = ["idCatalogoDoc"])
    ]
)
data class AfectadoLocal(
    @PrimaryKey val uuidAfectado: String,
    val uuidIncidencia: String,
    val idCatalogoDoc: Int, // Tipo de documento
    val idAfectadoRemoto: Int? = null,
    val documentoIdentidad: String,
    val nombres: String,
    val estadoSync: EstadoSync = EstadoSync.NUEVO
)