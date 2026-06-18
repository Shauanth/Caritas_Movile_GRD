package pucp.edu.caritas_movile_grd.Simulacros

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import pucp.edu.caritas_movile_grd.Network.MobileApiConfig
import pucp.edu.caritas_movile_grd.Network.MobileSyncApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class SimulacroRepository(
    private val dao: SimulacroDao,
    private val mobileSyncApi: MobileSyncApi = MobileSyncApi()
) {
    val allSimulacros: Flow<List<SimulacroLocal>> = dao.getAllSimulacros()

    fun getByEstado(estado: String) = dao.getSimulacrosByEstado(estado)

    suspend fun guardarSimulacro(s: SimulacroLocal) = dao.insertSimulacro(s)
    suspend fun actualizarSimulacro(s: SimulacroLocal) = dao.updateSimulacro(s)

    suspend fun descargarSimulacrosDesdeBackend(): Int {
        val response = mobileSyncApi.obtenerSimulacros()
        val items = response.optJSONArray("simulacros") ?: return 0
        val simulacros = mutableListOf<SimulacroLocal>()

        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            simulacros.add(item.toSimulacroLocal())
        }

        if (simulacros.isNotEmpty()) {
            dao.upsertSimulacrosDescargados(simulacros)
        }

        return simulacros.size
    }

    suspend fun marcarSimulacroEjecutadoLocalmente(
        uuidSimulacro: String,
        resultadoGeneral: String,
        reporteBrigadista: String,
        numeroParticipantesReal: Int?,
        duracionSimulacro: Int?,
        recomendaciones: String?,
        observaciones: String?
    ) {
        dao.marcarEjecucionLocal(
            uuidSimulacro = uuidSimulacro,
            resultadoGeneral = resultadoGeneral,
            reporteBrigadista = reporteBrigadista,
            numeroParticipantesReal = numeroParticipantesReal,
            duracionSimulacro = duracionSimulacro,
            recomendaciones = recomendaciones,
            observaciones = observaciones,
            fechaEjecucion = fechaIsoUtc()
        )
    }

    suspend fun sincronizarSimulacrosPendientes(): Int {
        var sincronizados = 0
        val pendientes = dao.getSimulacrosPendientesSincronizar()

        for (simulacro in pendientes) {
            val idRemoto = simulacro.idActividadPreventivaRemota
            if (idRemoto.isNullOrBlank()) continue

            val response = mobileSyncApi.sincronizarSimulacro(simulacro.toSyncPayload())

            if (response.optBoolean("ok", false)) {
                dao.marcarSincronizado(
                    uuidSimulacro = simulacro.uuidSimulacro,
                    estadoActividad = response.optString("estadoActividad", "EJECUTADA"),
                    fechaEjecucion = simulacro.fechaEjecucion,
                    fechaSincronizacion = response.optStringOrNull("fechaSincronizacion")
                )
                sincronizados++
            }
        }

        return sincronizados
    }
}

private fun JSONObject.toSimulacroLocal(): SimulacroLocal {
    val idRemoto = optStringOrNull("idActividadPreventiva")
    val uuidMovil = optStringOrNull("uuidMovil")
    val uuidLocal = uuidMovil ?: idRemoto?.let { "remote-$it" } ?: UUID.randomUUID().toString()
    val responsable = optJSONObject("responsable")

    return SimulacroLocal(
        uuidSimulacro = uuidLocal,
        idActividadPreventivaRemota = idRemoto,
        codigoActividad = optStringOrNull("codigoActividad"),
        estadoActividad = optStringOrNull("estadoActividad") ?: "PROGRAMADA",
        idParroquia = optStringOrNull("idParroquia"),
        parroquiaNombre = optStringOrNull("parroquiaNombre"),
        tipoActividadPreventiva = optStringOrNull("idTipoActividadPreventiva"),
        nombreActividad = optStringOrNull("nombreActividad") ?: "Simulacro",
        fechaProgramada = optStringOrNull("fechaProgramada"),
        horarioInicio = optStringOrNull("horarioInicio"),
        horarioFin = optStringOrNull("horarioFin"),
        lugarActividad = optStringOrNull("lugarActividad"),
        publicoObjetivo = optStringOrNull("publicoObjetivo"),
        numeroParticipantesEstimado = optIntOrNull("numeroParticipantesEstimado"),
        numeroParticipantesReal = optIntOrNull("numeroParticipantesReal"),
        descripcionActividad = optStringOrNull("descripcionActividad"),
        resultadoGeneral = optStringOrNull("resultadoGeneral"),
        recomendaciones = optStringOrNull("recomendaciones"),
        observaciones = optStringOrNull("observaciones"),
        indicacionesEquipo = optStringOrNull("indicacionesEquipo"),
        reporteBrigadista = optStringOrNull("reporteBrigadista"),
        duracionSimulacro = optIntOrNull("duracionSimulacro"),
        fechaEjecucion = optStringOrNull("fechaEjecucion"),
        updatedAtRemoto = optStringOrNull("updatedAt"),
        idBrigadistaParroquialResponsable = responsable?.optStringOrNull("idBrigadistaParroquial"),
        idUsuarioGRDResponsable = responsable?.optStringOrNull("idUsuarioGRD"),
        nombreResponsable = responsable?.optStringOrNull("nombre"),
        estadoSync = EstadoSync.SINCRONIZADO
    )
}

private fun SimulacroLocal.toSyncPayload(): JSONObject {
    return JSONObject().apply {
        put("uuidSync", "simulacro-${uuidSimulacro}-${System.currentTimeMillis()}")
        putNullable("idActividadPreventivaRemota", idActividadPreventivaRemota)
        put("idUsuarioGRD", idUsuarioGRDResponsable ?: MobileApiConfig.MOBILE_SYNC_USER_ID)
        putNullable("idBrigadistaParroquial", idBrigadistaParroquialResponsable)
        put("estadoActividad", "EJECUTADA")
        putNullable("fechaEjecucion", fechaEjecucion)
        putNullable("resultadoGeneral", resultadoGeneral)
        putNullable("reporteBrigadista", reporteBrigadista)
        putNullable("numeroParticipantesReal", numeroParticipantesReal)
        putNullable("duracionSimulacro", duracionSimulacro)
        putNullable("recomendaciones", recomendaciones)
        putNullable("observaciones", observaciones)
    }
}

private fun fechaIsoUtc(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date())
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() && it != "null" }
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return try {
        getInt(key)
    } catch (_: Exception) {
        optString(key).toIntOrNull()
    }
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
}
