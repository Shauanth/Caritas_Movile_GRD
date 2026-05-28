package pucp.edu.caritas_movile_grd.Cursos

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(
    tableName = "material_capacitacion_local",
    foreignKeys = [
        ForeignKey(
            entity = CursoCapacitacionLocal::class,
            parentColumns = ["idCursoRemoto"],
            childColumns = ["idCursoRemoto"]
        )
    ]
)
data class MaterialCapacitacionLocal(
    @PrimaryKey val idMaterialRemoto: Int,
    val idCursoRemoto: Int,
    val nombre: String,
    val url: String,
    val tipo: String, // PDF, Video, etc.
    val estadoSync: EstadoSync = EstadoSync.SINCRONIZADO
)
