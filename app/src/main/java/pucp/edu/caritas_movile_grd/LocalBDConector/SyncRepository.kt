package pucp.edu.caritas_movile_grd.LocalBDConector

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File
import org.json.JSONObject
import android.util.Log
import pucp.edu.caritas_movile_grd.Evidencias.EvidenciaLocal
import pucp.edu.caritas_movile_grd.Incidencias.AfectadoLocal
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaLocal
import pucp.edu.caritas_movile_grd.Network.MobileSyncApi
import pucp.edu.caritas_movile_grd.Observaciones.ObservacionLocal
import pucp.edu.caritas_movile_grd.Network.MobileApiConfig
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

class SyncRepository(
    private val syncDao: SyncDao,
    private val kitDao: KitDao,
    private val simulacroDao: SimulacroDao,
    private val loginDao: LoginDao,
    private val appContext: Context,
    private val mobileSyncApi: MobileSyncApi = MobileSyncApi()
) {

    private suspend fun getUserId(): String =
        loginDao.getPerfilUsuario().firstOrNull()?.uuidUsuario
            ?: MobileApiConfig.MOBILE_SYNC_USER_ID


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
        val userId = getUserId()
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

        for (evidencia in evidenciasPendientes) {
            val incidencia = syncDao.getIncidenciaPorUuid(evidencia.uuidReferencia)

            if (incidencia == null) {
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
            simulacrosSincronizados = simulacroRepository.sincronizarSimulacrosPendientes()
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
    suspend fun descargarIncidenciasDelServidor(idUsuario: String = MobileApiConfig.MOBILE_SYNC_USER_ID) {
        val response = mobileSyncApi.obtenerIncidenciasAsignadas(idUsuario)
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
                    uuidUsuario             = idUsuario,
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

                true
            }
        } catch (ex: Exception) {
            errores.add("Error al sincronizar entrega ${entrega.uuidEntrega}: ${ex.message}")
            false
        }
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
        leerArchivoBase64(context, rutaLocal)
    } else {
        null
    }

    val urlArchivoSeguro = urlS3
        ?.takeIf { it.isNotBlank() }
        ?: rutaLocal

    return JSONObject().apply {
        put("uuidEvidencia", uuidEvidencia)

        put("uuidReferencia", incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idIncidenciaRemota)
        put("idReferenciaRemota", idIncidenciaRemota)
        put("tipoReferencia", "INCIDENCIA")

        put("idUsuarioCargaGRD", idUsuario)

        put("nombreArchivo", nombreSeguro)
        put("contentType", tipoSeguro)
        put("formatoArchivo", tipoSeguro)
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

private fun EntregaKitLocal.toMobilePayload(
    incidencia: IncidenciaLocal,
    idIncidenciaRemota: String,
    idUsuario: String
): JSONObject {
    return JSONObject().apply {
        put("uuidEntrega", uuidEntrega)

        put("uuidIncidencia", uuidIncidencia ?: incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idIncidenciaRemota)

        put("uuidAfectadoMovil", uuidAfectado)
        put("uuidPersonaAfectada", uuidAfectado)
        put("uuidPersonaAfectadaMovil", uuidAfectado)

        put("tipoAyuda", kitEntregado)
        put("descripcionAyuda", "Entrega móvil de $kitEntregado")
        put("cantidadEntregada", cantidad)
        put("fechaEntrega", normalizarFechaRegistro(fechaEntrega))

        put("idUsuarioResponsableGRD", idUsuario)
    }
}

private data class ArchivoBase64(
    val base64: String,
    val tamanoBytes: Long
)

private fun leerArchivoBase64(context: Context, rutaLocal: String): ArchivoBase64? {
    return try {
        val bytes = if (
            rutaLocal.startsWith("content://") ||
            rutaLocal.startsWith("file://")
        ) {
            val uri = Uri.parse(rutaLocal)
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            }
        } else {
            val file = File(rutaLocal)
            if (file.exists()) file.readBytes() else null
        }

        if (bytes == null || bytes.isEmpty()) {
            null
        } else {
            ArchivoBase64(
                base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                tamanoBytes = bytes.size.toLong()
            )
        }
    } catch (_: Exception) {
        null
    }
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
    return JSONObject().apply {
        put("uuidSeguimiento", uuidSeguimiento)

        put("uuidIncidencia", incidencia.uuidIncidencia)
        put("uuidIncidenciaMovil", incidencia.uuidIncidencia)
        put("uuidReferencia", incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idIncidenciaRemota)
        putNullable("codigoCaso", incidencia.codigoCasoRemoto)

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
