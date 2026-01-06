package net.calvuz.qreport.export.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.file.domain.model.DirectorySpec
import net.calvuz.qreport.app.file.domain.repository.CoreFileRepository
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.app.util.DateTimeUtils.toFilenameSafeDate
import net.calvuz.qreport.export.domain.model.ExportDirectories
import net.calvuz.qreport.export.domain.reposirory.ExportFileInfo
import net.calvuz.qreport.export.domain.reposirory.ExportFileRepository
import net.calvuz.qreport.export.domain.reposirory.ExportFormat
import net.calvuz.qreport.export.domain.reposirory.ExportInfo
import net.calvuz.qreport.export.domain.reposirory.ExportStatus
import net.calvuz.qreport.share.domain.usecase.OpenFileUseCase
import net.calvuz.qreport.share.domain.usecase.ShareFileUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ExportFileRepository using CoreFileRepository
 *
 * Gestisce tutte le operazioni file per la feature Export:
 * - Directory management per exports
 * - File creation and management
 * - Cleanup and maintenance
 * - Metadata and validation
 */
@Singleton
class ExportFileRepositoryImpl @Inject constructor(
    private val coreFileRepo: CoreFileRepository,
    private val shareFile: ShareFileUseCase,
    private val openFile: OpenFileUseCase
) : ExportFileRepository {

    companion object {
        private const val PHOTOS_SUBDIRECTORY = "photos"
        private const val ATTACHMENTS_SUBDIRECTORY = "attachments"
        private const val REPORTS_SUBDIRECTORY = "reports"
        private const val TEMP_PREFIX = "temp_"

    }

    // ===== EXPORT DIRECTORIES =====

    override fun generateExportBaseName(
        checkupId: String,
        clientName: String,
        includeTimestamp: Boolean
    ): String {
        val timestamp = if (includeTimestamp) {
            Clock.System.now().toFilenameSafeDate()
        } else ""

        val prefix = if (includeTimestamp) "${timestamp}_" else ""
        return "${prefix}Checkup_${clientName}_${checkupId.take(8)}"
    }

    override fun generateExportFileName(
        checkupId: String,
        clientName:String,
        format: ExportFormat,
        includeTimestamp: Boolean
    ): String {
        val baseName = generateExportBaseName(checkupId, clientName, includeTimestamp)
        return "$baseName${format.extension}"
    }

    override suspend fun generateExportDirectoryName(
        checkupId: String,
        clientName: String,
        includeTimestamp: Boolean
    ): String {
        return generateExportBaseName(checkupId, clientName, includeTimestamp)
    }

    // Getters

    override suspend fun getExportsDirectory(): QrResult<String, QrError.FileError> {
        return try {
            coreFileRepo.getOrCreateDirectory(ExportDirectories.EXPORTS)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get exports directory")
            QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
        }
    }

    override suspend fun getExportSubDirectory(
        checkupId: String,
        clientName: String,
//        format: ExportFormat,
        includeTimestamp: Boolean
    ): QrResult<String, QrError.FileError> {

        return try {
            when (val dirName = getExportsDirectory()) {
                is QrResult.Error -> return dirName
                is QrResult.Success -> {

                    val exportSubDirName = generateExportDirectoryName(
                        checkupId = checkupId,
                        clientName = clientName,
                        includeTimestamp = includeTimestamp
                    )

                    val subDirSpec = DirectorySpec("${dirName.data}/$exportSubDirName")

                    val exportSubDir = when (val subDirName = coreFileRepo.getOrCreateDirectory(subDirSpec)) {
                        is QrResult.Error -> return subDirName
                        is QrResult.Success -> subDirName.data
                    }

                    QrResult.Success(exportSubDir)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to create export directory for checkup: $checkupId")
            QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
        }
    }

//    @Deprecated("DO NOT USE")
//    override suspend fun getCheckupExportDirectory(checkupId: String): QrResult<String, QrError> {
//        return try {
//            val exportsDir = when (val result = getExportsDirectory()) {
//                is QrResult.Error -> return result
//                is QrResult.Success -> result.data
//            }
//
//            val exportDirPath = "$exportsDir/checkup_$checkupId"
//            QrResult.Success(exportDirPath)
//
//        } catch (e: Exception) {
//            Timber.e(e, "Failed to get checkup export directory: $checkupId")
//            QrResult.Error(QrError.ExportError.DIRECTORY_ACCESS)
//        }
//    }

    // ===== EXPORT FILE MANAGEMENT =====

    override suspend fun createExportFile(
        directory: String,
        fileName: String,
        format: ExportFormat
    ): QrResult<String, QrError> {
        return try {
            // Ensure directory exists
            when (val dirResult = createDirectoryIfNotExists(directory)) {
                is QrResult.Error -> return dirResult
                is QrResult.Success -> Unit
            }

            // Add proper extension if not present
            val extension = getFileExtension(format)
            val fullFileName = if (fileName.endsWith(extension)) {
                fileName
            } else {
                "$fileName$extension"
            }

            val filePath = "$directory/$fullFileName"

            when (val result = createFile(filePath)) {
                is QrResult.Error -> result
                is QrResult.Success -> {
                    Timber.d("Created export file: $filePath")
                    QrResult.Success(filePath)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create export file: $fileName")
            QrResult.Error(QrError.ExportError.FILE_CREATE)
        }
    }

    override suspend fun saveExportContent(
        filePath: String,
        content: String
    ): QrResult<Unit, QrError> {
        return try {
            when (val result = writeTextToFile(filePath, content)) {
                is QrResult.Error -> result
                is QrResult.Success -> {
                    Timber.d("Saved export content to: $filePath (${content.length} chars)")
                    QrResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to save export content to: $filePath")
            QrResult.Error(QrError.ExportError.FILE_WRITE)
        }
    }

    override suspend fun saveExportBinary(
        filePath: String,
        data: ByteArray
    ): QrResult<Unit, QrError> {
        return try {
            when (val result = writeBinaryToFile(filePath, data)) {
                is QrResult.Error -> result
                is QrResult.Success -> {
                    Timber.d("Saved export binary to: $filePath (${data.size} bytes)")
                    QrResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to save export binary to: $filePath")
            QrResult.Error(QrError.ExportError.FILE_WRITE)
        }
    }

    override suspend fun copyFileToExport(
        sourceFilePath: String,
        exportDirectory: String,
        targetFileName: String
    ): QrResult<String, QrError> {
        return try {
            // Ensure export directory exists
            when (val dirResult = createDirectoryIfNotExists(exportDirectory)) {
                is QrResult.Error -> return QrResult.Error(dirResult.error)
                is QrResult.Success -> Unit
            }

            val targetFilePath = "$exportDirectory/$targetFileName"

            when (val result = coreFileRepo.copyFile(sourceFilePath, targetFilePath)) {
                is QrResult.Error -> QrResult.Error(result.error)
                is QrResult.Success -> {
                    Timber.d("Copied file to export: $sourceFilePath → $targetFilePath")
                    QrResult.Success(targetFilePath)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy file to export: $sourceFilePath")
            QrResult.Error(QrError.ExportError.FILE_COPY)
        }
    }

    override suspend fun copyFilesToExport(
        sourceFilePaths: List<String>,
        exportDirectory: String,
        preserveNames: Boolean
    ): QrResult<List<String>, QrError> {
        return try {
            // Ensure export directory exists
            when (val dirResult = createDirectoryIfNotExists(exportDirectory)) {
                is QrResult.Error -> return QrResult.Error(dirResult.error)
                is QrResult.Success -> Unit
            }

            val copiedFiles = mutableListOf<String>()

            for ((index, sourceFilePath) in sourceFilePaths.withIndex()) {
                val sourceFile = File(sourceFilePath)
                val targetFileName = if (preserveNames) {
                    sourceFile.name
                } else {
                    "file_${index + 1}.${sourceFile.extension}"
                }

                val copyResult = copyFileToExport(sourceFilePath, exportDirectory, targetFileName)
                when (copyResult) {
                    is QrResult.Error -> {
                        Timber.w("Failed to copy file: $sourceFilePath")
                        continue // Skip failed files but continue with others
                    }

                    is QrResult.Success -> {
                        copiedFiles.add(copyResult.data)
                    }
                }
            }

            QrResult.Success(copiedFiles)

        } catch (e: Exception) {
            Timber.e(e, "Failed to copy files to export")
            QrResult.Error(QrError.ExportError.FILE_COPY)
        }
    }

    // ===== EXPORT SUBDIRECTORIES =====

    override suspend fun createPhotosSubdirectory(exportDirectory: String): QrResult<String, QrError> {
        val photosDir = "$exportDirectory/$PHOTOS_SUBDIRECTORY"
        return createSubdirectory(photosDir, "photos")
    }

    override suspend fun createAttachmentsSubdirectory(exportDirectory: String): QrResult<String, QrError> {
        val attachmentsDir = "$exportDirectory/$ATTACHMENTS_SUBDIRECTORY"
        return createSubdirectory(attachmentsDir, "attachments")
    }

    override suspend fun createReportsSubdirectory(exportDirectory: String): QrResult<String, QrError> {
        val reportsDir = "$exportDirectory/$REPORTS_SUBDIRECTORY"
        return createSubdirectory(reportsDir, "reports")
    }

    private suspend fun createSubdirectory(
        dirPath: String,
        type: String
    ): QrResult<String, QrError> {
        return try {
            when (val result = createDirectoryIfNotExists(dirPath)) {
                is QrResult.Error -> result
                is QrResult.Success -> {
                    Timber.d("Created $type subdirectory: $dirPath")
                    QrResult.Success(dirPath)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create $type subdirectory: $dirPath")
            QrResult.Error(QrError.ExportError.DIRECTORY_CREATE)
        }
    }

    // ===== EXPORT CLEANUP =====

    override suspend fun cleanupOldExports(olderThanDays: Int): QrResult<Int, QrError> {
        return try {
            val exportsDir = when (val result = getExportsDirectory()) {
                is QrResult.Error -> return QrResult.Error(result.error)
                is QrResult.Success -> result.data
            }

            when (val listResult = coreFileRepo.listFiles(exportsDir)) {
                is QrResult.Error -> QrResult.Error(listResult.error)
                is QrResult.Success -> {
                    val cutoffTime =
                        System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
                    var deletedCount = 0

                    for (fileInfo in listResult.data) {
                        if (fileInfo.name.startsWith("checkup_") && fileInfo.lastModified < cutoffTime) {
                            when (coreFileRepo.deleteFile(fileInfo.path)) {
                                is QrResult.Success -> {
                                    deletedCount++
                                    Timber.d("Deleted old export: ${fileInfo.name}")
                                }

                                is QrResult.Error -> {
                                    Timber.w("Failed to delete old export: ${fileInfo.name}")
                                }
                            }
                        }
                    }

                    Timber.d("Cleaned up $deletedCount old exports (older than $olderThanDays days)")
                    QrResult.Success(deletedCount)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup old exports")
            QrResult.Error(QrError.ExportError.CLEANUP_FAILED)
        }
    }

//    override suspend fun deleteExport(checkupId: String): QrResult<Unit, QrError> {
//        return try {
//            val exportDir = when (val result = getCheckupExportDirectory(checkupId)) {
//                is QrResult.Error -> return QrResult.Error(result.error)
//                is QrResult.Success -> result.data
//            }
//
//            when (val result = coreFileRepo.deleteFile(exportDir)) {
//                is QrResult.Error -> result
//                is QrResult.Success -> {
//                    Timber.d("Deleted export for checkup: $checkupId")
//                    QrResult.Success(Unit)
//                }
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "Failed to delete export for checkup: $checkupId")
//            QrResult.Error(QrError.ExportError.DELETE_FAILED)
//        }
//    }

    override suspend fun cleanupTemporaryExports(): QrResult<Int, QrError> {
        return try {
            val exportsDir = when (val result = getExportsDirectory()) {
                is QrResult.Error -> return QrResult.Error(result.error)
                is QrResult.Success -> result.data
            }

            when (val listResult = coreFileRepo.listFiles(exportsDir)) {
                is QrResult.Error -> QrResult.Error(listResult.error)
                is QrResult.Success -> {
                    var deletedCount = 0

                    for (fileInfo in listResult.data) {
                        if (fileInfo.name.startsWith(TEMP_PREFIX)) {
                            when (coreFileRepo.deleteFile(fileInfo.path)) {
                                is QrResult.Success -> {
                                    deletedCount++
                                    Timber.d("Deleted temporary export: ${fileInfo.name}")
                                }

                                is QrResult.Error -> {
                                    Timber.w("Failed to delete temporary export: ${fileInfo.name}")
                                }
                            }
                        }
                    }

                    Timber.d("Cleaned up $deletedCount temporary exports")
                    QrResult.Success(deletedCount)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup temporary exports")
            QrResult.Error(QrError.ExportError.CLEANUP_FAILED)
        }
    }

    override suspend fun getExportsDirectorySize(): QrResult<Long, QrError> {
        return try {
            val exportsDir = when (val result = getExportsDirectory()) {
                is QrResult.Error -> return QrResult.Error(result.error)
                is QrResult.Success -> result.data
            }

            when (val result = coreFileRepo.getDirectorySize(exportsDir)) {
                is QrResult.Error -> result
                is QrResult.Success -> {
                    Timber.d("Exports directory size: ${result.data} bytes")
                    QrResult.Success(result.data)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get exports directory size")
            QrResult.Error(QrError.ExportError.SIZE_CALCULATION_FAILED)
        }
    }

    // ===== EXPORT VALIDATION =====

    override suspend fun validateExportDirectory(exportPath: String): QrResult<Boolean, QrError> {
        return try {
            when (val exists = coreFileRepo.fileExists(exportPath)) {
                true -> QrResult.Success(true)  // Already exists, no need to result
                false -> {
                    // Check if it's a directory and has proper structure
                    val file = File(exportPath)
                    val isValid = file.isDirectory && file.canRead()
                    QrResult.Success(isValid)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to validate export directory: $exportPath")
            QrResult.Error(QrError.ExportError.VALIDATION_FAILED)
        }
    }

//    override suspend fun exportExists(checkupId: String): QrResult<Boolean, QrError> {
//        return try {
//            val exportDir = when (val result = getCheckupExportDirectory(checkupId)) {
//                is QrResult.Error -> return QrResult.Error(result.error)
//                is QrResult.Success -> result.data
//            }
//
//            when (coreFileRepo.fileExists(exportDir)) {
//                true -> QrResult.Success(true)
//                false -> QrResult.Success(false)
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "Failed to check export existence for checkup: $checkupId")
//            QrResult.Error(QrError.ExportError.VALIDATION_FAILED)
//        }
//    }

    override suspend fun getExportSize(exportDirectory: String): QrResult<Long, QrError> {
        return try {
            when (val result = coreFileRepo.getDirectorySize(exportDirectory)) {
                is QrResult.Error -> result
                is QrResult.Success -> {
                    Timber.d("Export size: ${result.data} bytes")
                    QrResult.Success(result.data)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get export size: $exportDirectory")
            QrResult.Error(QrError.ExportError.SIZE_CALCULATION_FAILED)
        }
    }

    override suspend fun validateExportFile(
        filePath: String,
        format: ExportFormat
    ): QrResult<Boolean, QrError> {
        return try {
            when (val existsResult = coreFileRepo.fileExists(filePath)) {
                true -> {
                    // Basic validation - file exists and has expected extension
                    val expectedExtension = getFileExtension(format)
                    val hasCorrectExtension = filePath.endsWith(expectedExtension)
                    QrResult.Success(hasCorrectExtension)
                }

                false -> QrResult.Error(QrError.ExportError.VALIDATION_FAILED)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to validate export file: $filePath")
            QrResult.Error(QrError.ExportError.VALIDATION_FAILED)
        }
    }

    // ===== EXPORT LISTING & METADATA =====

    override suspend fun listExports(): QrResult<List<ExportInfo>, QrError> {
        return try {
            val exportsDir = when (val result = getExportsDirectory()) {
                is QrResult.Error -> return QrResult.Error(result.error)
                is QrResult.Success -> result.data
            }

            when (val listResult = coreFileRepo.listFiles(exportsDir)) {
                is QrResult.Error -> QrResult.Error(listResult.error)
                is QrResult.Success -> {
                    val exports = mutableListOf<ExportInfo>()

                    for (fileInfo in listResult.data) {
                        if ( fileInfo.isDirectory) {
                            val checkupId = extractCheckupId(fileInfo.name)
                            if (checkupId != null) {
                                val exportInfo = createExportInfo(
                                    checkupId,
                                    fileInfo.path,
                                    fileInfo.lastModified
                                )
                                exports.add(exportInfo)
                            }
                        }
                    }

                    QrResult.Success(exports.sortedByDescending { it.createdAt })
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to list exports")
            QrResult.Error(QrError.ExportError.LIST_FAILED)
        }
    }

//    override suspend fun getExportInfo(checkupId: String): QrResult<ExportInfo?, QrError> {
//        return try {
//            val exportDir = when (val result = getCheckupExportDirectory(checkupId)) {
//                is QrResult.Error -> return QrResult.Error(result.error)
//                is QrResult.Success -> result.data
//            }
//
//            when (coreFileRepo.fileExists(exportDir)) {
//                true -> {
//                    val file = File(exportDir)
//                    val exportInfo = createExportInfo(checkupId, exportDir, file.lastModified())
//                    QrResult.Success(exportInfo)
//                }
//
//                false -> {
//                    QrResult.Error(QrError.ExportError.INFO_FAILED)
//                }
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "Failed to get export info for checkup: $checkupId")
//            QrResult.Error(QrError.ExportError.INFO_FAILED)
//        }
//    }

    override suspend fun listExportsNewerThan(timestampMs: Long): QrResult<List<ExportInfo>, QrError> {
        return try {
            when (val allExports = listExports()) {
                is QrResult.Error -> allExports
                is QrResult.Success -> {
                    val recentExports = allExports.data.filter { it.createdAt > timestampMs }
                    QrResult.Success(recentExports)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to list exports newer than: $timestampMs")
            QrResult.Error(QrError.ExportError.LIST_FAILED)
        }
    }

    override suspend fun getExportFiles(exportDirectory: String): QrResult<List<ExportFileInfo>, QrError> {
        return try {
            when (val listResult = coreFileRepo.listFiles(exportDirectory)) {
                is QrResult.Error -> QrResult.Error(listResult.error)
                is QrResult.Success -> {
                    val exportFiles = listResult.data
                        .filter { !it.isDirectory }
                        .map { fileInfo ->
                            val format = detectExportFormat(fileInfo.name)
                            val isMainReport = isMainReportFile(fileInfo.name)

                            ExportFileInfo(
                                fileName = fileInfo.name,
                                filePath = fileInfo.path,
                                format = format,
                                sizeBytes = fileInfo.size,
                                createdAt = fileInfo.lastModified,
                                isMainReport = isMainReport
                            )
                        }

                    QrResult.Success(exportFiles)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get export files: $exportDirectory")
            QrResult.Error(QrError.ExportError.LIST_FAILED)
        }
    }

    // ===== EXPORT UTILITIES =====

    override fun getFileExtension(format: ExportFormat): String {
        return when (format) {
            ExportFormat.WORD -> ".docx"
            ExportFormat.TEXT -> ".txt"
            ExportFormat.PHOTO_FOLDER -> ""  // Directory
            ExportFormat.COMBINED_PACKAGE -> ".zip"
        }
    }

    override suspend fun checkStorageSpace(estimatedSizeMB: Int): QrResult<Boolean, QrError> {
        return try {
            val exportsDir = when (val result = getExportsDirectory()) {
                is QrResult.Error -> return QrResult.Error(result.error)
                is QrResult.Success -> result.data
            }

            val file = File(exportsDir)
            val availableSpaceBytes = file.freeSpace
            val estimatedSizeBytes = estimatedSizeMB * 1024 * 1024L

            val hasSpace = availableSpaceBytes > estimatedSizeBytes

            Timber.d("Storage check - Available: ${availableSpaceBytes / 1024 / 1024}MB, Required: ${estimatedSizeMB}MB")
            QrResult.Success(hasSpace)

        } catch (e: Exception) {
            Timber.e(e, "Failed to check storage space")
            QrResult.Error(QrError.ExportError.STORAGE_CHECK_FAILED)
        }
    }

    override suspend fun saveWordDocument(
        documentBytes: ByteArray,
        fileName: String,
        directory: String
    ): QrResult<String, QrError> {

        // 1. Create file with proper naming
        when (val fileResult = createExportFile(directory, fileName, ExportFormat.WORD)) {
            is QrResult.Error -> return fileResult
            is QrResult.Success -> {
                val filePath = fileResult.data

                // 2. Save binary content
                when (val saveResult = saveExportBinary(filePath, documentBytes)) {
                    is QrResult.Error -> return QrResult.Error(saveResult.error)
                    is QrResult.Success -> {

                        // 3. Validate saved file
                        when (val validateResult = validateExportFile(filePath, ExportFormat.WORD)) {
                            is QrResult.Error -> return QrResult.Error(validateResult.error)
                            is QrResult.Success -> {
                                return if (validateResult.data) {
                                    QrResult.Success(filePath)
                                } else {
                                    QrResult.Error(QrError.ExportError.VALIDATION_FAILED)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun getMainExportFile(
        exportDirectory: String,
        format: ExportFormat
    ): QrResult<String, QrError> {
        return try {
            when (val filesResult = getExportFiles(exportDirectory)) {
                is QrResult.Error -> return QrResult.Error(filesResult.error)
                is QrResult.Success -> {
                    // Find main report file
                    val mainFile = filesResult.data.firstOrNull { fileInfo ->
                        fileInfo.isMainReport && fileInfo.format == format
                    } ?: filesResult.data.firstOrNull { fileInfo ->
                        fileInfo.format == format
                    }

                    if (mainFile != null) {
                        QrResult.Success(mainFile.filePath)
                    } else {
                        // Fallback: construct expected filename
                        val expectedExtension = getFileExtension(format)
                        val fallbackFile = "$exportDirectory/report$expectedExtension"

                        when (val existsResult = fileExistsWithResult(fallbackFile)) {
                            is QrResult.Error -> QrResult.Error(existsResult.error)
                            is QrResult.Success -> {
                                if (existsResult.data) {
                                    QrResult.Success(fallbackFile)
                                } else {
                                    QrResult.Error(QrError.FileError.FILE_NOT_FOUND)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to find main export file in: $exportDirectory")
            QrResult.Error(QrError.FileError.FILE_NOT_FOUND)
        }
    }

    // ===== EXPORT FILE SHARING & OPENING IMPLEMENTATION =====

    override suspend fun openFileWith(
        filePath: String,
        format: ExportFormat
    ): QrResult<Unit, QrError> {
        return try {
            // Validate file exists first
            when (val existsResult = fileExistsWithResult(filePath)) {
                is QrResult.Error -> return QrResult.Error(existsResult.error)
                is QrResult.Success -> {
                    if (!existsResult.data) {
                        return QrResult.Error(QrError.FileError.FILE_NOT_FOUND)
                    }
                }
            }

            // Determine MIME type from export format
            val mimeType = when (format) {
                ExportFormat.WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ExportFormat.TEXT -> "text/plain"
                ExportFormat.PHOTO_FOLDER -> "application/zip"  // Will be zipped for opening
                ExportFormat.COMBINED_PACKAGE -> "application/zip"
            }

            val chooserTitle = "Apri report"

            // Use ShareFileRepository for actual opening
            when (
                openFile(
                    filePath = filePath,
                    mimeType = mimeType,
                    chooserTitle = chooserTitle
                )
            ) {
                is QrResult.Error -> {
                    Timber.e("Failed to open export file: $filePath")
                    QrResult.Error(QrError.ShareError.OPEN_FAILED)
                }

                is QrResult.Success -> {
                    Timber.d("Export file opened successfully: $filePath")
                    QrResult.Success(Unit)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Exception opening export file: $filePath")
            QrResult.Error(QrError.ShareError.OPEN_FAILED)
        }
    }

    override suspend fun shareFileWith(
        filePath: String,
        format: ExportFormat
    ): QrResult<Unit, QrError> {
        return try {
            // Validate file exists first
            when (val existsResult = fileExistsWithResult(filePath)) {
                is QrResult.Error -> return QrResult.Error(existsResult.error)
                is QrResult.Success -> {
                    if (!existsResult.data) {
                        return QrResult.Error(QrError.FileError.FILE_NOT_FOUND)
                    }
                }
            }

            // Determine MIME type and sharing title from export format
            val (mimeType, shareTitle) = when (format) {
                ExportFormat.WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "Condividi report QReport (Word)"
                ExportFormat.TEXT -> "text/plain" to "Condividi report QReport (Testo)"
                ExportFormat.PHOTO_FOLDER -> "application/zip" to "Condividi foto QReport"
                ExportFormat.COMBINED_PACKAGE -> "application/zip" to "Condividi package QReport completo"
            }

            // Use ShareFileRepository for actual sharing
            when (val result = shareFile(
                filePath = filePath,
                mimeType = mimeType,
                shareTitle = shareTitle,
                shareSubject = "QReport ${File(filePath).absoluteFile}."
            )) {
                is QrResult.Error -> {
                    Timber.e("Failed to share export file: $filePath")
                    QrResult.Error(QrError.ExportError.FILE_SHARE_FAILED)
                }

                is QrResult.Success -> {
                    Timber.d("Export file shared successfully: $filePath")
                    QrResult.Success(Unit)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Exception sharing export file: $filePath")
            QrResult.Error(QrError.ExportError.FILE_SHARE_FAILED)
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private fun extractCheckupId(directoryName: String): String? {
        return if (directoryName.startsWith("checkup_")) {
            val parts = directoryName.split("_")
            if (parts.size >= 2) parts[1] else null
        } else null
    }

    private suspend fun createExportInfo(
        checkupId: String,
        exportPath: String,
        createdAt: Long
    ): ExportInfo {
        val size = when (val result = getExportSize(exportPath)) {
            is QrResult.Error -> 0L
            is QrResult.Success -> {
                result.data
            }
        }
        val files = when (val result = getExportFiles(exportPath)) {
            is QrResult.Error -> emptyList()
            is QrResult.Success -> {
                result.data
            }
        }

        val hasPhotos = File("$exportPath/$PHOTOS_SUBDIRECTORY").exists()
        val hasAttachments = File("$exportPath/$ATTACHMENTS_SUBDIRECTORY").exists()

        val format = files.firstOrNull { it.isMainReport }?.format ?: ExportFormat.TEXT

        return ExportInfo(
            checkupId = checkupId,
            exportDirectory = exportPath,
            createdAt = createdAt,
            format = format,
            sizeBytes = size,
            fileCount = files.size,
            hasPhotos = hasPhotos,
            hasAttachments = hasAttachments,
            status = ExportStatus.COMPLETED
        )
    }

    private fun detectExportFormat(fileName: String): ExportFormat {
        return when {
            fileName.endsWith(".docx") -> ExportFormat.WORD
            fileName.endsWith(".txt") -> ExportFormat.TEXT
            fileName.endsWith(".zip") -> ExportFormat.COMBINED_PACKAGE
            else -> ExportFormat.TEXT
        }
    }

    private fun isMainReportFile(fileName: String): Boolean {
        val mainFileNames = listOf(
            "qreport_checkup",
            "report",
            "checkup_report"
        )
        return mainFileNames.any { fileName.startsWith(it) }
    }

    /**
     * Helper: Create directory if it doesn't exist
     * Sostituisce il metodo createDirectoryIfNotExists() che non esiste in CoreFileRepository
     */
    private suspend fun createDirectoryIfNotExists(dirPath: String): QrResult<String, QrError> {
        return try {
            val file = File(dirPath)
            if (file.exists() && file.isDirectory) {
                QrResult.Success(dirPath)
            } else {
                // Create directory with parents
                val created = file.mkdirs()
                if (created || file.exists()) {
                    Timber.d("Created directory: $dirPath")
                    QrResult.Success(dirPath)
                } else {
                    Timber.e("Failed to create directory: $dirPath")
                    QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception creating directory: $dirPath")
            QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
        }
    }

    /**
     * Helper: Create empty file
     * Sostituisce il metodo createFile() che non esiste in CoreFileRepository
     */
    private suspend fun createFile(filePath: String): QrResult<String, QrError> {
        return try {
            val file = File(filePath)

            // Ensure parent directory exists
            file.parentFile?.let { parentDir ->
                if (!parentDir.exists()) {
                    val created = parentDir.mkdirs()
                    if (!created) {
                        return QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
                    }
                }
            }

            // Create file
            val created = file.createNewFile()
            if (created || file.exists()) {
                Timber.d("Created file: $filePath")
                QrResult.Success(filePath)
            } else {
                Timber.e("Failed to create file: $filePath")
                QrResult.Error(QrError.FileError.FILE_CREATE)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception creating file: $filePath")
            QrResult.Error(QrError.FileError.FILE_CREATE)
        }
    }

    /**
     * Helper: Write text content to file
     * Sostituisce il metodo writeTextToFile() che non esiste in CoreFileRepository
     */
    private suspend fun writeTextToFile(
        filePath: String,
        content: String
    ): QrResult<Unit, QrError> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)

                // Ensure parent directory exists
                file.parentFile?.let { parentDir ->
                    if (!parentDir.exists()) {
                        val created = parentDir.mkdirs()
                        if (!created) {
                            return@withContext QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
                        }
                    }
                }

                // Write content
                file.writeText(content, Charsets.UTF_8)
                Timber.d("Wrote text to file: $filePath (${content.length} chars)")
                QrResult.Success(Unit)

            } catch (e: Exception) {
                Timber.e(e, "Exception writing text to file: $filePath")
                QrResult.Error(QrError.FileError.FILE_WRITE)
            }
        }
    }

    /**
     * Helper: Write binary data to file
     * Sostituisce il metodo writeBinaryToFile() che non esiste in CoreFileRepository
     */
    private suspend fun writeBinaryToFile(
        filePath: String,
        data: ByteArray
    ): QrResult<Unit, QrError> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)

                // Ensure parent directory exists
                file.parentFile?.let { parentDir ->
                    if (!parentDir.exists()) {
                        val created = parentDir.mkdirs()
                        if (!created) {
                            return@withContext QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
                        }
                    }
                }

                // Write binary data
                file.writeBytes(data)
                Timber.d("Wrote binary to file: $filePath (${data.size} bytes)")
                QrResult.Success(Unit)

            } catch (e: Exception) {
                Timber.e(e, "Exception writing binary to file: $filePath")
                QrResult.Error(QrError.FileError.FILE_WRITE)
            }
        }
    }

    /**
     * Helper: Wrap fileExists() result in QrResult
     * CoreFileRepository.fileExists() ritorna Boolean, non QrResult
     */
    private suspend fun fileExistsWithResult(filePath: String): QrResult<Boolean, QrError> {
        return try {
            val exists = coreFileRepo.fileExists(filePath)
            QrResult.Success(exists)
        } catch (e: Exception) {
            Timber.e(e, "Exception checking file existence: $filePath")
            QrResult.Error(QrError.FileError.FILE_ACCESS)
        }
    }
}