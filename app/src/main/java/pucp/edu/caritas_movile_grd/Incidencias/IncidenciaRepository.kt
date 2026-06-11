package pucp.edu.caritas_movile_grd.Incidencias
import pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal
import pucp.edu.caritas_movile_grd.Seguimientos.SeguimientoLocal
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
}
