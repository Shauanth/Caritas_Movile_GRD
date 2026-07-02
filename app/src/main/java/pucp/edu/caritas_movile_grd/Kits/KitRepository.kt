package pucp.edu.caritas_movile_grd.Kits

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import pucp.edu.caritas_movile_grd.LocalBDConector.EstadoSync
import java.util.UUID

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

    suspend fun todosKitsEntregados(uuidIncidencia: String): Boolean {
        val kits = kitDao.getKitsAsignadosPorIncidenciaSync(uuidIncidencia)
        if (kits.isEmpty()) return false

        val articulosPorKit = kitDao.getArticulosAsignadosPorKitsSync(
            kits.map { it.uuidKitAsignado }
        ).groupBy { it.uuidKitAsignado }

        return kits.all { kit ->
            when (kit.estadoEntrega) {
                "ENTREGADO" -> true
                "PARCIAL" -> {
                    val articulos = articulosPorKit[kit.uuidKitAsignado].orEmpty()
                    articulos.isNotEmpty() && articulos.all { it.confirmado && it.cantidadEntregada > 0 }
                }
                else -> false
            }
        }
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
        if (articulo.confirmado) return

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
        if (!kit.kitsEntregaHabilitada) return
        if (kit.estadoSync == EstadoSync.SINCRONIZADO &&
            (kit.estadoEntrega == "ENTREGADO" || kit.estadoEntrega == "PARCIAL")
        ) return
        if (kit.estadoEntrega == "ENTREGADO") return

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
        if (!kit.kitsEntregaHabilitada) return
        if (kit.estadoSync == EstadoSync.SINCRONIZADO &&
            (kit.estadoEntrega == "ENTREGADO" || kit.estadoEntrega == "PARCIAL")
        ) return
        if (kit.estadoEntrega == "ENTREGADO") return

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

    suspend fun confirmarEntregaKitAsignado(
        kit: KitAsignadoLocal,
        articulos: List<KitArticuloAsignadoLocal>,
        descripcionEntrega: String
    ): EntregaKitLocal {
        if (!kit.kitsEntregaHabilitada) {
            throw IllegalStateException("Kit pendiente de aprobacion del Comite.")
        }
        if (descripcionEntrega.isBlank()) {
            throw IllegalArgumentException("La descripcion de entrega es obligatoria.")
        }

        val articulosEntregados = articulos.filter { it.confirmado && it.cantidadEntregada > 0 }
        if (articulosEntregados.isEmpty()) {
            throw IllegalArgumentException("Marca al menos un articulo entregado.")
        }

        val fechaEntrega = System.currentTimeMillis()
        val cantidadEntregada = articulosEntregados.sumOf { it.cantidadEntregada }
        val estadoEntrega = if (articulosEntregados.size == articulos.size) "ENTREGADO" else "PARCIAL"
        val uuidEntrega = UUID.randomUUID().toString()
        val referenciaPersonaOFamilia = kit.uuidAfectado
            ?.takeIf { it.isNotBlank() }
            ?: kit.refIdFamilia?.takeIf { it.isNotBlank() }
            ?: kit.idPersonaAfectadaRemota?.takeIf { it.isNotBlank() }

        if (referenciaPersonaOFamilia.isNullOrBlank()) {
            throw IllegalArgumentException("El kit no tiene referencia valida de familia o persona afectada.")
        }

        val articulosJson = JSONArray().apply {
            articulosEntregados.forEach { articulo ->
                put(JSONObject().apply {
                    put("uuidArticuloAsignado", articulo.uuidArticuloAsignado)
                    put("uuidKitAsignado", articulo.uuidKitAsignado)
                    put("codigo", articulo.codigo)
                    put("descripcion", articulo.descripcion)
                    put("cantidadAsignada", articulo.cantidadAsignada)
                    put("cantidadEntregada", articulo.cantidadEntregada)
                    put("confirmado", true)
                })
            }
        }.toString()

        val entrega = EntregaKitLocal(
            uuidEntrega = uuidEntrega,
            uuidAfectado = kit.uuidAfectado?.takeIf { it.isNotBlank() },
            uuidGrupoFamiliar = kit.refIdFamilia,
            refIdFamilia = kit.refIdFamilia,
            idGrupoFamiliar = kit.refIdFamilia,
            idPersonaAfectadaRemota = kit.idPersonaAfectadaRemota,
            uuidIncidencia = kit.uuidIncidencia,
            idIncidenciaRemota = kit.idIncidenciaRemota,
            uuidKitAsignado = kit.uuidKitAsignado,
            kitEntregado = kit.tipoKit,
            estadoEntrega = estadoEntrega,
            cantidad = cantidadEntregada,
            descripcionAyuda = descripcionEntrega.trim(),
            observaciones = descripcionEntrega.trim(),
            articulosJson = articulosJson,
            fechaEntrega = fechaEntrega,
            estadoSync = EstadoSync.NUEVO
        )

        kitDao.confirmarEntregaKitAsignado(
            kit = kit,
            entrega = entrega,
            estadoEntrega = estadoEntrega
        )

        return entrega
    }
}
