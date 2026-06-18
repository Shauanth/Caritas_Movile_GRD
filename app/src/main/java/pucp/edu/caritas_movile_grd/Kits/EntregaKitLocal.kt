package pucp.edu.caritas_movile_grd.Kits

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(tableName = "entrega_kit_local")
data class EntregaKitLocal(
    @PrimaryKey val uuidEntrega: String,
    val idEntregaRemota: String? = null,
    val uuidAfectado: String,
    val uuidIncidencia: String? = null,
    val idIncidenciaRemota: String? = null,    
    val kitEntregado: String,
    val cantidad: Int,
    val fechaEntrega: Long,
    val estadoSync: EstadoSync = EstadoSync.NUEVO
)
