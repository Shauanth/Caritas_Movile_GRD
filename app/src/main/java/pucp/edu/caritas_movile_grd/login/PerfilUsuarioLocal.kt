package pucp.edu.caritas_movile_grd.login

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "perfil_usuario_local")
data class PerfilUsuarioLocal(
    @PrimaryKey val uuidUsuario: String,
    val idUsuarioRemoto: Int,
    val rol: String, // "BRIGADISTA", "ESPECIALISTA"
    val pinAccesoOffline: String, // Hash del PIN para entrar sin internet
    val jwtCacheado: String,
    val nombres: String
)
