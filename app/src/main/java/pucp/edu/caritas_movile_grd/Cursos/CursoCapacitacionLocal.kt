package pucp.edu.caritas_movile_grd.Cursos

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(tableName = "curso_capacitacion_local")
data class CursoCapacitacionLocal(
    @PrimaryKey val idCursoRemoto: Int,
    val nombre: String,
    val descripcion: String,
    val fecha: Long,
    val facilitador: String,
    val estadoSync: EstadoSync = EstadoSync.SINCRONIZADO
)
