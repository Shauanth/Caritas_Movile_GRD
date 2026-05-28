package pucp.edu.caritas_movile_grd.login

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoginDao {
    @Query("SELECT * FROM perfil_usuario_local LIMIT 1")
    fun getPerfilUsuario(): Flow<PerfilUsuarioLocal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerfil(perfil: PerfilUsuarioLocal)

    @Query("DELETE FROM perfil_usuario_local")
    suspend fun logout()

    @Query("SELECT * FROM asignacion_territorio WHERE uuidUsuario = :uuidUsuario")
    fun getAsignaciones(uuidUsuario: String): Flow<List<AsignacionTerritorio>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsignacion(asignacion: AsignacionTerritorio)
}
