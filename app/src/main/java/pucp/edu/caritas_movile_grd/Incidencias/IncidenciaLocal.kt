package pucp.edu.caritas_movile_grd.Incidencias

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import pucp.edu.caritas_movile_grd.Masters.*

@Entity(
    tableName = "incidencia_local",
    foreignKeys = [
        ForeignKey(entity = ParroquiaLocal::class, parentColumns = ["idParroquia"], childColumns = ["idParroquia"]),
        ForeignKey(entity = CatalogoLocal::class, parentColumns = ["idCatalogo"], childColumns = ["idCatalogoTipo"])
    ]
)
data class IncidenciaLocal(
    @PrimaryKey val uuidIncidencia: String,
    val idIncidenciaRemota: Int? = null, // Nulo hasta que se sincronice
    val uuidUsuario: String,
    val idParroquia: Int,
    val idCatalogoTipo: Int, // Ej: ID de "Incendio"
    val descripcion: String,
    val estado: String, // "Activa", "Cerrada"
    val estadoSync: EstadoSync = EstadoSync.NUEVO,
    val fechaUltimaModificacion: Long // Timestamp (System.currentTimeMillis())
)
