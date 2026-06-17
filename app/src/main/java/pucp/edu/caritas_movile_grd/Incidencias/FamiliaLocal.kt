package pucp.edu.caritas_movile_grd.Incidencias

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

/**
 * Grupo familiar afectado en el móvil.
 *
 * Espeja a `GrupoFamiliarAfectado` de la web. `familiaId` es la misma clave que
 * usa [AfectadoLocal.familiaId] para enlazar a sus integrantes, de modo que la
 * agrupación por familia y el comentario por familia queden consistentes con el
 * backend al sincronizar (`codigoGrupo` / `observacionesGrupo`).
 */
@Entity(tableName = "familia_local")
data class FamiliaLocal(
    @PrimaryKey val familiaId: String,          // mismo valor que AfectadoLocal.familiaId
    val uuidIncidencia: String,
    val nombreReferencia: String,
    val comentario: String? = null,             // = GrupoFamiliarAfectado.observaciones (web)
    val direccion: String? = null,              // = GrupoFamiliarAfectado.direccion (web)
    val condicionVivienda: String? = null,      // = GrupoFamiliarAfectado.condicionVivienda (web)
    /**
     * Verificación de empadronamiento en campo: el brigadista confirma que validó
     * los datos de la familia en el terreno. Sube al backend como
     * `GrupoFamiliarAfectado.condicionFinal = "VERIFICADO"`. Es independiente de
     * [estadoSync] (que refleja la sincronización, no la verificación).
     */
    val verificado: Boolean = false,
    val idFamiliaRemota: String? = null,        // idGrupoFamiliar remoto cuando ya se sincronizó
    val estadoSync: EstadoSync = EstadoSync.NUEVO,
    val fechaUltimaModificacion: Long = System.currentTimeMillis()
)
