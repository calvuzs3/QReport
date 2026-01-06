package net.calvuz.qreport.share.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.share.domain.repository.QReportMimeTypes
import net.calvuz.qreport.share.domain.repository.ShareFileRepository
import net.calvuz.qreport.share.domain.repository.ShareOptions
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * UseCase per condividere file esportati tramite Android sharing system
 *
 * Responsabilità:
 * - Determinare MIME type e configurazione sharing dal formato export
 * - Configurare ShareOptions appropriate per file exports
 * - Gestire validazione file e controlli pre-sharing
 * - Coordinare condivisione tramite ShareFileRepository
 */
class ShareFileUseCase @Inject constructor(
    private val shareFileRepository: ShareFileRepository
) {

    /**
     * Share exported file using Android sharing system
     *
     * @param filePath Percorso completo al file esportato
     * @param exportFormat Formato del file export per determinare configurazione sharing
     * @return QrResult.Success se condivisione avviata, QrResult.Error con dettagli se fallita
     */
    suspend operator fun invoke(
        filePath: String,
        mimeType: String =  QReportMimeTypes.UNKNOWN,
        shareTitle: String,
        shareSubject: String,
    ): QrResult<Unit, QrError> = withContext(Dispatchers.IO) {

        try {
            Timber.d("Sharing exported file: $filePath (mime: $mimeType)")

            // 1. Validate file existence and readability
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("Exported file not found: $filePath")
                return@withContext QrResult.Error(QrError.ShareError.FILE_NOT_FOUND)
            }

            if (!file.canRead()) {
                Timber.e("Cannot read exported file: $filePath")
                return@withContext QrResult.Error(QrError.ShareError.PERMISSION_DENIED)
            }

            // 2. Check file size constraints for sharing
            val fileSizeBytes = file.length()
            if (fileSizeBytes == 0L) {
                Timber.e("Exported file is empty: $filePath")
                return@withContext QrResult.Error(QrError.FileError.FILE_EMPTY)
            }

            // Warn for very large files (>100MB) but don't block
            if (fileSizeBytes > 100 * 1024 * 1024) {
                Timber.w("Large file being shared: ${fileSizeBytes / 1024 / 1024}MB")
            }

            // 3. Determine MIME type and sharing configuration from export format

            // 4. Configure share options for exported files
            val shareOptions = ShareOptions(
                subject = shareSubject,
                chooserTitle = shareTitle,
                mimeType = mimeType,
                excludePackages = emptySet(), // Don't exclude any apps for exports
                includeTextContent = false,   // File-only sharing for exports
                requireExternalApp = false    // Allow system apps
            )

            // 5. Validate file can be shared before attempting
            when (val validationResult = shareFileRepository.validateFileForSharing(filePath)) {
                is QrResult.Error -> {
                    Timber.e("File validation failed for sharing: $filePath")
                    return@withContext QrResult.Error(QrError.ShareError.VALIDATION_FAILED)
                }
                is QrResult.Success -> {
                    val validation = validationResult.data
                    if (!validation.canShare) {
                        val majorIssues = validation.issues.filter {
                            it.severity == net.calvuz.qreport.share.domain.repository.ShareIssueSeverity.ERROR
                        }
                        if (majorIssues.isNotEmpty()) {
                            Timber.e("Cannot share file due to: ${majorIssues.first().message}")
                            return@withContext QrResult.Error(QrError.ShareError.VALIDATION_FAILED)
                        }
                    }

                    // Log warnings but continue
                    validation.warnings.forEach { warning ->
                        Timber.w("Share warning: ${warning.message}")
                    }
                }
            }

            // 6. Check for compatible apps before sharing
            when (val compatibilityResult = shareFileRepository.getCompatibleApps(filePath)) {
                is QrResult.Error -> {
                    Timber.w("Cannot check app compatibility for: $filePath, proceeding anyway")
                    // Continue - some apps might still accept the file
                }
                is QrResult.Success -> {
                    if (compatibilityResult.data.isEmpty()) {
                        Timber.e("No compatible apps found for sharing: $filePath")
                        return@withContext QrResult.Error(QrError.ShareError.NO_COMPATIBLE_APP)
                    } else {
                        Timber.d("Found ${compatibilityResult.data.size} compatible apps for sharing")
                    }
                }
            }

            // 7. Share file using ShareFileRepository
            when (val shareResult = shareFileRepository.shareFile(filePath, shareOptions)) {
                is QrResult.Error -> {
                    Timber.e("Failed to share exported file: $filePath")
                    // Map ShareError to more specific error if needed
//                    when (shareResult.error) {
//                        is QrError.ShareError.FILE_NOT_FOUND -> QrResult.Error(QrError.ShareError.FILE_NOT_FOUND)
//                        is QrError.ShareError.PERMISSION_DENIED -> QrResult.Error(QrError.ShareError.PERMISSION_DENIED)
//                        is QrError.ShareError.NO_COMPATIBLE_APPS -> QrResult.Error(QrError.ShareError.NO_COMPATIBLE_APPS)
//                        else ->
                            QrResult.Error(QrError.ShareError.SHARE_FAILED)
//                    }
                }
                is QrResult.Success -> {
                    Timber.d("Exported file shared successfully: $filePath")
                    QrResult.Success(Unit)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Exception sharing exported file: $filePath")
            QrResult.Error(QrError.ShareError.SHARE_FAILED)
        }
    }

    /**
     * Check if exported file can be shared
     * Useful for UI to enable/disable share buttons
     */
    suspend fun canShareFile(
        filePath: String,
    ): QrResult<Boolean, QrError> = withContext(Dispatchers.IO) {

        try {
            // Basic file checks
            val file = File(filePath)
            if (!file.exists() || !file.canRead() || file.length() == 0L) {
                return@withContext QrResult.Success(false)
            }

            // Validation check
            when (val validationResult = shareFileRepository.validateFileForSharing(filePath)) {
                is QrResult.Error -> return@withContext QrResult.Success(false)
                is QrResult.Success -> {
                    if (!validationResult.data.canShare) {
                        return@withContext QrResult.Success(false)
                    }
                }
            }

            // Compatibility check
            when (val compatibilityResult = shareFileRepository.getCompatibleApps(filePath)) {
                is QrResult.Error -> QrResult.Success(false)
                is QrResult.Success -> QrResult.Success(compatibilityResult.data.isNotEmpty())
            }

        } catch (e: Exception) {
            Timber.e(e, "Exception checking if file can be shared: $filePath")
            QrResult.Success(false)
        }
    }
}