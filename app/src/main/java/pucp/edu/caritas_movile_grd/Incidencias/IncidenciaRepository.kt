package pucp.edu.caritas_movile_grd.Incidencias

import kotlinx.coroutines.flow.Flow
import pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal
import org.json.JSONObject
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import pucp.edu.caritas_movile_grd.Network.MobileApiConfig
import pucp.edu.caritas_movile_grd.Network.MobileSyncApi
import pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal
import pucp.edu.caritas_movile_grd.Seguimientos.SeguimientoLocal
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import android.util.Log
private const val TAG_ASIGNADAS = "IncidenciasAsignadas"
class IncidenciaRepository(
    private val incidenciaDao: IncidenciaDao,
    private val mobileSyncApi: MobileSyncApi = MobileSyncApi()
) {
    val allIncidencias: Flow<List<IncidenciaLocal>> = incidenciaDao.getAllIncidencias()

    fun getAfectados(uuidIncidencia: String): Flow<List<AfectadoLocal>> =
        incidenciaDao.getAfectadosByIncidencia(uuidIncidencia)

    suspend fun guardarIncidencia(incidencia: IncidenciaLocal) {
        incidenciaDao.insertIncidencia(incidencia)
    }

    suspend fun guardarAfectado(afectado: AfectadoLocal) {
        incidenciaDao.insertAfectado(afectado)
    }
    suspend fun guardarObservacion(observacion: ObservacionLocal) {
        incidenciaDao.insertObservacion(observacion)
    }   
    suspend fun guardarSeguimiento(seguimiento: SeguimientoLocal) {
        incidenciaDao.insertSeguimiento(seguimiento)
    }     

    fun getObservaciones(uuidIncidencia: String): Flow<List<ObservacionLocal>> =
        incidenciaDao.getObservacionesByIncidencia(uuidIncidencia)

    fun getSeguimientos(uuidIncidencia: String): Flow<List<SeguimientoLocal>> =
        incidenciaDao.getSeguimientosByIncidencia(uuidIncidencia)

    fun getEvidencias(uuidIncidencia: String): Flow<List<EvidenciaLocal>> =
        incidenciaDao.getEvidenciasByIncidencia(uuidIncidencia)

    suspend fun guardarEvidencia(evidencia: EvidenciaLocal) {
        incidenciaDao.insertEvidencia(evidencia)
    }   

    suspend fun refrescarIncidenciasAsignadas(
        idUsuarioGRD: String = MobileApiConfig.MOBILE_SYNC_USER_ID
    ) {
        try {
            Log.d(TAG_ASIGNADAS, "Iniciando descarga para idUsuarioGRD=$idUsuarioGRD")

            val response = mobileSyncApi.obtenerIncidenciasAsignadas(idUsuarioGRD)

            Log.d(TAG_ASIGNADAS, "Respuesta completa=$response")
            Log.d(TAG_ASIGNADAS, "Total backend=${response.optInt("total", -1)}")

            val items = response.optJSONArray("incidencias")

            if (items == null) {
                Log.d(TAG_ASIGNADAS, "No vino array incidencias")
                return
            }

            Log.d(TAG_ASIGNADAS, "Cantidad en array incidencias=${items.length()}")

            val incidencias = mutableListOf<IncidenciaLocal>()

            for (i in 0 until items.length()) {
                val wrapper = items.optJSONObject(i)

                if (wrapper == null) {
                    Log.d(TAG_ASIGNADAS, "Item $i no es JSONObject")
                    continue
                }

                val incidenciaJson = wrapper.optJSONObject("incidencia")

                if (incidenciaJson == null) {
                    Log.d(TAG_ASIGNADAS, "Item $i no tiene objeto incidencia. Keys=$wrapper")
                    continue
                }

                val asignacionJson = wrapper.optJSONObject("asignacion")
                val incidenciaLocal = incidenciaJson.toIncidenciaLocalAsignada(
                    idUsuarioGRD,
                    uuidMovilAsignacion = asignacionJson?.optString("uuidMovil")
                        ?.takeIf { it.isNotBlank() && it != "null" }
                )

                Log.d(
                    TAG_ASIGNADAS,
                    "Mapeada incidencia: uuid=${incidenciaLocal.uuidIncidencia}, codigo=${incidenciaLocal.codigoCasoRemoto}, titulo=${incidenciaLocal.nombre}"
                )

                incidencias.add(incidenciaLocal)
            }

            Log.d(TAG_ASIGNADAS, "Total mapeadas=${incidencias.size}")

            if (incidencias.isNotEmpty()) {
                incidenciaDao.upsertIncidencias(incidencias)
                Log.d(TAG_ASIGNADAS, "Insertadas en Room=${incidencias.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG_ASIGNADAS, "Error refrescando incidencias asignadas", e)
        }
    }     

}
private fun JSONObject.toIncidenciaLocalAsignada(
    idUsuarioGRD: String,
    uuidMovilAsignacion: String? = null
): IncidenciaLocal {
    val idRemoto = optStringOrNull("idIncidencia")
    val codigoCaso = optStringOrNull("codigoCaso")
    val tipoEvento = optStringOrNull("tipoEvento") ?: "Evento asignado"
    val distrito = optStringOrNull("distritoEvento")

    val titulo = if (!distrito.isNullOrBlank()) {
        "$tipoEvento - $distrito"
    } else {
        optStringOrNull("tituloIncidencia") ?: tipoEvento
    }

    return IncidenciaLocal(
        uuidIncidencia = uuidMovilAsignacion
            ?: optStringOrNull("uuidMovil")
            ?: "remote-${idRemoto ?: codigoCaso ?: System.currentTimeMillis()}",
        idIncidenciaRemota = idRemoto,
        uuidUsuario = idUsuarioGRD,
        idParroquia = 1,
        idCatalogoTipo = stableIntId(tipoEvento),
        tipoEventoNombre = tipoEvento,
        descripcion = optStringOrNull("descripcionEvento")
            ?: optStringOrNull("relatoActual")
            ?: optStringOrNull("contextoCaso")
            ?: "Incidencia asignada desde backend",
        nombre = titulo,
        numAfectados = optInt("numAfectadosReportado", 0),
        responsable = "Brigadista",
        estado = optStringOrNull("estadoActual") ?: "ASIGNADO",
        estadoSync = EstadoSync.SINCRONIZADO,
        fechaUltimaModificacion = parseFechaIsoMillis(optStringOrNull("fechaRegistro"))
            ?: System.currentTimeMillis(),
        causa = optStringOrNull("causaEvento"),
        reportadoPorNombre = optStringOrNull("reportadoPorNombre"),
        reportadoPorCelular = optStringOrNull("reportadoPorCelular"),
        reportadoPorDni = optStringOrNull("reportadoPorDni"),
        reportadoPorRol = optStringOrNull("reportadoPorRol"),
        necesidades = optStringOrNull("necesidades"),
        observacionesCampo = optStringOrNull("observacionesGenerales"),
        fechaSuceso = parseFechaIsoMillis(optStringOrNull("fechaSuceso")),
        distrito = optStringOrNull("distritoEvento"),
        direccion = optStringOrNull("direccionEvento"),
        referencia = optStringOrNull("referenciaEvento"),
        parroquiaNombre = optStringOrNull("parroquiaNombreSnapshot")
            ?: optJSONObject("parroquia")?.optStringOrNull("nombre"),
        nivelAfectacion = optStringOrNull("gravedad"),
        necesidadesObs = optStringOrNull("necesidadesObs"),
        latitud = optNullableDouble("latitud"),
        longitud = optNullableDouble("longitud"),
        codigoCasoRemoto = codigoCaso
    )
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null

    return optString(name)
        .trim()
        .takeIf { it.isNotBlank() && it != "null" }
}

private fun JSONObject.optNullableDouble(name: String): Double? {
    if (!has(name) || isNull(name)) return null

    val value = optDouble(name, Double.NaN)

    return if (value.isNaN()) null else value
}

private fun parseFechaIsoMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null

    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        format.parse(value)?.time
    } catch (_: Exception) {
        null
    }
}

private fun stableIntId(value: String): Int {
    val hash = value.hashCode()
    return if (hash == Int.MIN_VALUE) 0 else abs(hash)
}