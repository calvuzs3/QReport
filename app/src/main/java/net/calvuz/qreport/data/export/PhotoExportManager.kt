package net.calvuz.qreport.data.export.photo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.domain.model.checkup.CheckupSection
import net.calvuz.qreport.domain.model.export.ExportData
import net.calvuz.qreport.domain.model.export.PhotoNamingStrategy
import net.calvuz.qreport.domain.model.export.PhotoQuality
import net.calvuz.qreport.domain.model.photo.*
import net.calvuz.qreport.domain.repository.PhotoRepository
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager per l'export delle foto nella cartella FOTO separata
 * Gestisce naming, qualità, organizzazione e metadati delle foto esportate
 */
@Singleton
class PhotoExportManager @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val imageProcessor: ImageProcessor
) {

    /**
     * Esporta tutte le foto del checkup nella cartella target
     *
     * @param exportData Dati completi del checkup
     * @param targetDirectory Directory di destinazione per la cartella FOTO
     * @param namingStrategy Strategia di naming per i file
     * @param quality Qualità delle foto esportate
     * @param generateIndex Se generare file indice con mapping foto-items
     * @return Risultato dell'export con lista foto esportate
     */
    suspend fun exportPhotosToFolder(
        exportData: ExportData,
        targetDirectory: File,
        namingStrategy: PhotoNamingStrategy,
        quality: PhotoQuality = PhotoQuality.ORIGINAL,
        preserveExifData: Boolean = true,
        addWatermark: Boolean = false,
        watermarkText: String = "QReport",
        generateIndex: Boolean = true
    ): PhotoExportResult = withContext(Dispatchers.IO) {

        try {
            Timber.d("Avvio export foto: ${exportData.sections.size} sezioni")

            // 1. Crea cartella FOTO
            val photoFolder = createPhotoFolder(targetDirectory)

            // 2. Raccoglie tutte le foto da esportare
            val allPhotosToExport = collectPhotosFromSections(exportData.sections)
            Timber.d("Foto da esportare: ${allPhotosToExport.size}")

            if (allPhotosToExport.isEmpty()) {
                return@withContext PhotoExportResult.Success(
                    exportedPhotos = emptyList(),
                    totalFiles = 0,
                    totalSize = 0,
                    exportDirectory = photoFolder.absolutePath
                )
            }

            // 3. Esporta ogni foto
            val exportedPhotos = mutableListOf<ExportedPhoto>()
            var totalSize = 0L

            allPhotosToExport.forEachIndexed { globalIndex, photoData ->
                try {
                    val exportedPhoto = exportSinglePhoto(
                        photoData = photoData,
                        globalIndex = globalIndex,
                        targetDirectory = photoFolder,
                        namingStrategy = namingStrategy,
                        quality = quality,
                        preserveExifData = preserveExifData,
                        addWatermark = addWatermark,
                        watermarkText = watermarkText
                    )

                    exportedPhotos.add(exportedPhoto)
                    totalSize += exportedPhoto.fileSize

                    Timber.v("Foto esportata: ${exportedPhoto.exportedFileName}")

                } catch (e: Exception) {
                    Timber.e(e, "Errore export foto: ${photoData.photo.fileName}")
                    // Continua con le altre foto invece di fallire tutto
                }
            }

            // 4. Genera file indice se richiesto
            val indexFilePath = if (generateIndex) {
                generatePhotoIndex(photoFolder, exportedPhotos, exportData)
            } else null

            PhotoExportResult.Success(
                exportedPhotos = exportedPhotos,
                totalFiles = exportedPhotos.size,
                totalSize = totalSize,
                exportDirectory = photoFolder.absolutePath,
                indexFilePath = indexFilePath
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore generale export foto")
            PhotoExportResult.Error(
                exception = e,
                errorCode = "PHOTO_EXPORT_FAILED"
            )
        }
    }

    /**
     * Crea la cartella FOTO nella directory target
     */
    private fun createPhotoFolder(targetDirectory: File): File {
        val photoFolder = File(targetDirectory, "FOTO")
        if (!photoFolder.exists()) {
            photoFolder.mkdirs()
        }
        return photoFolder
    }

    /**
     * Raccoglie tutte le foto dalle sezioni con metadati di contesto
     */
    private fun collectPhotosFromSections(sections: List<CheckupSection>): List<PhotoWithContext> {
        val photos = mutableListOf<PhotoWithContext>()

        sections.forEachIndexed { sectionIndex, section ->
            val sectionInfo = PhotoSectionInfo(
                sectionId = section.id,
                sectionTitle = section.title,
                sectionIndex = sectionIndex,
                sectionPrefix = String.format("%02d", sectionIndex + 1)
            )

            section.items.forEachIndexed { itemIndex, item ->
                val checkItemInfo = PhotoCheckItemInfo(
                    checkItemId = item.id,
                    checkItemTitle = item.title,
                    checkItemIndex = itemIndex,
                    itemStatus = item.status.toString(),
                    itemCriticality = item.criticality.toString()
                )

                item.photos.forEachIndexed { photoIndex, photo ->
                    photos.add(PhotoWithContext(
                        photo = photo,
                        sectionInfo = sectionInfo,
                        checkItemInfo = checkItemInfo,
                        photoIndexInItem = photoIndex
                    ))
                }
            }
        }

        return photos
    }

    /**
     * Esporta una singola foto con tutte le trasformazioni richieste
     */
    private suspend fun exportSinglePhoto(
        photoData: PhotoWithContext,
        globalIndex: Int,
        targetDirectory: File,
        namingStrategy: PhotoNamingStrategy,
        quality: PhotoQuality,
        preserveExifData: Boolean,
        addWatermark: Boolean,
        watermarkText: String
    ): ExportedPhoto {

        val startTime = System.currentTimeMillis()

        // 1. Genera nome file
        val fileName = generateFileName(
            photoData = photoData,
            globalIndex = globalIndex,
            namingStrategy = namingStrategy
        )

        val targetFile = File(targetDirectory, fileName)
        val sourceFile = File(photoData.photo.filePath)

        // 2. Processa foto in base alla qualità richiesta
        val originalSize = sourceFile.length()
        val processedSize = when (quality) {
            PhotoQuality.ORIGINAL -> {
                // Copia diretta del file originale
                copyFilePreservingAttributes(sourceFile, targetFile, preserveExifData)
                targetFile.length()
            }
            PhotoQuality.OPTIMIZED -> {
                // Ottimizza mantenendo buona qualità
                processPhotoOptimized(sourceFile, targetFile, preserveExifData, addWatermark, watermarkText)
            }
            PhotoQuality.COMPRESSED -> {
                // Comprimi per ridurre spazio
                processPhotoCompressed(sourceFile, targetFile, preserveExifData, addWatermark, watermarkText)
            }
        }

        val processingTime = System.currentTimeMillis() - startTime

        return ExportedPhoto(
            originalPhoto = photoData.photo,
            exportedFileName = fileName,
            exportedPath = targetFile.absolutePath,
            fileSize = processedSize,
            namingStrategy = namingStrategy,
            sectionInfo = photoData.sectionInfo,
            checkItemInfo = photoData.checkItemInfo,
            exportMetadata = PhotoExportMetadata(
                originalFileSize = originalSize,
                compressionRatio = if (originalSize > 0) processedSize.toFloat() / originalSize.toFloat() else 1f,
                watermarkApplied = addWatermark,
                exifPreserved = preserveExifData,
                processingTimeMs = processingTime
            )
        )
    }

    /**
     * Genera nome file in base alla strategia specificata
     */
    private fun generateFileName(
        photoData: PhotoWithContext,
        globalIndex: Int,
        namingStrategy: PhotoNamingStrategy
    ): String {

        return when (namingStrategy) {
            PhotoNamingStrategy.STRUCTURED -> generateStructuredName(photoData)
            PhotoNamingStrategy.SEQUENTIAL -> generateSequentialName(globalIndex)
            PhotoNamingStrategy.TIMESTAMP -> generateTimestampName(photoData, globalIndex)
        }
    }

    /**
     * Genera nome strutturato: 01_Sicurezza_Check001_vista-frontale.jpg
     */
    private fun generateStructuredName(photoData: PhotoWithContext): String {
        val section = photoData.sectionInfo
        val item = photoData.checkItemInfo
        val photo = photoData.photo

        val sectionPrefix = section.sectionPrefix
        val sectionName = section.normalizedName
        val itemDesc = item.normalizedDescription
        val photoDesc = normalizePhotoDescription(photo.caption, photoData.photoIndexInItem)

        return "${sectionPrefix}_${sectionName}_${itemDesc}_${photoDesc}.jpg"
            .take(80) // Limitazione lunghezza per compatibilità file system
    }

    /**
     * Genera nome sequenziale: foto_001.jpg
     */
    private fun generateSequentialName(globalIndex: Int): String {
        return "foto_${String.format("%03d", globalIndex + 1)}.jpg"
    }

    /**
     * Genera nome con timestamp: 20251022_143052_001.jpg
     */
    private fun generateTimestampName(photoData: PhotoWithContext, globalIndex: Int): String {
        val timestamp = photoData.photo.takenAt.toString().replace(":", "").replace("-", "")
        return "${timestamp}_${String.format("%03d", globalIndex + 1)}.jpg"
    }

    /**
     * Normalizza descrizione foto per nome file
     */
    private fun normalizePhotoDescription(caption: String, photoIndex: Int): String {
        val normalizedCaption = if (caption.isBlank()) {
            "foto${photoIndex + 1}"
        } else {
            caption.replace(" ", "-")
                .replace(Regex("[^a-zA-Z0-9\\-]"), "")
                .lowercase()
        }

        return normalizedCaption.take(30)
    }

    /**
     * Copia file preservando attributi e metadati
     */
    private suspend fun copyFilePreservingAttributes(
        sourceFile: File,
        targetFile: File,
        preserveExifData: Boolean
    ) = withContext(Dispatchers.IO) {

        FileInputStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        // Preserva timestamp originale
        targetFile.setLastModified(sourceFile.lastModified())
    }

    /**
     * Processa foto in qualità ottimizzata
     */
    private suspend fun processPhotoOptimized(
        sourceFile: File,
        targetFile: File,
        preserveExifData: Boolean,
        addWatermark: Boolean,
        watermarkText: String
    ): Long = withContext(Dispatchers.IO) {

        // Per ora implementazione semplice - in futuro usare imageProcessor
        copyFilePreservingAttributes(sourceFile, targetFile, preserveExifData)
        return@withContext targetFile.length()
    }

    /**
     * Processa foto in qualità compressa
     */
    private suspend fun processPhotoCompressed(
        sourceFile: File,
        targetFile: File,
        preserveExifData: Boolean,
        addWatermark: Boolean,
        watermarkText: String
    ): Long = withContext(Dispatchers.IO) {

        // Per ora implementazione semplice - in futuro usare imageProcessor
        copyFilePreservingAttributes(sourceFile, targetFile, preserveExifData)
        return@withContext targetFile.length()
    }

    /**
     * Genera file indice con mapping foto -> check items
     */
    private suspend fun generatePhotoIndex(
        photoFolder: File,
        exportedPhotos: List<ExportedPhoto>,
        exportData: ExportData
    ): String = withContext(Dispatchers.IO) {

        val indexFile = File(photoFolder, "INDICE_FOTO.txt")

        val indexContent = buildString {
            appendLine("# INDICE FOTO - ${exportData.checkup.islandType.displayName}")
            appendLine("# Generato: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}")
            appendLine("# Cliente: ${exportData.metadata.clientInfo.companyName}")
            appendLine("# Tecnico: ${exportData.metadata.technicianName}")
            appendLine("#")
            appendLine("# Formato: [NOME_FILE] -> [SEZIONE] -> [CHECK_ITEM] -> [STATO]")
            appendLine("=" * 80)
            appendLine()

            // Raggruppa per sezione
            val photosBySection = exportedPhotos.groupBy { it.sectionInfo.sectionId }

            photosBySection.forEach { (sectionId, sectionPhotos) ->
                val sectionInfo = sectionPhotos.first().sectionInfo
                appendLine("SEZIONE ${sectionInfo.sectionIndex + 1}: ${sectionInfo.sectionTitle}")
                appendLine("-" * 50)

                val photosByItem = sectionPhotos.groupBy { it.checkItemInfo.checkItemId }

                photosByItem.forEach { (itemId, itemPhotos) ->
                    val itemInfo = itemPhotos.first().checkItemInfo
                    appendLine()
                    appendLine("  Check Item: ${itemInfo.checkItemTitle}")
                    appendLine("  Stato: ${itemInfo.itemStatus} | Criticità: ${itemInfo.itemCriticality}")
                    appendLine("  Foto:")

                    itemPhotos.forEach { photo ->
                        appendLine("    - ${photo.exportedFileName}")
                        if (photo.originalPhoto.caption.isNotBlank()) {
                            appendLine("      Caption: ${photo.originalPhoto.caption}")
                        }
                        appendLine("      Dimensione: ${photo.fileSizeFormatted}")
                        appendLine()
                    }
                }

                appendLine()
            }

            appendLine("=" * 80)
            appendLine("RIEPILOGO:")
            appendLine("Foto totali: ${exportedPhotos.size}")
            appendLine("Sezioni: ${photosBySection.size}")
            appendLine("Check items con foto: ${exportedPhotos.map { it.checkItemInfo.checkItemId }.distinct().size}")
            appendLine("Dimensione totale: ${formatFileSize(exportedPhotos.sumOf { it.fileSize })}")
        }

        indexFile.writeText(indexContent)
        return@withContext indexFile.absolutePath
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)}KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))}MB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))}GB"
        }
    }

    private fun String.times(n: Int): String = this.repeat(n)
}

/**
 * Classe di supporto per associare foto a contesto di sezione/item
 */
private data class PhotoWithContext(
    val photo: Photo,
    val sectionInfo: PhotoSectionInfo,
    val checkItemInfo: PhotoCheckItemInfo,
    val photoIndexInItem: Int
)

/**
 * Processore immagini placeholder - da implementare in futuro
 */
interface ImageProcessor {
    suspend fun optimizePhoto(
        sourceFile: File,
        targetFile: File,
        quality: Int,
        maxWidth: Int? = null,
        maxHeight: Int? = null
    ): Long

    suspend fun addWatermark(
        sourceFile: File,
        targetFile: File,
        watermarkText: String
    ): Long
}