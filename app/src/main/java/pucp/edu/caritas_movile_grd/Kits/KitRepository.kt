package pucp.edu.caritas_movile_grd.Kits

import kotlinx.coroutines.flow.Flow

class KitRepository(private val kitDao: KitDao) {
    val allEntregas: Flow<List<EntregaKitLocal>> = kitDao.getAllEntregas()

    fun getEntregasPorAfectado(uuidAfectado: String): Flow<List<EntregaKitLocal>> = 
        kitDao.getEntregasByAfectado(uuidAfectado)

    suspend fun insertarEntrega(entrega: EntregaKitLocal) {
        kitDao.insertEntrega(entrega)
    }

    suspend fun actualizarEntrega(entrega: EntregaKitLocal) {
        kitDao.updateEntrega(entrega)
    }

    suspend fun eliminarEntrega(entrega: EntregaKitLocal) {
        kitDao.deleteEntrega(entrega)
    }
}
