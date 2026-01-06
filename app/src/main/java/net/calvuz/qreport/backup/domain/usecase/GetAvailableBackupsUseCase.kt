package net.calvuz.qreport.backup.domain.usecase

import net.calvuz.qreport.backup.domain.model.BackupInfo
import net.calvuz.qreport.backup.domain.repository.BackupRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * FASE 5.4 - GET AVAILABLE BACKUPS USE CASE
 *
 * Use case per recuperare lista backup disponibili nel sistema.
 * Ordina per data (più recenti prima) e include metadati.
 */
class GetAvailableBackupsUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {

    /**
     * Recupera lista backup disponibili ordinati per data
     */

    suspend operator fun invoke(): List<BackupInfo> {
        return try {
            val backups = backupRepository.getAvailableBackups()

            Timber.d("Backup found ${backups.size}")
            backups.sortedByDescending { it.createdAt } // Most recent first

        } catch (e: Exception) {
            Timber.e(e, "Getting available backups failed")
            emptyList()
        }
    }
}