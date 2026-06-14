package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * DatabaseBackup - Contenitore per tutti i dati del database
 *
 * ===== CHECKLIST: aggiungere una nuova tabella al backup =====
 * Non esiste un registry: ogni tabella va wired a mano nei seguenti punti:
 * 1. DTO `XxxBackup.kt` in `backup/domain/model/backup/`
 * 2. Campo qui in [DatabaseBackup] + [getTotalRecordCount] + [empty]
 * 3. `DatabaseExporter`: DAO nel costruttore, export + mapper `toBackup()`
 * 4. `DatabaseImporter`: DAO nel costruttore, `clearAllTablesInOrder()` (ordine FK
 *    inverso), `importAllTablesInOrder()` (ordine FK), `validateImportedData()`,
 *    `logImportStatistics()`, mapper `toEntity()`
 * 5. Il DAO deve esporre `getAllForBackup/insertAllFromBackup/deleteAll/count`
 * 6. `DatabaseExportRepositoryImpl.clearAllTables()` - empty literal
 * 7. `BackupJsonSerializer.validateBackupJson()` - `expectedTables` (opzionale)
 * ================================================================
 */
@Serializable
data class DatabaseBackup(
    // ===== CORE CHECKUP =====
    val checkUps: List<CheckUpBackup>,
    val checkItems: List<CheckItemBackup>,
    val photos: List<PhotoBackup>,
    val spareParts: List<SparePartBackup>,

    // ===== CLIENT MANAGEMENT =====
    val clients: List<ClientBackup>,
    val contacts: List<ContactBackup>,
    val contracts: List<ContractBackup>,
    val facilities: List<FacilityBackup>,
    val facilityIslands: List<FacilityIslandBackup>,
    val mechanicalUnits: List<MechanicalUnitBackup>,

    // ===== ASSOCIATIONS =====
    val checkUpAssociations: List<CheckUpAssociationBackup>,

    // ===== TECHNICAL INTERVENTIONS =====
    val technicalInterventions: List<TechnicalInterventionBackup> = emptyList(),

    // ===== ISLAND MAINTENANCE =====
    val maintenanceLogs: List<MaintenanceLogBackup> = emptyList(),

    // ===== DOCUMENTS =====
    val documents: List<DocumentBackup> = emptyList(),

    // ===== METADATA =====
    @Contextual val exportedAt: Instant
) {
    /**
     * Conta totale record per validazione
     */
    fun getTotalRecordCount(): Int {
        return checkUps.size +
                checkItems.size +
                photos.size +
                spareParts.size +
                clients.size +
                contacts.size +
                contracts.size +
                facilities.size +
                facilityIslands.size +
                mechanicalUnits.size +
                checkUpAssociations.size +
                technicalInterventions.size +
                maintenanceLogs.size +
                documents.size
    }

    /**
     * Verifica se il backup è vuoto
     */
    fun isEmpty(): Boolean {
        return getTotalRecordCount() == 0
    }

    companion object {
        fun empty(): DatabaseBackup {
            return DatabaseBackup(
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
                exportedAt = Instant.fromEpochMilliseconds(0)
            )
        }
    }
}
