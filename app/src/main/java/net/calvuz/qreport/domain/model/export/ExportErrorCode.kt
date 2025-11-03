package net.calvuz.qreport.domain.model.export

/**
 * Codici di errore per operazioni di export
 */
enum class ExportErrorCode {
    /**
     * Spazio di archiviazione insufficiente
     */
    INSUFFICIENT_STORAGE,

    /**
     * Permessi di scrittura negati
     */
    PERMISSION_DENIED,

    /**
     * Template non trovato o corrotto
     */
    TEMPLATE_NOT_FOUND,

    /**
     * Errore nel processamento immagini
     */
    IMAGE_PROCESSING_ERROR,

    /**
     * Errore nella generazione documento
     */
    DOCUMENT_GENERATION_ERROR,

    /**
     * Errore nella creazione cartella foto
     */
    PHOTO_FOLDER_ERROR,

    /**
     * Errore nella generazione testo
     */
    TEXT_GENERATION_ERROR,

    /**
     * Dati del checkup incompleti o corrotti
     */
    INVALID_DATA,

    /**
     * Timeout nell'elaborazione
     */
    PROCESSING_TIMEOUT,

    /**
     * Errore di rete (per future implementazioni cloud)
     */
    NETWORK_ERROR,

    /**
     * Errore generico del sistema
     */
    SYSTEM_ERROR
}