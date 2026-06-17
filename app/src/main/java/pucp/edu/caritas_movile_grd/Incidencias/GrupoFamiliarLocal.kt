package pucp.edu.caritas_movile_grd.Incidencias

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

/**
 * Grupo familiar afectado recopilado en campo.
 *
 * El `uuidGrupo` coincide con el `familiaId` que llevan los [AfectadoLocal], de modo
 * que las personas se agrupan por ese identificador. Las observaciones se sincronizan
 * hacia `grupo_familiar_afectado.observaciones` en el backend a través del payload de
 * afectado (`observacionesGrupo`), respetando el modelo web.
 */
@Entity(tableName = "grupo_familiar_local")
data class GrupoFamiliarLocal(
    @PrimaryKey val uuidGrupo: String,
    val uuidIncidencia: String,
    val nombre: String,
    val observaciones: String? = null,
    val direccion: String? = null,
    val condicionVivienda: String? = null,
    /**
     * Verificación de empadronamiento en campo: el brigadista marca la familia como
     * confirmada una vez que validó sus datos en el terreno. Es un estado de la
     * recopilación, independiente de [estadoSync] (que refleja la sincronización).
     */
    val verificado: Boolean = false,
    val idGrupoRemoto: String? = null,
    val estadoSync: EstadoSync = EstadoSync.NUEVO
)
