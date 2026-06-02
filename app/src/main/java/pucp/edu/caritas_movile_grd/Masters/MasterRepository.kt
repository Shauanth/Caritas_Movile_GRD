package pucp.edu.caritas_movile_grd.Masters

import kotlinx.coroutines.flow.Flow

class MasterRepository(private val masterDao: MasterDao) {

    val allParroquias: Flow<List<ParroquiaLocal>> = masterDao.getAllParroquias()
    val allCatalogos: Flow<List<CatalogoLocal>> = masterDao.getAllCatalogos()

    fun getCatalogosPorCategoria(categoria: String) = masterDao.getCatalogosByCategoria(categoria)

    suspend fun refrescarParroquias(lista: List<ParroquiaLocal>) {
        masterDao.insertParroquias(lista)
    }

    suspend fun refrescarCatalogos(lista: List<CatalogoLocal>) {
        masterDao.insertCatalogos(lista)
    }
}
