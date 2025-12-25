package net.calvuz.qreport.domain.usecase.backup

import net.calvuz.qreport.data.backup.model.BackupInfo
import net.calvuz.qreport.domain.repository.backup.BackupRepository
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
            backups.sortedByDescending { it.timestamp } // Most recent first

        } catch (e: Exception) {
            Timber.e(e, "Getting available backups failed")
            emptyList()
        }
    }
}