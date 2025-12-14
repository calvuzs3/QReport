package net.calvuz.qreport.domain.model.photo

/**
 * Tipi di errore per le operazioni foto
 */
enum class PhotoErrorType {
    CAMERA_NOT_AVAILABLE,
    PERMISSION_DENIED,
    STORAGE_FULL,
    CAPTURE_FAILED,
    PROCESSING_ERROR,
    FILE_NOT_FOUND,
    INVALID_FILE_FORMAT,
    COMPRESSION_FAILED,
    THUMBNAIL_GENERATION_FAILED,

    CAPTURE_ERROR,      // Errori durante acquisizione foto
    IMPORT_ERROR,       // Errori durante import foto
    STORAGE_ERROR,      // Errori filesystem/storage
    REPOSITORY_ERROR,   // Errori database
    VALIDATION_ERROR,   // Errori validazione
    PERMISSION_ERROR,   // Errori permessi
    NETWORK_ERROR,      // Errori network (per future sync)
    UNKNOWN_ERROR       // Errori non classificati
}