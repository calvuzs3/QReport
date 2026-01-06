package net.calvuz.qreport.app.file.domain.repository

import android.net.Uri
import net.calvuz.qreport.app.file.domain.model.CoreFileInfo
import net.calvuz.qreport.app.file.domain.model.FileFilter
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.file.domain.model.DirectorySpec

/**
 * Core file operations interface
 * Contiene SOLO operazioni base e generiche sui file
 *
 * Principles:
 * - Zero business logic
 * - Zero knowledge di Backup/Export specifics
 * - Pure file system operations
 */
interface CoreFileRepository {

    // ===== DIRECTORY MANAGEMENT =====

    /**
     * Get or create app's base directory for specific type
     */
    suspend fun getOrCreateDirectory(spec: DirectorySpec): QrResult<String, QrError.FileError>

    /**
     * Create directory with specific name under base directory
     */
    suspend fun createSubDirectory(
        spec: DirectorySpec,
        subDirName: String
    ): QrResult<String, QrError.FileError>

    // ===== FILE OPERATIONS =====

    /**
     * Copy file from source to destination
     */
    suspend fun copyFile(
        sourcePath: String,
        destinationPath: String
    ): QrResult<Unit, QrError.FileError>

    /**
     * Move file from source to destination
     */
    suspend fun moveFile(
        sourcePath: String,
        destinationPath: String
    ): QrResult<Unit, QrError>

    /**
     * Delete single file
     */
    suspend fun deleteFile(filePath: String): QrResult<Unit, QrError.FileError>

    /**
     * Delete directory and all contents
     */
    suspend fun deleteDirectory(dirPath: String): QrResult<Unit, QrError.FileError>

    /**
     * Check if file exists
     */
    suspend fun fileExists(filePath: String): Boolean

    /**
     * Get file size in bytes
     */
    suspend fun getFileSize(filePath: String): QrResult<Long, QrError.FileError>

    /**
     * List files in directory with optional filter
     */
    suspend fun listFiles(
        dirPath: String,
        filter: FileFilter? = null
    ): QrResult<List<CoreFileInfo>, QrError.FileError>

    // ===== CLEANUP OPERATIONS =====

    /**
     * Clean files older than specified days in directory
     */
    suspend fun cleanupOldFiles(
        dirPath: String,
        olderThanDays: Int
    ): QrResult<Int, QrError.FileError>

    /**
     * Get directory total size
     */
    suspend fun getDirectorySize(dirPath: String): QrResult<Long, QrError.FileError>

    // ===== FILEPROVIDER SUPPORT =====

    /**
     * Creates FileProvider URI for secure file sharing
     * Used by ShareFileRepository and other features
     */
    suspend fun createFileProviderUri(filePath: String): QrResult<Uri, QrError.FileError>

    /**
     * Gets the FileProvider authority string
     */
    suspend fun getFileProviderAuthority(): String

    /**
     * Checks if FileProvider is properly configured
     */
    suspend fun isFileProviderConfigured(): Boolean
}