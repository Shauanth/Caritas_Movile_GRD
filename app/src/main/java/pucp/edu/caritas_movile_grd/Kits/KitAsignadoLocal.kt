package pucp.edu.caritas_movile_grd.Kits

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(tableName = "kit_asignado_local")
data class KitAsignadoLocal(
    @PrimaryKey val uuidKitAsignado: String,
    val uuidIncidencia: String,
    val idIncidenciaRemota: String? = null,

    // Referencia a familia/persona según venga del informe web
    val refIdFamilia: String? = null,
    val nombreFamilia: String? = null,
    val uuidAfectado: String? = null,
    val idPersonaAfectadaRemota: String? = null,

    // Kit asignado por el especialista
    val idKitEmergenciaRemoto: String? = null,
    val tipoKit: String,

    // Estado local de entrega
    val estadoEntrega: String = "PENDIENTE", // PENDIENTE, PARCIAL, ENTREGADO

    val fechaAsignacion: Long = System.currentTimeMillis(),
    val fechaEntrega: Long? = null,
    val descripcionEntrega: String? = null,
    val evidenciaLocalUri: String? = null,

    val estadoSync: EstadoSync = EstadoSync.SINCRONIZADO
)
