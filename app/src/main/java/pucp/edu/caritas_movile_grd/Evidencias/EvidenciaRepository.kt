package pucp.edu.caritas_movile_grd.Evidencias

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

class EvidenciaRepository(private val evidenciaDao: EvidenciaDao) {

    fun getEvidencias(uuidReferencia: String): Flow<List<EvidenciaLocal>> =
        evidenciaDao.getEvidenciasByReferencia(uuidReferencia)
            .map { lista -> lista.filter { it.estadoSync != EstadoSync.PENDIENTE_ELIMINACION } }

    suspend fun guardarEvidencia(evidencia: EvidenciaLocal) {
        evidenciaDao.insertEvidencia(evidencia)
    }

    suspend fun eliminarEvidencia(evidencia: EvidenciaLocal) {
        if (evidencia.estadoSync == EstadoSync.PENDIENTE_SUBIDA || evidencia.estadoSync == EstadoSync.NUEVO) {
            // Nunca se sincronizó al servidor — borrar directo de Room
            evidenciaDao.deleteEvidencia(evidencia)
        } else {
            // Ya está en el servidor — marcar para sincronizar el borrado
            evidenciaDao.marcarEvidenciaParaEliminar(evidencia.uuidEvidencia)
        }
    }

    suspend fun obtenerPendientesSubida(): List<EvidenciaLocal> =
        evidenciaDao.getEvidenciasPendientes()
}
