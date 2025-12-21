package net.calvuz.qreport.data.repository.backup

import kotlinx.datetime.Clock
import net.calvuz.qreport.data.backup.DatabaseExporter
import net.calvuz.qreport.data.backup.DatabaseImporter
import net.calvuz.qreport.domain.model.backup.BackupValidationResult
import net.calvuz.qreport.domain.model.backup.DatabaseBackup
import net.calvuz.qreport.domain.model.backup.RestoreStrategy
import net.calvuz.qreport.domain.repository.backup.DatabaseExportRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FASE 5.2 - DATABASE EXPORT REPOSITORY IMPLEMENTATION
 *
 * Implementazione concreta del repository per export/import database.
 * Coordina DatabaseExporter e DatabaseImporter seguendo Clean Architecture.
 */

@Singleton
class DatabaseExportRepositoryImpl @Inject constructor(
    private val databaseExporter: DatabaseExporter,
    private val databaseImporter: DatabaseImporter
) : DatabaseExportRepository {

    /**
     * Esporta tutte le tabelle del database
     */
    override suspend fun exportAllTables(): DatabaseBackup {
        Timber.Forest.d("Inizio export database via repository")

        return try {
            databaseExporter.exportAllTables()
        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore export database in repository")
            throw e
        }
    }

    /**
     * Importa tutte le tabelle nel database
     */
    override suspend fun importAllTables(
        databaseBackup: DatabaseBackup,
        strategy: RestoreStrategy
    ): Result<Unit> {
        Timber.Forest.d("Inizio import database via repository - strategia: $strategy")

        return try {
            databaseImporter.importAllTables(databaseBackup, strategy)
        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore import database in repository")
            Result.failure(e)
        }
    }

    /**
     * Valida l'integrità del database corrente
     */
    override suspend fun validateDatabaseIntegrity(): BackupValidationResult {
        Timber.Forest.d("Validazione integrità database")

        return try {
            databaseExporter.validateDatabaseIntegrity()
        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore validazione database")
            BackupValidationResult.Companion.invalid(listOf("Errore validazione: ${e.message}"))
        }
    }

    /**
     * Pulisce tutte le tabelle del database
     * ATTENZIONE: Operazione irreversibile!
     */
    override suspend fun clearAllTables(): Result<Unit> {
        Timber.Forest.w("⚠️  CLEAR ALL TABLES - Operazione irreversibile richiesta")

        return try {
            // Usa l'importer per clear sicuro con ordine FK
            val emptyBackup = DatabaseBackup(
                checkUps = emptyList(),
                checkItems = emptyList(),
                photos = emptyList(),
                spareParts = emptyList(),
                clients = emptyList(),
                contacts = emptyList(),
                facilities = emptyList(),
                facilityIslands = emptyList(),
                checkUpAssociations = emptyList(),
                exportedAt = Clock.System.now()
            )

            // Import backup vuoto con REPLACE_ALL = clear completo
            databaseImporter.importAllTables(emptyBackup, RestoreStrategy.REPLACE_ALL)

        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore clear database")
            Result.failure(e)
        }
    }

    /**
     * Conta i record totali stimati nel database
     */
    override suspend fun getEstimatedRecordCount(): Int {
        return try {
            databaseExporter.getEstimatedRecordCount()
        } catch (e: Exception) {
            Timber.Forest.e(e, "Errore conteggio record")
            0
        }
    }
}