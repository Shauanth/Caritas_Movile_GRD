package pucp.edu.caritas_movile_grd.Incidencias

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import pucp.edu.caritas_movile_grd.Network.MobileApiConfig
import pucp.edu.caritas_movile_grd.Network.MobileSyncApi
import pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal
import pucp.edu.caritas_movile_grd.Seguimientos.SeguimientoLocal
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar
import java.util.Date
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
            val afectados = mutableListOf<AfectadoLocal>()
            val observaciones = mutableListOf<ObservacionLocal>()

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

                val incidenciaLocal = incidenciaJson.toIncidenciaLocalAsignada(idUsuarioGRD)

                val afectadosLocales = incidenciaJson.toAfectadosLocalesAsignados(
                    uuidIncidenciaLocal = incidenciaLocal.uuidIncidencia
                )

                Log.d(
                    TAG_ASIGNADAS,
                    "Mapeados afectados para ${incidenciaLocal.codigoCasoRemoto}: ${afectadosLocales.size}"
                )

                afectados.addAll(afectadosLocales)
                val observacionesLocales = incidenciaJson.toObservacionesLocalesAsignadas(
                    uuidIncidenciaLocal = incidenciaLocal.uuidIncidencia
                )

                Log.d(
                    TAG_ASIGNADAS,
                    "Mapeadas observaciones para ${incidenciaLocal.codigoCasoRemoto}: ${observacionesLocales.size}"
                )

                observaciones.addAll(observacionesLocales)
                Log.d(
                    TAG_ASIGNADAS,
                    "Mapeada incidencia: uuid=${incidenciaLocal.uuidIncidencia}, codigo=${incidenciaLocal.codigoCasoRemoto}, titulo=${incidenciaLocal.nombre}"
                )

                incidencias.add(incidenciaLocal)
            }

            Log.d(TAG_ASIGNADAS, "Total mapeadas=${incidencias.size}")

            if (incidencias.isNotEmpty()) {
                incidenciaDao.upsertIncidencias(incidencias)
                Log.d(TAG_ASIGNADAS, "Incidencias insertadas en Room=${incidencias.size}")
            }

            if (afectados.isNotEmpty()) {
                incidenciaDao.insertAfectados(afectados)
                Log.d(TAG_ASIGNADAS, "Afectados insertados en Room=${afectados.size}")
            }
            if (observaciones.isNotEmpty()) {
                incidenciaDao.insertObservaciones(observaciones)
                Log.d(TAG_ASIGNADAS, "Observaciones insertadas en Room=${observaciones.size}")
            }

        } catch (e: Exception) {
            Log.e(TAG_ASIGNADAS, "Error refrescando incidencias asignadas", e)
        }
    }

    suspend fun finalizarRecopilacion(
        incidencia: IncidenciaLocal,
        observacionCierre: String = "Recopilación finalizada desde móvil"
    ): Boolean {
        val payload = JSONObject().apply {
            put("uuidCierre", "cierre-${incidencia.uuidIncidencia}")
            put("uuidIncidencia", incidencia.uuidIncidencia)
            putNullable("idIncidenciaRemota", incidencia.idIncidenciaRemota)
            putNullable("codigoCaso", incidencia.codigoCasoRemoto)
            put("idUsuarioGRD", MobileApiConfig.MOBILE_SYNC_USER_ID)
            put("observacionCierre", observacionCierre)
        }

        val response = mobileSyncApi.finalizarRecopilacion(payload)

        if (!response.optBoolean("ok", false)) {
            return false
        }

        incidenciaDao.updateIncidencia(
            incidencia.copy(
                estado = response.optString("estadoActual", "DATA RECOPILADA"),
                estadoSync = EstadoSync.SINCRONIZADO,
                fechaUltimaModificacion = System.currentTimeMillis()
            )
        )

        return true
    }    


}
private fun JSONObject.toIncidenciaLocalAsignada(idUsuarioGRD: String): IncidenciaLocal {
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
        uuidIncidencia = optStringOrNull("uuidMovil")
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

private fun JSONObject.toAfectadosLocalesAsignados(
    uuidIncidenciaLocal: String
): List<AfectadoLocal> {
    val grupos = optJSONArray("gruposFamiliares") ?: return emptyList()
    val afectados = mutableListOf<AfectadoLocal>()

    for (i in 0 until grupos.length()) {
        val grupo = grupos.optJSONObject(i) ?: continue

        val familiaId = grupo.optStringOrNull("uuidMovil")
            ?: grupo.optStringOrNull("idGrupoFamiliar")
            ?: grupo.optStringOrNull("codigoGrupo")

        val familiaNombre = grupo.optStringOrNull("nombreReferencia")
            ?: "Grupo Familiar ${i + 1}"

        val personas = grupo.optJSONArray("personas") ?: continue

        for (j in 0 until personas.length()) {
            val persona = personas.optJSONObject(j) ?: continue

            val idPersonaRemota = persona.optStringOrNull("idPersonaAfectada")
            val uuidPersona = persona.optStringOrNull("uuidMovil")
                ?: "remote-${idPersonaRemota ?: "${uuidIncidenciaLocal}-${persona.optStringOrNull("numeroDocumento") ?: "$i-$j"}"}"

            val apellidos = separarApellidos(persona.optStringOrNull("apellidos"))

            afectados.add(
                AfectadoLocal(
                    uuidAfectado = uuidPersona,
                    uuidIncidencia = uuidIncidenciaLocal,
                    idCatalogoDoc = persona.optStringOrNull("tipoDocumento")?.toIntOrNull() ?: 1,
                    idAfectadoRemoto = idPersonaRemota,
                    documentoIdentidad = persona.optStringOrNull("numeroDocumento").orEmpty(),
                    nombres = persona.optStringOrNull("nombres").orEmpty(),
                    apellidoPaterno = apellidos.first,
                    apellidoMaterno = apellidos.second,
                    edad = calcularEdadDesdeFechaNacimiento(
                        persona.optStringOrNull("fechaNacimiento")
                    ),
                    genero = persona.optStringOrNull("sexo"),
                    celular = persona.optStringOrNull("telefono"),
                    parentesco = persona.optStringOrNull("parentesco"),
                    situacionActual = persona.optStringOrNull("condicionEspecial")
                        ?: persona.optStringOrNull("condicionSalud"),
                    familiaId = familiaId,
                    familiaNombre = familiaNombre,
                    estadoSync = EstadoSync.SINCRONIZADO
                )
            )
        }
    }

    return afectados
}

private fun JSONObject.toObservacionesLocalesAsignadas(
    uuidIncidenciaLocal: String
): List<ObservacionLocal> {
    val items = optJSONArray("observaciones") ?: return emptyList()
    val observaciones = mutableListOf<ObservacionLocal>()

    for (i in 0 until items.length()) {
        val item = items.optJSONObject(i) ?: continue

        val idRemoto = item.optStringOrNull("idObservacionGRD")
            ?: item.optStringOrNull("idObservacionRemota")

        val uuidObservacion = item.optStringOrNull("uuidMovil")
            ?: item.optStringOrNull("uuidObservacion")
            ?: "remote-${idRemoto ?: "$uuidIncidenciaLocal-$i"}"

        val texto = item.optStringOrNull("textoObservacion")
            ?: item.optStringOrNull("observacion")
            ?: item.optStringOrNull("descripcion")
            ?: item.optStringOrNull("comentario")
            ?: continue

        val fechaRegistro = item.optStringOrNull("fechaRegistro")
            ?: item.optStringOrNull("fechaSincronizacion")
            ?: nowIsoUtc()

        observaciones.add(
            ObservacionLocal(
                uuidObservacion = uuidObservacion,
                uuidIncidencia = uuidIncidenciaLocal,
                textoObservacion = texto,
                fechaRegistro = fechaRegistro,
                estadoSync = EstadoSync.SINCRONIZADO,
                idObservacionRemota = idRemoto
            )
        )
    }

    return observaciones
}

private fun nowIsoUtc(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date())
}



private fun separarApellidos(apellidos: String?): Pair<String?, String?> {
    val partes = apellidos
        .orEmpty()
        .trim()
        .split("\\s+".toRegex(), limit = 2)
        .filter { it.isNotBlank() }

    return when (partes.size) {
        0 -> null to null
        1 -> partes[0] to null
        else -> partes[0] to partes[1]
    }
}

private fun calcularEdadDesdeFechaNacimiento(fechaNacimiento: String?): Int? {
    val millis = parseFechaIsoMillis(fechaNacimiento) ?: return null

    val nacimiento = Calendar.getInstance().apply {
        timeInMillis = millis
    }

    val hoy = Calendar.getInstance()

    var edad = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)

    if (hoy.get(Calendar.DAY_OF_YEAR) < nacimiento.get(Calendar.DAY_OF_YEAR)) {
        edad--
    }

    return edad.takeIf { it >= 0 }
}

private fun JSONObject.putNullable(name: String, value: String?) {
    if (value.isNullOrBlank()) {
        put(name, JSONObject.NULL)
    } else {
        put(name, value)
    }
}