package net.calvuz.qreport.domain.model.export

import java.time.LocalDateTime

/**
 * Risultato di un export multi-formato
 * Coordina i risultati di tutti i formati richiesti
 */
data class MultiFormatExportResult(
    /**
     * Risultato export Word (null se non richiesto)
     */
    val wordResult: ExportResult.Success? = null,

    /**
     * Risultato export Text (null se non richiesto)
     */
    val textResult: ExportResult.Success? = null,

    /**
     * Risultato export cartella foto (null se non richiesta)
     */
    val photoFolderResult: ExportResult.Success? = null,

    /**
     * Directory principale export (se createTimestampedDirectory = true)
     */
    val exportDirectory: String? = null,

    /**
     * Timestamp dell'export
     */
    val generatedAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Statistiche aggregate
     */
    val statistics: ExportStatistics
) {

    /**
     * Lista di tutti i risultati di successo
     */
    val successResults: List<ExportResult.Success>
        get() = listOfNotNull(wordResult, textResult, photoFolderResult)

    /**
     * Verifica se almeno un formato è stato esportato con successo
     */
    val hasAnySuccess: Boolean
        get() = successResults.isNotEmpty()

    /**
     * Verifica se tutti i formati richiesti sono stati esportati con successo
     */
    fun isCompleteSuccess(requestedFormats: Set<ExportFormat>): Boolean {
        val exportedFormats = successResults.map { it.format }.toSet()
        return exportedFormats.containsAll(requestedFormats)
    }

    /**
     * Ottieni dimensione totale di tutti i file esportati
     */
    val totalFileSize: Long
        get() = successResults.sumOf { it.fileSize }

    /**
     * Ottieni numero totale di file generati
     */
    val totalFileCount: Int
        get() = successResults.size + statistics.photosExported
}