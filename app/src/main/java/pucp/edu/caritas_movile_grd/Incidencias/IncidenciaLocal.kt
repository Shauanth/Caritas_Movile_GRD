package pucp.edu.caritas_movile_grd.Incidencias

import androidx.room.Entity
import androidx.room.PrimaryKey
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import pucp.edu.caritas_movile_grd.Masters.*




@Entity(tableName = "incidencia_local")
data class IncidenciaLocal(
    @PrimaryKey val uuidIncidencia: String,
    val idIncidenciaRemota: Int? = null,
    val uuidUsuario: String,
    val idParroquia: Int,
    val idCatalogoTipo: Int,
    val descripcion: String,
    val nombre: String = "",
    val numAfectados: Int = 0,
    val responsable: String = "Brigadista",
    val estado: String,
    val estadoSync: EstadoSync = EstadoSync.NUEVO,
    val fechaUltimaModificacion: Long,

    // Nuevos campos según diseño
    val causa: String? = null,
    val reportadoPorNombre: String? = null,
    val reportadoPorCelular: String? = null,
    val reportadoPorDni: String? = null,
    val reportadoPorRol: String? = null,
    val necesidades: String? = null, // Ej: "Agua, Alimentación"
    val observacionesCampo: String? = null,
    val fechaSuceso: Long? = null,
    val distrito: String? = null,
    val direccion: String? = null,
    val referencia: String? = null,
    val parroquiaNombre: String? = null,
    val nivelAfectacion: String? = null,  // Leve / Moderado / Severo
    val necesidadesObs: String? = null    // Observaciones sobre necesidades
)
