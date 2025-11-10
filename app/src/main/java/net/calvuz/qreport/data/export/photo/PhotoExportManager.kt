package net.calvuz.qreport.data.export.photo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.domain.model.export.ExportData
import net.calvuz.qreport.domain.model.export.PhotoNamingStrategy
import net.calvuz.qreport.domain.model.export.PhotoQuality
import net.calvuz.qreport.domain.model.photo.*
import net.calvuz.qreport.domain.model.module.ModuleType
import net.calvuz.qreport.domain.model.checkup.CheckItem
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
 *
 * VERSIONE CORRETTA: Usa SOLO domain models, nessun data class duplicato
 */
@Singleton
class PhotoExportManager @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val imageProcessor: ImageProcessor
) {

    /**
     * Esporta tutte le foto del checkup nella cartella target
     *
     * @param exportData Dati completi del checkup (con itemsByModule)
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
            Timber.d("Avvio export foto: ${exportData.itemsByModule.size} moduli")

            // 1. Crea cartella FOTO
            val photoFolder = createPhotoFolder(targetDirectory)

            // 2. Raccoglie tutte le foto da esportare usando domain models
            val allPhotosToExport = collectPhotosFromModules(exportData.itemsByModule)
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

            allPhotosToExport.forEachIndexed { globalIndex, photoContext ->
                try {
                    val exportedPhoto = exportSinglePhoto(
                        photoContext = photoContext,
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
                    Timber.e(e, "Errore export foto: ${photoContext.photo.fileName}")
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
     * Raccoglie tutte le foto dai moduli con metadati di contesto
     * USA SOLO DOMAIN MODELS
     */
    private fun collectPhotosFromModules(itemsByModule: Map<ModuleType, List<CheckItem>>): List<PhotoContext> {
        val photos = mutableListOf<PhotoContext>()

        itemsByModule.entries.forEachIndexed { moduleIndex, (moduleType, checkItems) ->
            val moduleInfo = PhotoModuleInfo(
                moduleType = moduleType,
                moduleDisplayName = moduleType.displayName,
                moduleIndex = moduleIndex,
                modulePrefix = String.format("%02d", moduleIndex + 1)
            )

            checkItems.forEachIndexed { itemIndex, item ->
                val checkItemInfo = PhotoCheckItemInfo(
                    checkItemId = item.id,
                    checkItemTitle = item.description,
                    checkItemIndex = itemIndex,
                    itemStatus = item.status.toString(),
                    itemCriticality = item.criticality.toString()
                )

                item.photos.forEachIndexed { photoIndex, photo ->
                    photos.add(
                        PhotoContext(
                            photo = photo,
                            moduleInfo = moduleInfo,
                            checkItemInfo = checkItemInfo,
                            photoIndexInItem = photoIndex
                        )
                    )
                }
            }
        }

        return photos
    }

    /**
     * Esporta una singola foto con tutte le trasformazioni richieste
     * USA SOLO DOMAIN MODELS
     */
    private suspend fun exportSinglePhoto(
        photoContext: PhotoContext,
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
            photoContext = photoContext,
            globalIndex = globalIndex,
            namingStrategy = namingStrategy
        )

        val targetFile = File(targetDirectory, fileName)
        val sourceFile = File(photoContext.photo.filePath)

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

        // 3. Crea ExportedPhoto usando DOMAIN MODEL
        return ExportedPhoto(
            originalPhoto = photoContext.photo,
            exportedFileName = fileName,
            exportedPath = targetFile.absolutePath,
            fileSize = processedSize,
            namingStrategy = namingStrategy,
            exportedAt = LocalDateTime.now(),
            moduleInfo = photoContext.moduleInfo,
            checkItemInfo = photoContext.checkItemInfo,
            exportMetadata = PhotoExportMetadata(
                quality = when (quality) {
                    PhotoQuality.ORIGINAL -> 100
                    PhotoQuality.OPTIMIZED -> 85
                    PhotoQuality.COMPRESSED -> 70
                },
                originalFileSize = originalSize,
                compressionRatio = if (originalSize > 0) processedSize.toFloat() / originalSize else 1f,
                watermarkApplied = addWatermark,
                exifPreserved = preserveExifData,
                transformationsApplied = buildList {
                    if (quality != PhotoQuality.ORIGINAL) add("Compression")
                    if (addWatermark) add("Watermark")
                    if (!preserveExifData) add("EXIF Removal")
                },
                processingTimeMs = processingTime
            )
        )
    }

    /**
     * Genera nome file basato sulla strategia di naming
     */
    private fun generateFileName(
        photoContext: PhotoContext,
        globalIndex: Int,
        namingStrategy: PhotoNamingStrategy
    ): String {
        val extension = photoContext.photo.fileName.substringAfterLast('.', "jpg")
        val modulePrefix = photoContext.moduleInfo.modulePrefix
        val itemNumber = String.format("%03d", photoContext.checkItemInfo.checkItemIndex + 1)
        val photoNumber = String.format("%02d", photoContext.photoIndexInItem + 1)

        return when (namingStrategy) {
            PhotoNamingStrategy.SEQUENTIAL ->
                "foto_${String.format("%03d", globalIndex + 1)}.${extension}"

            PhotoNamingStrategy.STRUCTURED ->
                "${modulePrefix}_${photoContext.moduleInfo.normalizedName}_Check${itemNumber}_${photoNumber}.${extension}"

            PhotoNamingStrategy.TIMESTAMP -> {
                // Format: 20251022_143052_001.jpg
                val timestamp = photoContext.photo.takenAt.toString()
                    .substring(0, 19) // Take only YYYY-MM-DDTHH:mm:SS
                    .replace("T", "_")
                    .replace("-", "")
                    .replace(":", "")
                "${timestamp}_${String.format("%03d", globalIndex + 1)}.${extension}"
            }
        }
    }

    /**
     * Copia file mantenendo attributi originali
     */
    private suspend fun copyFilePreservingAttributes(
        sourceFile: File,
        targetFile: File,
        preserveExif: Boolean
    ) = withContext(Dispatchers.IO) {
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        if (preserveExif) {
            // Mantieni timestamp originale
            targetFile.setLastModified(sourceFile.lastModified())
        }
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
     * USA SOLO DOMAIN MODELS
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
            appendLine("# Cliente: ${exportData.checkup.header.clientInfo.companyName}")
            appendLine("# Tecnico: ${exportData.checkup.header.technicianInfo.name}")
            appendLine("#")
            appendLine("# Formato: [NOME_FILE] -> [MODULO] -> [CHECK_ITEM] -> [STATO]")
            appendLine("=".repeat(80))
            appendLine()

            // Raggruppa per modulo usando DOMAIN MODEL
            val photosByModule = exportedPhotos.groupBy { it.moduleInfo.moduleType }

            photosByModule.forEach { (moduleType, modulePhotos) ->
                val moduleInfo = modulePhotos.first().moduleInfo
                appendLine("MODULO ${moduleInfo.moduleIndex + 1}: ${moduleInfo.moduleDisplayName}")
                appendLine("-".repeat(50))

                val photosByItem = modulePhotos.groupBy { it.checkItemInfo.checkItemId }

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

            appendLine("=".repeat(80))
            appendLine("RIEPILOGO:")
            appendLine("Foto totali: ${exportedPhotos.size}")
            appendLine("Moduli: ${photosByModule.size}")
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
}

// === HELPER CLASSES ===

/**
 * Classe di supporto per associare foto a contesto di modulo/item
 * SEMPLICE HELPER - Non duplicate domain models
 */
private data class PhotoContext(
    val photo: Photo,
    val moduleInfo: PhotoModuleInfo,
    val checkItemInfo: PhotoCheckItemInfo,
    val photoIndexInItem: Int
)