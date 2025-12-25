package net.calvuz.qreport.domain.usecase.backup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.domain.repository.backup.BackupRepository
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
                emit(BackupProgress.Error("Cannot include thumbnails without photos"))
                return@flow
            }

            // Start backup process
            emit(BackupProgress.InProgress("Inizializing backup...", 0.0f))

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
            emit(BackupProgress.Error("Backup creation failed: ${e.message}"))
        }
    }
}