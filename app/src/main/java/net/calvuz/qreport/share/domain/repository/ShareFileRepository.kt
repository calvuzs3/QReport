package net.calvuz.qreport.share.domain.repository

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.vector.ImageVector
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.export.domain.reposirory.ExportFormat
import java.io.File

/**
 * Share-specific file operations repository - UPDATED
 *
 * Responsabilità:
 * - File sharing tramite Android Intent system
 * - File opening con app esterne
 * - MIME type management
 * - FileProvider configuration
 * - Temporary file creation per sharing
 *
 * SI APPOGGIA a CoreFileRepository per operazioni base
 */
interface ShareFileRepository {

    // ===== FILE SHARING =====

    /**
     * Share single file using Android Intent system
     * Handles FileProvider URI generation and MIME type detection
     */
    suspend fun shareFile(
        filePath: String,
        shareOptions: ShareOptions = ShareOptions()
    ): QrResult<Unit, QrError>

    /**
     * Share multiple files as package
     * Creates temporary ZIP if needed
     */
    suspend fun shareFiles(
        filePaths: List<String>,
        shareOptions: ShareOptions = ShareOptions()
    ): QrResult<Unit, QrError>

    /**
     * Share file with custom content
     * Allows adding text content along with file attachment
     */
    suspend fun shareFileWithText(
        filePath: String,
        text: String,
        shareOptions: ShareOptions = ShareOptions()
    ): QrResult<Unit, QrError>

    // ===== ADVANCED SHARING (for ShareBackupRepository compatibility) =====

    /**
     * Create share intent without immediately showing it
     * Useful for custom handling (like ShareBackupRepository)
     */
    suspend fun createShareIntent(
        filePath: String,
        shareOptions: ShareOptions
    ): QrResult<Intent, QrError>

    /**
     * Create share intent for specific app
     */
    suspend fun createShareIntentForApp(
        filePath: String,
        targetPackage: String,
        shareOptions: ShareOptions
    ): QrResult<Intent, QrError>

    // ===== FILE OPENING =====

    /**
     * Open file with default system app
     * Automatically detects MIME type and handles FileProvider
     */
    suspend fun openFile(
        filePath: String,
        openOptions: OpenOptions = OpenOptions()
    ): QrResult<Unit, QrError>

    /**
     * Open file with specific app
     * Attempts to open with specified package name
     */
    suspend fun openFileWith(
        filePath: String,
        packageName: String,
        openOptions: OpenOptions = OpenOptions()
    ): QrResult<Unit, QrError>

    /**
     * Get list of apps that can handle the file type
     */
    suspend fun getCompatibleApps(
        filePath: String
    ): QrResult<List<ShareAppInfo>, QrError>

    /**
     * Get compatible apps by MIME type (without needing file)
     * For ShareBackupRepository compatibility
     */
    suspend fun getCompatibleAppsByMimeType(mimeType: String): List<ShareAppInfo>

    // ===== TEMPORARY FILE MANAGEMENT =====

    /**
     * Create temporary file for sharing purposes
     * File is automatically cleaned up after specified time
     */
    suspend fun generateTemporaryPath(
        sourceFilePath: String,
        tempFileName: String? = null,
        autoCleanupMinutes: Int = 30
    ): QrResult<String, QrError>

    /**
     * Create temporary ZIP package for sharing multiple files
     */
    suspend fun createTemporaryZip(
        filePaths: List<String>,
        zipName: String? = null,
        autoCleanupMinutes: Int = 30
    ): QrResult<String, QrError>

    /**
     * Create ZIP archive and return File object
     * For ShareBackupRepository compatibility
     */
    suspend fun createZipArchive(
        sourcePath: String,
        zipName: String
    ): QrResult<File, QrError>

    /**
     * Clean up all temporary files created for sharing
     */
    suspend fun cleanupTemporaryFiles(): QrResult<Int, QrError>

    // ===== MIME TYPE & METADATA =====

    /**
     * Detect MIME type for file
     * Uses multiple detection methods for accuracy
     */
    suspend fun detectMimeType(filePath: String): QrResult<String, QrError>

    /**
     * Get file metadata relevant for sharing
     */
    suspend fun getShareableFileInfo(filePath: String): QrResult<ShareableFileInfo, QrError>

    // ===== FILEPROVIDER OPERATIONS =====

    /**
     * Convert local file path to shareable FileProvider URI
     * Required for sharing files with other apps on Android
     */
    suspend fun createFileProviderUri(filePath: String): QrResult<Uri, QrError.FileError>

    /**
     * Validate that file can be shared (permissions, existence, size limits)
     */
    suspend fun validateFileForSharing(filePath: String): QrResult<ShareValidationResult, QrError>
}

// ===== DATA CLASSES =====

/**
 * Known MIME types for QReport file types
 */
object QReportMimeTypes {
    const val WORD = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val TEXT = "text/plain"
    const val JSON_BACKUP = "application/json"
    const val ZIP_BACKUP = "application/zip"
    const val PHOTO_JPEG = "image/jpeg"
    const val PHOTO_PNG = "image/png"
    const val PDF = "application/pdf"
    const val UNKNOWN = "*/*"

    /**
     * Get MIME type for export format
     */
    fun forExportFormat(format: ExportFormat): String = when (format) {
        ExportFormat.WORD -> WORD
        ExportFormat.TEXT -> TEXT
        ExportFormat.PHOTO_FOLDER -> ZIP_BACKUP
        ExportFormat.COMBINED_PACKAGE -> ZIP_BACKUP
    }

    /**
     * Get MIME type by file extension
     */
    fun forExtension(extension: String): String? = when (extension.lowercase()) {
        "docx" -> WORD
        "txt" -> TEXT
        "json" -> JSON_BACKUP
        "zip" -> ZIP_BACKUP
        "jpg", "jpeg" -> PHOTO_JPEG
        "png" -> PHOTO_PNG
        "pdf" -> PDF
        else -> null
    }
}

/**
 * Share operation configuration
 */
data class ShareOptions(
    val subject: String? = null,           // Email subject or share dialog title
    val chooserTitle: String? = null,      // Custom chooser dialog title
    val mimeType: String? = null,          // Override MIME type detection
    val excludePackages: Set<String> = emptySet(), // Apps to exclude from chooser
    val includeTextContent: Boolean = false, // Include additional text content
    val requireExternalApp: Boolean = false  // Fail if no external app available
)

/**
 * File opening configuration
 */
data class OpenOptions(
    val mimeType: String? = null,          // Override MIME type detection
    val allowChooser: Boolean = true,      // Show app chooser if multiple apps
    val chooserTitle: String? = null,      // Custom chooser title
    val requireExternalApp: Boolean = false // Fail if no external app available
)


/**
 * Complete file information for sharing
 */
data class ShareableFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val extension: String?,
    val lastModified: Long,
    val isShareable: Boolean,
    val estimatedShareSize: Long,         // Size when compressed/optimized
    val supportedShareMethods: List<ShareMethod>
)

/**
 * Sharing validation issue
 */
data class ShareIssue(
    val type: ShareIssueType,
    val message: String,
    val severity: ShareIssueSeverity,
    val suggestedAction: String? = null
)

/**
 * File sharing validation result
 */
data class ShareValidationResult(
    val canShare: Boolean,
    val issues: List<ShareIssue> = emptyList(),
    val warnings: List<ShareIssue> = emptyList(),
    val recommendedMethod: ShareMethod? = null,
    val maxSizeForDirectShare: Long? = null
)

data class ShareAppInfo(
    val packageName: String,
    val appName: String,
    val activityName: String,
    val icon: String? = null,
    val isDefault: Boolean
)

data class ShareIntentResult(
    val intent: Intent,
    val shareMethod: ShareMethod,
    val backupPath: String,
    val targetApp: String?,
    val shareTitle: String,
    val availableApps: List<ShareAppInfo>
)

data class ShareOptionOldVersion(
    val type: ShareOptionType,
    val title: String,
    val subtitle: String,
    val icon: Drawable? = null,
    val ivIcon: ImageVector? = null,
    val targetPackage: String? = null,
    val shareMethod: ShareMethod
)

// ===== ENUMS =====

enum class ShareIssueSeverity {
    INFO,      // Informational message
    WARNING,   // Share possible but not optimal
    ERROR      // Share not possible without resolution
}

enum class ShareIssueType {
    FILE_NOT_FOUND,
    FILE_TOO_LARGE,
    INSUFFICIENT_PERMISSIONS,
    UNSUPPORTED_MIME_TYPE,
    NO_COMPATIBLE_APPS,
    TEMPORARY_STORAGE_FULL
}

enum class ShareMethod {
    DIRECT,                // Direct file sharing
    COMPRESSED,            // Share as compressed ZIP
    CLOUD_UPLOAD,          // Upload to cloud and share link
    EMAIL_ATTACHMENT,      // Send via email
    MESSAGING_ATTACHMENT   // Send via messaging apps
}

enum class ShareOptionType {
    FILE_OPTION,    // Condivisione generica con chooser
    APP_SPECIFIC,   // Condivisione con app specifica
    APP_GENERIC,    // Condivisione backup completo
    QUICK_ACTION    // Condivisione compressa
}