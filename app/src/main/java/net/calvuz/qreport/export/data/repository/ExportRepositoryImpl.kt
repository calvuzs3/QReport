package net.calvuz.qreport.export.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.export.data.photo.PhotoExportManager
import net.calvuz.qreport.export.data.text.TextReportGenerator
import net.calvuz.qreport.export.data.word.WordReportGenerator
import net.calvuz.qreport.photo.domain.model.PhotoExportResult
import net.calvuz.qreport.export.data.model.getAllPhotos
import net.calvuz.qreport.export.data.word.ExportException
import net.calvuz.qreport.export.domain.reposirory.ExportEstimation
import net.calvuz.qreport.export.domain.reposirory.ExportRepository
import net.calvuz.qreport.export.domain.reposirory.FormatEstimation
import net.calvuz.qreport.export.domain.model.ExportErrorCode
import net.calvuz.qreport.export.domain.reposirory.ExportData
import net.calvuz.qreport.export.domain.reposirory.ExportFileRepository
import net.calvuz.qreport.export.domain.reposirory.ExportFormat
import net.calvuz.qreport.export.domain.reposirory.ExportInfo
import net.calvuz.qreport.export.domain.reposirory.ExportOptions
import net.calvuz.qreport.export.domain.reposirory.ExportResult
import net.calvuz.qreport.export.domain.reposirory.ExportStatistics
import net.calvuz.qreport.export.domain.reposirory.MultiFormatExportResult
import org.apache.poi.xwpf.usermodel.XWPFDocument
import timber.log.Timber
import java.io.ByteArrayOutputStream
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
    val wordResult: ExportResult.Success? = null,
    val textResult: ExportResult.Success? = null,
    val photoFolderResult: ExportResult.Success? = null,
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
    private val photoExportManager: PhotoExportManager,
    private val exportFileRepository: ExportFileRepository
) : ExportRepository {

    companion object {
        private const val MANIFEST_FILENAME = "export_manifest.json"
    }

    override suspend fun generateCompleteExport(
        exportData: ExportData,
        options: ExportOptions
    ): Flow<MultiFormatExportResult> = flow {

        Timber.i("Generating complete export {formats=${options.exportFormats}}")
        val startTime = System.currentTimeMillis()

        try {
            // 1. Validazione dati
            val validationErrors = validateExportData(exportData)
            if (validationErrors.isNotEmpty()) {
                throw IllegalArgumentException("Export data not valid {errors=${validationErrors.joinToString()}}")
            }

            // 2. ✅ Preparazione directory CENTRALE usando ExportFileRepository
            val exportDirectory = if (options.createTimestampedDirectory) {
                prepareMainExportDirectory(exportData)
            } else {
                getMainExportDirectory()
            }

            // 3. Inizializza risultato
            var result = MultiFormatExportResult(
                exportDirectory = exportDirectory.absolutePath,
                statistics = ExportStatistics()
            )

            // 4. Export per ogni formato richiesto
            options.exportFormats.forEach { format ->
                try {
                    Timber.d("Export format: $format")

                    when (format) {
                        ExportFormat.WORD -> {
                            val wordResult = generateWordReportInternal(
                                exportData = exportData,
                                options = options,
                                targetDirectory = exportDirectory  // ✅ Directory già creata
                            )
                            result = result.copy(wordResult = wordResult as? ExportResult.Success)
                        }

                        ExportFormat.TEXT -> {
                            val textResult = generateTextReportInternal(
                                exportData = exportData,
                                options = options,
                                targetDirectory = exportDirectory  // ✅ Stessa directory
                            )
                            result = result.copy(textResult = textResult as? ExportResult.Success)
                        }

                        ExportFormat.PHOTO_FOLDER -> {
                            val photoResult = generatePhotoFolderInternal(
                                exportData = exportData,
                                options = options,
                                targetDirectory = exportDirectory  // ✅ Stessa directory
                            )
                            if (photoResult is PhotoExportResult.Success) {
                                result = result.copy(
                                    photoFolderResult = ExportResult.Success(
                                        filePath = photoResult.exportDirectory,
                                        fileName = "FOTO",
                                        fileSize = photoResult.totalSize,
                                        format = ExportFormat.PHOTO_FOLDER
                                    )
                                )
                            }
                        }

                        ExportFormat.COMBINED_PACKAGE -> {
                            val combinedResult = generateCombinedPackage(
                                exportData = exportData,
                                options = options,
                                targetDirectory = exportDirectory  // ✅ Stessa directory
                            )
                            result = result.copy(
                                wordResult = combinedResult.wordResult,
                                textResult = combinedResult.textResult,
                                photoFolderResult = combinedResult.photoFolderResult
                            )
                        }
                    }

                    // Emetti progresso intermedio
                    val updatedStats = calculateStatistics(exportData, result)
                    emit(result.copy(statistics = updatedStats))

                } catch (e: Exception) {
                    Timber.e(e, "Export format error {format=$format}")
                    // Continua con altri formati
                }
            }

            // 5. Calcola statistiche finali
            val processingTime = System.currentTimeMillis() - startTime
            val finalStats = calculateStatistics(exportData, result).copy(
                processingTimeMs = processingTime
            )

            // 6. Emetti risultato finale
            emit(result.copy(statistics = finalStats))

            Timber.i("Complete export completed {time=${finalStats.processingTimeFormatted}}")

        } catch (e: Exception) {
            Timber.e(e, "Complete export failed")
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

        return lastResult ?: throw IllegalStateException("Export not completed")
    }

    private suspend fun generateWordReportInternal(
        exportData: ExportData,
        options: ExportOptions,
        targetDirectory: File  // ✅ Directory principale già esistente
    ): ExportResult {
        return try {
            Timber.d("Generating Word report in: ${targetDirectory.absolutePath}")

            // ✅ 1. Create WORD subdirectory inside target
            val wordSubDirectory = File(targetDirectory, "word").apply { mkdirs() }

            // 2. Create working directory for temp photos
            val workingDirectory = File(wordSubDirectory, "working").apply { mkdirs() }

            // 3. Generate document content
            val document = wordReportGenerator.generateDocumentContent(
                exportData = exportData,
                options = options,
                workingDirectory = workingDirectory
            )

            // 4. Generate filename using ExportFileRepository
            val fileName = exportFileRepository.generateExportFileName(
                checkupId = exportData.checkup.id,
                clientName = exportData.checkup.header.clientInfo.companyName,
                format = ExportFormat.WORD,
                includeTimestamp = true
            )

            // ✅ 5. Save document in WORD subdirectory
            val filePath = saveWordDocumentInternal(
                document = document,
                fileName = fileName,
                exportDirectory = wordSubDirectory.absolutePath  // ✅ Word subdirectory
            )

            // 6. Cleanup
            workingDirectory.deleteRecursively()
            document.close()

            ExportResult.Success(
                filePath = filePath,
                fileName = fileName,
                fileSize = File(filePath).length(),
                format = ExportFormat.WORD
            )

        } catch (e: Exception) {
            Timber.e(e, "Error generating Word report")
            ExportResult.Error(
                exception = e,
                errorCode = ExportErrorCode.DOCUMENT_GENERATION_ERROR,
                format = ExportFormat.WORD
            )
        }
    }

    private suspend fun saveWordDocumentInternal(
        document: XWPFDocument,
        fileName: String,
        exportDirectory: String
    ): String {
        // Convert XWPFDocument to ByteArray
        val documentBytes = ByteArrayOutputStream().use { byteStream ->
            document.write(byteStream)
            byteStream.toByteArray()
        }

        // Use ExportFileRepository for saving
        return when (val result = exportFileRepository.saveWordDocument(
            documentBytes = documentBytes,
            fileName = fileName,
            directory = exportDirectory
        )) {
            is QrResult.Success -> result.data
            is QrResult.Error -> throw Exception("Failed to save Word document: ${result.error}")
        }
    }

    override suspend fun generateTextReport(
        exportData: ExportData,
        options: ExportOptions
    ): ExportResult = withContext(Dispatchers.IO) {

        return@withContext generateTextReportInternal(
            exportData = exportData,
            options = options,
            targetDirectory = getMainExportDirectory()
        )
    }

    private suspend fun generateTextReportInternal(
        exportData: ExportData,
        options: ExportOptions,
        targetDirectory: File  // ✅ Directory principale già esistente
    ): ExportResult {
        return try {
            Timber.d("Generating Text report in: ${targetDirectory.absolutePath}")

            // ✅ 1. Create TEXT subdirectory inside target
            val textSubDirectory = File(targetDirectory, "text").apply { mkdirs() }

            // 2. Generate text report using TextReportGenerator
            val textResult = textReportGenerator.generateTextReport(exportData, options)

            // 3. Generate filename
            val fileName = exportFileRepository.generateExportFileName(
                checkupId = exportData.checkup.id,
                clientName = exportData.checkup.header.clientInfo.companyName,
                format = ExportFormat.TEXT,
                includeTimestamp = true
            )

            // ✅ 4. Save text file in TEXT subdirectory
            val filePath = File(textSubDirectory, fileName).absolutePath
            when (val saveResult = exportFileRepository.saveExportContent(filePath, textResult)) {
                is QrResult.Error -> throw Exception("Failed to save text report: ${saveResult.error}")
                is QrResult.Success -> {
                    ExportResult.Success(
                        filePath = filePath,
                        fileName = fileName,
                        fileSize = File(filePath).length(),
                        format = ExportFormat.TEXT
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Error generating Text report")
            ExportResult.Error(
                exception = e,
                errorCode = ExportErrorCode.DOCUMENT_GENERATION_ERROR,
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
        targetDirectory: File  // ✅ Directory principale già esistente
    ): PhotoExportResult {
        return try {
            Timber.d("Generating Photo folder in: ${targetDirectory.absolutePath}")

            // ✅ 1. Create PHOTOS subdirectory inside target
            val photosSubDirectory = File(targetDirectory, "photos").apply { mkdirs() }

            val options = options
            // 2. Use PhotoExportManager to export photos
            photoExportManager.exportPhotosToFolder(
                exportData = exportData,
                targetDirectory = photosSubDirectory,  // ✅ Photos subdirectory
                namingStrategy = options.photoNamingStrategy,
                quality = options.photoQuality,
                preserveExifData = options.preserveExifData,
                addWatermark = options.addWatermark,
                watermarkText = options.watermarkText,
                generateIndex = options.generatePhotoIndex,
            )

        } catch (e: Exception) {
            Timber.e(e, "Error generating Photo folder")
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

            var wordResult: ExportResult.Success? = null
            var textResult: ExportResult.Success? = null
            var photoFolderResult: ExportResult.Success? = null

            // 1. Genera documento Word
            try {
                val wordExportResult =
                    generateWordReportInternal(exportData, options, targetDirectory)
                if (wordExportResult is ExportResult.Success) {
                    wordResult = wordExportResult
                    Timber.d("Word document generato: ${wordResult.fileName}")
                }
            } catch (e: Exception) {
                Timber.w(e, "Errore generazione Word nel package combinato")
            }

            // 2. Genera riassunto testuale
            try {
                val textExportResult =
                    generateTextReportInternal(exportData, options, targetDirectory)
                if (textExportResult is ExportResult.Success) {
                    textResult = textExportResult
                    Timber.d("Text summary generato: ${textResult.fileName}")
                }
            } catch (e: Exception) {
                Timber.w(e, "Errore generazione Text nel package combinato")
            }

            // 3. Genera cartella foto
            try {
                val photoExportResult =
                    generatePhotoFolderInternal(exportData, options, targetDirectory)
                if (photoExportResult is PhotoExportResult.Success) {
                    photoFolderResult = ExportResult.Success(
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
            createPackageIndex(
                targetDirectory,
                wordResult,
                textResult,
                photoFolderResult,
                exportData
            )

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
        wordResult: ExportResult.Success?,
        textResult: ExportResult.Success?,
        photoFolderResult: ExportResult.Success?,
        exportData: ExportData
    ) = withContext(Dispatchers.IO) {

        try {
            val indexFile = File(packageDirectory, "INDICE_PACKAGE.txt")
            val timestamp =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

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
                appendLine(
                    "📷 Foto totali: ${
                        exportData.itemsByModule.values.flatten().flatMap { it.photos }.size
                    }"
                )
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

    override suspend fun createTimestampedExportDirectory(
        baseDirectory: File,
        checkupData: ExportData
    ): File = withContext(Dispatchers.IO) {

        val clientName =
            checkupData.checkup.header.clientInfo.companyName // .metadata.clientInfo.companyName
                .replace(" ", "")
                .replace("[^a-zA-Z0-9]".toRegex(), "")
                .take(20)

        val dirName = exportFileRepository.generateExportDirectoryName(checkupData.checkup.id, clientName)
        val exportDir = File(baseDirectory, dirName)

        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        return@withContext exportDir
    }

    override suspend fun cleanupOldExports(olderThanDays: Int): Int = withContext(Dispatchers.IO) {

        var deletedCount = 0

        val exportDir = getMainExportDirectory()
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

            exportData.checkup.checkItems.getAllPhotos().size * 200_000L // 200KB per photo
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

    private fun estimateCombinedPackageSize(
        exportData: ExportData,
        options: ExportOptions
    ): FormatEstimation {
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
            fileCount = (photoEstimation.fileCount) + 3 // Word + Text + Indice
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

    // WORD ==============================================

    /**
     * Save Word document using ExportFileRepository
     */
    private suspend fun saveWordDocument(
        document: XWPFDocument,
        fileName: String,
        exportDirectory: File
    ): String {
        return when (val fileResult = exportFileRepository.createExportFile(
            directory = exportDirectory.absolutePath,
            fileName = fileName,
            format = ExportFormat.WORD
        )) {
            is QrResult.Success -> {
                val filePath = fileResult.data

                // ✅ Converte XWPFDocument a ByteArray
                val documentBytes = ByteArrayOutputStream().use { byteStream ->
                    document.write(byteStream)
                    byteStream.toByteArray()
                }

                // ✅ USA ExportFileRepository per salvare
                when (val saveResult =
                    exportFileRepository.saveExportBinary(filePath, documentBytes)) {
                    is QrResult.Success -> {
                        // Validate document
                        when (val validateResult =
                            exportFileRepository.validateExportFile(filePath, ExportFormat.WORD)) {
                            is QrResult.Success -> {
                                if (validateResult.data) {
                                    Timber.d("Word document saved and validated: $filePath")
                                    filePath
                                } else {
                                    throw ExportException("Word document validation failed: $filePath")
                                }
                            }

                            is QrResult.Error -> {
                                throw ExportException(
                                    "Failed to validate Word document",
                                    validateResult
                                )
                            }
                        }
                    }

                    is QrResult.Error -> {
                        throw ExportException("Failed to save Word document", saveResult)
                    }
                }
            }

            is QrResult.Error -> {
                throw ExportException("Failed to create Word export file", fileResult)
            }
        }
    }

    private fun generateWordFileName(exportData: ExportData): String {
        return exportFileRepository.generateExportFileName(
            checkupId = exportData.checkup.id,
            clientName = exportData.checkup.header.clientInfo.companyName,
            format = ExportFormat.WORD
        )
    }

    private suspend fun createExportManifest(
        exportDirectory: String,
        exportInfo: ExportInfo
    ): QrResult<String, QrError> {
        return try {
            val manifestPath = "$exportDirectory/$MANIFEST_FILENAME"

            // Create simple JSON manifest
            val manifestContent = buildString {
                appendLine("{")
                appendLine("  \"checkupId\": \"${exportInfo.checkupId}\",")
                appendLine("  \"createdAt\": ${exportInfo.createdAt},")
                appendLine("  \"format\": \"${exportInfo.format}\",")
                appendLine("  \"sizeBytes\": ${exportInfo.sizeBytes},")
                appendLine("  \"fileCount\": ${exportInfo.fileCount},")
                appendLine("  \"hasPhotos\": ${exportInfo.hasPhotos},")
                appendLine("  \"hasAttachments\": ${exportInfo.hasAttachments},")
                appendLine("  \"status\": \"${exportInfo.status}\"")
                appendLine("}")
            }

            when (val result = exportFileRepository.saveExportContent(manifestPath, manifestContent)) {
                is QrResult.Error -> QrResult.Error(result.error)
                is QrResult.Success -> {
                    Timber.d("Created export manifest: $manifestPath")
                    QrResult.Success(manifestPath)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create export manifest")
            QrResult.Error(QrError.ExportError.MANIFEST_CREATE_FAILED)
        }
    }

    private suspend fun prepareMainExportDirectory(exportData: ExportData): File {
        return when (val result = exportFileRepository.getExportSubDirectory(
            checkupId = exportData.checkup.id,
            clientName = exportData.checkup.header.clientInfo.companyName,
            includeTimestamp = true
        )) {
            is QrResult.Success -> {
                Timber.d("Main export directory prepared: ${result.data}")
                File(result.data)
            }
            is QrResult.Error -> {
                throw Exception("Failed to create main export directory: ${result.error}")
            }
        }
    }

    private suspend fun getMainExportDirectory(): File {
        return when (val result = exportFileRepository.getExportsDirectory()) {
            is QrResult.Success -> File(result.data)
            is QrResult.Error -> throw Exception("Failed to get exports directory: ${result.error}")
        }
    }
}