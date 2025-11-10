package net.calvuz.qreport.data.repository

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import net.calvuz.qreport.data.export.photo.PhotoExportManager
import net.calvuz.qreport.data.export.text.TextReportGenerator
import net.calvuz.qreport.data.export.word.WordReportGenerator
import net.calvuz.qreport.domain.model.export.*
import net.calvuz.qreport.domain.model.export.ExportResult.*
import net.calvuz.qreport.domain.model.photo.PhotoExportResult
import net.calvuz.qreport.domain.repository.ExportEstimation
import net.calvuz.qreport.domain.repository.ExportRepository
import net.calvuz.qreport.domain.repository.FormatEstimation
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Risultato della generazione di un package completo
 */
data class CombinedPackageResult(
    val success: Boolean,
    val packageDirectory: String,
    val wordResult: Success? = null,
    val textResult: Success? = null,
    val photoFolderResult: Success? = null,
    val error: Throwable? = null
)

/**
 * Implementazione repository per export multi-formato
 * Coordina Word, Text e Photo export con gestione errori e progress tracking
 */
@Singleton
class ExportRepositoryImpl @Inject constructor(
    private val wordReportGenerator: WordReportGenerator,
    private val textReportGenerator: TextReportGenerator,
    private val photoExportManager: PhotoExportManager
) : ExportRepository {

    companion object {
        private const val EXPORT_BASE_DIR = "QReport"
        private const val TEMP_DIR = "temp"
        private const val MAX_EXPORT_RETRIES = 3
    }

    override suspend fun generateCompleteExport(
        exportData: ExportData,
        options: ExportOptions
    ): Flow<MultiFormatExportResult> = flow {

        Timber.i("Avvio export completo: formati=${options.exportFormats}")
        val startTime = System.currentTimeMillis()

        try {
            // 1. Validazione dati
            val validationErrors = validateExportData(exportData)
            if (validationErrors.isNotEmpty()) {
                throw IllegalArgumentException("Dati export non validi: ${validationErrors.joinToString()}")
            }

            // 2. Preparazione directory
            val exportDirectory = if (options.createTimestampedDirectory) {
                createTimestampedExportDirectory(getDefaultExportDirectory(), exportData)
            } else {
                getDefaultExportDirectory()
            }

            // 3. Inizializza risultato
            var result = MultiFormatExportResult(
                exportDirectory = exportDirectory.absolutePath,
                statistics = ExportStatistics()
            )

            // 4. Export per ogni formato richiesto
            val totalFormats = options.exportFormats.size
            var completedFormats = 0

            options.exportFormats.forEach { format ->
                try {
                    Timber.d("Export formato: $format")

                    when (format) {
                        ExportFormat.WORD -> {
                            val wordResult =
                                generateWordReportInternal(exportData, options, exportDirectory)
                            result = result.copy(wordResult = wordResult as? Success)
                        }

                        ExportFormat.TEXT -> {
                            val textResult =
                                generateTextReportInternal(exportData, options, exportDirectory)
                            result = result.copy(textResult = textResult as? Success)
                        }

                        ExportFormat.PHOTO_FOLDER -> {
                            val photoResult =
                                generatePhotoFolderInternal(exportData, options, exportDirectory)
                            if (photoResult is PhotoExportResult.Success) {
                                result = result.copy(
                                    photoFolderResult = Success(
                                        filePath = photoResult.exportDirectory,
                                        fileName = "FOTO",
                                        fileSize = photoResult.totalSize,
                                        format = ExportFormat.PHOTO_FOLDER
                                    )
                                )
                            }
                        }

                        ExportFormat.COMBINED_PACKAGE -> {
                            Timber.d("Generazione package completo (Word + Text + Foto)")

                            // Genera tutti i formati nella stessa directory
                            val combinedResult = generateCombinedPackage(exportData, options, exportDirectory)

                            // Aggiorna il risultato con tutti i componenti
                            result = result.copy(
                                wordResult = combinedResult.wordResult,
                                textResult = combinedResult.textResult,
                                photoFolderResult = combinedResult.photoFolderResult
                            )
                        }
                    }

                    completedFormats++

                    // Emetti progresso intermedio
                    val updatedStats = calculateStatistics(exportData, result)
                    emit(result.copy(statistics = updatedStats))

                } catch (e: Exception) {
                    Timber.e(e, "Errore export formato $format")
                    // Continua con altri formati invece di fallire tutto
                }
            }

            // 5. Calcola statistiche finali
            val processingTime = System.currentTimeMillis() - startTime
            val finalStats = calculateStatistics(exportData, result).copy(
                processingTimeMs = processingTime
            )

            // 6. Emetti risultato finale
            emit(result.copy(statistics = finalStats))

            Timber.i("Export completo completato in ${finalStats.processingTimeFormatted}")

        } catch (e: Exception) {
            Timber.e(e, "Errore export completo")
            throw e
        }
    }

    override suspend fun generateCompleteExportSync(
        exportData: ExportData,
        options: ExportOptions
    ): MultiFormatExportResult {
        var lastResult: MultiFormatExportResult? = null

        generateCompleteExport(exportData, options).collect { result ->
            lastResult = result
        }

        return lastResult ?: throw IllegalStateException("Export non completato")
    }

    override suspend fun generateWordReport(
        exportData: ExportData,
        options: ExportOptions
    ): ExportResult = withContext(Dispatchers.IO) {

        return@withContext generateWordReportInternal(
            exportData = exportData,
            options = options,
            targetDirectory = getDefaultExportDirectory()
        )
    }

    private suspend fun generateWordReportInternal(
        exportData: ExportData,
        options: ExportOptions,
        targetDirectory: File
    ): ExportResult {

        return try {
            Timber.d("Generazione Word report")

            // Genera report usando WordReportGenerator esistente
            val result = wordReportGenerator.generateWordReport(exportData, options)

            when (result) {
                is Success -> {
                    // Sposta file nella directory target se necessario
                    val sourceFile = File(result.filePath)
                    val targetFile = File(targetDirectory, result.fileName)

                    if (sourceFile.absolutePath != targetFile.absolutePath) {
                        sourceFile.copyTo(targetFile, overwrite = true)
                        sourceFile.delete()

                        Success(
                            filePath = targetFile.absolutePath,
                            fileName = result.fileName,
                            fileSize = targetFile.length(),
                            format = ExportFormat.WORD
                        )
                    } else {
                        result.copy(format = ExportFormat.WORD)
                    }
                }

                is Error -> result
                is Loading -> result
            }

        } catch (e: Exception) {
            Timber.e(e, "Errore generazione Word")
            Error(
                exception = e,
                errorCode = ExportErrorCode.DOCUMENT_GENERATION_ERROR,
                format = ExportFormat.WORD
            )
        }
    }

    override suspend fun generateTextReport(
        exportData: ExportData,
        options: ExportOptions
    ): ExportResult = withContext(Dispatchers.IO) {

        return@withContext generateTextReportInternal(
            exportData = exportData,
            options = options,
            targetDirectory = getDefaultExportDirectory()
        )
    }

    private suspend fun generateTextReportInternal(
        exportData: ExportData,
        options: ExportOptions,
        targetDirectory: File
    ): ExportResult {

        return try {
            Timber.d("Generazione Text report")

            // Genera contenuto testuale
            val textContent = textReportGenerator.generateTextReport(exportData, options)

            // Crea nome file
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
            val clientName =
                exportData.checkup.header.clientInfo.companyName // metadata.clientInfo.companyName
                    .replace(" ", "")
                    .replace("[^a-zA-Z0-9]".toRegex(), "")
                    .take(20)

            val fileName = "Checkup_Summary_${clientName}_$timestamp.txt"
            val targetFile = File(targetDirectory, fileName)

            // Scrivi file
            targetFile.writeText(textContent, Charsets.UTF_8)

            Success(
                filePath = targetFile.absolutePath,
                fileName = fileName,
                fileSize = targetFile.length(),
                format = ExportFormat.TEXT
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore generazione Text")
            Error(
                exception = e,
                errorCode = ExportErrorCode.TEXT_GENERATION_ERROR,
                format = ExportFormat.TEXT
            )
        }
    }

    override suspend fun generatePhotoFolder(
        exportData: ExportData,
        targetDirectory: File,
        options: ExportOptions
    ): PhotoExportResult {

        return generatePhotoFolderInternal(exportData, options, targetDirectory)
    }

    private suspend fun generatePhotoFolderInternal(
        exportData: ExportData,
        options: ExportOptions,
        targetDirectory: File
    ): PhotoExportResult {

        return try {
            Timber.d("Generazione cartella foto")

            return photoExportManager.exportPhotosToFolder(
                exportData = exportData,
                targetDirectory = targetDirectory,
                namingStrategy = options.photoNamingStrategy,
                quality = options.photoFolderQuality,
                preserveExifData = options.preserveExifData,
                addWatermark = options.addWatermark,
                watermarkText = options.watermarkText,
                generateIndex = options.generatePhotoIndex
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore generazione cartella foto")
            PhotoExportResult.Error(
                exception = e,
                errorCode = "PHOTO_FOLDER_GENERATION_ERROR"
            )
        }
    }

    /**
     * Genera un package completo con Word + Text + Foto nella stessa directory
     */
    private suspend fun generateCombinedPackage(
        exportData: ExportData,
        options: ExportOptions,
        targetDirectory: File
    ): CombinedPackageResult {

        return try {
            Timber.d("Avvio generazione package completo")

            var wordResult: Success? = null
            var textResult: Success? = null
            var photoFolderResult: Success? = null

            // 1. Genera documento Word
            try {
                val wordExportResult = generateWordReportInternal(exportData, options, targetDirectory)
                if (wordExportResult is Success) {
                    wordResult = wordExportResult
                    Timber.d("Word document generato: ${wordResult.fileName}")
                }
            } catch (e: Exception) {
                Timber.w(e, "Errore generazione Word nel package combinato")
            }

            // 2. Genera riassunto testuale
            try {
                val textExportResult = generateTextReportInternal(exportData, options, targetDirectory)
                if (textExportResult is Success) {
                    textResult = textExportResult
                    Timber.d("Text summary generato: ${textResult.fileName}")
                }
            } catch (e: Exception) {
                Timber.w(e, "Errore generazione Text nel package combinato")
            }

            // 3. Genera cartella foto
            try {
                val photoExportResult = generatePhotoFolderInternal(exportData, options, targetDirectory)
                if (photoExportResult is PhotoExportResult.Success) {
                    photoFolderResult = Success(
                        filePath = photoExportResult.exportDirectory,
                        fileName = "FOTO",
                        fileSize = photoExportResult.totalSize,
                        format = ExportFormat.PHOTO_FOLDER
                    )
                    Timber.d("Cartella foto generata: ${photoFolderResult.fileName}")
                }
            } catch (e: Exception) {
                Timber.w(e, "Errore generazione foto nel package combinato")
            }

            // 4. Crea file indice del package
            createPackageIndex(targetDirectory, wordResult, textResult, photoFolderResult, exportData)

            Timber.i("Package completo generato in: ${targetDirectory.absolutePath}")

            CombinedPackageResult(
                success = true,
                packageDirectory = targetDirectory.absolutePath,
                wordResult = wordResult,
                textResult = textResult,
                photoFolderResult = photoFolderResult
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore generazione package completo")
            CombinedPackageResult(
                success = false,
                packageDirectory = targetDirectory.absolutePath,
                error = e
            )
        }
    }

    /**
     * Crea un file indice del package con informazioni sui file generati
     */
    private suspend fun createPackageIndex(
        packageDirectory: File,
        wordResult: Success?,
        textResult: Success?,
        photoFolderResult: Success?,
        exportData: ExportData
    ) = withContext(Dispatchers.IO) {

        try {
            val indexFile = File(packageDirectory, "INDICE_PACKAGE.txt")
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

            val content = buildString {
                appendLine("🔧 QREPORT - PACKAGE EXPORT COMPLETO")
                appendLine("=".repeat(50))
                appendLine()
                appendLine("📅 Generato il: $timestamp")
                appendLine("🏭 Cliente: ${exportData.checkup.header.clientInfo.companyName}")
                appendLine("🏝️ Isola: ${exportData.checkup.islandType.displayName}")
                appendLine("👨‍🔧 Tecnico: ${exportData.checkup.header.technicianInfo.name}")
                appendLine()
                appendLine("📁 CONTENUTO PACKAGE:")
                appendLine("-".repeat(30))

                // Word document
                if (wordResult != null) {
                    appendLine("📄 ${wordResult.fileName}")
                    appendLine("   Tipo: Documento Word completo")
                    appendLine("   Dimensione: ${formatFileSize(wordResult.fileSize)}")
                } else {
                    appendLine("❌ Documento Word: Non generato")
                }
                appendLine()

                // Text summary
                if (textResult != null) {
                    appendLine("📝 ${textResult.fileName}")
                    appendLine("   Tipo: Riassunto testuale")
                    appendLine("   Dimensione: ${formatFileSize(textResult.fileSize)}")
                } else {
                    appendLine("❌ Riassunto testuale: Non generato")
                }
                appendLine()

                // Photo folder
                if (photoFolderResult != null) {
                    appendLine("📁 ${photoFolderResult.fileName}/")
                    appendLine("   Tipo: Cartella foto organizzate per modulo")
                    appendLine("   Dimensione totale: ${formatFileSize(photoFolderResult.fileSize)}")

                    // Conta foto per modulo
                    val photosByModule = exportData.itemsByModule.mapValues { (_, items) ->
                        items.flatMap { it.photos }.size
                    }.filter { it.value > 0 }

                    if (photosByModule.isNotEmpty()) {
                        appendLine("   Foto per modulo:")
                        photosByModule.forEach { (moduleType, count) ->
                            appendLine("     - ${moduleType.displayName}: $count foto")
                        }
                    }
                } else {
                    appendLine("❌ Cartella foto: Non generata")
                }
                appendLine()

                // Statistiche checkup
                appendLine("📊 STATISTICHE CHECKUP:")
                appendLine("-".repeat(30))
                appendLine("🔧 Moduli controllati: ${exportData.itemsByModule.size}")
                appendLine("✅ Check items totali: ${exportData.itemsByModule.values.flatten().size}")
                appendLine("📷 Foto totali: ${exportData.itemsByModule.values.flatten().flatMap { it.photos }.size}")
                appendLine("🔩 Parti di ricambio: ${exportData.checkup.spareParts.size}")
                appendLine()
                appendLine("Generato da QReport v1.0")
            }

            indexFile.writeText(content)
            Timber.d("File indice package creato: ${indexFile.name}")

        } catch (e: Exception) {
            Timber.w(e, "Errore creazione file indice package")
        }
    }

    /**
     * Formatta la dimensione del file in modo leggibile
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    override suspend fun validateExportData(exportData: ExportData): List<String> {
        val errors = mutableListOf<String>()

        // Validazioni checkup
        if (exportData.checkup.islandType.displayName.isBlank()) {
            errors.add("Tipo isola non specificato")
        }

        // Validazioni sezioni
        if (exportData.itemsByModule.isEmpty()) {
            errors.add("Nessuna sezione presente nel checkup")
        }

        exportData.itemsByModule.forEach { module ->
            if (module.key.displayName.isBlank()) { // .title.isBlank()) {
                errors.add("Sezione senza titolo trovata")
            }

            if (module.value.isEmpty()) { // section.items.isEmpty()) {
                errors.add("Sezione '${module.key.displayName}' senza check items")
            }
        }

        // Validazioni metadata
        if (exportData.checkup.header.technicianInfo.name.isBlank()) { // .metadata.technicianName.isBlank()) {
            errors.add("Nome tecnico non specificato")
        }

        if (exportData.checkup.header.clientInfo.companyName.isBlank()) { // .metadata.clientInfo.companyName.isBlank()) {
            errors.add("Nome cliente non specificato")
        }

        return errors
    }

    override suspend fun estimateExportSize(
        exportData: ExportData,
        options: ExportOptions
    ): ExportEstimation = withContext(Dispatchers.IO) {

        val formatEstimations = mutableMapOf<ExportFormat, FormatEstimation>()
        var totalSize = 0L
        var totalTime = 0L

        options.exportFormats.forEach { format ->
            val estimation = when (format) {
                ExportFormat.WORD -> estimateWordSize(exportData, options)
                ExportFormat.TEXT -> estimateTextSize(exportData)
                ExportFormat.PHOTO_FOLDER -> estimatePhotoFolderSize(exportData)
                ExportFormat.COMBINED_PACKAGE -> estimateCombinedPackageSize(exportData, options)
            }

            formatEstimations[format] = estimation
            totalSize += estimation.estimatedSizeBytes
            totalTime += estimation.estimatedTimeMs
        }

        val warnings = mutableListOf<String>()
        if (totalSize > 100 * 1024 * 1024) { // > 100MB
            warnings.add("Export di grandi dimensioni (>100MB) - potrebbero servire diversi minuti")
        }

        ExportEstimation(
            estimatedSizeBytes = totalSize,
            estimatedTimeMs = totalTime,
            formatEstimations = formatEstimations,
            warnings = warnings
        )
    }

    override fun getDefaultExportDirectory(): File {
        val documentsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val exportDir = File(documentsDir, EXPORT_BASE_DIR)

        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        return exportDir
    }

    override suspend fun createTimestampedExportDirectory(
        baseDirectory: File,
        checkupData: ExportData
    ): File = withContext(Dispatchers.IO) {

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
        val clientName =
            checkupData.checkup.header.clientInfo.companyName // .metadata.clientInfo.companyName
                .replace(" ", "")
                .replace("[^a-zA-Z0-9]".toRegex(), "")
                .take(15)

        val dirName = "Export_Checkup_${clientName}_$timestamp"
        val exportDir = File(baseDirectory, dirName)

        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        return@withContext exportDir
    }

    override suspend fun cleanupOldExports(olderThanDays: Int): Int = withContext(Dispatchers.IO) {

        var deletedCount = 0
        val exportDir = getDefaultExportDirectory()
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)

        exportDir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.lastModified() < cutoffTime) {
                try {
                    file.deleteRecursively()
                    deletedCount++
                    Timber.d("Eliminata directory export obsoleta: ${file.name}")
                } catch (e: Exception) {
                    Timber.w(e, "Errore eliminazione directory: ${file.name}")
                }
            }
        }

        return@withContext deletedCount
    }

    // === UTILITY METHODS ===

    private fun estimateWordSize(exportData: ExportData, options: ExportOptions): FormatEstimation {
        val baseSize = 500_000L // 500KB base per documento Word
        val photosSize = if (options.includePhotos) {
            val photoCount = exportData.itemsByModule.values.flatten().flatMap { checkItem ->
                checkItem.photos
            }.size
            photoCount * 200_000L // 200KB per foto nel documento Word

            //exportData.sections.flatMap { it.items.flatMap { item -> item.photos } }.size * 200_000L
        } else 0L

        val totalSize = baseSize + photosSize
        val estimatedTime = 3000L + (photosSize / 100_000L) // Base 3s + tempo per foto

        return FormatEstimation(
            format = ExportFormat.WORD,
            estimatedSizeBytes = totalSize,
            estimatedTimeMs = estimatedTime
        )
    }

    private fun estimateTextSize(exportData: ExportData): FormatEstimation {
        val textLength = exportData.itemsByModule.values.flatten().sumOf { checkItem ->
            checkItem.description.length + checkItem.notes.length
        }

        val estimatedSize = (textLength * 2L).coerceAtLeast(10_000L) // Min 10KB

        return FormatEstimation(
            format = ExportFormat.TEXT,
            estimatedSizeBytes = estimatedSize,
            estimatedTimeMs = 1000L // 1 secondo
        )
    }

    private fun estimatePhotoFolderSize(exportData: ExportData): FormatEstimation {
        val photoCount = exportData.itemsByModule.values.flatten().flatMap { checkItem ->
            checkItem.photos
        }.size
        val avgPhotoSize = 2_000_000L // 2MB per foto stimato

        return FormatEstimation(
            format = ExportFormat.PHOTO_FOLDER,
            estimatedSizeBytes = photoCount * avgPhotoSize,
            estimatedTimeMs = photoCount * 500L, // 500ms per foto
            fileCount = photoCount
        )
    }

    private fun estimateCombinedPackageSize(exportData: ExportData, options: ExportOptions): FormatEstimation {
        // Combina le stime di tutti i formati
        val wordEstimation = estimateWordSize(exportData, options)
        val textEstimation = estimateTextSize(exportData)
        val photoEstimation = estimatePhotoFolderSize(exportData)

        // Aggiunge overhead per file indice e struttura cartelle
        val indexFileSize = 10_000L // 10KB per file indice
        val directoryOverhead = 5_000L // 5KB per struttura cartelle

        val totalSize = wordEstimation.estimatedSizeBytes +
                textEstimation.estimatedSizeBytes +
                photoEstimation.estimatedSizeBytes +
                indexFileSize +
                directoryOverhead

        val totalTime = wordEstimation.estimatedTimeMs +
                textEstimation.estimatedTimeMs +
                photoEstimation.estimatedTimeMs +
                2000L // 2s per creazione indice e organizzazione

        return FormatEstimation(
            format = ExportFormat.COMBINED_PACKAGE,
            estimatedSizeBytes = totalSize,
            estimatedTimeMs = totalTime,
            fileCount = (photoEstimation.fileCount ?: 0) + 3 // Word + Text + Indice
        )
    }

    private fun calculateStatistics(
        exportData: ExportData,
        result: MultiFormatExportResult
    ): ExportStatistics {
        val allItems = exportData.itemsByModule.values.flatten()
        val allPhotos = allItems.flatMap { it.photos }

        return ExportStatistics(
            sectionsProcessed = exportData.itemsByModule.size,
            checkItemsProcessed = allItems.size,
            photosProcessed = allPhotos.size,
            photosExported = result.photoFolderResult?.let {
                // Assumiamo che sia una cartella con file count
                allPhotos.size
            } ?: 0,
            sparePartsIncluded = exportData.checkup.spareParts.size,
            dataProcessedBytes = result.totalFileSize
        )
    }
}