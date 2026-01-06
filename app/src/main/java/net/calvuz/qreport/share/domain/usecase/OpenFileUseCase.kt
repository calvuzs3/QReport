package net.calvuz.qreport.share.domain.usecase

import android.content.Context
import android.content.Intent
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.share.domain.repository.ShareFileRepository
import net.calvuz.qreport.share.domain.repository.OpenOptions
import net.calvuz.qreport.share.domain.repository.QReportMimeTypes
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * UseCase per aprire file esportati con app appropriate
 *
 * Responsabilità:
 * - Determinare MIME type appropriato dal formato export
 * - Configurare OpenOptions per file exports
 * - Gestire validazione file e error handling
 * - Coordinare apertura file tramite ShareFileRepository
 */
class OpenFileUseCase @Inject constructor(
    @ApplicationContext val context: Context,
    private val shareFileRepository: ShareFileRepository
) {

    /**
     * Open exported file with appropriate application
     *
     * @param filePath Percorso completo al file esportato
     * @param mimeType MIME type del file export
     * @param chooserTitle Titolo per il dialog di selezione app
     * @param allowChooser Se consentire lo chooser
     * @return QrResult.Success se apertura avviata, QrResult.Error con dettagli se fallita
     */
    suspend operator fun invoke(
        filePath: String,
        mimeType: String = QReportMimeTypes.UNKNOWN,
        chooserTitle: String,
        allowChooser: Boolean = true,
        requireExternalApp: Boolean = false,    // Allow system apps
        openAsDirectory: Boolean = false,       // Open as directory
        copyToDocuments: Boolean = false        // Auto-copy to Documents if internal path
    ): QrResult<Unit, QrError> = withContext(Dispatchers.IO) {

        try {
            Timber.d("Opening exported file use case {filePath=$filePath, mime=$mimeType}")

            var actualFilePath = filePath

            // ✅ STEP 1: Copy to Documents if requested and path is internal
            if (copyToDocuments && isInternalAppPath(filePath)) {
                Timber.d("Copying internal export to Documents: $filePath")

                when (val copyResult = copyExportToDocuments(filePath)) {
                    is QrResult.Error -> return@withContext QrResult.Error(copyResult.error)
                    is QrResult.Success -> {
                        actualFilePath = copyResult.data
                        Timber.d("Export copied to Documents: $actualFilePath")
                    }
                }
            }

            val file = File(actualFilePath)

            // Validation
            if (!file.exists()) {
                Timber.e("File/directory not found: $actualFilePath")
                return@withContext QrResult.Error(QrError.ShareError.FILE_NOT_FOUND)
            }

            // ✅ STEP 2: Handle directory opening
            if (openAsDirectory && file.isDirectory) {
                return@withContext openDirectory(actualFilePath, chooserTitle)
            }

            // ✅ STEP 3: Handle regular file opening (existing logic)
            if (!file.canRead()) {
                Timber.e("Cannot read file: $actualFilePath")
                return@withContext QrResult.Error(QrError.ShareError.PERMISSION_DENIED)
            }


            // 3. Configure open options for exported files
            val openOptions = OpenOptions(
                mimeType = mimeType,
                allowChooser = allowChooser,
                chooserTitle = chooserTitle,
                requireExternalApp = requireExternalApp  // Allow system apps
            )

            // 4. Validate file can be opened before attempting
            when (val compatibilityResult = shareFileRepository.getCompatibleApps(filePath)) {
                is QrResult.Error -> {
                    Timber.w("Cannot check app compatibility for: $filePath")
                    // Continue anyway - might still work
                }

                is QrResult.Success -> {
                    if (compatibilityResult.data.isEmpty()) {
                        Timber.w("No compatible apps found for: $filePath")
                        return@withContext QrResult.Error(QrError.ShareError.NO_COMPATIBLE_APP)
                    } else {
                        Timber.d("Found ${compatibilityResult.data.size} compatible apps for file")
                    }
                }
            }

            // 5. Open file using ShareFileRepository
            when (val openResult = shareFileRepository.openFile(filePath, openOptions)) {
                is QrResult.Error -> {
                    Timber.e("Failed to open exported file: $filePath")
                    // Map ShareError to more specific error if needed
//                    when (openResult.error) {
//                        is QrError.ShareError.FILE_NOT_FOUND -> QrResult.Error(QrError.ShareError.FILE_NOT_FOUND)
//                        is QrError.ShareError.NO_COMPATIBLE_APPS -> QrResult.Error(QrError.ShareError.NO_COMPATIBLE_APPS)
//                        is QrError.ShareError.PERMISSION_DENIED -> QrResult.Error(QrError.ShareError.PERMISSION_DENIED)
//                        else ->
                    QrResult.Error(QrError.ShareError.OPEN_FAILED)
//                    }
                }

                is QrResult.Success -> {
                    Timber.d("Exported file opened successfully: $filePath")
                    QrResult.Success(Unit)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Exception opening exported file: $filePath")
            QrResult.Error(QrError.ShareError.OPEN_FAILED)
        }
    }

    /**
     * Check if exported file can be opened
     * Useful for UI to enable/disable open buttons
     */
    suspend fun canOpenFile(
        filePath: String,
    ): QrResult<Boolean, QrError> = withContext(Dispatchers.IO) {

        try {
            // Basic file checks
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) {
                return@withContext QrResult.Success(false)
            }

            // Check for compatible apps
            when (val compatibilityResult = shareFileRepository.getCompatibleApps(filePath)) {
                is QrResult.Error -> QrResult.Success(false) // Assume not openable if can't check
                is QrResult.Success -> QrResult.Success(compatibilityResult.data.isNotEmpty())
            }

        } catch (e: Exception) {
            Timber.e(e, "Exception checking if file can be opened: $filePath")
            QrResult.Success(false)
        }
    }

    private suspend fun openDirectory(
        directory: String,
        chooserTitle: String
    ): QrResult<Unit, QrError> {
        return try {
            val directoryUri = when (val result =
                shareFileRepository.createFileProviderUri(directory)) { // .getUriForFile(
                is QrResult.Error -> {
                    Timber.e("Failed to create URI for directory: ${directory}")
                    return QrResult.Error(QrError.ShareError.OPEN_FAILED)
                }
                is QrResult.Success -> result.data
            }
//            val directoryUri = FileProvider.getUriForFile(
//                context,
//                "net.calvuz.qreport.fileprovider",
//                directory
//            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(directoryUri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                QrResult.Success(Unit)
            } else {
                // Fallback: show file manager chooser
                val chooserIntent = Intent.createChooser(intent, chooserTitle).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
                QrResult.Success(Unit)
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to open directory: ${directory}")
            QrResult.Error(QrError.ShareError.OPEN_FAILED)
        }
    }

    // ✅ HELPER: Check if path is internal app storage
    private fun isInternalAppPath(path: String): Boolean {
        return path.contains("/data/") && path.contains("/files/")
    }

    // ✅ HELPER: Copy export to Documents (inline logic)
    private suspend fun copyExportToDocuments(internalPath: String): QrResult<String, QrError> {
        return try {
            // Get Documents/QReport directory
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val qreportDir = File(documentsDir, "QReport")

            if (!qreportDir.exists() && !qreportDir.mkdirs()) {
                Timber.e("Failed to create Documents/QReport directory")
                return QrResult.Error(QrError.FileError.DIRECTORY_CREATE)
            }

            // Copy source to target
            val sourceFile = File(internalPath)
            val targetFile = File(qreportDir, sourceFile.name)

            if (sourceFile.isDirectory) {
                // Copy entire directory
                if (targetFile.exists()) {
                    targetFile.deleteRecursively()
                }
                sourceFile.copyRecursively(targetFile, overwrite = true)
            } else {
                // Copy single file
                sourceFile.copyTo(targetFile, overwrite = true)
            }

            Timber.d("Export copied successfully: ${targetFile.absolutePath}")
            QrResult.Success(targetFile.absolutePath)

        } catch (e: Exception) {
            Timber.e(e, "Failed to copy export to Documents")
            QrResult.Error(QrError.FileError.FILE_COPY)
        }
    }
}