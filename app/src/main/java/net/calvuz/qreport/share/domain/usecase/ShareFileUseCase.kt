@file:Suppress("HardCodedStringLiteral")
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
                return@withContext QrResult.Error(QrError.ShareError.FileNotFound())
            }

            if (!file.canRead()) {
                Timber.e("Cannot read exported file: $filePath")
                return@withContext QrResult.Error(QrError.ShareError.PermissionDenied())
            }

            // 2. Check file size constraints for sharing
            val fileSizeBytes = file.length()
            if (fileSizeBytes == 0L) {
                Timber.e("Exported file is empty: $filePath")
                return@withContext QrResult.Error(QrError.FileError.FileEmpty)
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
                    return@withContext QrResult.Error(QrError.ShareError.ValidationFailed())
                }
                is QrResult.Success -> {
                    val validation = validationResult.data
                    if (!validation.canShare) {
                        val majorIssues = validation.issues.filter {
                            it.severity == net.calvuz.qreport.share.domain.repository.ShareIssueSeverity.ERROR
                        }
                        if (majorIssues.isNotEmpty()) {
                            Timber.e("Cannot share file due to: ${majorIssues.first().message}")
                            return@withContext QrResult.Error(QrError.ShareError.ValidationFailed())
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
                        return@withContext QrResult.Error(QrError.ShareError.NoCompatibleApp())
                    } else {
                        Timber.d("Found ${compatibilityResult.data.size} compatible apps for sharing")
                    }
                }
            }

            // 7. Share file using ShareFileRepository
            @Suppress("unused")
            when (val shareResult = shareFileRepository.shareFile(filePath,
                shareOptions)) {
                is QrResult.Error -> {
                    Timber.e("Failed to share exported file: $filePath")
                    // Map ShareError to more specific error if needed
//                    when (shareResult.error) {
//                        is QrError.ShareError.FileNotFound -> QrResult.Error(QrError.ShareError.FileNotFound())
//                        is QrError.ShareError.PermissionDenied -> QrResult.Error(QrError.ShareError.PermissionDenied())
//                        is QrError.ShareError.NoCompatibleApp -> QrResult.Error(QrError.ShareError.NoCompatibleApp())
//                        else ->
                            QrResult.Error(QrError.ShareError.ShareFailed())
//                    }
                }
                is QrResult.Success -> {
                    Timber.d("Exported file shared successfully: $filePath")
                    QrResult.Success(Unit)
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Exception sharing exported file: $filePath")
            QrResult.Error(QrError.ShareError.ShareFailed())
        }
    }
}