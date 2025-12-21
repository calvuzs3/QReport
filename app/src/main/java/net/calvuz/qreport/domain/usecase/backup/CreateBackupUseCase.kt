package net.calvuz.qreport.domain.usecase.backup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.backup.*
import net.calvuz.qreport.domain.repository.backup.BackupRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * BACKUP USE CASES - FASE 5.4
 *
 * Use Cases per il sistema backup seguendo Clean Architecture:
 * - Single Responsibility per ogni operation
 * - Domain logic puro senza Android dependencies
 * - Flow-based per progress tracking
 * - Result pattern per error handling
 */

// ===== CREATE BACKUP USE CASE =====

/**
 * Creates a new backup with specified options
 */
class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    operator fun invoke(
        includePhotos: Boolean = true,
        includeThumbnails: Boolean = false,
        backupMode: BackupMode = BackupMode.LOCAL,
        description: String? = null
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
                description = description ?: "Backup ${Clock.System.now()}"
            ).collect { progress ->
                emit(progress)
            }

        } catch (e: Exception) {
            Timber.e(e, "Error in CreateBackupUseCase")
            emit(BackupProgress.Error("Backup creation failed: ${e.message}"))
        }
    }
}


/*
=============================================================================
                            BACKUP USE CASES ARCHITECTURE
=============================================================================

RESPONSABILITÀ USE CASES:
✅ CreateBackupUseCase - Orchestratore creazione backup
✅ RestoreBackupUseCase - Gestione ripristino con strategie
✅ GetAvailableBackupsUseCase - Retrieval lista backup ordinata
✅ DeleteBackupUseCase - Eliminazione sicura backup
✅ GetBackupSizeUseCase - Stima dimensioni backup
✅ ShareBackupUseCase - Preparazione condivisione backup
✅ ValidateBackupUseCase - Verifica integrità backup
✅ GetBackupSummaryUseCase - Overview sistema backup
✅ BackupHealthCheckUseCase - Diagnostics sistema backup

DESIGN PATTERNS:
✅ Single Responsibility Principle
✅ Dependency Injection con Hilt
✅ Flow per progress tracking long-running operations
✅ Result pattern per error handling
✅ Timber logging per debugging
✅ Input validation e sanitization

CLEAN ARCHITECTURE:
✅ Domain layer puro (no Android dependencies)
✅ Repository interface dependency inversion
✅ Business logic centralizzato
✅ Testable con mocking
✅ Immutable data models
✅ Error handling consistente

INTEGRATION:
✅ Dependency injection ready
✅ ViewModel consumption ready
✅ Repository pattern compliant
✅ Testing framework ready
✅ Logging e monitoring ready

=============================================================================
*/