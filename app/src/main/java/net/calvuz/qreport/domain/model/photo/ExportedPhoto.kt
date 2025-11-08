package net.calvuz.qreport.domain.model.photo

import net.calvuz.qreport.domain.model.export.PhotoNamingStrategy
import net.calvuz.qreport.domain.model.module.ModuleType
import java.time.LocalDateTime

/**
 * Rappresenta una foto esportata nella cartella FOTO
 * Mantiene il collegamento con la foto originale e i dettagli dell'export
 *
 * VERSIONE AGGIORNATA: Usa PhotoModuleInfo (modules) invece di PhotoSectionInfo (sections)
 */
data class ExportedPhoto(
    /**
     * Foto originale dal database
     */
    val originalPhoto: Photo,

    /**
     * Nome file nella cartella export
     */
    val exportedFileName: String,

    /**
     * Path completo del file esportato
     */
    val exportedPath: String,

    /**
     * Dimensione del file esportato (bytes)
     */
    val fileSize: Long,

    /**
     * Strategia di naming utilizzata
     */
    val namingStrategy: PhotoNamingStrategy,

    /**
     * Timestamp dell'export
     */
    val exportedAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Informazioni sul modulo di appartenenza (AGGIORNATO)
     */
    val moduleInfo: PhotoModuleInfo,

    /**
     * Informazioni sul check item di appartenenza
     */
    val checkItemInfo: PhotoCheckItemInfo,

    /**
     * Metadati aggiuntivi dell'export
     */
    val exportMetadata: PhotoExportMetadata? = null
) {

    /**
     * Ottieni estensione del file
     */
    val fileExtension: String
        get() = exportedFileName.substringAfterLast('.', "")

    /**
     * Verifica se il file esportato esiste
     */
    fun exists(): Boolean {
        return try {
            java.io.File(exportedPath).exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ottieni dimensione file in formato leggibile
     */
    val fileSizeFormatted: String
        get() = formatFileSize(fileSize)

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
 * Informazioni sul modulo per l'export foto
 * NUOVA: Sostituisce PhotoSectionInfo per allineamento con architettura modules
 */
data class PhotoModuleInfo(
    val moduleType: ModuleType,
    val moduleDisplayName: String,
    val moduleIndex: Int,
    val modulePrefix: String // es. "01", "02", etc.
) {

    /**
     * Nome modulo normalizzato per file system
     */
    val normalizedName: String
        get() = moduleDisplayName
            .replace(" ", "-")
            .replace(Regex("[^a-zA-Z0-9\\-]"), "")
            .lowercase()
            .take(20)
}

/**
 * Informazioni sul check item per l'export foto
 */
data class PhotoCheckItemInfo(
    val checkItemId: String,
    val checkItemTitle: String,
    val checkItemIndex: Int,
    val itemStatus: String, // CheckItemStatus as string
    val itemCriticality: String // CheckItemCriticality as string
) {

    /**
     * Descrizione item normalizzata per file system
     */
    val normalizedDescription: String
        get() = checkItemTitle
            .replace(" ", "-")
            .replace(Regex("[^a-zA-Z0-9\\-]"), "")
            .lowercase()
            .take(30)
}

/**
 * Metadati aggiuntivi dell'export foto
 */
data class PhotoExportMetadata(
    /**
     * Qualità utilizzata per l'export
     */
    val quality: Int? = null,

    /**
     * Dimensioni del file originale
     */
    val originalFileSize: Long? = null,

    /**
     * Fattore di compressione applicato
     */
    val compressionRatio: Float? = null,

    /**
     * Watermark applicato
     */
    val watermarkApplied: Boolean = false,

    /**
     * Metadati EXIF preservati
     */
    val exifPreserved: Boolean = false,

    /**
     * Eventuali trasformazioni applicate
     */
    val transformationsApplied: List<String> = emptyList(),

    /**
     * Tempo di processamento (ms)
     */
    val processingTimeMs: Long? = null
) {

    /**
     * Calcola efficienza compressione
     */
    val compressionEfficiency: String?
        get() = compressionRatio?.let {
            "${String.format("%.1f", (1 - it) * 100)}% riduzione"
        }
}

/**
 * Risultato dell'export di foto in cartella
 */
sealed class PhotoExportResult {

    /**
     * Export foto completato con successo
     */
    data class Success(
        val exportedPhotos: List<ExportedPhoto>,
        val totalFiles: Int,
        val totalSize: Long,
        val exportDirectory: String,
        val indexFilePath: String? = null // Path del file indice, se generato
    ) : PhotoExportResult() {

        /**
         * Raggruppa foto per modulo (AGGIORNATO)
         */
        val photosByModule: Map<ModuleType, List<ExportedPhoto>>
            get() = exportedPhotos.groupBy { it.moduleInfo.moduleType }

        /**
         * Raggruppa foto per check item
         */
        val photosByCheckItem: Map<String, List<ExportedPhoto>>
            get() = exportedPhotos.groupBy { it.checkItemInfo.checkItemId }

        /**
         * Dimensione totale in formato leggibile
         */
        val totalSizeFormatted: String
            get() = formatFileSize(totalSize)

        /**
         * Statistiche dell'export
         */
        val statistics: PhotoExportStatistics
            get() = PhotoExportStatistics(
                totalPhotos = exportedPhotos.size,
                uniqueModules = photosByModule.size, // AGGIORNATO: uniqueModules invece di uniqueSections
                uniqueCheckItems = photosByCheckItem.size,
                totalSizeBytes = totalSize,
                averageFileSize = if (exportedPhotos.isNotEmpty()) totalSize / exportedPhotos.size else 0,
                largestFile = exportedPhotos.maxByOrNull { it.fileSize },
                smallestFile = exportedPhotos.minByOrNull { it.fileSize }
            )

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
     * Export foto fallito
     */
    data class Error(
        val exception: Throwable,
        val errorCode: String,
        val partialResults: List<ExportedPhoto> = emptyList()
    ) : PhotoExportResult()
}

/**
 * Statistiche dell'export foto
 * AGGIORNATA: uniqueModules invece di uniqueSections
 */
data class PhotoExportStatistics(
    val totalPhotos: Int,
    val uniqueModules: Int, // AGGIORNATO: modules invece di sections
    val uniqueCheckItems: Int,
    val totalSizeBytes: Long,
    val averageFileSize: Long,
    val largestFile: ExportedPhoto?,
    val smallestFile: ExportedPhoto?
)