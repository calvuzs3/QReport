package net.calvuz.qreport.ti.data.local.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.file.domain.model.CoreFileInfo
import net.calvuz.qreport.app.file.domain.model.DirectorySpec
import net.calvuz.qreport.app.file.domain.repository.CoreFileRepository
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.ti.domain.repository.SignatureFileRepository
import net.calvuz.qreport.ti.domain.model.SignatureFileInfo
import net.calvuz.qreport.ti.domain.model.SignatureFileNaming
import net.calvuz.qreport.ti.domain.model.SignatureStorageStats
import net.calvuz.qreport.ti.domain.model.SignatureType
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.iterator

/**
 * Implementation of SignatureFileRepository using CoreFileRepository
 * Follows QReport's clean architecture patterns for file operations
 *
 * All file operations delegate to CoreFileRepository for consistency
 */
@Singleton
class SignatureFileRepositoryImpl @Inject constructor(
    private val coreFileRepository: CoreFileRepository
) : SignatureFileRepository {

    companion object {
        private const val COMPRESSION_QUALITY = 90
        private const val MAX_SIGNATURE_SIZE_MB = 2L
    }

    // ✅ CORRECTED: Use simple DirectorySpec constructor
    private val signatureDirectorySpec = DirectorySpec("signatures")

    // ===== SIGNATURE PERSISTENCE =====

    override suspend fun saveTechnicianSignature(
        interventionId: String,
        signatureBitmap: ImageBitmap
    ): QrResult<String, QrError.FileError> {
        return saveSignatureInternal(
            interventionId = interventionId,
            signatureBitmap = signatureBitmap,
            signatureType = SignatureType.TECHNICIAN
        )
    }

    override suspend fun saveCustomerSignature(
        interventionId: String,
        signatureBitmap: ImageBitmap
    ): QrResult<String, QrError.FileError> {
        return saveSignatureInternal(
            interventionId = interventionId,
            signatureBitmap = signatureBitmap,
            signatureType = SignatureType.CUSTOMER
        )
    }

    /**
     * Internal signature saving logic using CoreFileRepository
     */
    private suspend fun saveSignatureInternal(
        interventionId: String,
        signatureBitmap: ImageBitmap,
        signatureType: SignatureType
    ): QrResult<String, QrError.FileError> {
        return try {
            // Get signatures directory using CoreFileRepository
            val signatureDirResult = coreFileRepository.getOrCreateDirectory(signatureDirectorySpec)
            when (signatureDirResult) {
                is QrResult.Error -> return signatureDirResult
                is QrResult.Success -> {
                    val signatureDir = File(signatureDirResult.data)

                    // Generate filename with timestamp
                    val filename = SignatureFileNaming.generateFilename(
                        type = signatureType,
                        interventionId = interventionId,
                        timestamp = System.currentTimeMillis()
                    )
                    val signatureFile = File(signatureDir, filename)

                    Timber.d("SignatureFileRepository: Saving ${signatureType.displayName} to: ${signatureFile.absolutePath}")

                    // Convert and validate bitmap
                    val androidBitmap = signatureBitmap.asAndroidBitmap()
                    if (androidBitmap.isRecycled) {
                        Timber.e("SignatureFileRepository: Signature bitmap is recycled")
                        return QrResult.Error(QrError.FileError.IO_ERROR)
                    }

                    // Save bitmap to file
                    val saveResult = saveBitmapToFile(androidBitmap, signatureFile)
                    when (saveResult) {
                        is QrResult.Error -> QrResult.Error(QrError.FileError.FILE_WRITE) // saveResult
                        is QrResult.Success -> {
                            // Verify file was created and has reasonable size
                            val fileSize = coreFileRepository.getFileSize(signatureFile.absolutePath)
                            when (fileSize) {
                                is QrResult.Error -> {
                                    Timber.e("SignatureFileRepository: Could not verify saved file size")
                                    QrResult.Error(QrError.FileError.FILE_WRITE)
                                }

                                is QrResult.Success -> {
                                    val fileSizeKB = fileSize.data / 1024
                                    val fileSizeMB = fileSize.data / (1024 * 1024)

                                    if (fileSizeMB > MAX_SIGNATURE_SIZE_MB) {
                                        Timber.w("SignatureFileRepository: Signature file too large: ${fileSizeMB}MB")
                                        coreFileRepository.deleteFile(signatureFile.absolutePath)
                                        return QrResult.Error(QrError.FileError.FILE_WRITE)
                                    }

                                    Timber.d("SignatureFileRepository: ${signatureType.displayName} saved successfully - Size: ${fileSizeKB}KB")
                                    QrResult.Success(signatureFile.absolutePath)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error saving ${signatureType.displayName}")
            QrResult.Error(QrError.FileError.FILE_WRITE)
        }
    }

    /**
     * Helper to save bitmap to file with proper error handling
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File): QrResult<Unit, QrError.FileError> {
        return try {
            FileOutputStream(file).use { outputStream ->
                val success = bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    COMPRESSION_QUALITY,
                    outputStream
                )

                if (!success) {
                    QrResult.Error(QrError.FileError.FILE_WRITE)
                } else {
                    QrResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error saving bitmap to file: ${file.absolutePath}")
            QrResult.Error(QrError.FileError.FILE_WRITE)
        }
    }

    // ===== SIGNATURE RETRIEVAL =====

    override suspend fun loadSignatureBitmap(filePath: String): QrResult<Bitmap?, QrError.FileError> {
        return try {
            // Use CoreFileRepository to check existence
            if (!coreFileRepository.fileExists(filePath)) {
                Timber.w("SignatureFileRepository: Signature file does not exist: $filePath")
                return QrResult.Success(null)
            }

            // Load bitmap using BitmapFactory
            val bitmap = BitmapFactory.decodeFile(filePath)

            if (bitmap == null) {
                Timber.e("SignatureFileRepository: Failed to decode signature bitmap from: $filePath")
                return QrResult.Error(QrError.FileError.FILE_READ)
            }

            Timber.d("SignatureFileRepository: Loaded signature bitmap: ${bitmap.width}x${bitmap.height}")
            QrResult.Success(bitmap)

        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error loading signature from: $filePath")
            QrResult.Error(QrError.FileError.FILE_READ)
        }
    }

    override suspend fun isSignatureValid(filePath: String): QrResult<Boolean, QrError.FileError> {
        return try {
            // Check file exists using CoreFileRepository
            if (!coreFileRepository.fileExists(filePath)) {
                return QrResult.Success(false)
            }

            // Validate filename format
            val filename = File(filePath).name
            if (!SignatureFileNaming.isValidSignatureFilename(filename)) {
                Timber.w("SignatureFileRepository: Invalid signature filename: $filename")
                return QrResult.Success(false)
            }

            // Try to load bitmap to verify it's not corrupted
            val loadResult = loadSignatureBitmap(filePath)
            val isValid = when (loadResult) {
                is QrResult.Success -> loadResult.data != null
                is QrResult.Error -> false
            }

            QrResult.Success(isValid)
        } catch (e: Exception) {
            Timber.w(e, "SignatureFileRepository: Error validating signature: $filePath")
            QrResult.Error(QrError.FileError.IO_ERROR)
        }
    }

    override suspend fun getSignatureFileSize(filePath: String): QrResult<Long, QrError.FileError> {
        return coreFileRepository.getFileSize(filePath)
    }

    // ===== SIGNATURE MANAGEMENT =====

    override suspend fun deleteSignature(filePath: String): QrResult<Unit, QrError.FileError> {
        Timber.d("SignatureFileRepository: Deleting signature: $filePath")
        return coreFileRepository.deleteFile(filePath)
    }

    override suspend fun getInterventionSignatures(interventionId: String): QrResult<List<SignatureFileInfo>, QrError.FileError> {
        return try {
            val allSignaturesResult = getAllSignatures()
            when (allSignaturesResult) {
                is QrResult.Error -> allSignaturesResult
                is QrResult.Success -> {
                    val interventionSignatures = allSignaturesResult.data.filter { signature ->
                        signature.interventionId == interventionId
                    }
                    Timber.d("SignatureFileRepository: Found ${interventionSignatures.size} signatures for intervention: $interventionId")
                    QrResult.Success(interventionSignatures)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error getting signatures for intervention: $interventionId")
            QrResult.Error(QrError.FileError.FILE_READ)
        }
    }

    override suspend fun getAllSignatures(): QrResult<List<SignatureFileInfo>, QrError.FileError> {
        return try {
            // Get signatures directory
            val signatureDirResult = coreFileRepository.getOrCreateDirectory(signatureDirectorySpec)
            when (signatureDirResult) {
                is QrResult.Error -> return QrResult.Error(QrError.FileError.IO_ERROR) //signatureDirResult
                is QrResult.Success -> {
                    val signatureDir = signatureDirResult.data

                    // ✅ CORRECTED: List all files, then filter by extension
                    val filesResult = coreFileRepository.listFiles(signatureDir, null)

                    when (filesResult) {
                        is QrResult.Error -> return QrResult.Error(QrError.FileError.FILE_READ) //filesResult
                        is QrResult.Success -> {
                            val signatureInfos = filesResult.data
                                .filter { coreFileInfo ->
                                    // Filter to PNG files only
                                    coreFileInfo.extension?.lowercase() == "png"
                                }
                                .mapNotNull { coreFileInfo ->
                                    convertToSignatureFileInfo(coreFileInfo)
                                }

                            Timber.d("SignatureFileRepository: Found ${signatureInfos.size} valid signature files")
                            QrResult.Success(signatureInfos)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error getting all signatures")
            QrResult.Error(QrError.FileError.FILE_READ)
        }
    }

    /**
     * Convert CoreFileInfo to SignatureFileInfo with parsing
     */
    private fun convertToSignatureFileInfo(coreFileInfo: CoreFileInfo): SignatureFileInfo? {
        return try {
            val filename = coreFileInfo.name

            // Validate and parse signature filename
            if (!SignatureFileNaming.isValidSignatureFilename(filename)) {
                Timber.w("SignatureFileRepository: Invalid signature filename: $filename")
                return null
            }

            val signatureType = SignatureType.fromFilename(filename)
            val interventionId = SignatureFileNaming.parseInterventionId(filename)
            val timestamp = SignatureFileNaming.parseTimestamp(filename)

            if (signatureType != null && interventionId != null && timestamp != null) {
                SignatureFileInfo(
                    coreFileInfo = coreFileInfo,
                    signatureType = signatureType,
                    interventionId = interventionId,
                    timestamp = timestamp
                )
            } else {
                Timber.w("SignatureFileRepository: Could not parse signature filename: $filename")
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "SignatureFileRepository: Error processing signature file: ${coreFileInfo.name}")
            null
        }
    }

    // ===== CLEANUP & MAINTENANCE =====

    override suspend fun cleanupOldSignatures(olderThanDays: Int): QrResult<Int, QrError.FileError> {
        return try {
            val signatureDirResult = coreFileRepository.getOrCreateDirectory(signatureDirectorySpec)
            when (signatureDirResult) {
                is QrResult.Error -> return QrResult.Error(QrError.FileError.IO_ERROR) //signatureDirResult
                is QrResult.Success -> {
                    val cleanupResult = coreFileRepository.cleanupOldFiles(signatureDirResult.data, olderThanDays)
                    when (cleanupResult) {
                        is QrResult.Error -> cleanupResult
                        is QrResult.Success -> {
                            Timber.d("SignatureFileRepository: Cleaned up ${cleanupResult.data} old signature files")
                            cleanupResult
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error during cleanup")
            QrResult.Error(QrError.FileError.FILE_DELETE)
        }
    }

    override suspend fun getStorageStats(): QrResult<SignatureStorageStats, QrError.FileError> {
        return try {
            val allSignaturesResult = getAllSignatures()
            when (allSignaturesResult) {
                is QrResult.Error -> return QrResult.Error(allSignaturesResult.error)
                is QrResult.Success -> {
                    val signatures = allSignaturesResult.data

                    val stats = SignatureStorageStats(
                        totalFiles = signatures.size,
                        totalSizeMB = signatures.sumOf { it.size } / (1024.0 * 1024.0),
                        technicianSignatures = signatures.count { it.signatureType == SignatureType.TECHNICIAN },
                        customerSignatures = signatures.count { it.signatureType == SignatureType.CUSTOMER },
                        averageFileSizeKB = if (signatures.isNotEmpty()) {
                            signatures.sumOf { it.size } / (1024.0 * signatures.size)
                        } else 0.0,
                        oldestSignatureDate = signatures.minOfOrNull { it.lastModified },
                        newestSignatureDate = signatures.maxOfOrNull { it.lastModified },
                        directoryPath = when (val dirResult = coreFileRepository.getOrCreateDirectory(signatureDirectorySpec)) {
                            is QrResult.Success -> dirResult.data
                            is QrResult.Error -> ""
                        }
                    )

                    Timber.d("SignatureFileRepository: Storage stats - ${stats.totalFiles} files, ${String.format("%.2f", stats.totalSizeMB)}MB")
                    QrResult.Success(stats)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error getting storage stats")
            QrResult.Error(QrError.FileError.IO_ERROR)
        }
    }

    override suspend fun verifyStorageIntegrity(): QrResult<Boolean, QrError.FileError> {
        return try {
            val allSignaturesResult = getAllSignatures()
            when (allSignaturesResult) {
                is QrResult.Error -> return QrResult.Error(allSignaturesResult.error)
                is QrResult.Success -> {
                    var corruptedCount = 0

                    for (signature in allSignaturesResult.data) {
                        val validationResult = isSignatureValid(signature.path)
                        when (validationResult) {
                            is QrResult.Error -> return validationResult
                            is QrResult.Success -> {
                                if (!validationResult.data) {
                                    corruptedCount++
                                    Timber.w("SignatureFileRepository: Corrupted signature detected: ${signature.name}")
                                }
                            }
                        }
                    }

                    val isIntegrityOk = corruptedCount == 0
                    if (isIntegrityOk) {
                        Timber.d("SignatureFileRepository: Storage integrity verified - all signatures valid")
                    } else {
                        Timber.w("SignatureFileRepository: Storage integrity issues - ${corruptedCount} corrupted signatures")
                    }

                    QrResult.Success(isIntegrityOk)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error verifying storage integrity")
            QrResult.Error(QrError.FileError.IO_ERROR)
        }
    }

    override suspend fun getSignatureDirectorySize(): QrResult<Long, QrError.FileError> {
        return try {
            val signatureDirResult = coreFileRepository.getOrCreateDirectory(signatureDirectorySpec)
            when (signatureDirResult) {
                is QrResult.Error -> return QrResult.Error(QrError.FileError.FILE_READ) //signatureDirResult
                is QrResult.Success -> {
                    coreFileRepository.getDirectorySize(signatureDirResult.data)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error getting directory size")
            QrResult.Error(QrError.FileError.IO_ERROR)
        }
    }

    // ===== EXPORT & SHARING =====

    override suspend fun copySignatureForExport(
        signaturePath: String,
        exportDirPath: String,
        newFilename: String?
    ): QrResult<String, QrError.FileError> {
        return try {
            val sourceFile = File(signaturePath)
            val filename = newFilename ?: sourceFile.name
            val destinationPath = File(exportDirPath, filename).absolutePath

            val copyResult = coreFileRepository.copyFile(signaturePath, destinationPath)
            when (copyResult) {
                is QrResult.Error -> QrResult.Error(copyResult.error)
                is QrResult.Success -> {
                    Timber.d("SignatureFileRepository: Signature copied for export: $destinationPath")
                    QrResult.Success(destinationPath)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error copying signature for export: $signaturePath")
            QrResult.Error(QrError.FileError.FILE_COPY)
        }
    }

    override suspend fun createSignatureShareUri(signaturePath: String): QrResult<Uri, QrError.FileError> {
        return coreFileRepository.createFileProviderUri(signaturePath)
    }

    // ===== ORGANIZATION =====

    override suspend fun organizeSignaturesByIntervention(): QrResult<Int, QrError.FileError> {
        return try {
            // This is a maintenance operation to organize loose files
            val allSignaturesResult = getAllSignatures()
            when (allSignaturesResult) {
                is QrResult.Error -> return QrResult.Error(QrError.FileError.FILE_READ) // allSignaturesResult
                is QrResult.Success -> {
                    var organizedCount = 0

                    // Group by intervention ID
                    val signaturesByIntervention = allSignaturesResult.data.groupBy { it.interventionId }

                    for ((interventionId, signatures) in signaturesByIntervention) {
                        if (signatures.size > 1) {
                            // Create subdirectory for this intervention
                            val subDirResult = coreFileRepository.createSubDirectory(
                                signatureDirectorySpec,
                                "intervention_$interventionId"
                            )

                            when (subDirResult) {
                                is QrResult.Success -> {
                                    // Move signatures to subdirectory
                                    for (signature in signatures) {
                                        val newPath = File(subDirResult.data, signature.name).absolutePath
                                        coreFileRepository.moveFile(signature.path, newPath)
                                        organizedCount++
                                    }
                                }
                                is QrResult.Error -> {
                                    Timber.w("SignatureFileRepository: Failed to create subdirectory for intervention: $interventionId")
                                }
                            }
                        }
                    }

                    Timber.d("SignatureFileRepository: Organized $organizedCount signature files")
                    QrResult.Success(organizedCount)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error organizing signatures")
            QrResult.Error(QrError.FileError.FILE_MOVE)
        }
    }

    override suspend fun getSignaturesByIntervention(): QrResult<Map<String, List<SignatureFileInfo>>, QrError.FileError> {
        return try {
            val allSignaturesResult = getAllSignatures()
            when (allSignaturesResult) {
                is QrResult.Error -> return QrResult.Error(QrError.FileError.FILE_READ) // allSignaturesResult
                is QrResult.Success -> {
                    val signaturesByIntervention = allSignaturesResult.data.groupBy { it.interventionId }
                    QrResult.Success(signaturesByIntervention)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SignatureFileRepository: Error grouping signatures by intervention")
            QrResult.Error(QrError.FileError.IO_ERROR)
        }
    }

     override suspend fun getSignaturesDirectory(): QrResult<String, QrError.FileError> {
     return coreFileRepository.getOrCreateDirectory(signatureDirectorySpec)
 }
}