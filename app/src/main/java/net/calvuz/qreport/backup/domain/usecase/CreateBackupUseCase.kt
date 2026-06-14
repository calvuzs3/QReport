package net.calvuz.qreport.backup.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText.StringResource
import net.calvuz.qreport.app.error.presentation.UiText.StringResources
import net.calvuz.qreport.backup.domain.model.enum.BackupMode
import net.calvuz.qreport.backup.presentation.ui.model.BackupProgress
import net.calvuz.qreport.backup.domain.repository.BackupRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Creates a new backup with specified options
 * Clean Architecture:
 * - Single Responsibility
 * - Pure domain logic
 * - Flow-based for progress tracking
 * - Result pattern for error handling
 */
class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    operator fun invoke(
        includePhotos: Boolean = true,
        includeThumbnails: Boolean = true,
        backupMode: BackupMode = BackupMode.LOCAL,
        description: String
    ): Flow<BackupProgress> = flow {

        Timber.d("Creating backup: photos=$includePhotos, thumbnails=$includeThumbnails, mode=$backupMode")

        try {
            // Validate inputs
            if (includeThumbnails && !includePhotos) {
                emit(BackupProgress.Error(StringResource(R.string.backup_progress_error_thumbnails_without_photos)))
                return@flow
            }

            // Start backup process
            emit(BackupProgress.InProgress(StringResource(R.string.backup_progress_step_initializing), 0.0f))

            // Delegate to repository
            backupRepository.createFullBackup(
                includePhotos = includePhotos,
                includeThumbnails = includeThumbnails,
                backupMode = backupMode,
                description = description
            ).collect { progress ->
                emit(progress)
            }

        } catch (e: Exception) {
            Timber.e(e, "Error in CreateBackupUseCase")
            emit(BackupProgress.Error(StringResources(R.string.backup_progress_error_generic, e.message ?: "")))
        }
    }
}