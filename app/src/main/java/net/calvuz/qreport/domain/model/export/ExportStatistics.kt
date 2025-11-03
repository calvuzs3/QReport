package net.calvuz.qreport.domain.model.export

/**
 * Statistiche dettagliate dell'export
 */
data class ExportStatistics(
    /**
     * Numero di sezioni processate
     */
    val sectionsProcessed: Int = 0,

    /**
     * Numero di check items processati
     */
    val checkItemsProcessed: Int = 0,

    /**
     * Numero di foto processate
     */
    val photosProcessed: Int = 0,

    /**
     * Numero di foto esportate nella cartella
     */
    val photosExported: Int = 0,

    /**
     * Numero di parti di ricambio incluse
     */
    val sparePartsIncluded: Int = 0,

    /**
     * Tempo totale di elaborazione (millisecondi)
     */
    val processingTimeMs: Long = 0,

    /**
     * Dimensione dati processati (bytes)
     */
    val dataProcessedBytes: Long = 0,

    /**
     * Errori non fatali incontrati
     */
    val warnings: List<String> = emptyList()
) {

    /**
     * Tempo di elaborazione in formato leggibile
     */
    val processingTimeFormatted: String
        get() {
            val seconds = processingTimeMs / 1000.0
            return when {
                seconds < 60 -> "${String.format("%.1f", seconds)}s"
                else -> {
                    val minutes = (seconds / 60).toInt()
                    val remainingSeconds = (seconds % 60).toInt()
                    "${minutes}m ${remainingSeconds}s"
                }
            }
        }

    /**
     * Dimensione dati in formato leggibile
     */
    val dataProcessedFormatted: String
        get() = formatFileSize(dataProcessedBytes)

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)}KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))}MB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))}GB"
        }
    }
}