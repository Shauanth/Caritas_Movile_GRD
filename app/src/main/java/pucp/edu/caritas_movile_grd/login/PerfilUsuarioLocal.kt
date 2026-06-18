package pucp.edu.caritas_movile_grd.login

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "perfil_usuario_local")
data class PerfilUsuarioLocal(
    @PrimaryKey val uuidUsuario: String,
    val idUsuarioRemoto: String? = null,
    val rol: String,
    val pinAccesoOffline: String,
    val jwtCacheado: String,
    val nombres: String,
    val fechaLogin: Long = System.currentTimeMillis()
)
