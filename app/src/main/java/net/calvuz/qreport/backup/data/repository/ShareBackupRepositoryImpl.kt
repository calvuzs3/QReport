package net.calvuz.qreport.backup.data.repository

import android.content.Intent
import net.calvuz.qreport.backup.domain.repository.ShareBackupRepository
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.share.domain.repository.*
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ShareBackupRepositoryImpl - CLEANED VERSION
 *
 * ✅ Uses ShareAppInfo from ShareFileRepository interface ONLY
 * ✅ NO duplicate data classes
 * ✅ Clean imports and dependencies
 */
@Singleton
class ShareBackupRepositoryImpl @Inject constructor(
    private val shareFileRepo: ShareFileRepository
) : ShareBackupRepository {

    companion object {
        private const val BACKUP_MIME_TYPE_JSON = QReportMimeTypes.JSON_BACKUP
        private const val BACKUP_MIME_TYPE_ZIP = QReportMimeTypes.ZIP_BACKUP
    }

    // ===== BACKUP COMPRESSION =====

    override suspend fun createCompressedBackup(
        backupPath: String,
        includeAllFiles: Boolean
    ): QrResult<File, QrError> {
        return try {
            val timestamp = System.currentTimeMillis()
            val zipName = "qreport_backup_${timestamp}.zip"

            when (val result = shareFileRepo.createZipArchive(backupPath, zipName)) {
                is QrResult.Success -> {
                    Timber.d("Compressed backup created: ${result.data.absolutePath}")
                    QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    Timber.e("Failed to create compressed backup")
                    QrResult.Error(QrError.BackupError.ZIP_CREATE)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Create compressed backup failed")
            QrResult.Error(QrError.BackupError.ZIP_CREATE)
        }
    }

    override suspend fun createCompressedBackupWithName(
        backupPath: String,
        outputName: String,
        includeAllFiles: Boolean
    ): QrResult<File, QrError> {
        return try {
            when (val result = shareFileRepo.createZipArchive(backupPath, outputName)) {
                is QrResult.Success -> {
                    Timber.d("Compressed backup created: $outputName")
                    QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    Timber.e("Failed to create compressed backup: $outputName")
                    QrResult.Error(QrError.BackupError.ZIP_CREATE)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Create compressed backup with name failed: $outputName")
            QrResult.Error(QrError.BackupError.ZIP_CREATE)
        }
    }

    // ===== BACKUP SHARING =====

    override suspend fun shareBackupFile(
        filePath: String,
        shareTitle: String
    ): QrResult<Intent, QrError> {
        return try {
            val mimeType = getBackupMimeType(filePath)
            val shareOptions = ShareOptions(
                subject = shareTitle,
                chooserTitle = shareTitle,
                mimeType = mimeType
            )

            when (val result = shareFileRepo.createShareIntent(filePath, shareOptions)) {
                is QrResult.Success -> {
                    Timber.d("Backup file share intent created: $filePath")
                    QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    Timber.e("Failed to create backup file share intent")
                    QrResult.Error(QrError.BackupError.SHARE_CREATE)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Share backup file failed: $filePath")
            QrResult.Error(QrError.BackupError.SHARE_CREATE)
        }
    }

    override suspend fun shareBackupDirectory(
        dirPath: String,
        shareTitle: String
    ): QrResult<Intent, QrError> {
        return try {
            val compressedResult = createCompressedBackup(dirPath, true)
            when (compressedResult) {
                is QrResult.Error -> QrResult.Error(compressedResult.error)
                is QrResult.Success -> {
                    val zipShareTitle = "$shareTitle (ZIP Archive)"
                    shareBackupFile(compressedResult.data.absolutePath, zipShareTitle)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Share backup directory failed: $dirPath")
            QrResult.Error(QrError.BackupError.SHARE_CREATE)
        }
    }

    override suspend fun shareBackupWithApp(
        filePath: String,
        targetPackage: String,
        shareTitle: String
    ): QrResult<Intent, QrError> {
        return try {
            val mimeType = getBackupMimeType(filePath)
            val shareOptions = ShareOptions(
                subject = shareTitle,
                chooserTitle = shareTitle,
                mimeType = mimeType
            )

            when (val result = shareFileRepo.createShareIntentForApp(filePath, targetPackage, shareOptions)) {
                is QrResult.Success -> {
                    Timber.d("Backup share intent created for app: $targetPackage")
                    QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    Timber.e("Failed to create backup share intent for app: $targetPackage")
                    QrResult.Error(QrError.BackupError.SHARE_CREATE)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Share backup with app failed: $targetPackage")
            QrResult.Error(QrError.BackupError.SHARE_CREATE)
        }
    }

    // ===== BACKUP SHARING UTILITIES =====

    override suspend fun getAvailableShareApps(filePath: String): List<ShareAppInfo> {
        return try {
            val mimeType = getBackupMimeType(filePath)

            // ✅ FIXED: Use ShareFileRepository.getCompatibleAppsByMimeType() directly
            // Returns List<ShareAppInfo> already - no conversion needed!
            shareFileRepo.getCompatibleAppsByMimeType(mimeType)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get available share apps")
            emptyList()
        }
    }

    override suspend fun getBackupMimeType(filePath: String): String {
        val file = File(filePath)
        return when (file.extension.lowercase()) {
            "json" -> BACKUP_MIME_TYPE_JSON
            "zip" -> BACKUP_MIME_TYPE_ZIP
            else -> {
                if (file.isDirectory) {
                    BACKUP_MIME_TYPE_ZIP // Will be compressed
                } else {
                    BACKUP_MIME_TYPE_JSON // Default for backup files
                }
            }
        }
    }

    override suspend fun validateFileForSharing(filePath: String): QrResult<Boolean, QrError> {
        return try {
            when (val result = shareFileRepo.validateFileForSharing(filePath)) {
                is QrResult.Success -> {
                    QrResult.Success(result.data.canShare)
                }
                is QrResult.Error -> {
                    Timber.e("Backup file validation failed: $filePath")
                    QrResult.Error(QrError.BackupError.VALIDATE)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Validate backup file for sharing failed: $filePath")
            QrResult.Error(QrError.BackupError.VALIDATE)
        }
    }

    // ===== CLEANUP =====

    override suspend fun cleanupTemporaryShareFiles(): QrResult<Int, QrError> {
        return try {
            when (val result = shareFileRepo.cleanupTemporaryFiles()) {
                is QrResult.Success -> {
                    Timber.d("Cleanup completed: ${result.data} files deleted")
                    QrResult.Success(result.data)
                }
                is QrResult.Error -> {
                    Timber.e("Cleanup failed")
                    QrResult.Error(QrError.BackupError.CLEANUP_FAILED)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Cleanup temporary share files failed")
            QrResult.Error(QrError.BackupError.CLEANUP_FAILED)
        }
    }
}

// ✅ NO DUPLICATE DATA CLASSES - Use ShareAppInfo from ShareFileRepository interface!