package pucp.edu.caritas_movile_grd.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pucp.edu.caritas_movile_grd.Network.MobileApiConfig
import java.net.HttpURLConnection
import java.net.URL

sealed class LoginEstado {
    object Idle : LoginEstado()
    object Cargando : LoginEstado()
    data class Error(val mensaje: String) : LoginEstado()
}

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {

    val perfil: StateFlow<PerfilUsuarioLocal?> = repository.perfilUsuario
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _estado = MutableStateFlow<LoginEstado>(LoginEstado.Idle)
    val estado: StateFlow<LoginEstado> = _estado.asStateFlow()

    fun loginConApi(email: String, password: String, onExito: () -> Unit) {
        _estado.value = LoginEstado.Cargando
        viewModelScope.launch {
            try {
                val resultado = llamarLoginApi(email, password)
                val perfil = PerfilUsuarioLocal(
                    uuidUsuario = resultado.getString("idUsuarioGRD"),
                    idUsuarioRemoto = resultado.getString("idUsuarioGRD"),
                    nombres = "${resultado.getString("nombres")} ${resultado.optString("apellidos")}".trim(),
                    rol = resultado.optString("rol", "BRIGADISTA"),
                    jwtCacheado = resultado.optString("accessToken", ""),
                    pinAccesoOffline = ""
                )
                repository.loginExitoso(perfil)
                _estado.value = LoginEstado.Idle
                withContext(Dispatchers.Main) { onExito() }
            } catch (e: LoginApiException) {
                _estado.value = LoginEstado.Error(e.message ?: "Error al iniciar sesión")
            } catch (e: Exception) {
                _estado.value = LoginEstado.Error("Sin conexión al servidor. Verifica tu red.")
            }
        }
    }

    private suspend fun llamarLoginApi(email: String, password: String): JSONObject {
        return withContext(Dispatchers.IO) {
            val url = URL("${MobileApiConfig.BASE_URL}/api/mobile/auth/login")
            val payload = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-mobile-sync-key", MobileApiConfig.MOBILE_SYNC_API_KEY)
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = conn.responseCode
            val body = if (code < 400) conn.inputStream.bufferedReader().readText()
                       else conn.errorStream?.bufferedReader()?.readText() ?: "{}"
            conn.disconnect()
            val json = JSONObject(body)
            if (!json.optBoolean("ok", false)) {
                throw LoginApiException(json.optString("message", "Credenciales incorrectas"))
            }
            json
        }
    }

    fun sesionVigente(): Boolean {
        val p = perfil.value ?: return false
        val unaHora24 = 24L * 60 * 60 * 1000
        return (System.currentTimeMillis() - p.fechaLogin) < unaHora24
    }

    fun login(perfil: PerfilUsuarioLocal) {
        viewModelScope.launch { repository.loginExitoso(perfil) }
    }

    fun logout() {
        viewModelScope.launch { repository.cerrarSesion() }
    }
}

class LoginApiException(message: String) : Exception(message)
