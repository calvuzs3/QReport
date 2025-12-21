package net.calvuz.qreport.domain.repository.backup

import net.calvuz.qreport.domain.model.backup.BackupValidationResult
import net.calvuz.qreport.domain.model.backup.DatabaseBackup
import net.calvuz.qreport.domain.model.backup.RestoreStrategy

/**
 * Repository per export/import database
 */
interface DatabaseExportRepository {

    /**
     * Esporta tutte le tabelle
     */
    suspend fun exportAllTables(): DatabaseBackup

    /**
     * Importa tutte le tabelle
     */
    suspend fun importAllTables(
        databaseBackup: DatabaseBackup,
        strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL
    ): Result<Unit>

    /**
     * Valida integrità database
     */
    suspend fun validateDatabaseIntegrity(): BackupValidationResult

    /**
     * Pulisce tutte le tabelle
     */
    suspend fun clearAllTables(): Result<Unit>

    /**
     * Conta record totali
     */
    suspend fun getEstimatedRecordCount(): Int
}