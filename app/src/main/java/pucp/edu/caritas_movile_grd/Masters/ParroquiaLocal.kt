package pucp.edu.caritas_movile_grd.Masters

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parroquia_local")
data class ParroquiaLocal(
    @PrimaryKey val idParroquia: Int,
    val nombre: String
)