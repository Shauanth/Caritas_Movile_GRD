package pucp.edu.caritas_movile_grd.Masters

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalogo_local")
data class CatalogoLocal(
    @PrimaryKey val idCatalogo: Int,
    val categoria: String, // Ej: "TIPO_INCIDENCIA", "TIPO_DOCUMENTO"
    val valor: String      // Ej: "Inundación", "Cédula"
)
