package net.calvuz.qreport.backup.data

import androidx.room.withTransaction
import net.calvuz.qreport.backup.domain.model.BackupValidationResult
import net.calvuz.qreport.backup.domain.model.backup.CheckItemBackup
import net.calvuz.qreport.backup.domain.model.backup.CheckUpAssociationBackup
import net.calvuz.qreport.backup.domain.model.backup.CheckUpBackup
import net.calvuz.qreport.backup.domain.model.backup.CheckUpMaintenanceLogAssociationBackup
import net.calvuz.qreport.backup.domain.model.backup.ClientBackup
import net.calvuz.qreport.backup.domain.model.backup.ContactBackup
import net.calvuz.qreport.backup.domain.model.backup.DatabaseBackup
import net.calvuz.qreport.backup.domain.model.backup.FacilityBackup
import net.calvuz.qreport.backup.domain.model.backup.FacilityIslandBackup
import net.calvuz.qreport.backup.domain.model.backup.PhotoBackup
import net.calvuz.qreport.backup.domain.model.enum.RestoreStrategy
import net.calvuz.qreport.backup.domain.model.backup.SparePartBackup
import net.calvuz.qreport.backup.domain.model.backup.TiAssociationBackup
import net.calvuz.qreport.backup.domain.model.backup.TiMaintenanceLogAssociationBackup
import net.calvuz.qreport.app.database.data.local.QReportDatabase
import net.calvuz.qreport.backup.domain.model.backup.ContractBackup
import net.calvuz.qreport.backup.domain.model.backup.MechanicalUnitBackup
import net.calvuz.qreport.backup.domain.model.backup.TechnicalInterventionBackup
import net.calvuz.qreport.backup.domain.model.backup.MaintenanceLogBackup
import net.calvuz.qreport.backup.domain.model.backup.DocumentBackup
import net.calvuz.qreport.checkup.checkup.data.local.dao.CheckUpMaintenanceLogAssociationDao
import net.calvuz.qreport.client.island.maintenance.data.local.dao.MaintenanceLogDao
import net.calvuz.qreport.client.island.maintenance.data.local.entity.MaintenanceLogEntity
import net.calvuz.qreport.client.document.data.local.dao.DocumentDao
import net.calvuz.qreport.client.document.data.local.entity.DocumentEntity
import net.calvuz.qreport.checkup.items.data.local.dao.CheckItemDao
import net.calvuz.qreport.checkup.checkup.data.local.dao.CheckUpAssociationDao
import net.calvuz.qreport.checkup.checkup.data.local.dao.CheckUpDao
import net.calvuz.qreport.client.client.data.local.dao.ClientDao
import net.calvuz.qreport.client.contact.data.local.dao.ContactDao
import net.calvuz.qreport.client.facility.data.local.dao.FacilityDao
import net.calvuz.qreport.client.island.data.local.dao.IslandDao
import net.calvuz.qreport.checkup.checkup.data.local.dao.SparePartDao
import net.calvuz.qreport.checkup.items.data.local.entity.CheckItemEntity
import net.calvuz.qreport.checkup.checkup.data.local.entity.CheckUpEntity
import net.calvuz.qreport.checkup.checkup.data.local.entity.CheckUpIslandAssociationEntity
import net.calvuz.qreport.checkup.checkup.data.local.entity.CheckUpMaintenanceLogAssociationEntity
import net.calvuz.qreport.client.client.data.local.entity.ClientEntity
import net.calvuz.qreport.client.contact.data.local.entity.ContactEntity
import net.calvuz.qreport.client.facility.data.local.entity.FacilityEntity
import net.calvuz.qreport.client.island.data.local.entity.IslandEntity
import net.calvuz.qreport.checkup.checkup.data.local.entity.SparePartEntity
import net.calvuz.qreport.client.contract.data.local.dao.ContractDao
import net.calvuz.qreport.client.contract.data.local.entity.ContractEntity
import net.calvuz.qreport.client.unit.data.local.dao.MechanicalUnitDao
import net.calvuz.qreport.client.unit.data.local.entity.MechanicalUnitEntity
import net.calvuz.qreport.photo.data.local.dao.PhotoDao
import net.calvuz.qreport.photo.data.local.entity.PhotoEntity
import net.calvuz.qreport.ti.data.local.dao.TiAssociationDao
import net.calvuz.qreport.ti.data.local.dao.TiMaintenanceLogAssociationDao
import net.calvuz.qreport.ti.data.local.dao.TechnicalInterventionDao
import net.calvuz.qreport.ti.data.local.entity.TechnicalInterventionEntity
import net.calvuz.qreport.ti.data.local.entity.TiIslandAssociationEntity
import net.calvuz.qreport.ti.data.local.entity.TiMaintenanceLogAssociationEntity
import net.calvuz.qreport.ti.domain.model.InterventionStatus
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.iterator

/**
 * Secure import of DatabaseBackup into db
 * - FK dependencies
 * - validation
 * - rollback on errors
 */

@Singleton
class DatabaseImporter @Inject constructor(
    private val database: QReportDatabase,
    private val checkUpDao: CheckUpDao,
    private val checkItemDao: CheckItemDao,
    private val photoDao: PhotoDao,
    private val sparePartDao: SparePartDao,
    private val clientDao: ClientDao,
    private val contactDao: ContactDao,
    private val contractDao: ContractDao,
    private val facilityDao: FacilityDao,
    private val islandDao: IslandDao,
    private val mechanicalUnitDao: MechanicalUnitDao,
    private val checkUpAssociationDao: CheckUpAssociationDao,
    private val tiAssociationDao: TiAssociationDao,
    private val checkUpMaintenanceLogAssociationDao: CheckUpMaintenanceLogAssociationDao,
    private val tiMaintenanceLogAssociationDao: TiMaintenanceLogAssociationDao,
    private val technicalInterventionDao: TechnicalInterventionDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val documentDao: DocumentDao
) {

    /**
     * Import data with a single transaction
     * @param databaseBackup Backup
     * @param strategy Import strategy (REPLACE_ALL, MERGE, etc.)
     * @return Result<Unit> for error handling
     */
    suspend fun importAllTables(
        databaseBackup: DatabaseBackup,
        strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL
    ): Result<Unit> = try {

        database.withTransaction {
            Timber.v("Database import begin\n- records: ${databaseBackup.getTotalRecordCount()}")
            val startTime = System.currentTimeMillis()

            when (strategy) {
                RestoreStrategy.REPLACE_ALL -> {
                    clearAllTablesInOrder()
                    importAllTablesInOrder(databaseBackup)
                }

                RestoreStrategy.MERGE -> {
                    // TODO: Implementazione merge (future)
                    throw UnsupportedOperationException("Strategia di unione non ancora implementata")
                }

                RestoreStrategy.SELECTIVE -> {
                    // TODO: Implementazione selettiva (future)
                    throw UnsupportedOperationException("Strategia di ripristino selettivo non ancora implementata")
                }
            }

            // Validazione post-import
            val validation = validateImportedData(databaseBackup)
            if (!validation.isValid) {
                throw DatabaseImportException("Validazione post-importazione non riuscita: ${validation.issues.joinToString()}")
            }

            val duration = System.currentTimeMillis() - startTime
            Timber.v("Import completed in ${duration} ms")

            // Log finale
            logImportStatistics()
        }

        Result.success(Unit)

    } catch (e: Exception) {
        Timber.e(e, "Database import failed")
        Result.failure(DatabaseImportException("Importazione database non riuscita: ${e.message}", e))
    }

    /**
     * Delete all tables in FK inverse order
     */
    private suspend fun clearAllTablesInOrder() {
        Timber.v("Database cleaning - FK inverse order")

        // Ordine critico: elimina prima le tabelle con FK, poi quelle referenziate
        try {
            // 1. Associazioni (dipende da checkups + facility_islands)
            checkUpAssociationDao.deleteAll()
            Timber.v("Cleared checkup_island_associations")

            // 1a. Association tables with FK dependencies
            tiAssociationDao.deleteAll()
            Timber.v("Cleared ti_island_associations")

            checkUpMaintenanceLogAssociationDao.deleteAll()
            Timber.v("Cleared checkup_maintenance_log_associations")

            tiMaintenanceLogAssociationDao.deleteAll()
            Timber.v("Cleared ti_maintenance_log_associations")

            // 1b. Technical Interventions (no FK dependencies)
            technicalInterventionDao.deleteAll()
            Timber.v("Cleared technical_interventions")

            // 1c. Documents (no FK dependencies)
            documentDao.deleteAll()
            Timber.v("Cleared island_documents")

            // 1d. Maintenance logs (CASCADE FK su island_id, ma cancellati
            // esplicitamente prima delle islands per chiarezza)
            maintenanceLogDao.deleteAll()
            Timber.v("Cleared maintenance_logs")

            // 2. Foto (dipende da check_items)
            photoDao.deleteAll()
            Timber.v("Cleared photos")

            // 3. Check items (dipende da checkups)
            checkItemDao.deleteAll()
            Timber.v("Cleared check_items")

            // 4. Spare parts (dipende da checkups)
            sparePartDao.deleteAll()
            Timber.v("Cleared spare_parts")

            // 5. CheckUps (dipende da facility_islands via associations)
            checkUpDao.deleteAll()
            Timber.v("Cleared checkups")

            // 6. Contatti (dipende da clients)
            contactDao.deleteAll()
            Timber.v("Cleared contacts")

            // 6.1 Contract (depends on Client)
            contractDao.deleteAll()
            Timber.v("Cleared contracts")

            // 6.2 Mechanical Unit (depèends on facility islands)
            mechanicalUnitDao.deleteAll()
            Timber.d("Cleared mechanical_units")

            // 7. Facility Islands (dipende da facilities)
            islandDao.deleteAll()
            Timber.v("Cleared facility_islands")

            // 8. FacilityError (dipende da clients)
            facilityDao.deleteAll()
            Timber.v("Cleared facilities")

            // 9. Clients (tabella radice)
            clientDao.deleteAll()
            Timber.v("Cleared clients")

            Timber.v("Database cleared")

        } catch (e: Exception) {
            Timber.e(e, "Database clearing failed")
            throw DatabaseImportException("Pulizia database fallita: ${e.message}", e)
        }
    }

    /**
     * Import tables in FK order
     */
    private suspend fun importAllTablesInOrder(backup: DatabaseBackup) {

        Timber.v("Database import - FK order")

        try {
            // 1. Clients (nessuna dipendenza)
            if (backup.clients.isNotEmpty()) {
                clientDao.insertAllFromBackup(backup.clients.map { it.toEntity() })
                Timber.v("Imported ${backup.clients.size} clients")
            }

            // 2. FacilityError (dipende da clients)
            if (backup.facilities.isNotEmpty()) {
                facilityDao.insertAllFromBackup(backup.facilities.map { it.toEntity() })
                Timber.v("Imported ${backup.facilities.size} facilities")
            }

            // 3. ContactsError (dipende da clients)
            if (backup.contacts.isNotEmpty()) {
                contactDao.insertAllFromBackup(backup.contacts.map { it.toEntity() })
                Timber.v("Imported ${backup.contacts.size} contacts")
            }

            // 3.1. ContractsError (depends on Client)
            if (backup.contracts.isNotEmpty()) {
                contractDao.insertAllFromBackup(backup.contracts.map { it.toEntity() })
                Timber.v("Imported ${backup.contracts.size} contracts")
            }

            // 4. Facility Islands (dipende da facilities)
            if (backup.facilityIslands.isNotEmpty()) {
                islandDao.insertAllFromBackup(backup.facilityIslands.map { it.toEntity() })
                Timber.v("Imported ${backup.facilityIslands.size} facility islands")
            }

            // 4. Mechanical Units (dipende da islands)
            if (backup.mechanicalUnits.isNotEmpty()) {
                mechanicalUnitDao.insertAllFromBackup(backup.mechanicalUnits.map { it.toEntity() })
                Timber.v("Imported ${backup.mechanicalUnits.size} mechanical units")
            }

            // 4b. Maintenance Logs (dipende da islands, opzionalmente da mechanical units)
            if (backup.maintenanceLogs.isNotEmpty()) {
                maintenanceLogDao.insertAllFromBackup(backup.maintenanceLogs.map { it.toEntity() })
                Timber.v("Imported ${backup.maintenanceLogs.size} maintenance logs")
            }

            // 5. Checkups (dipende da facility_islands tramite associations)
            if (backup.checkUps.isNotEmpty()) {
                checkUpDao.insertAllFromBackup(backup.checkUps.map { it.toEntity() })
                Timber.v("Imported ${backup.checkUps.size} checkups")
            }

            // 6. Check Items (dipende da checkups)
            if (backup.checkItems.isNotEmpty()) {
                checkItemDao.insertAllFromBackup(backup.checkItems.map { it.toEntity() })
                Timber.v("Imported ${backup.checkItems.size} check items")
            }

            // 7. Photos (dipende da check_items)
            if (backup.photos.isNotEmpty()) {
                photoDao.insertAllFromBackup(backup.photos.map { it.toEntity() })
                Timber.v("Imported ${backup.photos.size} photos")
            }

            // 8. Spare Parts (dipende da checkups)
            if (backup.spareParts.isNotEmpty()) {
                sparePartDao.insertAllFromBackup(backup.spareParts.map { it.toEntity() })
                Timber.v("Imported ${backup.spareParts.size} spare parts")
            }

            // 9. Associations (dipende da checkups + facility_islands)
            if (backup.checkUpAssociations.isNotEmpty()) {
                checkUpAssociationDao.insertAllFromBackup(backup.checkUpAssociations.map { it.toEntity() })
                Timber.v("Imported ${backup.checkUpAssociations.size} checkup associations")
            }

            if (backup.tiIslandAssociations.isNotEmpty()) {
                tiAssociationDao.insertAllFromBackup(backup.tiIslandAssociations.map { it.toEntity() })
                Timber.v("Imported ${backup.tiIslandAssociations.size} ti island associations")
            }

            if (backup.checkUpMaintenanceLogAssociations.isNotEmpty()) {
                checkUpMaintenanceLogAssociationDao.insertAllFromBackup(backup.checkUpMaintenanceLogAssociations.map { it.toEntity() })
                Timber.v("Imported ${backup.checkUpMaintenanceLogAssociations.size} checkup maintenance log associations")
            }

            if (backup.tiMaintenanceLogAssociations.isNotEmpty()) {
                tiMaintenanceLogAssociationDao.insertAllFromBackup(backup.tiMaintenanceLogAssociations.map { it.toEntity() })
                Timber.v("Imported ${backup.tiMaintenanceLogAssociations.size} ti maintenance log associations")
            }

            // 10. Technical Interventions (no FK dependencies)
            if (backup.technicalInterventions.isNotEmpty()) {
                technicalInterventionDao.insertAllFromBackup(
                    backup.technicalInterventions.map { it.toEntity() }
                )
                Timber.v("Imported ${backup.technicalInterventions.size} technical interventions")
            }

            // 11. Documents (no FK dependencies)
            if (backup.documents.isNotEmpty()) {
                documentDao.insertAllFromBackup(backup.documents.map { it.toEntity() })
                Timber.v("Imported ${backup.documents.size} documents")
            }

            Timber.v("Database import completed")

        } catch (e: Exception) {
            Timber.e(e, "Database import failed")
            throw DatabaseImportException("Import tabelle fallito: ${e.message}", e)
        }
    }

    /**
     * Post-import validation
     */
    private suspend fun validateImportedData(originalBackup: DatabaseBackup): BackupValidationResult {
        return try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            Timber.v("Post-import validation")

            // Verifica conteggi record
            val importedCounts = mapOf(
                "clients" to clientDao.count(),
                "facilities" to facilityDao.count(),
                "contacts" to contactDao.count(),
                "contracts" to contractDao.count(),
                "facilityIslands" to islandDao.count(),
                "mechanicalUnits" to mechanicalUnitDao.count(),
                "checkUps" to checkUpDao.count(),
                "checkItems" to checkItemDao.count(),
                "photos" to photoDao.count(),
                "spareParts" to sparePartDao.count(),
                "associations" to checkUpAssociationDao.count(),
                "tiIslandAssociations" to tiAssociationDao.count(),
                "checkUpMaintenanceLogAssociations" to checkUpMaintenanceLogAssociationDao.count(),
                "tiMaintenanceLogAssociations" to tiMaintenanceLogAssociationDao.count(),
                "technicalInterventions" to technicalInterventionDao.count(),
                "maintenanceLogs" to maintenanceLogDao.count(),
                "documents" to documentDao.count()
            )

            val expectedCounts = mapOf(
                "clients" to originalBackup.clients.size,
                "facilities" to originalBackup.facilities.size,
                "contacts" to originalBackup.contacts.size,
                "contracts" to originalBackup.contracts.size,
                "facilityIslands" to originalBackup.facilityIslands.size,
                "mechanicalUnits" to originalBackup.mechanicalUnits.size,
                "checkUps" to originalBackup.checkUps.size,
                "checkItems" to originalBackup.checkItems.size,
                "photos" to originalBackup.photos.size,
                "spareParts" to originalBackup.spareParts.size,
                "associations" to originalBackup.checkUpAssociations.size,
                "tiIslandAssociations" to originalBackup.tiIslandAssociations.size,
                "checkUpMaintenanceLogAssociations" to originalBackup.checkUpMaintenanceLogAssociations.size,
                "tiMaintenanceLogAssociations" to originalBackup.tiMaintenanceLogAssociations.size,
                "technicalInterventions" to originalBackup.technicalInterventions.size,
                "maintenanceLogs" to originalBackup.maintenanceLogs.size,
                "documents" to originalBackup.documents.size
            )

            for ((table, expectedCount) in expectedCounts) {
                val actualCount = importedCounts[table] ?: 0
                if (actualCount != expectedCount) {
                    errors.add("$table: expected $expectedCount records, found $actualCount")
                } else {
                    Timber.v("✓ $table: $actualCount records correctly imported")
                }
            }

            // Check FK integrity (sample)
            if (originalBackup.checkItems.isNotEmpty()) {
                val orphanedItems = database.query(
                    "SELECT COUNT(*) FROM check_items ci WHERE NOT EXISTS (SELECT 1 FROM checkups c WHERE c.id = ci.checkup_id)",
                    null
                ).use { cursor ->
                    cursor.moveToFirst()
                    cursor.getInt(0)
                }

                if (orphanedItems > 0) {
                    errors.add("Found $orphanedItems orphaned check items")
                }
            }

            val totalExpected = expectedCounts.values.sum()
            val totalImported = importedCounts.values.sum()

            Timber.v("Validation completed\n- expected: $totalExpected\nimported: $totalImported")

            BackupValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Post-import validation failed")
            BackupValidationResult.invalid(listOf("Validation failed: ${e.message}"))
        }
    }

    /**
     * Log final import stats
     */
    private suspend fun logImportStatistics() {
        try {
            val stats = mapOf(
                "Clients" to clientDao.count(),
                "FacilityError" to facilityDao.count(),
                "ContactsError" to contactDao.count(),
                "ContractsError" to contractDao.count(),
                "Facility Islands" to islandDao.count(),
                "Mechanical Units" to mechanicalUnitDao.count(),
                "Checkups" to checkUpDao.count(),
                "Check Items" to checkItemDao.count(),
                "Photos" to photoDao.count(),
                "Spare Parts" to sparePartDao.count(),
                "Associations" to checkUpAssociationDao.count(),
                "TI Island Associations" to tiAssociationDao.count(),
                "CheckUp MLog Associations" to checkUpMaintenanceLogAssociationDao.count(),
                "TI MLog Associations" to tiMaintenanceLogAssociationDao.count(),
                "Technical Interventions" to technicalInterventionDao.count(),
                "Maintenance Logs" to maintenanceLogDao.count(),
                "Documents" to documentDao.count()
            )

            val total = stats.values.sum()
            Timber.d("DATABASE IMPORT COMPLETED")
            Timber.d("- imported records: $total")

            for ((name, count) in stats) {
                if (count > 0) {
                    Timber.d("   • $name: $count")
                }
            }

        } catch (e: Exception) {
            Timber.w(e, "Final import stats logging failed")
        }
    }
}

/**
 * Custom exception for import errors
 */
class DatabaseImportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

// ===== EXTENSION FUNCTIONS PER MAPPING BACKUP → ENTITY =====

/**
 * Mapping da CheckUpBackup a CheckUpEntity
 */
fun CheckUpBackup.toEntity(): CheckUpEntity {
    return CheckUpEntity(
        id = id,
        clientCompanyName = clientCompanyName,
        clientContactPerson = clientContactPerson,
        clientSite = clientSite,
        clientAddress = clientAddress,
        clientPhone = clientPhone,
        clientEmail = clientEmail,
        islandSerialNumber = islandSerialNumber,
        islandModel = islandModel,
        islandInstallationDate = islandInstallationDate,
        islandLastMaintenanceDate = islandLastMaintenanceDate,
        islandOperatingHours = islandOperatingHours,
        islandCycleCount = islandCycleCount,
        technicianName = technicianName,
        technicianCompany = technicianCompany,
        technicianCertification = technicianCertification,
        technicianPhone = technicianPhone,
        technicianEmail = technicianEmail,
        checkUpDate = checkUpDate,
        headerNotes = headerNotes,
        islandType = islandType,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt
    )
}

/**
 * Mapping da CheckItemBackup a CheckItemEntity
 */
fun CheckItemBackup.toEntity(): CheckItemEntity {
    return CheckItemEntity(
        id = id,
        checkUpId = checkUpId,
        moduleType = moduleType,
        itemCode = itemCode,
        description = description,
        status = status,
        criticality = criticality,
        notes = notes,
        checkedAt = checkedAt,
        orderIndex = orderIndex
    )
}

/**
 * Mapping da PhotoBackup a PhotoEntity
 */
fun PhotoBackup.toEntity(): PhotoEntity {
    return PhotoEntity(
        id = id,
        checkItemId = checkItemId,
        fileName = fileName,
        filePath = filePath,
        thumbnailPath = thumbnailPath,
        caption = caption,
        takenAt = takenAt,
        fileSize = fileSize,
        orderIndex = orderIndex,
        width = width,
        height = height,
        gpsLocation = gpsLocation,
        resolution = resolution,
        perspective = perspective,
        exifData = exifData,
        cameraSettings = cameraSettings
    )
}

/**
 * Mapping da SparePartBackup a SparePartEntity
 */
fun SparePartBackup.toEntity(): SparePartEntity {
    return SparePartEntity(
        id = id,
        checkUpId = checkUpId,
        partNumber = partNumber,
        description = description,
        quantity = quantity,
        urgency = urgency,
        category = category,
        estimatedCost = estimatedCost,
        notes = notes,
        supplierInfo = supplierInfo,
        addedAt = addedAt
    )
}

/**
 * Mapping da ClientBackup a ClientEntity
 */
fun ClientBackup.toEntity(): ClientEntity {
    return ClientEntity(
        id = id,
        companyName = companyName,
        notes = notes,
        headquartersJson = headquartersJson,
        isActive = isActive,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}

/**
 * Mapping da ContactBackup a ContactEntity
 */
fun ContactBackup.toEntity(): ContactEntity {
    return ContactEntity(
        id = id,
        clientId = clientId,
        firstName = firstName,
        lastName = lastName,
        title = title,
        role = role,
        department = department,
        phone = phone,
        mobilePhone = mobilePhone,
        email = email,
        alternativeEmail = alternativeEmail,
        isPrimary = isPrimary,
        isActive = isActive,
        preferredContactMethod = preferredContactMethod,
        notes = notes,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}

fun ContractBackup.toEntity(): ContractEntity {
    return ContractEntity(
        id = id,
        clientId = clientId,
        name = name,
        description = description,
        startDate = startDate.toEpochMilliseconds(),
        endDate = endDate.toEpochMilliseconds(),
        hasPriority = hasPriority,
        hasRemoteAssistance = hasRemoteAssistance,
        hasMaintenance = hasMaintenance,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}

/**
 * Mapping da FacilityBackup a FacilityEntity
 */
fun FacilityBackup.toEntity(): FacilityEntity {
    return FacilityEntity(
        id = id,
        clientId = clientId,
        name = name,
        code = code,
        notes = description,
        facilityType = facilityType,
        addressJson = addressJson,
        isPrimary = isPrimary,
        isActive = isActive,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}

/**
 * Mapping da FacilityIslandBackup a IslandEntity
 */
fun FacilityIslandBackup.toEntity(): IslandEntity {
    return IslandEntity(
        id = id,
        facilityId = facilityId,
        islandType = islandType,
        serialNumber = serialNumber,
        model = model,
        installationDate = installationDate?.toEpochMilliseconds(),
        warrantyExpiration = warrantyExpiration?.toEpochMilliseconds(),
        isActive = isActive,
        operatingHours = operatingHours,
        cycleCount = cycleCount,
        lastMaintenanceDate = lastMaintenanceDate?.toEpochMilliseconds(),
        nextScheduledMaintenance = nextScheduledMaintenance?.toEpochMilliseconds(),
        customName = customName,
        location = location,
        notes = notes,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}


/**
 * Mapping da MechanicalUnitBackup a MechanicalUnitEntity
 */
fun MechanicalUnitBackup.toEntity(): MechanicalUnitEntity {
    return MechanicalUnitEntity(
    id = id,
    islandId = islandId,
    unitType = unitType,
    name = name,
    serialNumber = serialNumber,
    model = model,
    notes = notes,
    isActive = isActive,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds()
    )
}


/**
 * Mapping da CheckUpAssociationBackup a CheckUpIslandAssociationEntity
 */
fun CheckUpAssociationBackup.toEntity(): CheckUpIslandAssociationEntity {
    return CheckUpIslandAssociationEntity(
        id = id,
        checkupId = checkupId,
        islandId = islandId,
        associationType = associationType,
        notes = notes,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}

fun TiAssociationBackup.toEntity(): TiIslandAssociationEntity {
    return TiIslandAssociationEntity(
        id = id,
        interventionId = interventionId,
        islandId = islandId,
        associationType = associationType,
        notes = notes,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        syncedAt = syncedAt,
        isDeleted = isDeleted
    )
}

fun CheckUpMaintenanceLogAssociationBackup.toEntity(): CheckUpMaintenanceLogAssociationEntity {
    return CheckUpMaintenanceLogAssociationEntity(
        id = id,
        checkupId = checkupId,
        maintenanceLogId = maintenanceLogId,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        syncedAt = syncedAt,
        isDeleted = isDeleted
    )
}

fun TiMaintenanceLogAssociationBackup.toEntity(): TiMaintenanceLogAssociationEntity {
    return TiMaintenanceLogAssociationEntity(
        id = id,
        interventionId = interventionId,
        maintenanceLogId = maintenanceLogId,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        syncedAt = syncedAt,
        isDeleted = isDeleted
    )
}

/**
 * Extension function to convert TechnicalInterventionBackup to TechnicalInterventionEntity
 *
 * Reverse of TechnicalInterventionEntity.toBackup()
 * Used during restore/import operations
 */
fun TechnicalInterventionBackup.toEntity(): TechnicalInterventionEntity {
    return TechnicalInterventionEntity(
        id = id,
        intervention_number = interventionNumber,
        created_at = createdAt,
        updated_at = updatedAt,
        status = InterventionStatus.valueOf(status),
        customer_data = customerDataJson,
        robot_data = robotDataJson,
        work_location = workLocationJson,
        technicians = techniciansJson,
        work_days = workDaysJson,
        intervention_description = interventionDescription,
        materials_used = materialsUsedJson,
        external_report = externalReportJson,
        is_complete = isComplete,
        technician_signature = technicianSignatureJson,
        customer_signature = customerSignatureJson,
        customer_name = customerName
    )
}

/**
 * Mapping da MaintenanceLogBackup a MaintenanceLogEntity
 */
fun MaintenanceLogBackup.toEntity(): MaintenanceLogEntity {
    return MaintenanceLogEntity(
        id = id,
        islandId = islandId,
        operationType = operationType,
        customOperationLabel = customOperationLabel,
        mechanicalUnitId = mechanicalUnitId,
        componentLabel = componentLabel,
        description = description,
        technicianName = technicianName,
        technicianCompany = technicianCompany,
        operatingHoursAtEvent = operatingHoursAtEvent,
        cycleCountAtEvent = cycleCountAtEvent,
        outcome = outcome,
        durationMinutes = durationMinutes,
        notes = notes,
        performedAt = performedAt.toEpochMilliseconds(),
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        syncedAt = syncedAt,
        isActive = isActive,
        isDeleted = isDeleted
    )
}

/**
 * Mapping da DocumentBackup a DocumentEntity
 */
fun DocumentBackup.toEntity(): DocumentEntity {
    return DocumentEntity(
        id = id,
        scope = scope,
        islandId = islandId,
        facilityId = facilityId,
        clientId = clientId,
        fileName = fileName,
        filePath = filePath,
        fileSize = fileSize,
        mimeType = mimeType,
        fileHash = fileHash,
        title = title,
        category = category,
        notes = notes,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
        isActive = isActive,
        isDeleted = isDeleted,
        syncedAt = syncedAt
    )
}