package pucp.edu.caritas_movile_grd.login

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.Masters.ParroquiaLocal

@Entity(
    tableName = "asignacion_territorio",
    foreignKeys = [
        ForeignKey(
            entity = PerfilUsuarioLocal::class,
            parentColumns = ["uuidUsuario"],
            childColumns = ["uuidUsuario"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ParroquiaLocal::class,
            parentColumns = ["idParroquia"],
            childColumns = ["idParroquia"]
        )
    ]
)
data class AsignacionTerritorio(
    @PrimaryKey val uuidAsignacion: String,
    val uuidUsuario: String,
    val idParroquia: Int
)
