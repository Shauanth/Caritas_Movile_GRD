package pucp.edu.caritas_movile_grd.Kits

import kotlinx.coroutines.flow.Flow
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync

class KitRepository(private val kitDao: KitDao) {
    val allEntregas: Flow<List<EntregaKitLocal>> = kitDao.getAllEntregas()

    fun getEntregasPorAfectado(uuidAfectado: String): Flow<List<EntregaKitLocal>> =
        kitDao.getEntregasByAfectado(uuidAfectado)

    fun getEntregasPorIncidencia(uuidIncidencia: String): Flow<List<EntregaKitLocal>> =
        kitDao.getEntregasByIncidencia(uuidIncidencia)

    fun getKitsAsignadosPorIncidencia(uuidIncidencia: String): Flow<List<KitAsignadoLocal>> =
        kitDao.getKitsAsignadosPorIncidencia(uuidIncidencia)

    fun getArticulosPorKit(uuidKitAsignado: String): Flow<List<KitArticuloAsignadoLocal>> =
        kitDao.getArticulosPorKit(uuidKitAsignado)

    suspend fun insertarEntrega(entrega: EntregaKitLocal) {
        kitDao.insertEntrega(entrega)
    }

    suspend fun actualizarEntrega(entrega: EntregaKitLocal) {
        kitDao.updateEntrega(entrega)
    }

    suspend fun eliminarEntrega(entrega: EntregaKitLocal) {
        kitDao.deleteEntrega(entrega)
    }

    suspend fun guardarKitsAsignados(
        kits: List<KitAsignadoLocal>,
        articulos: List<KitArticuloAsignadoLocal>
    ) {
        if (kits.isNotEmpty()) kitDao.insertKitsAsignados(kits)
        if (articulos.isNotEmpty()) kitDao.insertArticulosAsignados(articulos)
    }

    suspend fun actualizarConfirmacionArticulo(
        articulo: KitArticuloAsignadoLocal,
        confirmado: Boolean
    ) {
        kitDao.actualizarConfirmacionArticulo(
            uuidArticuloAsignado = articulo.uuidArticuloAsignado,
            confirmado = confirmado,
            cantidadEntregada = if (confirmado) articulo.cantidadAsignada else 0
        )
    }

    suspend fun marcarKitEntregado(
        kit: KitAsignadoLocal,
        descripcionEntrega: String?,
        evidenciaLocalUri: String?,
        estadoEntrega: String
    ) {
        kitDao.marcarKitEntregado(
            uuidKitAsignado = kit.uuidKitAsignado,
            estadoEntrega = estadoEntrega,
            fechaEntrega = System.currentTimeMillis(),
            descripcionEntrega = descripcionEntrega,
            evidenciaLocalUri = evidenciaLocalUri,
            estadoSync = EstadoSync.NUEVO
        )
    }

    suspend fun confirmarKitCompleto(
        kit: KitAsignadoLocal,
        descripcionEntrega: String?,
        evidenciaLocalUri: String? = null
    ) {
        kitDao.marcarTodosArticulosDeKitEntregados(
            uuidKitAsignado = kit.uuidKitAsignado
        )

        kitDao.marcarKitEntregado(
            uuidKitAsignado = kit.uuidKitAsignado,
            estadoEntrega = "ENTREGADO",
            fechaEntrega = System.currentTimeMillis(),
            descripcionEntrega = descripcionEntrega,
            evidenciaLocalUri = evidenciaLocalUri,
            estadoSync = EstadoSync.NUEVO
        )
    }
}
