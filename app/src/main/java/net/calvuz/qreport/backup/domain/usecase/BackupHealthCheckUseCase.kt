package net.calvuz.qreport.backup.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.backup.data.model.BackupHealthResult
import net.calvuz.qreport.backup.domain.repository.BackupRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Performs health check on backup system
 */
class BackupHealthCheckUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): BackupHealthResult {
        return try {
            Timber.Forest.d("Performing backup system health check")

            val issues = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Check available storage
            val estimatedSize = backupRepository.getEstimatedBackupSize(includePhotos = true)
            // TODO: Check actual available storage vs estimated size

            // Check backup age
            val backups = backupRepository.getAvailableBackups()
            val lastBackup = backups.maxByOrNull { it.createdAt }

            // TODO: Add more health checks:
            // - Last backup age > 30 days
            // - Backup corruption detection
            // - Storage space warnings
            // - Permission checks

            BackupHealthResult(
                isHealthy = issues.isEmpty(),
                issues = issues,
                warnings = warnings,
                lastCheckTimestamp = Clock.System.now()
            )

        } catch (e: Exception) {
            Timber.Forest.e(e, "Error during backup health check")
            BackupHealthResult.Companion.unhealthy(listOf("Health check failed: ${e.message}"))
        }
    }
}