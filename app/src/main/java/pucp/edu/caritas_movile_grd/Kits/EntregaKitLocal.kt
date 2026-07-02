package pucp.edu.caritas_movile_grd.Kits

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(tableName = "entrega_kit_local")
data class EntregaKitLocal(
    @PrimaryKey val uuidEntrega: String,
    val idEntregaRemota: String? = null,
    val uuidAfectado: String? = null,
    val uuidGrupoFamiliar: String? = null,
    val refIdFamilia: String? = null,
    val idGrupoFamiliar: String? = null,
    val idPersonaAfectadaRemota: String? = null,
    val uuidIncidencia: String? = null,
    val idIncidenciaRemota: String? = null,
    val uuidKitAsignado: String? = null,
    val kitEntregado: String,
    val estadoEntrega: String? = null,
    val cantidad: Int,
    val descripcionAyuda: String? = null,
    val observaciones: String? = null,
    val articulosJson: String? = null,
    val fechaEntrega: Long,
    val estadoSync: EstadoSync = EstadoSync.NUEVO
)
