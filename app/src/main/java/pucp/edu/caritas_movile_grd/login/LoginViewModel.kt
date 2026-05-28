package pucp.edu.caritas_movile_grd.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {

    val perfil: StateFlow<PerfilUsuarioLocal?> = repository.perfilUsuario
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun login(perfil: PerfilUsuarioLocal) {
        viewModelScope.launch {
            repository.loginExitoso(perfil)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.cerrarSesion()
        }
    }
}
