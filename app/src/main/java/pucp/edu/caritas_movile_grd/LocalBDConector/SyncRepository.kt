package pucp.edu.caritas_movile_grd.LocalBDConector

import org.json.JSONObject
import pucp.edu.caritas_movile_grd.Incidencias.AfectadoLocal
import pucp.edu.caritas_movile_grd.Incidencias.IncidenciaLocal
import pucp.edu.caritas_movile_grd.Network.MobileSyncApi

data class SyncResult(
    val incidenciasSincronizadas: Int,
    val afectadosSincronizados: Int,
    val errores: List<String>
)

class SyncRepository(
    private val syncDao: SyncDao,
    private val mobileSyncApi: MobileSyncApi = MobileSyncApi()
) {

    suspend fun sincronizarPendientes(): SyncResult {
        var incidenciasSincronizadas = 0
        var afectadosSincronizados = 0
        val errores = mutableListOf<String>()

        val incidenciasPendientes = syncDao.getIncidenciasNuevasParaSincronizar()

        for (incidencia in incidenciasPendientes) {
            try {
                val responseIncidencia = mobileSyncApi.sincronizarIncidencia(
                    incidencia.toMobilePayload()
                )

                val idIncidenciaRemota = responseIncidencia.optString(
                    "idIncidenciaRemota",
                    responseIncidencia.optString("idServidor", "")
                )

                val codigoCaso = responseIncidencia.optString("codigoCaso", null)

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
                    try {
                        val responseAfectado = mobileSyncApi.sincronizarAfectado(
                            afectado.toMobilePayload(
                                incidencia = incidencia,
                                idIncidenciaRemota = idIncidenciaRemota,
                                codigoCasoRemoto = codigoCaso
                            )
                        )

                        val idAfectadoRemoto = responseAfectado.optString(
                            "idPersonaAfectadaRemota",
                            responseAfectado.optString("idServidor", "")
                        )

                        if (idAfectadoRemoto.isBlank()) {
                            errores.add("El afectado ${afectado.uuidAfectado} no devolvió id remoto.")
                            continue
                        }

                        syncDao.marcarAfectadoComoSincronizado(
                            uuidAfectado = afectado.uuidAfectado,
                            idRemoto = idAfectadoRemoto
                        )

                        afectadosSincronizados++
                    } catch (ex: Exception) {
                        errores.add("Error al sincronizar afectado ${afectado.uuidAfectado}: ${ex.message}")
                    }
                }
            } catch (ex: Exception) {
                errores.add("Error al sincronizar incidencia ${incidencia.uuidIncidencia}: ${ex.message}")
            }
        }

        return SyncResult(
            incidenciasSincronizadas = incidenciasSincronizadas,
            afectadosSincronizados = afectadosSincronizados,
            errores = errores
        )
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

    val codigoGrupo = familiaId?.takeIf { it.isNotBlank() }
        ?: "MOVIL-${codigoCasoRemoto ?: incidencia.uuidIncidencia}"

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

private fun JSONObject.putNullable(key: String, value: Any?) {
    if (value == null) {
        put(key, JSONObject.NULL)
    } else {
        put(key, value)
    }
}