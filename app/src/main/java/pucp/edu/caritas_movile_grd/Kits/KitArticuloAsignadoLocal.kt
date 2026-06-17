package pucp.edu.caritas_movile_grd.Kits

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kit_articulo_asignado_local")
data class KitArticuloAsignadoLocal(
    @PrimaryKey val uuidArticuloAsignado: String,
    val uuidKitAsignado: String,

    val codigo: String,
    val descripcion: String,
    val cantidadAsignada: Int,
    val cantidadEntregada: Int = 0,

    val confirmado: Boolean = false
)
