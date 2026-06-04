package pucp.edu.caritas_movile_grd.Kits

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.Incidencias.AfectadoLocal
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(
    tableName = "entrega_kit_local",
    foreignKeys = [
        ForeignKey(
            entity = AfectadoLocal::class,
            parentColumns = ["uuidAfectado"],
            childColumns = ["uuidAfectado"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EntregaKitLocal(
    @PrimaryKey val uuidEntrega: String,
    val idEntregaRemota: Int? = null,
    val uuidAfectado: String,
    val kitEntregado: String,
    val cantidad: Int,
    val fechaEntrega: Long,
    val estadoSync: EstadoSync = EstadoSync.NUEVO
)
