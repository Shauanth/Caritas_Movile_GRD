package pucp.edu.caritas_movile_grd.LocalBDConector

import org.json.JSONObject
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

data class SyncResult(
    val incidenciasSincronizadas: Int,
    val afectadosSincronizados: Int,
    val evidenciasSincronizadas: Int,
    val observacionesSincronizadas: Int,
    val seguimientosSincronizados: Int,
    val errores: List<String>
)

class SyncRepository(
    private val syncDao: SyncDao,
    private val mobileSyncApi: MobileSyncApi = MobileSyncApi()
) {

    suspend fun sincronizarPendientes(): SyncResult {
        var incidenciasSincronizadas = 0
        var afectadosSincronizados = 0
        var evidenciasSincronizadas = 0
        var observacionesSincronizadas = 0
        var seguimientosSincronizados = 0
        val errores = mutableListOf<String>()

        val incidenciasPendientes = syncDao.getIncidenciasNuevasParaSincronizar()

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

        // 2. Segunda pasada: sincronizar afectados pendientes aunque la incidencia ya esté sincronizada
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
                errores = errores
            )

            if (sincronizada) {
                evidenciasSincronizadas++
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
                errores = errores
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
                errores = errores
            )

            if (sincronizado) {
                seguimientosSincronizados++
            }
        }

        return SyncResult(
            incidenciasSincronizadas = incidenciasSincronizadas,
            afectadosSincronizados = afectadosSincronizados,
            evidenciasSincronizadas = evidenciasSincronizadas,
            observacionesSincronizadas = observacionesSincronizadas,
            seguimientosSincronizados = seguimientosSincronizados,
            errores = errores
        )
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
        errores: MutableList<String>
    ): Boolean {
        return try {
            val responseEvidencia = mobileSyncApi.sincronizarEvidencia(
                evidencia.toMobilePayload(
                    incidencia = incidencia,
                    idIncidenciaRemota = idIncidenciaRemota
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
                    .optString("urlArchivo")
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
        errores: MutableList<String>
    ): Boolean {
        return try {
            val responseObservacion = mobileSyncApi.sincronizarObservacion(
                observacion.toMobilePayload(
                    incidencia = incidencia,
                    idIncidenciaRemota = idIncidenciaRemota
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
        errores: MutableList<String>
    ): Boolean {
        return try {
            val responseSeguimiento = mobileSyncApi.sincronizarSeguimiento(
                seguimiento.toMobilePayload(
                    incidencia = incidencia,
                    idIncidenciaRemota = idIncidenciaRemota
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
}

private fun IncidenciaLocal.toMobilePayload(): JSONObject {
    return JSONObject().apply {
        put("uuidIncidencia", uuidIncidencia)
        put("uuidUsuario", uuidUsuario)
        put("idParroquia", idParroquia)
        put("idCatalogoTipo", idCatalogoTipo)
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
    idIncidenciaRemota: String
): JSONObject {
    val nombreSeguro = nombreArchivo
        ?.takeIf { it.isNotBlank() }
        ?: rutaLocal.substringAfterLast('/').substringBefore('?').ifBlank {
            "evidencia-movil-${uuidEvidencia}.bin"
        }

    val tipoSeguro = contentType
        ?.takeIf { it.isNotBlank() }
        ?: "application/octet-stream"

    val urlArchivoSeguro = urlS3
        ?.takeIf { it.isNotBlank() }
        ?: rutaLocal

    return JSONObject().apply {
        put("uuidEvidencia", uuidEvidencia)

        put("uuidReferencia", incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idIncidenciaRemota)
        put("idReferenciaRemota", idIncidenciaRemota)
        put("tipoReferencia", "INCIDENCIA")

        put("nombreArchivo", nombreSeguro)
        put("contentType", tipoSeguro)
        put("formatoArchivo", tipoSeguro)
        putNullable("tamanoArchivo", tamanoArchivo)
        putNullable("descripcion", descripcion)

        // MVP: registra la ruta/URI local como metadata.
        // Luego se puede reemplazar por base64 para subir realmente a S3.
        put("urlArchivo", urlArchivoSeguro)
    }
}


private fun ObservacionLocal.toMobilePayload(
    incidencia: IncidenciaLocal,
    idIncidenciaRemota: String
): JSONObject {
    return JSONObject().apply {
        put("uuidObservacion", uuidObservacion)

        put("uuidReferencia", incidencia.uuidIncidencia)
        put("uuidIncidencia", incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idIncidenciaRemota)
        put("idReferenciaRemota", idIncidenciaRemota)
        put("tipoReferencia", "INCIDENCIA")

        // Usuario que registra la observación.
        // Se envían varios alias para ser compatible con el endpoint backend.
        put("idUsuarioGRD", MobileApiConfig.USUARIO_GRD_ID)
        put("idUsuarioRemoto", MobileApiConfig.USUARIO_GRD_ID)
        put("idUsuarioCargaGRD", MobileApiConfig.USUARIO_GRD_ID)

        put("textoObservacion", textoObservacion)
        put("observacion", textoObservacion)
        put("fechaRegistro", normalizarFechaRegistro(fechaRegistro))
    }
}

private fun SeguimientoLocal.toMobilePayload(
    incidencia: IncidenciaLocal,
    idIncidenciaRemota: String
): JSONObject {
    return JSONObject().apply {
        put("uuidSeguimiento", uuidSeguimiento)

        put("uuidIncidencia", incidencia.uuidIncidencia)
        put("uuidIncidenciaMovil", incidencia.uuidIncidencia)
        put("uuidReferencia", incidencia.uuidIncidencia)
        put("idIncidenciaRemota", idIncidenciaRemota)
        putNullable("codigoCaso", incidencia.codigoCasoRemoto)

        put("idUsuarioGRD", MobileApiConfig.USUARIO_GRD_ID)

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
private fun JSONObject.putNullable(key: String, value: Any?) {
    if (value == null) {
        put(key, JSONObject.NULL)
    } else {
        put(key, value)
    }
}