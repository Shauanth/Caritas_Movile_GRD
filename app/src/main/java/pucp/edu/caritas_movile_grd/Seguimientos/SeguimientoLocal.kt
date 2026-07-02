package pucp.edu.caritas_movile_grd.Seguimientos

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(tableName = "seguimiento_local")
data class SeguimientoLocal(
    @PrimaryKey val uuidSeguimiento: String,
    val uuidIncidencia: String,
    val idIncidenciaRemota: String? = null,
    val codigoCasoRemoto: String? = null,
    val situacion: String? = null,
    val descripcion: String? = null,
    val necesidadesPendientes: String? = null,
    val recomendaciones: String? = null,
    val estado: String = "REGISTRADO",
    val observaciones: String? = null,
    val fechaSeguimiento: String,
    val estadoSync: EstadoSync = EstadoSync.NUEVO,
    val idSeguimientoRemoto: String? = null
)
