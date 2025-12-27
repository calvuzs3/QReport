package net.calvuz.qreport.domain.model.share

/**
 * Modalità di condivisione backup
 */
enum class ShareMode {
    SINGLE_FILE,        // Solo il file principale del backup
    COMPLETE_BACKUP,    // Tutti i file del backup
    COMPRESSED          // Backup compresso in un singolo file
}