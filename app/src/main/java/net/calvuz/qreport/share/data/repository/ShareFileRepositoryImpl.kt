package net.calvuz.qreport.share.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Clock
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.file.domain.repository.CoreFileRepository
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.app.util.DateTimeUtils.toFilenameSafeDateTime
import net.calvuz.qreport.share.domain.model.ShareDirectories
import net.calvuz.qreport.share.domain.repository.*
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ShareFileRepositoryImpl - CLEANED VERSION
 *
 * ✅ Uses ONLY definitions from ShareFileRepository.kt interface
 * ✅ NO duplicate data classes
 * ✅ Fixed icon error
 */
@Singleton
class ShareFileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coreFileRepo: CoreFileRepository
) : ShareFileRepository {

    companion object {
        private const val TEMP_SHARE_PREFIX = "shares_"
        private const val TEMP_ZIP_PREFIX = "shared_archive_"
        private const val DEFAULT_MIME_TYPE = "*/*"
    }

    // ===== FILE SHARING =====

    override suspend fun shareFile(
        filePath: String,
        shareOptions: ShareOptions
    ): QrResult<Unit, QrError> {
        return try {
            val intentResult = createShareIntent(filePath, shareOptions)
            when (intentResult) {
                is QrResult.Error -> QrResult.Error(intentResult.error)
                is QrResult.Success -> {
                    val intent = intentResult.data
                    val chooserIntent = Intent.createChooser(intent, shareOptions.chooserTitle ?: "Share with")
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)

                    QrResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Share file failed: $filePath")
            QrResult.Error(QrError.ShareError.SHARE_FAILED)
        }
    }

    override suspend fun shareFiles(
        filePaths: List<String>,
        shareOptions: ShareOptions
    ): QrResult<Unit, QrError> {
        return try {
            if (filePaths.size == 1) {
                return shareFile(filePaths.first(), shareOptions)
            }

            val zipResult = createTemporaryZip(filePaths, shareOptions.chooserTitle)
            when (zipResult) {
                is QrResult.Error -> QrResult.Error(zipResult.error)
                is QrResult.Success -> shareFile(zipResult.data, shareOptions)
            }

        } catch (e: Exception) {
            Timber.e(e, "Share multiple files failed")
            QrResult.Error(QrError.ShareError.SHARE_FAILED)
        }
    }

    override suspend fun shareFileWithText(
        filePath: String,
        text: String,
        shareOptions: ShareOptions
    ): QrResult<Unit, QrError> {
        return try {
            val intentResult = createShareIntent(filePath, shareOptions)
            when (intentResult) {
                is QrResult.Error -> QrResult.Error(intentResult.error)
                is QrResult.Success -> {
                    val intent = intentResult.data
                    intent.putExtra(Intent.EXTRA_TEXT, text)

                    val chooserIntent = Intent.createChooser(intent, shareOptions.chooserTitle ?: "Condividi")
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)

                    QrResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Share file with text failed")
            QrResult.Error(QrError.ShareError.SHARE_FAILED)
        }
    }

    // ===== ADVANCED SHARING =====

    override suspend fun createShareIntent(
        filePath: String,
        shareOptions: ShareOptions
    ): QrResult<Intent, QrError> {
        return try {
            val validation = validateFileForSharing(filePath)
            when (validation) {
                is QrResult.Error -> return QrResult.Error(validation.error)
                is QrResult.Success -> {
                    if (!validation.data.canShare) {
                        return QrResult.Error(QrError.ShareError.VALIDATION_FAILED)
                    }
                }
            }

            val mimeType = shareOptions.mimeType ?:
            when(detectMimeType(filePath)) {
                is QrResult.Error -> DEFAULT_MIME_TYPE
                is QrResult.Success -> shareOptions.mimeType ?: DEFAULT_MIME_TYPE
            }

            val uriResult = createFileProviderUri(filePath)
            when (uriResult) {
                is QrResult.Error -> QrResult.Error(uriResult.error)
                is QrResult.Success -> {
                    val fileUri = uriResult.data

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        shareOptions.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                    }

                    if (shareOptions.excludePackages.isNotEmpty()) {
                        filterExcludedPackages(shareIntent, shareOptions.excludePackages)
                    }

                    Timber.d("Share intent created for: $filePath (MIME: $mimeType)")
                    QrResult.Success(shareIntent)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Create share intent failed: $filePath")
            QrResult.Error(QrError.ShareError.INTENT_CREATION_FAILED)
        }
    }

    override suspend fun createShareIntentForApp(
        filePath: String,
        targetPackage: String,
        shareOptions: ShareOptions
    ): QrResult<Intent, QrError> {
        return try {
            val baseIntentResult = createShareIntent(filePath, shareOptions)
            when (baseIntentResult) {
                is QrResult.Error -> baseIntentResult
                is QrResult.Success -> {
                    val intent = baseIntentResult.data
                    intent.`package` = targetPackage

                    val resolveInfos = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    if (resolveInfos.isEmpty()) {
                        return QrResult.Error(QrError.ShareError.APP_NOT_FOUND)
                    }

                    QrResult.Success(intent)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Create share intent for app failed: $targetPackage")
            QrResult.Error(QrError.ShareError.INTENT_CREATION_FAILED)
        }
    }

    // ===== FILE OPENING =====

    override suspend fun openFile(
        filePath: String,
        openOptions: OpenOptions
    ): QrResult<Unit, QrError> {
        return try {
            val mimeType = openOptions.mimeType ?:
            when(detectMimeType(filePath)) {
                is QrResult.Error -> DEFAULT_MIME_TYPE
                is QrResult.Success -> openOptions.mimeType ?: DEFAULT_MIME_TYPE
            }

            val uriResult = createFileProviderUri(filePath)

            when (uriResult) {
                is QrResult.Error -> QrResult.Error(uriResult.error)
                is QrResult.Success -> {
                    val fileUri = uriResult.data

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    if (openOptions.allowChooser) {
                        val chooserIntent = Intent.createChooser(intent, openOptions.chooserTitle ?: "Open with")
                        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooserIntent)
                    } else {
                        context.startActivity(intent)
                    }

                    QrResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Open file failed: $filePath")
            QrResult.Error(QrError.ShareError.OPEN_FAILED)
        }
    }

    override suspend fun openFileWith(
        filePath: String,
        packageName: String,
        openOptions: OpenOptions
    ): QrResult<Unit, QrError> {
        return try {
            val mimeType = openOptions.mimeType ?:
            when(detectMimeType(filePath)) {
                is QrResult.Error -> DEFAULT_MIME_TYPE
                is QrResult.Success -> openOptions.mimeType ?: DEFAULT_MIME_TYPE
            }
            val uriResult = createFileProviderUri(filePath)

            when (uriResult) {
                is QrResult.Error -> QrResult.Error(uriResult.error)
                is QrResult.Success -> {
                    val fileUri = uriResult.data

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, mimeType)
                        `package` = packageName
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    context.startActivity(intent)
                    QrResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Open file with app failed: $packageName")
            QrResult.Error(QrError.ShareError.OPEN_FAILED)
        }
    }

    override suspend fun getCompatibleApps(filePath: String): QrResult<List<ShareAppInfo>, QrError> {
        return try {
            val mimeType =  when(val result = detectMimeType(filePath)) {
                is QrResult.Error -> DEFAULT_MIME_TYPE
                is QrResult.Success -> result.data
            }
            val apps = getCompatibleAppsByMimeType(mimeType)
            QrResult.Success(apps)
        } catch (e: Exception) {
            Timber.e(e, "Get compatible apps failed")
            QrResult.Error(QrError.ShareError.APP_QUERY_FAILED)
        }
    }

    override suspend fun getCompatibleAppsByMimeType(mimeType: String): List<ShareAppInfo> {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
            }

            val resolveInfos = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

            resolveInfos.mapNotNull { resolveInfo ->
                try {
                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    val appName = context.packageManager.getApplicationLabel(appInfo).toString()

                    // ✅ FIXED: Use ShareAppInfo from interface, icon = null (simplified)
                    ShareAppInfo(
                        packageName = resolveInfo.activityInfo.packageName,
                        appName = appName,
                        activityName = resolveInfo.activityInfo.name,
                        icon = null, // ✅ FIXED: Simplified icon handling
                        isDefault = resolveInfo.isDefault
                    )
                } catch (e: Exception) {
                    Timber.w(e, "Failed to get app info for: ${resolveInfo.activityInfo.packageName}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Get compatible apps by MIME type failed: $mimeType")
            emptyList()
        }
    }

    // ===== TEMPORARY FILE MANAGEMENT =====

    override suspend fun generateTemporaryPath(
        sourceFilePath: String,
        tempFileName: String?,
        autoCleanupMinutes: Int
    ): QrResult<String, QrError> {
        return try {
            val tempDirResult = coreFileRepo.getOrCreateDirectory(ShareDirectories.SHARES)
            when (tempDirResult) {
                is QrResult.Error -> QrResult.Error(tempDirResult.error)
                is QrResult.Success -> {
                    val sourceFile = File(sourceFilePath)
                    val fileName = tempFileName ?: "${TEMP_SHARE_PREFIX}_${Clock.System.now().toFilenameSafeDateTime()}_${sourceFile.name}"
                    val tempFile = File(tempDirResult.data, fileName)

                    val copyResult = coreFileRepo.copyFile(sourceFilePath, tempFile.absolutePath)
                    when (copyResult) {
                        is QrResult.Error -> QrResult.Error(copyResult.error)
                        is QrResult.Success -> {
                            Timber.d("Created temporary file: ${tempFile.absolutePath}")
                            QrResult.Success(tempFile.absolutePath)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Create temporary file failed")
            QrResult.Error(QrError.ShareError.TEMP_FILE_FAILED)
        }
    }

    override suspend fun createTemporaryZip(
        filePaths: List<String>,
        zipName: String?,
        autoCleanupMinutes: Int
    ): QrResult<String, QrError> {
        return try {

            when (val tempDirResult = getTempZipDirectory()) {
                is QrResult.Error -> QrResult.Error(tempDirResult.error)
                is QrResult.Success -> {
                    val fileName = zipName ?: "${TEMP_ZIP_PREFIX}${System.currentTimeMillis()}.zip"
                    val zipFile = File(tempDirResult.data, fileName)

                    val zipResult = createZipFromFiles(filePaths, zipFile)
                    when (zipResult) {
                        is QrResult.Error -> QrResult.Error(zipResult.error)
                        is QrResult.Success -> {
                            Timber.d("Created temporary ZIP: ${zipFile.absolutePath}")
                            QrResult.Success(zipFile.absolutePath)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Create temporary ZIP failed")
            QrResult.Error(QrError.ShareError.ZIP_CREATION_FAILED)
        }
    }

    override suspend fun createZipArchive(
        sourcePath: String,
        zipName: String
    ): QrResult<File, QrError> {
        return try {

            when (val tempDirResult = getTempZipDirectory()) {
                is QrResult.Error -> QrResult.Error(tempDirResult.error)
                is QrResult.Success -> {
                    val zipFile = File(tempDirResult.data, zipName)
                    val sourceFile = File(sourcePath)

                    val filePaths = if (sourceFile.isDirectory) {
                        sourceFile.listFiles()?.filter { it.isFile }?.map { it.absolutePath } ?: emptyList()
                    } else {
                        listOf(sourcePath)
                    }

                    val zipResult = createZipFromFiles(filePaths, zipFile)
                    when (zipResult) {
                        is QrResult.Error ->QrResult.Error( zipResult.error)
                        is QrResult.Success -> {
                            Timber.d("Created ZIP archive: ${zipFile.absolutePath}")
                            QrResult.Success(zipFile)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Create ZIP archive failed")
            QrResult.Error(QrError.ShareError.ZIP_CREATION_FAILED)
        }
    }

    override suspend fun cleanupTemporaryFiles(): QrResult<Int, QrError> {
        return try {

            when (val tempDirResult = getTempZipDirectory()) {
                is QrResult.Error -> QrResult.Error(tempDirResult.error)
                is QrResult.Success -> {
                    val listResult = coreFileRepo.listFiles(tempDirResult.data)
                    when (listResult) {
                        is QrResult.Error -> QrResult.Error(listResult.error)
                        is QrResult.Success -> {
                            var deletedCount = 0
                            val currentTime = System.currentTimeMillis()
                            val cleanupAge = 24 * 60 * 60 * 1000L // 24 hours

                            for (fileInfo in listResult.data) {
                                if ((fileInfo.name.startsWith(TEMP_SHARE_PREFIX) ||
                                            fileInfo.name.startsWith(TEMP_ZIP_PREFIX)) &&
                                    (currentTime - fileInfo.lastModified > cleanupAge)) {

                                    when (coreFileRepo.deleteFile(fileInfo.path)) {
                                        is QrResult.Success -> deletedCount++
                                        is QrResult.Error -> Timber.w("Failed to delete: ${fileInfo.name}")
                                    }
                                }
                            }

                            Timber.d("Cleaned up $deletedCount temporary share files")
                            QrResult.Success(deletedCount)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Cleanup temporary files failed")
            QrResult.Error(QrError.ShareError.CLEANUP_FAILED)
        }
    }

    // ===== MIME TYPE & METADATA =====

    override suspend fun detectMimeType(filePath: String): QrResult<String, QrError> {
        return try {
            val file = File(filePath)

            val extensionMimeType = QReportMimeTypes.forExtension(file.extension)
            if (extensionMimeType != null) {
                return QrResult.Success(extensionMimeType)
            }

            val systemMimeType = context.contentResolver.getType(Uri.fromFile(file))
            val mimeType = systemMimeType ?: DEFAULT_MIME_TYPE

            Timber.d("Detected MIME type: $mimeType for ${file.extension}")
            QrResult.Success(mimeType)

        } catch (e: Exception) {
            Timber.e(e, "MIME type detection failed")
            QrResult.Success(DEFAULT_MIME_TYPE)
        }
    }

    override suspend fun getShareableFileInfo(filePath: String): QrResult<ShareableFileInfo, QrError> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return QrResult.Error(QrError.ShareError.FILE_NOT_FOUND)
            }

            val mimeType =  when(val result = detectMimeType(filePath)) {
                is QrResult.Error -> DEFAULT_MIME_TYPE
                is QrResult.Success -> result.data
            }

            val isShareable = when (val validation = validateFileForSharing(filePath)) {
                is QrResult.Error -> false
                is QrResult.Success -> validation.data.canShare
            }

            val shareableFileInfo = ShareableFileInfo(
                name = file.name,
                path = filePath,
                size = file.length(),
                mimeType = mimeType,
                extension = file.extension.takeIf { it.isNotEmpty() },
                lastModified = file.lastModified(),
                isShareable = isShareable,
                estimatedShareSize = file.length(),
                supportedShareMethods = listOf(ShareMethod.DIRECT)
            )

            QrResult.Success(shareableFileInfo)

        } catch (e: Exception) {
            Timber.e(e, "Get shareable file info failed")
            QrResult.Error(QrError.ShareError.METADATA_FAILED)
        }
    }

    // ===== FILEPROVIDER OPERATIONS =====

    override suspend fun createFileProviderUri(filePath: String): QrResult<Uri, QrError.FileError> {
        return try {
            coreFileRepo.createFileProviderUri(filePath)

        } catch (e: Exception) {
            Timber.e(e, "Create FileProvider URI failed: $filePath")
            QrResult.Error(QrError.FileError.IO_ERROR)
        }
    }
    override suspend fun validateFileForSharing(filePath: String): QrResult<ShareValidationResult, QrError> {
        return try {
            val file = File(filePath)
            val issues = mutableListOf<ShareIssue>()
            val warnings = mutableListOf<ShareIssue>()

            if (!file.exists()) {
                issues.add(ShareIssue(
                    type = ShareIssueType.FILE_NOT_FOUND,
                    message = "File not found: $filePath",
                    severity = ShareIssueSeverity.ERROR,
                    suggestedAction = "Verify file path exists"
                ))
            }

            val maxSize = 100 * 1024 * 1024L
            if (file.length() > maxSize) {
                warnings.add(ShareIssue(
                    type = ShareIssueType.FILE_TOO_LARGE,
                    message = "File is ${file.length() / (1024*1024)}MB, may not be supported by all apps",
                    severity = ShareIssueSeverity.WARNING,
                    suggestedAction = "Consider compressing the file"
                ))
            }

            val result = ShareValidationResult(
                canShare = issues.isEmpty(),
                issues = issues,
                warnings = warnings,
                recommendedMethod = if (file.length() > maxSize) ShareMethod.COMPRESSED else ShareMethod.DIRECT
            )

            QrResult.Success(result)

        } catch (e: Exception) {
            Timber.e(e, "Share validation failed")
            QrResult.Error(QrError.ShareError.VALIDATION_FAILED)
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    private suspend fun createZipFromFiles(filePaths: List<String>, zipFile: File): QrResult<Unit, QrError> {
        return try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                for (filePath in filePaths) {
                    val file = File(filePath)
                    if (file.exists() && file.isFile) {
                        addFileToZip(zipOut, file)
                    }
                }
            }

            Timber.d("Created ZIP with ${filePaths.size} files: ${zipFile.absolutePath}")
            QrResult.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "ZIP creation failed")
            QrResult.Error(QrError.ShareError.ZIP_CREATION_FAILED)
        }
    }

    private fun addFileToZip(zipOut: ZipOutputStream, file: File) {
        FileInputStream(file).use { input ->
            val entry = ZipEntry(file.name)
            zipOut.putNextEntry(entry)
            input.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }

    private fun filterExcludedPackages(intent: Intent, excludePackages: Set<String>) {
        try {
            val resolveInfos = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val allowedInfos = resolveInfos.filter { !excludePackages.contains(it.activityInfo.packageName) }

            Timber.d("Filtered ${resolveInfos.size - allowedInfos.size} excluded apps")

        } catch (e: Exception) {
            Timber.w(e, "Failed to filter excluded packages")
        }
    }

    private suspend fun getTempZipDirectory(): QrResult<String, QrError> {
        return try {
            when (val result = coreFileRepo.getOrCreateDirectory(ShareDirectories.TEMP_ZIP_CREATION)) {
                is QrResult.Success -> result
                is QrResult.Error -> QrResult.Error(result.error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get Temporary directory")
            return QrResult.Error(QrError.ShareError.TEMP_FILE_FAILED)
        }
    }
}