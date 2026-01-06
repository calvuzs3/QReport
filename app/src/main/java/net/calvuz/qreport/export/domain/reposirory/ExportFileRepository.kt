package net.calvuz.qreport.export.domain.reposirory

import kotlinx.datetime.Instant
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.domain.model.CheckItem
import net.calvuz.qreport.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.domain.model.CheckUpProgress
import net.calvuz.qreport.checkup.domain.model.CheckUpSingleStatistics
import net.calvuz.qreport.checkup.domain.model.module.ModuleType
import net.calvuz.qreport.export.domain.model.ExportErrorCode
import java.time.LocalDateTime
import kotlin.text.format

/**
 * Repository per operazioni file specifiche della feature Export
 *
 * Responsabilità:
 * - Gestione directory exports
 * - Creazione e management file esportati
 * - Cleanup e maintenance exports
 * - Validation e metadata exports
 *
 * SI APPOGGIA a CoreFileRepository per operazioni base
 */
interface ExportFileRepository {

    suspend fun getMainExportFile(
        exportDirectory: String,
        format: ExportFormat
    ): QrResult<String, QrError>

    suspend fun saveWordDocument(
        documentBytes: ByteArray,
        fileName: String,
        directory: String
    ): QrResult<String, QrError>


    // ===== EXPORT FILE MANAGEMENT =====

    /**
     * Create export file with proper naming and extension
     */
    suspend fun createExportFile(
        directory: String,
        fileName: String,
        format: ExportFormat
    ): QrResult<String, QrError>

    /**
     * Save text content to export file
     */
    suspend fun saveExportContent(
        filePath: String,
        content: String
    ): QrResult<Unit, QrError>

    /**
     * Save binary content to export file
     */
    suspend fun saveExportBinary(
        filePath: String,
        data: ByteArray
    ): QrResult<Unit, QrError>

    /**
     * Copy file to export directory with new name
     */
    suspend fun copyFileToExport(
        sourceFilePath: String,
        exportDirectory: String,
        targetFileName: String
    ): QrResult<String, QrError>

    /**
     * Copy multiple files to export directory
     */
    suspend fun copyFilesToExport(
        sourceFilePaths: List<String>,
        exportDirectory: String,
        preserveNames: Boolean = true
    ): QrResult<List<String>, QrError>

    // ===== EXPORT SUBDIRECTORIES =====

    /**
     * Create photos subdirectory in export
     * Creates: export_dir/photos/
     */
    suspend fun createPhotosSubdirectory(exportDirectory: String): QrResult<String, QrError>

    /**
     * Create attachments subdirectory in export
     * Creates: export_dir/attachments/
     */
    suspend fun createAttachmentsSubdirectory(exportDirectory: String): QrResult<String, QrError>

    /**
     * Create reports subdirectory in export
     * Creates: export_dir/reports/
     */
    suspend fun createReportsSubdirectory(exportDirectory: String): QrResult<String, QrError>

    // ===== EXPORT CLEANUP =====

    /**
     * Delete old export directories older than specified days
     */
    suspend fun cleanupOldExports(olderThanDays: Int = 30): QrResult<Int, QrError>

    /**
     * Delete specific export directory completely
     */
//    suspend fun deleteExport(checkupId: String): QrResult<Unit, QrError>

    /**
     * Delete temporary export files
     */
    suspend fun cleanupTemporaryExports(): QrResult<Int, QrError>

    /**
     * Get total size of exports directory
     */
    suspend fun getExportsDirectorySize(): QrResult<Long, QrError>

    // ===== EXPORT VALIDATION =====

    suspend fun validateExportDirectory(exportPath: String): QrResult<Boolean, QrError>
//    suspend fun exportExists(checkupId: String): QrResult<Boolean, QrError>
    suspend fun getExportSize(exportDirectory: String): QrResult<Long, QrError>
    suspend fun validateExportFile(
        filePath: String,
        format: ExportFormat
    ): QrResult<Boolean, QrError>

    // ===== EXPORT LISTING & METADATA =====

    suspend fun listExports(): QrResult<List<ExportInfo>, QrError>
//    suspend fun getExportInfo(checkupId: String): QrResult<ExportInfo?, QrError>
    suspend fun listExportsNewerThan(timestampMs: Long): QrResult<List<ExportInfo>, QrError>
    suspend fun getExportFiles(exportDirectory: String): QrResult<List<ExportFileInfo>, QrError>

    // ===== EXPORT FILE SHARING & OPENING =====

    /**
     * Open exported file with appropriate default application
     * Convenience method that automatically detects MIME type from ExportFormat
     *
     * @param filePath Path to the exported file
     * @param format Export format to determine appropriate app
     * @return Success if file opened, Error with details if failed
     */
    suspend fun openFileWith(
        filePath: String,
        format: ExportFormat
    ): QrResult<Unit, QrError>

    /**
     * Share exported file using Android sharing system
     * Convenience method with export-specific sharing configuration
     *
     * @param filePath Path to the exported file
     * @param format Export format to determine sharing options
     * @return Success if sharing started, Error with details if failed
     */
    suspend fun shareFileWith(
        filePath: String,
        format: ExportFormat
    ): QrResult<Unit, QrError>

    // ===== NAMING =====

    /**
     * Generate base name without extension (used for both files and directories)
     *
     * @param checkupId string included in the filename
     * @param clientName string included in the filename
     * @param includeTimestamp Boolean true for a timestamp at the beginning of the file name
     * @return String following the rule "{YYYYMMDD}_Checkup_{ClientName}_{CheckupId.take(8)}"
     */
    fun generateExportBaseName(
        checkupId: String,
        clientName: String,
        includeTimestamp: Boolean = true
    ): String

    /**
     * Get export filename
     * @param checkupId string included in the filename
     * @param clientName string included in the filename
     * @param format Export Format
     * @param includeTimestamp Boolean true for a timestamp at the beginning of the file name
     * @return String following the rule "{YYYYMMDD}_Checkup_{ClientName}_{CheckupId.take(8)}"
     */
    fun generateExportFileName(
        checkupId: String,
        clientName: String,
        format: ExportFormat,
        includeTimestamp: Boolean = true
    ): String


    /**
     * Get export directory name
     * @param checkupId string
     * @param clientName string
     * @return String following the rule "{YYYYMMDD}_Checkup_{ClientName}_{CheckupId.take(8)}"
     */
    suspend fun generateExportDirectoryName(
        checkupId: String,
        clientName: String,
        includeTimestamp: Boolean = true
    ): String

    /**
     * Get main exports directory path
     */
    suspend fun getExportsDirectory(): QrResult<String, QrError>

    /**
     * Create export directory for specific checkup
     * Creates: exports/checkup_{checkupId}/
     */
    suspend fun getExportSubDirectory(
        checkupId: String,
        clientName: String,
//        format: ExportFormat,
        includeTimestamp: Boolean = true
    ): QrResult<String, QrError.FileError>

    /**
     * Get export file extension for format
     */
    fun getFileExtension(format: ExportFormat): String

    /**
     * Check available storage space for export
     */
    suspend fun checkStorageSpace(estimatedSizeMB: Int): QrResult<Boolean, QrError>

    /**
     * Create export manifest file
     */
//    suspend fun createExportManifest(
//        exportDirectory: String,
//        exportInfo: ExportInfo
//    ): QrResult<String, QrError>
}

/**
 * Formati di export supportati dal sistema QReport
 */
enum class ExportFormat(val extension: String, val mimeType: String) {
    /**
     * Documento Word (.docx) con foto integrate
     * - Report professionale per cliente
     * - Foto embedded nel documento
     * - Formattazione corporate
     */
    WORD(
        extension = ".docx",
        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ),

    /**
     * Report testuale ASCII (.txt)
     * - Formato universalmente leggibile
     * - Riepilogo completo del checkup
     * - Riferimenti alle foto esterne
     */
    TEXT(
        extension = ".txt",
        mimeType = "text/plain",
    ),

    /**
     * Cartella con foto originali
     * - Foto in qualità originale
     * - Naming strutturato per sezione/item
     * - Separato dai documenti per gestione flessibile
     */
    PHOTO_FOLDER(
        extension = ".zip",
        mimeType = "application/zip",
    ),


    /**
     * Tutti i formati insieme
     */
    COMBINED_PACKAGE(
        extension = ".zip",
        mimeType = "application/zip",
    );

    // Helper

    companion object {
        fun fromExtension(extension: String): ExportFormat? {
            return entries.find { it.extension.equals(extension, ignoreCase = true) }
        }
    }
}

/**
 * Modello principale per dati di export - VERSIONE CORRETTA
 *
 * AGGIORNATO per utilizzare SOLO i modelli domain esistenti:
 * - CheckUp (contiene già header con ClientInfo, TechnicianInfo, IslandInfo)
 * - CheckItem (contiene già Photo)
 * - CheckUpSingleStatistics e CheckUpProgress per metadati
 * - Raggruppamento per ModuleType invece di "sections" artificiali
 */
data class ExportData(
    /**
     * Checkup principale - contiene già TUTTO:
     * - header (ClientInfo, TechnicianInfo, IslandInfo)
     * - checkItems con Photo
     * - spareParts
     * - date e status
     */
    val checkup: CheckUp,

    /**
     * Items raggruppati per modulo (basato sui modelli reali)
     * Sostituisce le "sections" artificiali
     */
    val itemsByModule: Map<ModuleType, List<CheckItem>>,

    /**
     * Statistiche del checkup (modello reale esistente)
     */
    val statistics: CheckUpSingleStatistics,

    /**
     * Progresso compilazione (modello reale esistente)
     */
    val progress: CheckUpProgress,

    /**
     * Metadati per l'export (solo info tecniche di generazione)
     */
    val exportMetadata: ExportTechnicalMetadata
)

/**
 * Opzioni di configurazione per l'export del checkup
 * Supporta export multi-formato e configurazioni granulari
 */
data class ExportOptions(
    // ===== FORMATI EXPORT =====

    /**
     * Set di formati da generare nell'export
     * Default: tutti i formati (Word + Text + Photo Folder)
     */
    val exportFormats: Set<ExportFormat> = setOf(
        ExportFormat.WORD,
        ExportFormat.TEXT,
        ExportFormat.PHOTO_FOLDER
    ),

    // ===== OPZIONI ESISTENTI =====

    /**
     * Include foto nei documenti Word/Text
     * - Word: foto embedded nel documento
     * - Text: riferimenti alle foto nella cartella
     */
    val includePhotos: Boolean = true,

    /**
     * Include note testuali nei report
     */
    val includeNotes: Boolean = true,

    /**
     * Livello di compressione per foto nel Word
     */
    val photoQuality: PhotoQuality = PhotoQuality.OPTIMIZED,

    /**
     * Larghezza massima foto nel Word (pixel)
     */
    val photoMaxWidth: Int = 800,

    /**
     * Template personalizzato (opzionale)
     */
    val customTemplate: ReportTemplate? = null,

    // ===== NUOVE OPZIONI =====

    /**
     * Strategia di naming per foto nella cartella FOTO
     */
    val photoNamingStrategy: PhotoNamingStrategy = PhotoNamingStrategy.STRUCTURED,

    /**
     * Crea directory export con timestamp
     * - true: Export_Checkup_20251022_1430/
     * - false: File diretti nella directory target
     */
    val createTimestampedDirectory: Boolean = true,

    /**
     * Qualità foto per cartella FOTO
     * - ORIGINAL: Copia foto originali (massima qualità)
     * - OPTIMIZED: Ottimizza dimensioni mantenendo qualità
     * - COMPRESSED: Comprime per ridurre spazio
     */
    val photoFolderQuality: PhotoQuality = PhotoQuality.ORIGINAL,

    /**
     * Include metadati EXIF nelle foto esportate
     */
    val preserveExifData: Boolean = true,

    /**
     * Aggiungi watermark alle foto esportate
     */
    val addWatermark: Boolean = false,

    /**
     * Testo watermark (se abilitato)
     */
    val watermarkText: String = "QReport",

    /**
     * Genera file indice nella cartella FOTO
     * Contiene mapping foto -> check item
     */
    val generatePhotoIndex: Boolean = true
) {

    companion object {
        /**
         * Configurazione per solo Word (comportamento precedente)
         */
        fun wordOnly() = ExportOptions(
            exportFormats = setOf(ExportFormat.WORD),
            createTimestampedDirectory = false
        )

        /**
         * Configurazione completa (default)
         */
        fun complete() = ExportOptions()

        /**
         * Configurazione minimal (Word + Text, no foto)
         */
        fun textOnly() = ExportOptions(
            exportFormats = setOf(ExportFormat.WORD, ExportFormat.TEXT),
            includePhotos = false,
            createTimestampedDirectory = false
        )

        /**
         * Configurazione archivio (solo foto + indice)
         */
        fun photoArchive() = ExportOptions(
            exportFormats = setOf(ExportFormat.PHOTO_FOLDER),
            photoFolderQuality = PhotoQuality.ORIGINAL,
            preserveExifData = true,
            generatePhotoIndex = true
        )
    }

    // ===== VALIDATION =====

    /**
     * Valida la configurazione export
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (exportFormats.isEmpty()) {
            errors.add("Almeno un formato export deve essere specificato")
        }

        if (photoMaxWidth <= 0) {
            errors.add("photoMaxWidth deve essere > 0")
        }

        if (includePhotos && ExportFormat.PHOTO_FOLDER !in exportFormats) {
            // Warning: foto nei documenti ma non cartella separata
        }

        if (addWatermark && watermarkText.isBlank()) {
            errors.add("watermarkText richiesto se addWatermark = true")
        }

        return errors
    }

    /**
     * Verifica se il formato è abilitato
     */
    fun isFormatEnabled(format: ExportFormat): Boolean = format in exportFormats

    /**
     * Verifica se foto sono richieste in qualche formato
     */
    fun requiresPhotos(): Boolean = includePhotos || isFormatEnabled(ExportFormat.PHOTO_FOLDER)
}

/**
 * Risultato di un'operazione di export singolo formato
 */
sealed class ExportResult {

    /**
     * Export completato con successo
     */
    data class Success(
        val filePath: String,
        val fileName: String,
        val fileSize: Long,
        val format: ExportFormat,
        val generatedAt: LocalDateTime = LocalDateTime.now()
    ) : ExportResult() {

        /**
         * Verifica se il file è una directory
         */
        val isDirectory: Boolean
            get() = format == ExportFormat.PHOTO_FOLDER || format == ExportFormat.COMBINED_PACKAGE
    }

    /**
     * Export fallito con errore
     */
    data class Error(
        val exception: Throwable,
        val errorCode: ExportErrorCode,
        val format: ExportFormat? = null,
        val partialResults: List<Success> = emptyList()
    ) : ExportResult()

    /**
     * Export in corso (per UI reactive)
     */
    data class Loading(
        val format: ExportFormat,
        val progress: Float = 0f,
        val statusMessage: String = ""
    ) : ExportResult()
}

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

/**
 * Metadati TECNICI per l'export (non business domain)
 * Contiene solo informazioni tecniche per la generazione del report
 */
data class ExportTechnicalMetadata(
    /**
     * Timestamp di generazione del report
     */
    val generatedAt: Instant,

    /**
     * Versione del template/generatore utilizzato
     */
    val templateVersion: String,

    /**
     * Formato di output richiesto
     */
    val exportFormat: ExportFormat,

    /**
     * Configurazioni tecniche per l'export
     */
    val exportOptions: ExportOptions
)

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

/**
 * Template semplificato per la formattazione del report
 * Mantiene solo le configurazioni essenziali
 */
data class ReportTemplate(
    /**
     * Nome identificativo del template
     */
    val name: String,

    /**
     * Colore principale per header (formato HEX senza #)
     */
    val headerColor: String = "2E7D32", // Verde corporate

    /**
     * Path al logo aziendale
     */
    val logoPath: String = "",

    /**
     * Testo per footer del documento
     */
    val footerText: String = "Report generato da QReport",

    /**
     * Font family
     */
    val fontFamily: String = "Calibri",

    /**
     * Dimensione font base
     */
    val baseFontSize: Int = 11
) {
    companion object {
        /**
         * Template di default aziendale
         */
        fun default() = ReportTemplate(
            name = "QReport Minimal",
            headerColor = "333333",
            logoPath = "",
            footerText = "Report generato da QReport v1.0"
        )

        /**
         * Template minimale per test
         */
        fun minimal() = ReportTemplate(
            name = "Standard",
            headerColor = "2E7D32",
            logoPath = "/assets/qreport_logo.png",
            footerText = "QReport"
        )
    }
}

/**
 * Export information metadata
 */
data class ExportInfo(
    val checkupId: String,
    val exportDirectory: String,
    val createdAt: Long,
    val format: ExportFormat,
    val sizeBytes: Long,
    val fileCount: Int,
    val hasPhotos: Boolean,
    val hasAttachments: Boolean,
    val status: ExportStatus = ExportStatus.COMPLETED
)

/**
 * Export file information
 */
data class ExportFileInfo(
    val fileName: String,
    val filePath: String,
    val format: ExportFormat,
    val sizeBytes: Long,
    val createdAt: Long,
    val isMainReport: Boolean = false
)

/**
 * Export status
 */
enum class ExportStatus {
    IN_PROGRESS,    // Export in corso
    COMPLETED,      // Export completato con successo
    FAILED,         // Export fallito
    CORRUPTED       // Export danneggiato
}

/**
 * Strategie di naming per le foto esportate
 */
enum class PhotoNamingStrategy {
    /**
     * Naming strutturato: 01_Sezione_Check001_descrizione.jpg
     * - Migliore organizzazione
     * - Facile identificazione
     * - Ordinamento logico
     */
    STRUCTURED,

    /**
     * Naming sequenziale: foto_001.jpg, foto_002.jpg
     * - Semplice e lineare
     * - Compatibilità universale
     * - Meno informativo
     */
    SEQUENTIAL,

    /**
     * Naming timestamp: 20251022_143052_001.jpg
     * - Ordinamento cronologico
     * - Evita conflitti di nome
     * - Meno intuitivo per utente
     */
    TIMESTAMP
}

/**
 * Qualità foto per export
 */
enum class PhotoQuality {
    /**
     * Foto originali senza modifiche
     * - Massima qualità
     * - Dimensioni file maggiori
     * - Tempo export più lungo
     */
    ORIGINAL,

    /**
     * Ottimizzate per dimensioni ragionevoli
     * - Buona qualità
     * - Dimensioni equilibrate
     * - Velocità export bilanciata
     */
    OPTIMIZED,

    /**
     * Compresse per minimizzare spazio
     * - Qualità ridotta
     * - Dimensioni file minime
     * - Export più veloce
     */
    COMPRESSED
}