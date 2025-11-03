package net.calvuz.qreport.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.domain.model.export.*
import net.calvuz.qreport.domain.model.photo.PhotoExportResult
import java.io.File

/**
 * Repository per operazioni di export checkup
 * Supporta export multi-formato e coordinamento tra diversi generatori
 */
interface ExportRepository {

    // ===== EXPORT MULTI-FORMATO =====

    /**
     * Genera export completo multi-formato
     * Coordina tutti i generatori per creare Word, Text e/o cartella FOTO
     *
     * @param exportData Dati completi del checkup
     * @param options Configurazione export con formati richiesti
     * @return Flow con progresso e risultato finale
     */
    suspend fun generateCompleteExport(
        exportData: ExportData,
        options: ExportOptions
    ): Flow<MultiFormatExportResult>

    /**
     * Versione sincrona per export completo
     * Utile quando non serve monitoraggio progresso
     */
    suspend fun generateCompleteExportSync(
        exportData: ExportData,
        options: ExportOptions
    ): MultiFormatExportResult

    // ===== EXPORT SINGOLI FORMATI =====

    /**
     * Genera solo documento Word (.docx)
     * Mantiene compatibilità con implementazione esistente
     */
    suspend fun generateWordReport(
        exportData: ExportData,
        options: ExportOptions
    ): ExportResult

    /**
     * Genera solo report testuale (.txt)
     */
    suspend fun generateTextReport(
        exportData: ExportData,
        options: ExportOptions
    ): ExportResult

    /**
     * Genera solo cartella foto con foto organizzate
     */
    suspend fun generatePhotoFolder(
        exportData: ExportData,
        targetDirectory: File,
        options: ExportOptions
    ): PhotoExportResult

    // ===== UTILITY METHODS =====

    /**
     * Valida i dati prima dell'export
     * Verifica che tutti i dati necessari siano presenti
     */
    suspend fun validateExportData(exportData: ExportData): List<String>

    /**
     * Stima dimensioni e tempo per l'export
     * Utile per validazione spazio disco e UI progress
     */
    suspend fun estimateExportSize(
        exportData: ExportData,
        options: ExportOptions
    ): ExportEstimation

    /**
     * Ottieni directory di default per export
     */
    fun getDefaultExportDirectory(): File

    /**
     * Crea directory con timestamp per export organizzato
     */
    suspend fun createTimestampedExportDirectory(
        baseDirectory: File,
        checkupData: ExportData
    ): File

    /**
     * Pulisce export temporanei e file obsoleti
     */
    suspend fun cleanupOldExports(olderThanDays: Int = 30): Int
}

/**
 * Stima per operazione di export
 */
data class ExportEstimation(
    /**
     * Dimensione stimata totale (bytes)
     */
    val estimatedSizeBytes: Long,

    /**
     * Tempo stimato di elaborazione (millisecondi)
     */
    val estimatedTimeMs: Long,

    /**
     * Stime per singoli formati
     */
    val formatEstimations: Map<ExportFormat, FormatEstimation>,

    /**
     * Spazio libero richiesto (con margine di sicurezza)
     */
    val requiredFreeSpaceBytes: Long = estimatedSizeBytes * 2,

    /**
     * Warning se dimensioni eccessive
     */
    val warnings: List<String> = emptyList()
) {

    /**
     * Dimensione stimata in formato leggibile
     */
    val estimatedSizeFormatted: String
        get() = formatFileSize(estimatedSizeBytes)

    /**
     * Tempo stimato in formato leggibile
     */
    val estimatedTimeFormatted: String
        get() = formatDuration(estimatedTimeMs)

    /**
     * Verifica se export è fattibile con spazio disponibile
     */
    fun isFeasible(availableSpaceBytes: Long): Boolean {
        return availableSpaceBytes >= requiredFreeSpaceBytes
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)}KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))}MB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))}GB"
        }
    }

    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}

/**
 * Stima per singolo formato
 */
data class FormatEstimation(
    val format: ExportFormat,
    val estimatedSizeBytes: Long,
    val estimatedTimeMs: Long,
    val fileCount: Int = 1,
    val complexity: ExportComplexity = ExportComplexity.MEDIUM
)

/**
 * Complessità dell'export per stime accurate
 */
enum class ExportComplexity {
    /**
     * Export semplice - poche foto, dati standard
     */
    LOW,

    /**
     * Export standard - quantità normale foto e dati
     */
    MEDIUM,

    /**
     * Export complesso - molte foto, dati estesi
     */
    HIGH,

    /**
     * Export molto complesso - centinaia di foto, processamento pesante
     */
    VERY_HIGH
}