package pucp.edu.caritas_movile_grd.LocalBDConector

enum class EstadoSync {
    NUEVO,                  // Creado offline, pendiente de hacer POST
    EDITADO,                // Modificado offline, pendiente de hacer PUT/PATCH
    PENDIENTE_SUBIDA,       // Para archivos (fotos) que esperan subir a S3
    SINCRONIZADO,           // Todo coincide con el backend
    PENDIENTE_ELIMINACION   // Marcado para borrar en el servidor al próximo sync
}