package pucp.edu.caritas_movile_grd.Incidencias

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import pucp.edu.caritas_movile_grd.Masters.*

@Entity(
    tableName = "incidencia_local",
    foreignKeys = [
        ForeignKey(entity = ParroquiaLocal::class, parentColumns = ["idParroquia"], childColumns = ["idParroquia"], deferred = true),
        ForeignKey(entity = CatalogoLocal::class, parentColumns = ["idCatalogo"], childColumns = ["idCatalogoTipo"], deferred = true)
    ]
)
data class IncidenciaLocal(
    @PrimaryKey val uuidIncidencia: String,
    val idIncidenciaRemota: Int? = null,
    val uuidUsuario: String,
    val idParroquia: Int,
    val idCatalogoTipo: Int,
    val descripcion: String,
    val nombre: String = "",           // título del incidente
    val numAfectados: Int = 0,
    val responsable: String = "Brigadista",
    val estado: String,                // "ABIERTO", "ASIGNADO", "DATA RECOPILADA", etc.
    val estadoSync: EstadoSync = EstadoSync.NUEVO,
    val fechaUltimaModificacion: Long
)
