package pucp.edu.caritas_movile_grd.LocalBDConector

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.File
import org.json.JSONObject
import android.util.Log
import pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal
import pucp.edu.caritas_movile_grd.Incidencias.AfectadoLocal
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaLocal
import pucp.edu.caritas_movile_grd.Network.MobileSyncApi
import pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import pucp.edu.caritas_movile_grd.Seguimientos.SeguimientoLocal
import pucp.edu.caritas_movile_grd.Kits.EntregaKitLocal
import pucp.edu.caritas_movile_grd.Kits.KitDao
import pucp.edu.caritas_movile_grd.Kits.KitAsignadoLocal
import pucp.edu.caritas_movile_grd.Kits.KitArticuloAsignadoLocal
import pucp.edu.caritas_movile_grd.Simulacros.SimulacroDao
import pucp.edu.caritas_movile_grd.Simulacros.SimulacroRepository
import pucp.edu.caritas_movile_grd.login.LoginDao
import kotlinx.coroutines.flow.firstOrNull
private const val TAG_SYNC_EVIDENCIAS = "SyncEvidencias"
data class SyncResult(
    val incidenciasSincronizadas: Int,
    val afectadosSincronizados: Int,
    val evidenciasSincronizadas: Int,
    val observacionesSincronizadas: Int,
    val seguimientosSincronizados: Int,
    val entregasSincronizadas: Int,
    val simulacrosSincronizados: Int,
    val simulacrosDescargados: Int,
    val errores: List<String>
)

data class FinalizarEntregaResult(
    val ok: Boolean,
    val mensaje: String,
    val estadoIncidencia: String? = null
)

class SyncRepository(
    private val syncDao: SyncDao,
    private val kitDao: KitDao,
    private val simulacroDao: SimulacroDao,
    private val loginDao: LoginDao,
    private val appContext: Context,
    private val mobileSyncApi: MobileSyncApi = MobileSyncApi()
) {

    private suspend fun obtenerIdUsuarioGRDActual(): String {
        val perfil = loginDao.getPerfilUsuario().firstOrNull()
            ?: throw IllegalStateException("No hay sesión activa de brigadista")

        return perfil.uuidUsuario.takeIf { it.isNotBlank() }
            ?: perfil.idUsuarioRemoto?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("No hay sesión activa de brigadista")
    }

    suspend fun sincronizarPendientes(): SyncResult {
        var incidenciasSincronizadas = 0
        var afectadosSincronizados = 0
        var evidenciasSincronizadas = 0
        var observacionesSincronizadas = 0
        var seguimientosSincronizados = 0
        var entregasSincronizadas = 0
        var simulacrosSincronizados = 0
        var simulacrosDescargados = 0
        val errores = mutableListOf<String>()
        val userId = obtenerIdUsuarioGRDActual()
        val simulacroRepository = SimulacroRepository(simulacroDao, mobileSyncApi)

        val incidenciasPendientes = syncDao.getIncidenciasNuevasParaSincronizar()
        val avancesKitsSincronizados = sincronizarAvancesKitsAsignados(errores, userId)
        val evidenciasServidor = mutableListOf<pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal>()
        // 1. Sincronizar incidencias nuevas
        for (incidencia in incidenciasPendientes) {
            try {
                val responseIncidencia = mobileSyncApi.sincronizarIncidencia(
                    incidencia.toMobilePayload()
                )

                val idIncidenciaRemota = responseIncidencia.optString(
                    "idIncidenciaRemota",
                    responseIncidencia.optString("idServidor", "")
                )

                val codigoCaso = responseIncidencia
                    .optString("codigoCaso")
                    .ifBlank { null }

                if (idIncidenciaRemota.isBlank()) {
                    errores.add("La incidencia ${incidencia.uuidIncidencia} no devolvió id remoto.")
                    continue
                }

                syncDao.marcarIncidenciaComoSincronizada(
                    uuid = incidencia.uuidIncidencia,
                    idRemoto = idIncidenciaRemota,
                    codigoCaso = codigoCaso
                )

                incidenciasSincronizadas++

                val afectadosPendientes = syncDao.getAfectadosPendientesPorIncidencia(
                    incidencia.uuidIncidencia
                )

                for (afectado in afectadosPendientes) {
                    val sincronizado = sincronizarAfectadoPendiente(
                        afectado = afectado,
                        incidencia = incidencia,
                        idIncidenciaRemota = idIncidenciaRemota,
                        codigoCasoRemoto = codigoCaso,
                        errores = errores
                    )

                    if (sincronizado) {
                        afectadosSincronizados++
                    }
                }
            } catch (ex: Exception) {
                errores.add("Error al sincronizar incidencia ${incidencia.uuidIncidencia}: ${ex.message}")
            }
        }

        // 2. Sincronizar incidencias editadas (cambios de estado como DATA RECOPILADA)
        val incidenciasEditadas = syncDao.getIncidenciasEditadasParaSincronizar()
        for (incidencia in incidenciasEditadas) {
            try {
                val idRemoto = incidencia.idIncidenciaRemota
                if (idRemoto.isNullOrBlank()) {
                    errores.add("Incidencia ${incidencia.uuidIncidencia} no tiene id remoto para actualizar.")
                    Log.w("SyncRepo", "EDITADO skip: uuid=${incidencia.uuidIncidencia} sin idRemoto")
                    continue
                }
                Log.d("SyncRepo", "EDITADO upload: uuid=${incidencia.uuidIncidencia} idRemoto=$idRemoto estado=${incidencia.estado}")
                val respuesta = mobileSyncApi.sincronizarIncidencia(incidencia.toMobilePayload())
                Log.d("SyncRepo", "EDITADO respuesta: $respuesta")
                syncDao.marcarIncidenciaComoSincronizada(
                    uuid = incidencia.uuidIncidencia,
                    idRemoto = idRemoto,
                    codigoCaso = incidencia.codigoCasoRemoto
                )
                incidenciasSincronizadas++
            } catch (ex: Exception) {
                Log.e("SyncRepo", "EDITADO error: ${incidencia.uuidIncidencia}", ex)
                errores.add("Error al actualizar incidencia ${incidencia.uuidIncidencia}: ${ex.message}")
            }
        }

        // 3. Segunda pasada: sincronizar afectados pendientes aunque la incidencia ya esté sincronizada
        val afectadosPendientesGlobales = syncDao.getAfectadosPendientesParaSincronizar()

        for (afectado in afectadosPendientesGlobales) {
            val incidencia = syncDao.getIncidenciaPorUuid(afectado.uuidIncidencia)

            if (incidencia == null) {
                errores.add("No se encontró la incidencia local del afectado ${afectado.uuidAfectado}.")
                continue
            }

            val idIncidenciaRemota = incidencia.idIncidenciaRemota

            if (idIncidenciaRemota.isNullOrBlank()) {
                errores.add(
                    "La incidencia ${incidencia.uuidIncidencia} aún no tiene id remoto. " +
                            "No se puede sincronizar el afectado ${afectado.uuidAfectado}."
                )
                continue
            }

            val sincronizado = sincronizarAfectadoPendiente(
                afectado = afectado,
                incidencia = incidencia,
                idIncidenciaRemota = idIncidenciaRemota,
                codigoCasoRemoto = incidencia.codigoCasoRemoto,
                errores = errores
            )

            if (sincronizado) {
                afectadosSincronizados++
            }
        }

        // 3. Tercera pasada: sincronizar evidencias pendientes
        val evidenciasPendientes = syncDao.getEvidenciasPendientes()
        Log.d(TAG_SYNC_EVIDENCIAS, "Evidencias pendientes=${evidenciasPendientes.size}")

        for (evidencia in evidenciasPendientes) {
            Log.d(
                TAG_SYNC_EVIDENCIAS,
                "Sincronizando evidencia uuid=${evidencia.uuidEvidencia}, uuidReferencia=${evidencia.uuidReferencia}"
            )
            val incidencia = syncDao.getIncidenciaPorUuid(evidencia.uuidReferencia)
            val entregaReferencia = syncDao.getEntregaPorUuid(evidencia.uuidReferencia)

            if (incidencia == null) {
                if (entregaReferencia != null) {
                    Log.d(
                        TAG_SYNC_EVIDENCIAS,
                        "Evidencia ${evidencia.uuidEvidencia} asociada a entrega; se sincroniza despues de la entrega."
                    )
                    continue
                }
                errores.add("No se encontró la incidencia local de la evidencia ${evidencia.uuidEvidencia}.")
                continue
            }

            val idIncidenciaRemota = incidencia.idIncidenciaRemota

            if (idIncidenciaRemota.isNullOrBlank()) {
                errores.add(
                    "La incidencia ${incidencia.uuidIncidencia} aún no tiene id remoto. " +
                            "No se puede sincronizar la evidencia ${evidencia.uuidEvidencia}."
                )
                continue
            }

            val sincronizada = sincronizarEvidenciaPendiente(
                evidencia = evidencia,
                incidencia = incidencia,
                idIncidenciaRemota = idIncidenciaRemota,
                errores = errores,
                idUsuario = userId
            )

            if (sincronizada) {
                evidenciasSincronizadas++
            }
        }

        // 3a. Notificar al servidor los afectados eliminados en móvil.
        val afectadosPendientesEliminacion = syncDao.getAfectadosPendientesEliminacion()
        for (afectado in afectadosPendientesEliminacion) {
            try {
                val payload = JSONObject().apply {
                    put("idAfectadoRemoto", afectado.idAfectadoRemoto)
                    put("uuidAfectado", afectado.uuidAfectado)
                }
                mobileSyncApi.eliminarAfectado(payload)
                syncDao.deleteAfectadoByUuid(afectado.uuidAfectado)
            } catch (ex: Exception) {
                errores.add("Error al eliminar afectado ${afectado.uuidAfectado}: ${ex.message}")
            }
        }

        // 3b. Notificar al servidor las evidencias eliminadas en móvil.
        // NO se borra de Room aquí — el cleanup del step 7 (download) las eliminará
        // cuando el servidor confirme que ya no las devuelve en la lista activa.
        val evidenciasPendientesEliminacion = syncDao.getEvidenciasPendientesEliminacion()
        for (evidencia in evidenciasPendientesEliminacion) {
            try {
                val payload = JSONObject().apply {
                    put("uuidEvidencia", evidencia.uuidEvidencia)
                }
                mobileSyncApi.eliminarEvidencia(payload)
                // No se llama deleteEvidenciaByUuid aquí:
                // la eliminación de Room ocurre en el cleanup de descargarIncidenciasDelServidor()
            } catch (ex: Exception) {
                errores.add("Error al eliminar evidencia ${evidencia.uuidEvidencia}: ${ex.message}")
            }
        }

        // 4. Cuarta pasada: sincronizar observaciones pendientes
        val observacionesPendientes = syncDao.getObservacionesPendientesParaSincronizar()

        for (observacion in observacionesPendientes) {
            val incidencia = syncDao.getIncidenciaPorUuid(observacion.uuidIncidencia)

            if (incidencia == null) {
                errores.add("No se encontró la incidencia local de la observación ${observacion.uuidObservacion}.")
                continue
            }

            val idIncidenciaRemota = incidencia.idIncidenciaRemota

            if (idIncidenciaRemota.isNullOrBlank()) {
                errores.add(
                    "La incidencia ${incidencia.uuidIncidencia} aún no tiene id remoto. " +
                            "No se puede sincronizar la observación ${observacion.uuidObservacion}."
                )
                continue
            }

            val sincronizada = sincronizarObservacionPendiente(
                observacion = observacion,
                incidencia = incidencia,
                idIncidenciaRemota = idIncidenciaRemota,
                errores = errores,
                idUsuario = userId
            )

            if (sincronizada) {
                observacionesSincronizadas++
            }
        }

        // 5. Quinta pasada: sincronizar seguimientos pendientes
        val seguimientosPendientes = syncDao.getSeguimientosPendientesParaSincronizar()

        for (seguimiento in seguimientosPendientes) {
            val incidencia = syncDao.getIncidenciaPorUuid(seguimiento.uuidIncidencia)

            if (incidencia == null) {
                errores.add("No se encontró la incidencia local del seguimiento ${seguimiento.uuidSeguimiento}.")
                continue
            }

            val idIncidenciaRemota = incidencia.idIncidenciaRemota

            if (idIncidenciaRemota.isNullOrBlank()) {
                errores.add(
                    "La incidencia ${incidencia.uuidIncidencia} aún no tiene id remoto. " +
                            "No se puede sincronizar el seguimiento ${seguimiento.uuidSeguimiento}."
                )
                continue
            }

            val sincronizado = sincronizarSeguimientoPendiente(
                seguimiento = seguimiento,
                incidencia = incidencia,
                idIncidenciaRemota = idIncidenciaRemota,
                errores = errores,
                idUsuario = userId
            )

            if (sincronizado) {
                seguimientosSincronizados++
            }
        }
        // 6. Sexta pasada: sincronizar entregas de kits pendientes
        val entregasPendientes = syncDao.getEntregasPendientesParaSincronizar()

        for (entrega in entregasPendientes) {
            val uuidIncidencia = entrega.uuidIncidencia

            if (uuidIncidencia.isNullOrBlank()) {
                errores.add("La entrega ${entrega.uuidEntrega} no tiene uuidIncidencia.")
                continue
            }

            val incidencia = syncDao.getIncidenciaPorUuid(uuidIncidencia)

            if (incidencia == null) {
                errores.add("No se encontró la incidencia local de la entrega ${entrega.uuidEntrega}.")
                continue
            }

            val idIncidenciaRemota = entrega.idIncidenciaRemota
                ?: incidencia.idIncidenciaRemota

            if (idIncidenciaRemota.isNullOrBlank()) {
                errores.add(
                    "La incidencia ${incidencia.uuidIncidencia} aún no tiene id remoto. " +
                            "No se puede sincronizar la entrega ${entrega.uuidEntrega}."
                )
                continue
            }

            val sincronizada = sincronizarEntregaPendiente(
                entrega = entrega,
                incidencia = incidencia,
                idIncidenciaRemota = idIncidenciaRemota,
                errores = errores,
                idUsuario = userId
            )

            if (sincronizada) {
                entregasSincronizadas++
            }
        }
        // 7. Descargar incidencias asignadas desde el servidor
        try {
            descargarIncidenciasDelServidor(userId)
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error descargando incidencias", e)
            errores.add("No se pudieron descargar incidencias: ${e.message}")
        }
        // 8. Sincronizar ejecuciones de simulacros y refrescar asignaciones.
        try {
            simulacrosSincronizados = simulacroRepository.sincronizarSimulacrosPendientes(userId)
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error sincronizando simulacros", e)
            errores.add("No se pudieron sincronizar simulacros: ${e.message}")
        }

        try {
            simulacrosDescargados = simulacroRepository.descargarSimulacrosDesdeBackend()
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error descargando simulacros", e)
            errores.add("No se pudieron descargar simulacros: ${e.message}")
        }

        return SyncResult(
            incidenciasSincronizadas = incidenciasSincronizadas,
            afectadosSincronizados = afectadosSincronizados,
            evidenciasSincronizadas = evidenciasSincronizadas,
            observacionesSincronizadas = observacionesSincronizadas,
            seguimientosSincronizados = seguimientosSincronizados,
            entregasSincronizadas = entregasSincronizadas,
            simulacrosSincronizados = simulacrosSincronizados,
            simulacrosDescargados = simulacrosDescargados,
            errores = errores
        )
    }

    private suspend fun sincronizarAvancesKitsAsignados(
        errores: MutableList<String>,
        idUsuario: String
    ): Int {
        val kitsPendientes = kitDao.getKitsAsignadosPendientesSync()
            .filter { kit ->
                if (!kit.kitsEntregaHabilitada) {
                    errores.add(
                        "Kit ${kit.tipoKit} pendiente de aprobación del Comité. No se sincronizó la entrega."
                    )
                    false
                } else {
                    true
                }
            }

        if (kitsPendientes.isEmpty()) return 0

        val articulos = kitDao.getArticulosAsignadosPorKitsSync(
            kitsPendientes.map { it.uuidKitAsignado }
        )

        val articulosPorKit = articulos.groupBy { it.uuidKitAsignado }
        val kitsPorIncidencia = kitsPendientes.groupBy { it.uuidIncidencia }

        var sincronizados = 0

        for ((uuidIncidencia, kits) in kitsPorIncidencia) {
            try {
                val incidencia = syncDao.getIncidenciaPorUuid(uuidIncidencia)
                    ?: continue

                val idIncidenciaRemota = incidencia.idIncidenciaRemota
                    ?: continue

                val payload = JSONObject().apply {
                    put(
                        "uuidEntregaMovil",
                        "entrega-asignada-${incidencia.uuidIncidencia}"
                    )
                    put("uuidIncidencia", incidencia.uuidIncidencia)
                    put("idIncidenciaRemota", idIncidenciaRemota)
                    put("codigoCaso", incidencia.codigoCasoRemoto)
                    put("idUsuarioGRD", idUsuario)
                    put("fechaEntrega", normalizarFechaRegistro(System.currentTimeMillis()))

                    val descripcion = kits
                        .mapNotNull { it.descripcionEntrega?.takeIf { d -> d.isNotBlank() } }
                        .lastOrNull()
                        ?: "Avance de entrega registrado desde móvil."

                    put("descripcionEntrega", descripcion)
                    put("marcarComoAtendido", false)

                    val kitsArray = org.json.JSONArray()

                    kits.forEach { kit ->
                        val articulosKit = articulosPorKit[kit.uuidKitAsignado].orEmpty()

                        val articulosArray = org.json.JSONArray()
                        articulosKit.forEach { articulo ->
                            articulosArray.put(JSONObject().apply {
                                put("codigo", articulo.codigo)
                                put("descripcion", articulo.descripcion)
                                put("cantidadAsignada", articulo.cantidadAsignada)
                                put("cantidadEntregada", articulo.cantidadEntregada)
                                put("confirmado", articulo.confirmado)
                            })
                        }

                        kitsArray.put(JSONObject().apply {
                            put("uuidKitAsignado", kit.uuidKitAsignado)
                            put("refIdFamilia", kit.refIdFamilia)
                            put("nombreFamilia", kit.nombreFamilia)
                            put("tipoKit", kit.tipoKit)
                            put("estadoEntrega", kit.estadoEntrega)
                            put("articulos", articulosArray)
                        })
                    }

                    put("kits", kitsArray)
                }

                val response = mobileSyncApi.sincronizarEntregaAsignada(payload)

                if (response.optBoolean("ok", false)) {
                    kitDao.marcarKitsAsignadosSincronizados(
                        kits.map { it.uuidKitAsignado }
                    )
                    sincronizados += kits.size
                } else {
                    errores.add(
                        response.optString(
                            "message",
                            "No se pudo sincronizar avance de kits asignados."
                        )
                    )
                }
            } catch (ex: Exception) {
                errores.add("Error al sincronizar avance de kits asignados: ${ex.message}")
            }
        }

        return sincronizados
    }
    suspend fun descargarIncidenciasDelServidor(idUsuario: String? = null) {
        val idUsuarioActual = idUsuario?.takeIf { it.isNotBlank() }
            ?: obtenerIdUsuarioGRDActual()

        val response = mobileSyncApi.obtenerIncidenciasAsignadas(idUsuarioActual)
        val items = response.optJSONArray("incidencias") ?: return
        val incidencias = mutableListOf<pucp.edu.caritas_movile_grd.Incidencias.IncidenciaLocal>()

        for (i in 0 until items.length()) {
            val wrapper = items.optJSONObject(i) ?: continue
            val inc = wrapper.optJSONObject("incidencia") ?: continue
            val asignacion = wrapper.optJSONObject("asignacion")

            val idRemoto = inc.optString("idIncidencia").takeIf { it.isNotBlank() }
            val codigoCaso = inc.optString("codigoCaso").takeIf { it.isNotBlank() }
            val tipoEvento = inc.optString("tipoEvento").takeIf { it.isNotBlank() } ?: "Evento"
            val distrito = inc.optString("distritoEvento").takeIf { it.isNotBlank() }
            val titulo = if (!distrito.isNullOrBlank()) "$tipoEvento - $distrito"
                         else inc.optString("tituloIncidencia").takeIf { it.isNotBlank() } ?: tipoEvento

            // uuidMovil puede venir en asignacion o en incidencia — priorizar asignacion
            val uuidMovilRaw = asignacion?.optString("uuidMovil")?.takeIf { it.isNotBlank() && it != "null" }
                ?: inc.optString("uuidMovil").takeIf { it.isNotBlank() && it != "null" }
            val uuid = uuidMovilRaw ?: "remote-${idRemoto ?: codigoCaso ?: System.currentTimeMillis()}"

            incidencias.add(
                pucp.edu.caritas_movile_grd.Incidencias.IncidenciaLocal(
                    uuidIncidencia          = uuid,
                    idIncidenciaRemota      = idRemoto,
                    uuidUsuario             = idUsuarioActual,
                    idParroquia             = 1,
                    idCatalogoTipo          = 1,
                    tipoEventoNombre        = tipoEvento,
                    nombre                  = titulo,
                    descripcion             = inc.optString("descripcionEvento").takeIf { it.isNotBlank() }
                                                ?: inc.optString("relatoActual").takeIf { it.isNotBlank() }
                                                ?: "Sin descripción",
                    numAfectados            = inc.optInt("numAfectadosReportado", 0),
                    estado                  = inc.optString("estadoActual").takeIf { it.isNotBlank() } ?: "ABIERTO",
                    estadoSync              = EstadoSync.SINCRONIZADO,
                    fechaUltimaModificacion = System.currentTimeMillis(),
                    causa                   = inc.optString("causaEvento").takeIf { it.isNotBlank() },
                    distrito                = distrito,
                    direccion               = inc.optString("direccionEvento").takeIf { it.isNotBlank() },
                    nivelAfectacion         = inc.optString("gravedad").takeIf { it.isNotBlank() },
                    necesidades             = inc.optString("necesidades").takeIf { it.isNotBlank() },
                    latitud                 = inc.optDouble("latitud").takeIf { !it.isNaN() },
                    longitud                = inc.optDouble("longitud").takeIf { !it.isNaN() },
                    codigoCasoRemoto        = codigoCaso,
                    idResponsableGRD        = inc.optString("idUsuarioResponsableGRD").takeIf { it.isNotBlank() && it != "null" },
                    reportadoPorNombre      = inc.optString("reportadoPorNombre").takeIf { it.isNotBlank() },
                    reportadoPorCelular     = inc.optString("reportadoPorCelular").takeIf { it.isNotBlank() },
                    reportadoPorDni         = inc.optString("reportadoPorDni").takeIf { it.isNotBlank() },
                    reportadoPorRol         = inc.optString("reportadoPorRol").takeIf { it.isNotBlank() },
                    parroquiaNombre         = inc.optString("parroquiaNombreSnapshot").takeIf { it.isNotBlank() },
                )
            )
        }

        if (incidencias.isNotEmpty()) {
            // IGNORE: inserta las nuevas sin pisar las que tienen cambios locales (EDITADO/NUEVO)
            syncDao.upsertIncidencias(incidencias)
            // Solo actualiza las que ya están SINCRONIZADAS (no tiene cambios pendientes del usuario)
            for (inc in incidencias) {
                Log.d("SyncRepo", "DOWNLOAD actualizar: uuid=${inc.uuidIncidencia} estado=${inc.estado}")
                syncDao.actualizarIncidenciaSincronizada(
                    uuid                    = inc.uuidIncidencia,
                    idRemoto                = inc.idIncidenciaRemota,
                    codigoCaso              = inc.codigoCasoRemoto,
                    nombre                  = inc.nombre,
                    descripcion             = inc.descripcion,
                    estado                  = inc.estado,
                    idResponsableGRD        = inc.idResponsableGRD,
                    fechaUltimaModificacion = inc.fechaUltimaModificacion
                )
            }
            Log.d("SyncRepository", "Descargadas ${incidencias.size} incidencias del servidor")
        }

        // Descargar evidencias del servidor para cada incidencia
        // Mapa idRemoto → uuidLocal para encontrar la incidencia correcta
        val mapaIdRemotoAUuid = incidencias
            .filter { it.idIncidenciaRemota != null }
            .associate { it.idIncidenciaRemota!! to it.uuidIncidencia }


        // Descargar kits asignados por especialista desde el servidor
        val kitsAsignadosServidor = mutableListOf<KitAsignadoLocal>()
        val articulosAsignadosServidor = mutableListOf<KitArticuloAsignadoLocal>()

        for (i in 0 until items.length()) {
            val wrapper = items.optJSONObject(i) ?: continue
            val inc = wrapper.optJSONObject("incidencia") ?: continue

            val idRemoto = inc.optString("idIncidencia")
                .takeIf { it.isNotBlank() && it != "null" }
                ?: continue

            val uuidIncidencia = mapaIdRemotoAUuid[idRemoto] ?: continue
            val kitsJson = wrapper.optJSONArray("kitsAsignados") ?: continue
            val kitsEntregaHabilitada = obtenerKitsEntregaHabilitada(wrapper, inc)

            for (k in 0 until kitsJson.length()) {
                val kitJson = kitsJson.optJSONObject(k) ?: continue

                val uuidKitAsignado = kitJson.optString("uuidKitAsignado")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?: continue

                val tipoKit = kitJson.optString("tipoKit")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?: continue

                kitsAsignadosServidor.add(
                    KitAsignadoLocal(
                        uuidKitAsignado = uuidKitAsignado,
                        uuidIncidencia = uuidIncidencia,
                        idIncidenciaRemota = idRemoto,
                        refIdFamilia = kitJson.optString("refIdFamilia")
                            .takeIf { it.isNotBlank() && it != "null" },
                        nombreFamilia = kitJson.optString("nombreFamilia")
                            .takeIf { it.isNotBlank() && it != "null" },
                        idKitEmergenciaRemoto = kitJson.optString("idKitEmergenciaRemoto")
                            .takeIf { it.isNotBlank() && it != "null" },
                        tipoKit = tipoKit,
                        estadoEntrega = "PENDIENTE",
                        kitsEntregaHabilitada = kitsEntregaHabilitada,
                        estadoSync = EstadoSync.SINCRONIZADO
                    )
                )

                val articulosJson = kitJson.optJSONArray("articulos") ?: continue

                for (a in 0 until articulosJson.length()) {
                    val artJson = articulosJson.optJSONObject(a) ?: continue

                    val descripcion = artJson.optString("descripcion")
                        .takeIf { it.isNotBlank() && it != "null" }
                        ?: continue

                    articulosAsignadosServidor.add(
                        KitArticuloAsignadoLocal(
                            uuidArticuloAsignado = "$uuidKitAsignado::$a",
                            uuidKitAsignado = uuidKitAsignado,
                            codigo = artJson.optString("codigo")
                                .takeIf { it.isNotBlank() && it != "null" }
                                ?: "",
                            descripcion = descripcion,
                            cantidadAsignada = artJson.optInt("cantidad", 1),
                            cantidadEntregada = 0,
                            confirmado = false
                        )
                    )
                }
            }
        }
        if (kitsAsignadosServidor.isNotEmpty()) {
            kitDao.insertKitsAsignados(kitsAsignadosServidor)
            Log.d("SyncRepository", "Descargados ${kitsAsignadosServidor.size} kits asignados")
        }

        if (articulosAsignadosServidor.isNotEmpty()) {
            kitDao.insertArticulosAsignados(articulosAsignadosServidor)
            Log.d("SyncRepository", "Descargados ${articulosAsignadosServidor.size} artículos de kits asignados")
        }

        // Aplicar avance de entrega móvil ya registrado en el servidor
        // Esto permite restaurar checks y estados después de sincronizar o reinstalar la app.
        for (i in 0 until items.length()) {
            val wrapper = items.optJSONObject(i) ?: continue
            val entregaMovil = wrapper.optJSONObject("entregaMovil") ?: continue

            val fechaEntregaMs = parseFechaServidorMillis(
                entregaMovil.optString("fechaEntrega")
            ) ?: System.currentTimeMillis()

            val descripcionEntrega = entregaMovil.optString("descripcionEntrega")
                .takeIf { it.isNotBlank() && it != "null" }

            val kitsEntregados = entregaMovil.optJSONArray("kitsEntregados") ?: continue

            for (k in 0 until kitsEntregados.length()) {
                val kitJson = kitsEntregados.optJSONObject(k) ?: continue

                val uuidKitAsignado = kitJson.optString("uuidKitAsignado")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?: continue

                val estadoEntrega = kitJson.optString("estadoEntrega")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?: "PENDIENTE"

                kitDao.marcarKitEntregado(
                    uuidKitAsignado = uuidKitAsignado,
                    estadoEntrega = estadoEntrega,
                    fechaEntrega = fechaEntregaMs,
                    descripcionEntrega = descripcionEntrega,
                    evidenciaLocalUri = null,
                    estadoSync = EstadoSync.SINCRONIZADO
                )

                val articulos = kitJson.optJSONArray("articulos") ?: continue

                for (a in 0 until articulos.length()) {
                    val artJson = articulos.optJSONObject(a) ?: continue

                    val codigo = artJson.optString("codigo")
                        .takeIf { it.isNotBlank() && it != "null" }
                        ?: ""

                    val descripcion = artJson.optString("descripcion")
                        .takeIf { it.isNotBlank() && it != "null" }
                        ?: ""

                    val confirmado = artJson.optBoolean("confirmado", false)
                    val cantidadEntregada = artJson.optInt(
                        "cantidadEntregada",
                        if (confirmado) artJson.optInt("cantidadAsignada", 1) else 0
                    )

                    kitDao.actualizarConfirmacionArticuloPorKitCodigo(
                        uuidKitAsignado = uuidKitAsignado,
                        codigo = codigo,
                        descripcion = descripcion,
                        confirmado = confirmado,
                        cantidadEntregada = cantidadEntregada
                    )
                }
            }
        }


        val evidenciasServidor = mutableListOf<pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal>()
        for (i in 0 until items.length()) {
            val wrapper = items.optJSONObject(i) ?: continue
            val inc = wrapper.optJSONObject("incidencia") ?: continue
            val idRemoto = inc.optString("idIncidencia").takeIf { it.isNotBlank() } ?: continue
            val uuidIncidencia = mapaIdRemotoAUuid[idRemoto] ?: continue
            val evArray = inc.optJSONArray("evidencias") ?: continue
            for (j in 0 until evArray.length()) {
                val ev = evArray.optJSONObject(j) ?: continue
                val uuidEv = ev.optString("uuidMovil").takeIf { it.isNotBlank() && it != "null" }
                    ?: ev.optString("idEvidenciaGRD").takeIf { it.isNotBlank() }
                    ?: continue
                val urlFirmada = ev.optString("urlFirmada").takeIf { it.isNotBlank() && it != "null" }
                    ?: ev.optString("urlArchivo").takeIf { it.isNotBlank() && it != "null" }
                    ?: continue
                evidenciasServidor.add(
                    pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal(
                        uuidEvidencia  = uuidEv,
                        uuidReferencia = uuidIncidencia,
                        rutaLocal      = urlFirmada,
                        urlS3          = urlFirmada,
                        nombreArchivo  = ev.optString("nombreArchivo").takeIf { it.isNotBlank() },
                        contentType    = ev.optString("formatoArchivo").takeIf { it.isNotBlank() },
                        descripcion    = ev.optString("descripcion").takeIf { it.isNotBlank() },
                        estadoSync     = EstadoSync.SINCRONIZADO
                    )
                )
            }
        }
        if (evidenciasServidor.isNotEmpty()) {
            syncDao.insertEvidenciasIfNotExists(evidenciasServidor)
            // Refrescar URLs firmadas de las que ya existían (expiran en 15 min)
            for (ev in evidenciasServidor) {
                val url = ev.urlS3 ?: continue
                syncDao.refrescarUrlEvidencia(ev.uuidEvidencia, url)
            }
            Log.d("SyncRepository", "Descargadas/refrescadas ${evidenciasServidor.size} evidencias del servidor")
        }

        // Descargar afectados/familias del servidor (registrados desde web)
        val afectadosServidor = mutableListOf<pucp.edu.caritas_movile_grd.Incidencias.AfectadoLocal>()
        for (i in 0 until items.length()) {
            val wrapper = items.optJSONObject(i) ?: continue
            val inc = wrapper.optJSONObject("incidencia") ?: continue
            val idRemoto = inc.optString("idIncidencia").takeIf { it.isNotBlank() } ?: continue
            val uuidIncidencia = mapaIdRemotoAUuid[idRemoto] ?: continue
            val grupos = inc.optJSONArray("gruposFamiliares") ?: continue
            for (g in 0 until grupos.length()) {
                val grupo = grupos.optJSONObject(g) ?: continue
                val familiaId = grupo.optString("idGrupoFamiliar").takeIf { it.isNotBlank() } ?: continue
                val familiaNombre = grupo.optString("nombreReferencia").takeIf { it.isNotBlank() }
                val personas = grupo.optJSONArray("personas") ?: continue
                for (p in 0 until personas.length()) {
                    val persona = personas.optJSONObject(p) ?: continue
                    // Usar uuidMovil si existe, si no usar idPersonaAfectada como clave
                    val uuidAfectado = persona.optString("uuidMovil").takeIf { it.isNotBlank() && it != "null" }
                        ?: persona.optString("idPersonaAfectada").takeIf { it.isNotBlank() }
                        ?: continue
                    val apellidos = persona.optString("apellidos").takeIf { it.isNotBlank() }
                    val (apellidoPaterno, apellidoMaterno) = if (apellidos != null) {
                        val partes = apellidos.trim().split(" ", limit = 2)
                        partes.getOrNull(0) to partes.getOrNull(1)
                    } else null to null
                    afectadosServidor.add(
                        pucp.edu.caritas_movile_grd.Incidencias.AfectadoLocal(
                            uuidAfectado      = uuidAfectado,
                            uuidIncidencia    = uuidIncidencia,
                            idCatalogoDoc     = 1,
                            idAfectadoRemoto  = persona.optString("idPersonaAfectada").takeIf { it.isNotBlank() },
                            documentoIdentidad = persona.optString("numeroDocumento").takeIf { it.isNotBlank() } ?: "",
                            nombres           = persona.optString("nombres").takeIf { it.isNotBlank() } ?: "Sin nombre",
                            apellidoPaterno   = apellidoPaterno,
                            apellidoMaterno   = apellidoMaterno,
                            genero            = persona.optString("sexo").takeIf { it.isNotBlank() },
                            parentesco        = persona.optString("parentesco").takeIf { it.isNotBlank() },
                            situacionActual   = persona.optString("condicionSalud").takeIf { it.isNotBlank() },
                            familiaId         = familiaId,
                            familiaNombre     = familiaNombre,
                            estadoSync        = EstadoSync.SINCRONIZADO
                        )
                    )
                }
            }
        }
        if (afectadosServidor.isNotEmpty()) {
            syncDao.insertAfectadosIfNotExists(afectadosServidor)
            Log.d("SyncRepository", "Descargados ${afectadosServidor.size} afectados del servidor")
        }

        // Eliminar localmente evidencias SINCRONIZADAS que el servidor ya no devuelve (fueron borradas en web)
        val evidenciasPorIncidencia = evidenciasServidor.groupBy { it.uuidReferencia }
        for (incidencia in incidencias) {
            val uuidsActivos = evidenciasPorIncidencia[incidencia.uuidIncidencia]?.map { it.uuidEvidencia } ?: emptyList()
            if (uuidsActivos.isEmpty()) {
                syncDao.eliminarTodasEvidenciasRemotas(incidencia.uuidIncidencia)
            } else {
                syncDao.eliminarEvidenciasRemotas(incidencia.uuidIncidencia, uuidsActivos)
            }
        }
    }
    private suspend fun sincronizarAfectadoPendiente(
        afectado: AfectadoLocal,
        incidencia: IncidenciaLocal,
        idIncidenciaRemota: String,
        codigoCasoRemoto: String?,
        errores: MutableList<String>
    ): Boolean {
        return try {
            val responseAfectado = mobileSyncApi.sincronizarAfectado(
                afectado.toMobilePayload(
                    incidencia = incidencia,
                    idIncidenciaRemota = idIncidenciaRemota,
                    codigoCasoRemoto = codigoCasoRemoto
                )
            )

            val idAfectadoRemoto = responseAfectado.optString(
                "idPersonaAfectadaRemota",
                responseAfectado.optString("idServidor", "")
            )

            if (idAfectadoRemoto.isBlank()) {
                errores.add("El afectado ${afectado.uuidAfectado} no devolvió id remoto.")
                false
            } else {
                syncDao.marcarAfectadoComoSincronizado(
                    uuidAfectado = afectado.uuidAfectado,
                    idRemoto = idAfectadoRemoto
                )

                true
            }
        } catch (ex: Exception) {
            errores.add("Error al sincronizar afectado ${afectado.uuidAfectado}: ${ex.message}")
            false
        }
    }

    private suspend fun sincronizarEvidenciaPendiente(
        evidencia: EvidenciaLocal,
        incidencia: IncidenciaLocal,
        idIncidenciaRemota: String,
        errores: MutableList<String>,
        idUsuario: String
    ): Boolean {
        return try {
            val responseEvidencia = mobileSyncApi.sincronizarEvidencia(
                evidencia.toMobilePayload(
                    incidencia = incidencia,
                    idIncidenciaRemota = idIncidenciaRemota,
                    context = appContext,
                    idUsuario = idUsuario
                )
            )

            val idEvidenciaRemota = responseEvidencia.optString(
                "idEvidenciaRemota",
                responseEvidencia.optString("idServidor", "")
            )

            if (idEvidenciaRemota.isBlank()) {
                errores.add("La evidencia ${evidencia.uuidEvidencia} no devolvió id remoto.")
                false
            } else {
                val urlArchivo = responseEvidencia
                    .optString("urlFirmada")
                    .ifBlank { responseEvidencia.optString("urlArchivo") }
                    .ifBlank { evidencia.urlS3 ?: evidencia.rutaLocal }

                syncDao.marcarEvidenciaComoSincronizada(
                    uuidEvidencia = evidencia.uuidEvidencia,
                    urlArchivo = urlArchivo
                )

                true
            }
        } catch (ex: Exception) {
            errores.add("Error al sincronizar evidencia ${evidencia.uuidEvidencia}: ${ex.message}")
            false
        }
    }

    private suspend fun sincronizarEvidenciaEntregaPendiente(
        evidencia: EvidenciaLocal,
        entrega: EntregaKitLocal,
        incidencia: IncidenciaLocal,
        idIncidenciaRemota: String,
        idEntregaRemota: String,
        errores: MutableList<String>,
        idUsuario: String
    ): Boolean {
        return try {
            val responseEvidencia = mobileSyncApi.sincronizarEvidencia(
                evidencia.toMobilePayloadEntrega(
                    entrega = entrega,
                    incidencia = incidencia,
                    idIncidenciaRemota = idIncidenciaRemota,
                    idEntregaRemota = idEntregaRemota,
                    context = appContext,
                    idUsuario = idUsuario
                )
            )

            val idEvidenciaRemota = responseEvidencia.optString(
                "idEvidenciaRemota",
                responseEvidencia.optString("idServidor", "")
            )

            if (idEvidenciaRemota.isBlank()) {
                errores.add("La evidencia ${evidencia.uuidEvidencia} no devolvio id remoto.")
                false
            } else {
                val urlArchivo = responseEvidencia
                    .optString("urlFirmada")
                    .ifBlank { responseEvidencia.optString("urlArchivo") }
                    .ifBlank { evidencia.urlS3 ?: evidencia.rutaLocal }

                syncDao.marcarEvidenciaComoSincronizada(
                    uuidEvidencia = evidencia.uuidEvidencia,
                    urlArchivo = urlArchivo
                )

                true
            }
        } catch (ex: Exception) {
            errores.add("Error al sincronizar evidencia ${evidencia.uuidEvidencia}: ${ex.message}")
            false
        }
    }

    private suspend fun sincronizarObservacionPendiente(
        observacion: ObservacionLocal,
        incidencia: IncidenciaLocal,
        idIncidenciaRemota: String,
        errores: MutableList<String>,
        idUsuario: String
    ): Boolean {
        return try {
            val responseObservacion = mobileSyncApi.sincronizarObservacion(
                observacion.toMobilePayload(
                    incidencia = incidencia,
                    idIncidenciaRemota = idIncidenciaRemota,
                    idUsuario = idUsuario
                )
            )

            val idObservacionRemota = responseObservacion.optString(
                "idObservacionRemota",
                responseObservacion.optString("idServidor", "")
            )

            if (idObservacionRemota.isBlank()) {
                errores.add("La observación ${observacion.uuidObservacion} no devolvió id remoto.")
                false
            } else {
                syncDao.marcarObservacionComoSincronizada(
                    uuidObservacion = observacion.uuidObservacion,
                    idRemoto = idObservacionRemota
                )

                true
            }
        } catch (ex: Exception) {
            errores.add("Error al sincronizar observación ${observacion.uuidObservacion}: ${ex.message}")
            false
        }
    }  

    private suspend fun sincronizarSeguimientoPendiente(
        seguimiento: SeguimientoLocal,
        incidencia: IncidenciaLocal,
        idIncidenciaRemota: String,
        errores: MutableList<String>,
        idUsuario: String
    ): Boolean {
        return try {
            val responseSeguimiento = mobileSyncApi.sincronizarSeguimiento(
                seguimiento.toMobilePayload(
                    incidencia = incidencia,
                    idIncidenciaRemota = idIncidenciaRemota,
                    idUsuario = idUsuario
                )
            )

            val idSeguimientoRemoto = responseSeguimiento.optString(
                "idSeguimientoRemoto",
                responseSeguimiento.optString("idServidor", "")
            )

            if (idSeguimientoRemoto.isBlank()) {
                errores.add("El seguimiento ${seguimiento.uuidSeguimiento} no devolvió id remoto.")
                false
            } else {
                syncDao.marcarSeguimientoComoSincronizado(
                    uuidSeguimiento = seguimiento.uuidSeguimiento,
                    idRemoto = idSeguimientoRemoto
                )

                true
            }
        } catch (ex: Exception) {
            errores.add("Error al sincronizar seguimiento ${seguimiento.uuidSeguimiento}: ${ex.message}")
            false
        }
    }  
    private suspend fun sincronizarEntregaPendiente(
        entrega: EntregaKitLocal,
        incidencia: IncidenciaLocal,
        idIncidenciaRemota: String,
        errores: MutableList<String>,
        idUsuario: String
    ): Boolean {
        return try {
            val responseEntrega = mobileSyncApi.sincronizarEntrega(
                entrega.toMobilePayload(
                    incidencia = incidencia,
                    idIncidenciaRemota = idIncidenciaRemota,
                    idUsuario = idUsuario
                )
            )

            val idEntregaRemota = responseEntrega.optString(
                "idEntregaRemota",
                responseEntrega.optString("idServidor", "")
            )

            if (idEntregaRemota.isBlank()) {
                errores.add("La entrega ${entrega.uuidEntrega} no devolvió id remoto.")
                false
            } else {
                syncDao.marcarEntregaComoSincronizada(
                    uuidEntrega = entrega.uuidEntrega,
                    idRemoto = idEntregaRemota
                )

                val estadoActual = responseEntrega.optString("estadoActual").takeIf { it.isNotBlank() }
                    ?: responseEntrega.optString("estadoIncidencia").takeIf { it.isNotBlank() }
                if (estadoActual == "ATENDIDO") {
                    syncDao.actualizarEstadoIncidencia(
                        uuidIncidencia = incidencia.uuidIncidencia,
                        estado = estadoActual,
                        fechaUltimaModificacion = System.currentTimeMillis()
                    )
                }

                val evidenciasEntrega = syncDao.getEvidenciasPendientesPorReferencia(entrega.uuidEntrega)
                for (evidencia in evidenciasEntrega) {
                    sincronizarEvidenciaEntregaPendiente(
                        evidencia = evidencia,
                        entrega = entrega.copy(idEntregaRemota = idEntregaRemota),
                        incidencia = incidencia,
                        idIncidenciaRemota = idIncidenciaRemota,
                        idEntregaRemota = idEntregaRemota,
                        errores = errores,
                        idUsuario = idUsuario
                    )
                }

                true
            }
        } catch (ex: Exception) {
            errores.add("Error al sincronizar entrega ${entrega.uuidEntrega}: ${ex.message}")
            false
        }
    }    

    suspend fun finalizarEntregaIncidencia(uuidIncidencia: String): FinalizarEntregaResult {
        val incidencia = syncDao.getIncidenciaPorUuid(uuidIncidencia)
            ?: return FinalizarEntregaResult(false, "No se encontro la incidencia local.")

        val idIncidenciaRemota = incidencia.idIncidenciaRemota
        if (idIncidenciaRemota.isNullOrBlank()) {
            return FinalizarEntregaResult(false, "La incidencia aun no tiene id remoto.")
        }

        val kits = kitDao.getKitsAsignadosPorIncidenciaSync(uuidIncidencia)
        if (kits.isEmpty()) {
            return FinalizarEntregaResult(false, "No hay kits asignados para finalizar.")
        }

        val articulosPorKit = kitDao.getArticulosAsignadosPorKitsSync(
            kits.map { it.uuidKitAsignado }
        ).groupBy { it.uuidKitAsignado }

        val faltanKits = kits.any { kit ->
            when (kit.estadoEntrega) {
                "ENTREGADO" -> false
                "PARCIAL" -> {
                    val articulos = articulosPorKit[kit.uuidKitAsignado].orEmpty()
                    articulos.isEmpty() || articulos.any { !it.confirmado || it.cantidadEntregada <= 0 }
                }
                else -> true
            }
        }

        if (faltanKits) {
            return FinalizarEntregaResult(
                ok = false,
                mensaje = "Aun hay kits sin entregar. Completa todas las entregas antes de finalizar."
            )
        }

        Log.d(
            "SyncRepository",
            "Finalizando entrega incidencia uuid=${incidencia.uuidIncidencia}, idRemota=$idIncidenciaRemota"
        )

        val payload = JSONObject().apply {
            put("uuidIncidencia", incidencia.uuidIncidencia)
            put("idIncidenciaRemota", idIncidenciaRemota)
            putNullable("codigoCaso", incidencia.codigoCasoRemoto)
            put("fechaFinalizacion", normalizarFechaRegistro(System.currentTimeMillis()))
            put("kitsEntregados", org.json.JSONArray().apply {
                kits.forEach { kit ->
                    val articulosArray = org.json.JSONArray().apply {
                        articulosPorKit[kit.uuidKitAsignado].orEmpty().forEach { articulo ->
                            put(JSONObject().apply {
                                put("uuidArticuloAsignado", articulo.uuidArticuloAsignado)
                                put("codigo", articulo.codigo)
                                put("descripcion", articulo.descripcion)
                                put("cantidadAsignada", articulo.cantidadAsignada)
                                put("cantidadEntregada", articulo.cantidadEntregada)
                                put("confirmado", articulo.confirmado)
                            })
                        }
                    }

                    put(JSONObject().apply {
                        put("uuidKitAsignado", kit.uuidKitAsignado)
                        putNullable("uuidGrupoFamiliar", kit.refIdFamilia)
                        putNullable("idGrupoFamiliar", kit.refIdFamilia)
                        putNullable("refIdFamilia", kit.refIdFamilia)
                        putNullable("uuidAfectadoMovil", kit.uuidAfectado)
                        putNullable("idPersonaAfectadaRemota", kit.idPersonaAfectadaRemota)
                        put("tipoKit", kit.tipoKit)
                        put("estadoEntrega", kit.estadoEntrega)
                        put("articulos", articulosArray)
                    })
                }
            })
        }

        val response = mobileSyncApi.finalizarEntrega(payload)
        val estadoRespuesta = response.optString("estadoIncidencia")
            .ifBlank { response.optString("estadoActual") }
            .ifBlank { null }
        val incidenciaAtendida = response.optBoolean("incidenciaAtendida", false) ||
            estadoRespuesta == "ATENDIDO"

        if (!incidenciaAtendida) {
            return FinalizarEntregaResult(
                ok = false,
                mensaje = response.optString(
                    "message",
                    "El backend no confirmo la finalizacion de entrega."
                ),
                estadoIncidencia = estadoRespuesta
            )
        }

        syncDao.actualizarEstadoIncidencia(
            uuidIncidencia = incidencia.uuidIncidencia,
            estado = "ATENDIDO",
            fechaUltimaModificacion = System.currentTimeMillis()
        )

        return FinalizarEntregaResult(
            ok = true,
            mensaje = response.optString("message", "Entrega finalizada."),
            estadoIncidencia = "ATENDIDO"
        )
    }

}

private fun IncidenciaLocal.toMobilePayload(): JSONObject {
    return JSONObject().apply {
        put("uuidIncidencia", uuidIncidencia)
        putNullable("idIncidenciaRemota", idIncidenciaRemota)
        put("uuidUsuario", uuidUsuario)
        put("idParroquia", idParroquia)
        put("idCatalogoTipo", idCatalogoTipo)
        val tipoEventoSeguro = tipoEventoNombre?.takeIf { it.isNotBlank() }

        if (tipoEventoSeguro != null) {
            put("tipoEvento", tipoEventoSeguro)
            put("categoria", tipoEventoSeguro)
            put("categoriaNombre", tipoEventoSeguro)
        }        
        put("descripcion", descripcion)
        put("nombre", nombre)
        put("numAfectados", numAfectados)
        put("responsable", responsable)
        put("estado", estado)
        put("fechaUltimaModificacion", fechaUltimaModificacion)

        putNullable("causa", causa)
        putNullable("reportadoPorNombre", reportadoPorNombre)
        putNullable("reportadoPorCelular", reportadoPorCelular)
        putNullable("reportadoPorDni", reportadoPorDni)
        putNullable("reportadoPorRol", reportadoPorRol)
        putNullable("necesidades", necesidades)
        putNullable("observacionesCampo", observacionesCampo)
        putNullable("fechaSuceso", fechaSuceso)
        putNullable("distrito", distrito)
        putNullable("direccion", direccion)
        putNullable("referencia", referencia)
        putNullable("parroquiaNombre", parroquiaNombre)
        putNullable("nivelAfectacion", nivelAfectacion)
        putNullable("necesidadesObs", necesidadesObs)
        putNullable("latitud", latitud)
        putNullable("longitud", longitud)
    }
}

private fun AfectadoLocal.toMobilePayload(
    incidencia: IncidenciaLocal,
    idIncidenciaRemota: String,
    codigoCasoRemoto: String?
): JSONObject {
    val apellidos = listOfNotNull(
        apellidoPaterno?.takeIf { it.isNotBlank() },
        apellidoMaterno?.takeIf { it.isNotBlank() }
    ).joinToString(" ")

    val codigoCasoSeguro = codigoCasoRemoto
        ?.takeIf { it.isNotBlank() }
        ?: incidencia.uuidIncidencia

    val codigoGrupo = familiaId?.takeIf { it.isNotBlank() }
        ?: "MOVIL-$codigoCasoSeguro"

    val nombreReferencia = familiaNombre?.takeIf { it.isNotBlank() }
        ?: nombres

    return JSONObject().apply {
        put("uuidAfectado", uuidAfectado)
        put("uuidIncidencia", incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idIncidenciaRemota)
        putNullable("codigoCaso", codigoCasoRemoto)

        put("codigoGrupo", codigoGrupo)
        put("nombreReferencia", nombreReferencia)

        put("tipoDocumento", idCatalogoDoc.toString())
        put("documentoIdentidad", documentoIdentidad)
        put("numeroDocumento", documentoIdentidad)
        put("nombres", nombres)
        putNullable("apellidos", apellidos.ifBlank { null })

        putNullable("sexo", genero)
        putNullable("parentesco", parentesco)
        putNullable("condicionSalud", situacionActual)
        putNullable("telefono", celular)
        putNullable("observaciones", situacionActual)

        put("esVulnerable", false)
    }
}

private fun EvidenciaLocal.toMobilePayload(
    incidencia: IncidenciaLocal,
    idIncidenciaRemota: String,
    context: Context,
    idUsuario: String
): JSONObject {
    val nombreSeguro = nombreArchivo
        ?.takeIf { it.isNotBlank() }
        ?: rutaLocal.substringAfterLast('/').substringBefore('?').ifBlank {
            "evidencia-movil-${uuidEvidencia}.bin"
        }

    val tipoSeguro = contentType
        ?.takeIf { it.isNotBlank() }
        ?: "application/octet-stream"

    val archivoBase64 = if (urlS3.isNullOrBlank()) {
        leerArchivoBase64(context, rutaLocal, tipoSeguro, nombreSeguro)
    } else {
        null
    }

    val urlArchivoSeguro = urlS3
        ?.takeIf { it.isNotBlank() }
        ?: rutaLocal

    val nombreArchivoEnvio = archivoBase64?.nombreArchivo ?: nombreSeguro
    val tipoArchivoEnvio = archivoBase64?.contentType ?: tipoSeguro

    return JSONObject().apply {
        put("uuidEvidencia", uuidEvidencia)

        put("uuidReferencia", incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idIncidenciaRemota)
        put("idReferenciaRemota", idIncidenciaRemota)
        put("tipoReferencia", "INCIDENCIA")

        put("idUsuarioCargaGRD", idUsuario)

        put("nombreArchivo", nombreArchivoEnvio)
        put("contentType", tipoArchivoEnvio)
        put("formatoArchivo", tipoArchivoEnvio)
        putNullable("descripcion", descripcion)

        if (archivoBase64 != null) {
            put("base64", archivoBase64.base64)
            put("tamanoArchivo", archivoBase64.tamanoBytes)
        } else {
            putNullable("tamanoArchivo", tamanoArchivo)
            put("urlArchivo", urlArchivoSeguro)
        }
    }
}

private fun EvidenciaLocal.toMobilePayloadEntrega(
    entrega: EntregaKitLocal,
    incidencia: IncidenciaLocal,
    idIncidenciaRemota: String,
    idEntregaRemota: String,
    context: Context,
    idUsuario: String
): JSONObject {
    val nombreSeguro = nombreArchivo
        ?.takeIf { it.isNotBlank() }
        ?: rutaLocal.substringAfterLast('/').substringBefore('?').ifBlank {
            "evidencia-entrega-${uuidEvidencia}.bin"
        }

    val tipoSeguro = contentType
        ?.takeIf { it.isNotBlank() }
        ?: "application/octet-stream"

    val archivoBase64 = if (urlS3.isNullOrBlank()) {
        leerArchivoBase64(context, rutaLocal, tipoSeguro, nombreSeguro)
    } else {
        null
    }

    val urlArchivoSeguro = urlS3
        ?.takeIf { it.isNotBlank() }
        ?: rutaLocal

    val nombreArchivoEnvio = archivoBase64?.nombreArchivo ?: nombreSeguro
    val tipoArchivoEnvio = archivoBase64?.contentType ?: tipoSeguro

    return JSONObject().apply {
        put("uuidEvidencia", uuidEvidencia)

        // EvidenciaLocal no tiene tipoReferencia; para evidencias de entrega se usa
        // uuidReferencia = uuidEntrega y se envia este tipo al backend al sincronizar.
        put("uuidReferencia", entrega.uuidEntrega)
        put("idIncidenciaRemota", idIncidenciaRemota)
        put("idReferenciaRemota", idEntregaRemota)
        put("tipoReferencia", "ENTREGA_AYUDA_HUMANITARIA")
        put("uuidIncidencia", incidencia.uuidIncidencia)
        put("uuidEntrega", entrega.uuidEntrega)

        put("idUsuarioCargaGRD", idUsuario)

        put("nombreArchivo", nombreArchivoEnvio)
        put("contentType", tipoArchivoEnvio)
        put("formatoArchivo", tipoArchivoEnvio)
        putNullable("descripcion", descripcion)

        if (archivoBase64 != null) {
            put("base64", archivoBase64.base64)
            put("tamanoArchivo", archivoBase64.tamanoBytes)
        } else {
            putNullable("tamanoArchivo", tamanoArchivo)
            put("urlArchivo", urlArchivoSeguro)
        }
    }
}

private fun obtenerKitsEntregaHabilitada(
    wrapper: JSONObject,
    incidencia: JSONObject
): Boolean {
    return when {
        wrapper.has("kitsEntregaHabilitada") && !wrapper.isNull("kitsEntregaHabilitada") ->
            wrapper.optBoolean("kitsEntregaHabilitada", false)
        incidencia.has("kitsEntregaHabilitada") && !incidencia.isNull("kitsEntregaHabilitada") ->
            incidencia.optBoolean("kitsEntregaHabilitada", false)
        else -> estadoPermiteEntregaKits(incidencia.optString("estadoActual"))
    }
}

private fun estadoPermiteEntregaKits(estadoActual: String?): Boolean {
    return estadoActual
        ?.trim()
        ?.uppercase(Locale.ROOT) in setOf(
        "APROBADO",
        "ATENDIDO",
        "SEGUIMIENTO ABIERTO",
        "CERRADO"
    )
}

private fun EntregaKitLocal.toMobilePayload(
    incidencia: IncidenciaLocal,
    idIncidenciaRemota: String,
    idUsuario: String
): JSONObject {
    return JSONObject().apply {
        put("uuidEntrega", uuidEntrega)
        put("uuidEntregaMovil", uuidEntrega)

        put("uuidIncidencia", uuidIncidencia ?: incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idIncidenciaRemota)
        putNullable("codigoCaso", incidencia.codigoCasoRemoto)

        putNullable("uuidAfectadoMovil", uuidAfectado)
        putNullable("uuidAfectado", uuidAfectado)
        putNullable("uuidPersonaAfectada", uuidAfectado)
        putNullable("uuidPersonaAfectadaMovil", uuidAfectado)
        putNullable("idPersonaAfectadaRemota", idPersonaAfectadaRemota)
        putNullable("idPersonaAfectada", idPersonaAfectadaRemota)

        // El modelo movil no distingue si refIdFamilia es uuidMovil o id remoto.
        // Se envia en las claves compatibles para que backend resuelva la familia.
        putNullable("uuidGrupoFamiliar", uuidGrupoFamiliar)
        putNullable("idGrupoFamiliar", idGrupoFamiliar ?: refIdFamilia ?: uuidGrupoFamiliar)
        putNullable("idGrupoFamiliarRemota", idGrupoFamiliar ?: refIdFamilia)
        putNullable("refIdFamilia", refIdFamilia ?: uuidGrupoFamiliar)
        putNullable("uuidKitAsignado", uuidKitAsignado)

        put("tipoAyuda", kitEntregado)
        put("kitEntregado", kitEntregado)
        put("descripcionAyuda", descripcionAyuda?.takeIf { it.isNotBlank() } ?: "Entrega movil de $kitEntregado")
        put("descripcionEntrega", descripcionAyuda?.takeIf { it.isNotBlank() } ?: "Entrega movil de $kitEntregado")
        putNullable("observaciones", observaciones ?: descripcionAyuda)
        put("cantidadEntregada", cantidad)
        put("fechaEntrega", normalizarFechaRegistro(fechaEntrega))

        put("idUsuarioResponsableGRD", idUsuario)
        put("idUsuarioGRD", idUsuario)

        val articulosArray = articulosJson
            ?.takeIf { it.isNotBlank() }
            ?.let { org.json.JSONArray(it) }
            ?: org.json.JSONArray()
        put("articulos", articulosArray)
        put("kits", org.json.JSONArray().apply {
            put(JSONObject().apply {
                putNullable("uuidKitAsignado", uuidKitAsignado)
                putNullable("uuidGrupoFamiliar", uuidGrupoFamiliar)
                putNullable("idGrupoFamiliar", idGrupoFamiliar ?: refIdFamilia ?: uuidGrupoFamiliar)
                putNullable("refIdFamilia", refIdFamilia ?: uuidGrupoFamiliar)
                put("tipoKit", kitEntregado)
                put("estadoEntrega", estadoEntrega ?: "ENTREGADO")
                put("articulos", articulosArray)
            })
        })
    }
}

private data class ArchivoBase64(
    val base64: String,
    val tamanoBytes: Long,
    val contentType: String? = null,
    val nombreArchivo: String? = null
)

private const val MAX_EVIDENCIA_IMAGEN_DIMENSION = 1280
private const val MAX_EVIDENCIA_IMAGEN_BYTES = 1_500_000
private const val JPEG_QUALITY_INICIAL = 75
private const val JPEG_QUALITY_MINIMA = 50

private fun leerArchivoBase64(
    context: Context,
    rutaLocal: String,
    contentType: String?,
    nombreArchivo: String
): ArchivoBase64? {
    return try {
        val esImagen = esEvidenciaImagen(rutaLocal, contentType)

        val bytes = if (esImagen) {
            comprimirImagenParaEnvio(context, rutaLocal)
                ?: leerBytesArchivo(context, rutaLocal)
        } else {
            leerBytesArchivo(context, rutaLocal)
        }

        if (bytes == null || bytes.isEmpty()) {
            null
        } else {
            ArchivoBase64(
                base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                tamanoBytes = bytes.size.toLong(),
                contentType = if (esImagen) "image/jpeg" else contentType,
                nombreArchivo = if (esImagen) normalizarNombreImagenJpeg(nombreArchivo) else nombreArchivo
            )
        }
    } catch (ex: Exception) {
        Log.w(TAG_SYNC_EVIDENCIAS, "No se pudo leer/comprimir evidencia: $rutaLocal", ex)
        null
    }
}

private fun leerBytesArchivo(context: Context, rutaLocal: String): ByteArray? {
    return when {
        rutaLocal.startsWith("content://") -> {
            val uri = Uri.parse(rutaLocal)
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            }
        }

        rutaLocal.startsWith("file://") -> {
            val uri = Uri.parse(rutaLocal)
            val file = File(uri.path ?: return null)
            if (file.exists()) file.readBytes() else null
        }

        else -> {
            val file = File(rutaLocal)
            if (file.exists()) file.readBytes() else null
        }
    }
}

private fun esEvidenciaImagen(rutaLocal: String, contentType: String?): Boolean {
    val mime = contentType?.lowercase(Locale.ROOT)
    if (mime?.startsWith("image/") == true) return true

    val ruta = rutaLocal.substringBefore('?').lowercase(Locale.ROOT)
    return ruta.endsWith(".jpg") ||
            ruta.endsWith(".jpeg") ||
            ruta.endsWith(".png") ||
            ruta.endsWith(".webp")
}

private fun comprimirImagenParaEnvio(context: Context, rutaLocal: String): ByteArray? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    abrirInputStreamEvidencia(context, rutaLocal)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    val sampleSize = calcularInSampleSize(
        width = bounds.outWidth,
        height = bounds.outHeight,
        maxDimension = MAX_EVIDENCIA_IMAGEN_DIMENSION
    )

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    val bitmapOriginal = abrirInputStreamEvidencia(context, rutaLocal)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOptions)
    } ?: return null

    val bitmapFinal = escalarBitmapSiNecesario(bitmapOriginal, MAX_EVIDENCIA_IMAGEN_DIMENSION)

    val output = ByteArrayOutputStream()
    var calidad = JPEG_QUALITY_INICIAL
    var bytes: ByteArray

    do {
        output.reset()
        bitmapFinal.compress(Bitmap.CompressFormat.JPEG, calidad, output)
        bytes = output.toByteArray()
        calidad -= 10
    } while (bytes.size > MAX_EVIDENCIA_IMAGEN_BYTES && calidad >= JPEG_QUALITY_MINIMA)

    if (bitmapFinal !== bitmapOriginal) {
        bitmapFinal.recycle()
    }
    bitmapOriginal.recycle()

    Log.d(
        TAG_SYNC_EVIDENCIAS,
        "Imagen comprimida para evidencia: ${bounds.outWidth}x${bounds.outHeight}, bytes=${bytes.size}"
    )

    return bytes
}

private fun abrirInputStreamEvidencia(context: Context, rutaLocal: String): java.io.InputStream? {
    return when {
        rutaLocal.startsWith("content://") -> {
            context.contentResolver.openInputStream(Uri.parse(rutaLocal))
        }

        rutaLocal.startsWith("file://") -> {
            val uri = Uri.parse(rutaLocal)
            val file = File(uri.path ?: return null)
            if (file.exists()) file.inputStream() else null
        }

        else -> {
            val file = File(rutaLocal)
            if (file.exists()) file.inputStream() else null
        }
    }
}

private fun calcularInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var inSampleSize = 1
    var halfWidth = width / 2
    var halfHeight = height / 2

    while ((halfWidth / inSampleSize) >= maxDimension || (halfHeight / inSampleSize) >= maxDimension) {
        inSampleSize *= 2
    }

    return inSampleSize.coerceAtLeast(1)
}

private fun escalarBitmapSiNecesario(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val ladoMayor = maxOf(bitmap.width, bitmap.height)
    if (ladoMayor <= maxDimension) return bitmap

    val escala = maxDimension.toFloat() / ladoMayor.toFloat()
    val nuevoAncho = (bitmap.width * escala).toInt().coerceAtLeast(1)
    val nuevoAlto = (bitmap.height * escala).toInt().coerceAtLeast(1)

    return Bitmap.createScaledBitmap(bitmap, nuevoAncho, nuevoAlto, true)
}

private fun normalizarNombreImagenJpeg(nombreArchivo: String): String {
    val base = nombreArchivo
        .substringBeforeLast('.', nombreArchivo)
        .ifBlank { "evidencia" }

    return "$base.jpg"
}

private fun ObservacionLocal.toMobilePayload(
    incidencia: IncidenciaLocal,
    idIncidenciaRemota: String,
    idUsuario: String
): JSONObject {
    return JSONObject().apply {
        put("uuidObservacion", uuidObservacion)

        put("uuidReferencia", incidencia.uuidIncidencia)
        put("uuidIncidencia", incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idIncidenciaRemota)
        put("idReferenciaRemota", idIncidenciaRemota)
        put("tipoReferencia", "INCIDENCIA")

        put("idUsuarioGRD", idUsuario)
        put("idUsuarioRemoto", idUsuario)
        put("idUsuarioCargaGRD", idUsuario)

        put("textoObservacion", textoObservacion)
        put("observacion", textoObservacion)
        put("fechaRegistro", normalizarFechaRegistro(fechaRegistro))
    }
}

private fun SeguimientoLocal.toMobilePayload(
    incidencia: IncidenciaLocal,
    idIncidenciaRemota: String,
    idUsuario: String
): JSONObject {
    val idRemotoSeguro = seguimientoIdIncidenciaRemota(idIncidenciaRemota, incidencia)
    val codigoCasoSeguro = seguimientoCodigoCasoRemoto(incidencia)

    return JSONObject().apply {
        put("uuidSeguimiento", uuidSeguimiento)

        put("uuidIncidencia", incidencia.uuidIncidencia)
        put("uuidIncidenciaMovil", incidencia.uuidIncidencia)
        put("uuidReferencia", incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idRemotoSeguro)
        putNullable("codigoCaso", codigoCasoSeguro)

        put("idUsuarioGRD", idUsuario)

        put("fechaSeguimiento", normalizarFechaRegistro(fechaSeguimiento))
        putNullable("situacion", situacion)
        putNullable("descripcion", descripcion)
        putNullable("necesidadesPendientes", necesidadesPendientes)
        putNullable("recomendaciones", recomendaciones)
        putNullable("estado", estado)
        putNullable("observaciones", observaciones)
    }
}

private fun SeguimientoLocal.seguimientoIdIncidenciaRemota(
    idIncidenciaRemota: String,
    incidencia: IncidenciaLocal
): String = this.idIncidenciaRemota?.takeIf { it.isNotBlank() }
    ?: idIncidenciaRemota

private fun SeguimientoLocal.seguimientoCodigoCasoRemoto(
    incidencia: IncidenciaLocal
): String? = this.codigoCasoRemoto?.takeIf { it.isNotBlank() }
    ?: incidencia.codigoCasoRemoto?.takeIf { it.isNotBlank() }

private fun normalizarFechaRegistro(fechaRegistro: String): String {
    val millis = fechaRegistro.toLongOrNull()

    if (millis != null) {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(millis))
    }

    return fechaRegistro
}
private fun normalizarFechaRegistro(fechaRegistro: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(fechaRegistro))
}
private fun JSONObject.putNullable(key: String, value: Any?) {
    if (value == null) {
        put(key, JSONObject.NULL)
    } else {
        put(key, value)
    }
}
private fun parseFechaServidorMillis(fecha: String?): Long? {
    val raw = fecha
        ?.takeIf { it.isNotBlank() && it != "null" }
        ?: return null

    val patrones = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    for (patron in patrones) {
        try {
            val formatter = SimpleDateFormat(patron, Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            val parsed = formatter.parse(raw)
            if (parsed != null) return parsed.time
        } catch (_: Exception) {
            // intenta con el siguiente patrón
        }
    }

    return null
}
