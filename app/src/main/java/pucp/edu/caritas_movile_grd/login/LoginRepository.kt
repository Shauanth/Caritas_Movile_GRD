package pucp.edu.caritas_movile_grd.login

import kotlinx.coroutines.flow.Flow

class LoginRepository(private val loginDao: LoginDao) {

    val perfilUsuario: Flow<PerfilUsuarioLocal?> = loginDao.getPerfilUsuario()

    suspend fun loginExitoso(perfil: PerfilUsuarioLocal) {
        loginDao.logout()
        loginDao.insertPerfil(perfil)
    }

    suspend fun cerrarSesion() {
        loginDao.logout()
    }

    fun getAsignaciones(uuidUsuario: String) = loginDao.getAsignaciones(uuidUsuario)
}
