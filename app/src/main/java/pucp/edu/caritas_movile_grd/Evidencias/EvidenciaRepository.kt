package pucp.edu.caritas_movile_grd.Evidencias

import kotlinx.coroutines.flow.Flow

class EvidenciaRepository(private val evidenciaDao: EvidenciaDao) {

    fun getEvidencias(uuidReferencia: String): Flow<List<EvidenciaLocal>> =
        evidenciaDao.getEvidenciasByReferencia(uuidReferencia)

    suspend fun guardarEvidencia(evidencia: EvidenciaLocal) {
        evidenciaDao.insertEvidencia(evidencia)
    }

    suspend fun eliminarEvidencia(evidencia: EvidenciaLocal) {
        evidenciaDao.deleteEvidencia(evidencia)
    }

    suspend fun obtenerPendientesSubida(): List<EvidenciaLocal> =
        evidenciaDao.getEvidenciasPendientes()
}
