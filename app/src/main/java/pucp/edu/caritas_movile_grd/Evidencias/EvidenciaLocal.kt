package pucp.edu.caritas_movile_grd.Evidencias

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

@Entity(
    tableName = "evidencia_local"
)
data class EvidenciaLocal(
    @PrimaryKey val uuidEvidencia: String,
    val uuidReferencia: String, // Puede ser UUID de Incidencia o de Seguimiento
    val rutaLocal: String,      // URI local: content://media/external/images/...
    val urlS3: String? = null,  // Se llena después de subirla
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE_SUBIDA
)