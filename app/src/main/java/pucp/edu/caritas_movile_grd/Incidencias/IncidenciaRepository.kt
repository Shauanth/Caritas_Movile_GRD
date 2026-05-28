package pucp.edu.caritas_movile_grd.Incidencias

import kotlinx.coroutines.flow.Flow

class IncidenciaRepository(private val incidenciaDao: IncidenciaDao) {

    val allIncidencias: Flow<List<IncidenciaLocal>> = incidenciaDao.getAllIncidencias()

    fun getAfectados(uuidIncidencia: String): Flow<List<AfectadoLocal>> =
        incidenciaDao.getAfectadosByIncidencia(uuidIncidencia)

    suspend fun guardarIncidencia(incidencia: IncidenciaLocal) {
        incidenciaDao.insertIncidencia(incidencia)
    }

    suspend fun guardarAfectado(afectado: AfectadoLocal) {
        incidenciaDao.insertAfectado(afectado)
    }
}
