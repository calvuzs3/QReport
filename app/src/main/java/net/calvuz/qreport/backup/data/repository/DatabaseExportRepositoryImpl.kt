@file:Suppress("HardCodedStringLiteral")
package net.calvuz.qreport.backup.data.repository

import kotlinx.datetime.Clock
import net.calvuz.qreport.backup.data.DatabaseExporter
import net.calvuz.qreport.backup.data.DatabaseImporter
import net.calvuz.qreport.backup.domain.model.BackupValidationResult
import net.calvuz.qreport.backup.domain.model.backup.DatabaseBackup
import net.calvuz.qreport.backup.domain.model.enum.RestoreStrategy
import net.calvuz.qreport.backup.domain.repository.DatabaseExportRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates DatabaseExporter e DatabaseImporter following the Clean Architecture.
 */

@Singleton
class DatabaseExportRepositoryImpl @Inject constructor(
    private val databaseExporter: DatabaseExporter,
    private val databaseImporter: DatabaseImporter
) : DatabaseExportRepository {

    /**
     * Export all tables
     */
    override suspend fun exportAllTables(): DatabaseBackup {
        Timber.i("Export db tables via repository")

        return try {
            databaseExporter.exportAllTables()
        } catch (e: Exception) {
            Timber.e(e, "Export db tables via repository failed")
            throw e
        }
    }

    /**
     * Import all tables
     */
    override suspend fun importAllTables(
        databaseBackup: DatabaseBackup,
        strategy: RestoreStrategy
    ): Result<Unit> {
        Timber.i("Import db tables via repository (strategy: $strategy)")

        return try {
            databaseImporter.importAllTables(databaseBackup, strategy)
        } catch (e: Exception) {
            Timber.e(e, "Import db tables via repository failed")
            Result.failure(e)
        }
    }

    /**
     * DB integrity check
     */
    override suspend fun validateDatabaseIntegrity(): BackupValidationResult {
        Timber.i("Database integrity check")

        return try {
            databaseExporter.validateDatabaseIntegrity()
        } catch (e: Exception) {
            Timber.e(e, "Database integrity check failed")
            BackupValidationResult.invalid(listOf("Database integrity check failed: ${e.message}"))
        }
    }

    /**
     * Clear all tables
     * tips: use DatabaseImporter for a secure cleanup with FK order
     */
    override suspend fun clearAllTables(): Result<Unit> {
        Timber.w("Clear all tables")

        return try {
            val emptyBackup = DatabaseBackup(
                checkUps = emptyList(),
                checkItems = emptyList(),
                photos = emptyList(),
                spareParts = emptyList(),
                clients = emptyList(),
                contacts = emptyList(),
                contracts = emptyList(),
                facilities = emptyList(),
                facilityIslands = emptyList(),
                mechanicalUnits = emptyList(),
                checkUpAssociations = emptyList(),
                technicalInterventions = emptyList(),
                maintenanceLogs = emptyList(),
                documents = emptyList(),
                exportedAt = Clock.System.now()
            )

            // Import empty backup with strategy REPLACE_ALL
            databaseImporter.importAllTables(emptyBackup, RestoreStrategy.REPLACE_ALL)

        } catch (e: Exception) {
            Timber.e(e, "Clear all tables failed")
            Result.failure(e)
        }
    }

    /**
     * Count total records
     */
    override suspend fun getEstimatedRecordCount(): Int {
        Timber.i("Get estimated record count")

        return try {
            databaseExporter.getEstimatedRecordCount()
        } catch (e: Exception) {
            Timber.e(e, "Get estimated record count failed")
            0
        }
    }
}