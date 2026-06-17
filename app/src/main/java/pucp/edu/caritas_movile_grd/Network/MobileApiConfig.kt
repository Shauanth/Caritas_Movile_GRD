package pucp.edu.caritas_movile_grd.Network

object MobileApiConfig {
    const val BASE_URL = "http://10.0.2.2:3000"
    const val MOBILE_SYNC_USER_ID = "d635589f-364b-49db-b0b3-2f1f411bae9b"

    /**
     * Clave que el backend exige en el header `x-mobile-sync-key` (variable de
     * entorno MOBILE_SYNC_API_KEY de la web). Debe coincidir EXACTAMENTE con la
     * del servidor o las peticiones a las rutas /api/mobile responderán 401.
     *
     * Si se deja en blanco, el cliente no envía el header (útil solo si el
     * servidor corre sin MOBILE_SYNC_API_KEY configurada).
     */
    const val MOBILE_SYNC_API_KEY = ""
}
