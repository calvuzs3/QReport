package net.calvuz.qreport.backup.domain.repository

import android.content.Intent
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.share.domain.repository.ShareAppInfo
import java.io.File

/**
 * ShareBackupRepository - CLEANED VERSION
 *
 * ✅ Uses ShareAppInfo from ShareFileRepository interface
 * ✅ NO duplicate types
 * ✅ Single source of truth for app information
 */
interface ShareBackupRepository {

    // ===== BACKUP COMPRESSION =====

    /**
     * Create compressed backup for sharing
     */
    suspend fun createCompressedBackup(
        backupPath: String,
        includeAllFiles: Boolean = true
    ): QrResult<File, QrError>

    /**
     * Create compressed backup with custom name
     */
    suspend fun createCompressedBackupWithName(
        backupPath: String,
        outputName: String,
        includeAllFiles: Boolean = true
    ): QrResult<File, QrError>

    // ===== BACKUP SHARING =====

    /**
     * Share single backup file with proper MIME type
     */
    suspend fun shareBackupFile(
        filePath: String,
        shareTitle: String
    ): QrResult<Intent, QrError>

    /**
     * Share complete backup directory as ZIP
     */
    suspend fun shareBackupDirectory(
        dirPath: String,
        shareTitle: String
    ): QrResult<Intent, QrError>

    /**
     * Share backup with specific app
     */
    suspend fun shareBackupWithApp(
        filePath: String,
        targetPackage: String,
        shareTitle: String
    ): QrResult<Intent, QrError>

    // ===== BACKUP SHARING UTILITIES =====

    /**
     * Get apps that can handle backup files (JSON, ZIP)
     * ✅ Uses ShareAppInfo from ShareFileRepository interface
     */
    suspend fun getAvailableShareApps(filePath: String): List<ShareAppInfo>

    /**
     * Get backup-specific MIME type
     */
    suspend fun getBackupMimeType(filePath: String): String

    /**
     * Validate file can be shared
     */
    suspend fun validateFileForSharing(filePath: String): QrResult<Boolean, QrError>

    // ===== CLEANUP =====

    /**
     * Cleanup temporary ZIP files created for sharing
     */
    suspend fun cleanupTemporaryShareFiles(): QrResult<Int, QrError>
}