package pucp.edu.caritas_movile_grd.Cursos

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(
    tableName = "participante_local",
    foreignKeys = [
        ForeignKey(
            entity = CursoCapacitacionLocal::class,
            parentColumns = ["idCursoRemoto"],
            childColumns = ["idCursoRemoto"]
        )
    ]
)
data class ParticipanteLocal(
    @PrimaryKey val idParticipanteRemoto: Int,
    val idCursoRemoto: Int,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val asistio: Boolean = false,
    val estadoSync: EstadoSync = EstadoSync.SINCRONIZADO
)
