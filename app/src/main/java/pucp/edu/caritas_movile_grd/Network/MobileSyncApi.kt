package pucp.edu.caritas_movile_grd.Network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MobileSyncException(
    message: String,
    val statusCode: Int
) : Exception(message)

class MobileSyncApi(
    private val baseUrl: String = MobileApiConfig.BASE_URL
) {
    suspend fun sincronizarIncidencia(payload: JSONObject): JSONObject {
        return postJson("/api/mobile/sync/incidencias", payload)
    }

    suspend fun sincronizarAfectado(payload: JSONObject): JSONObject {
        return postJson("/api/mobile/sync/afectados", payload)
    }
    suspend fun sincronizarEvidencia(payload: JSONObject): JSONObject {
    return postJson("/api/mobile/sync/evidencias", payload)
    }
    suspend fun sincronizarObservacion(payload: JSONObject): JSONObject {
        return postJson("/api/mobile/sync/observaciones", payload)
    }
    suspend fun sincronizarSeguimiento(payload: JSONObject): JSONObject {
        return postJson("/api/mobile/sync/seguimientos", payload)
    } 
    suspend fun sincronizarEntrega(payload: JSONObject): JSONObject {
        return postJson("/api/mobile/sync/entregas", payload)
    }   
    private suspend fun postJson(path: String, payload: JSONObject): JSONObject {
        return withContext(Dispatchers.IO) {
            val url = URL(baseUrl.trimEnd('/') + path)

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            try {
                connection.outputStream.use { output ->
                    output.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val statusCode = connection.responseCode

                val responseBody = if (statusCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }

                val responseJson = if (responseBody.isBlank()) {
                    JSONObject()
                } else {
                    JSONObject(responseBody)
                }

                if (statusCode !in 200..299) {
                    val message = responseJson.optString(
                        "message",
                        "Error HTTP $statusCode al sincronizar."
                    )

                    throw MobileSyncException(message, statusCode)
                }

                responseJson
            } finally {
                connection.disconnect()
            }
        }
    }
}