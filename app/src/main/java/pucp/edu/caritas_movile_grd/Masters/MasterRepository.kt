package pucp.edu.caritas_movile_grd.Masters

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import pucp.edu.caritas_movile_grd.Network.MobileSyncApi
import kotlin.math.abs

class MasterRepository(
    private val masterDao: MasterDao,
    private val mobileSyncApi: MobileSyncApi = MobileSyncApi()
) {

    val allParroquias: Flow<List<ParroquiaLocal>> = masterDao.getAllParroquias()
    val allCatalogos: Flow<List<CatalogoLocal>> = masterDao.getAllCatalogos()

    fun getCatalogosPorCategoria(categoria: String) = masterDao.getCatalogosByCategoria(categoria)

    suspend fun refrescarParroquias(lista: List<ParroquiaLocal>) {
        masterDao.insertParroquias(lista)
    }

    suspend fun refrescarCatalogos(lista: List<CatalogoLocal>) {
        masterDao.insertCatalogos(lista)
    }

    suspend fun refrescarCatalogosDesdeBackend() {
        val response = mobileSyncApi.obtenerCatalogos()
        val catalogos = response.optJSONArray("catalogos") ?: return

        val locales = mutableListOf<CatalogoLocal>()

        for (i in 0 until catalogos.length()) {
            val catalogo = catalogos.optJSONObject(i) ?: continue
            val nombreCatalogo = catalogo.optString("nombreCatalogo").trim()
            val detalles = catalogo.optJSONArray("detalles") ?: continue

            for (j in 0 until detalles.length()) {
                val detalle = detalles.optJSONObject(j) ?: continue
                val idRemoto = detalle.optString("idCatalogoDetalleGRD").trim()
                val valor = detalle.optString("valor").trim()

                if (nombreCatalogo.isBlank() || valor.isBlank()) continue

                locales.add(
                    CatalogoLocal(
                        idCatalogo = stableIntId(idRemoto.ifBlank { "$nombreCatalogo-$valor" }),
                        categoria = nombreCatalogo,
                        valor = valor
                    )
                )
            }
        }

        masterDao.reemplazarCatalogos(locales)
    }

    private fun stableIntId(value: String): Int {
        val hash = value.hashCode()
        return if (hash == Int.MIN_VALUE) 0 else abs(hash)
    }
}