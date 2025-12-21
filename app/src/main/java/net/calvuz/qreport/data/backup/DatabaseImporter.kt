package net.calvuz.qreport.data.backup

import androidx.room.withTransaction
import kotlinx.datetime.Clock
import net.calvuz.qreport.data.local.dao.*
import net.calvuz.qreport.data.local.QReportDatabase
import net.calvuz.qreport.data.local.entity.*
import net.calvuz.qreport.domain.model.backup.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FASE 5.2 - DATABASE IMPORTER
 *
 * Gestisce l'import sicuro di DatabaseBackup nel database QReport.
 * Include gestione FK dependencies, validazione e rollback su errori.
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
    private val facilityDao: FacilityDao,
    private val facilityIslandDao: FacilityIslandDao,
    private val checkUpAssociationDao: CheckUpAssociationDao
) {

    /**
     * Importa tutti i dati da DatabaseBackup in una singola transazione
     *
     * @param databaseBackup Backup da importare
     * @param strategy Strategia di import (REPLACE_ALL, MERGE, etc.)
     * @return Result<Unit> per gestione errori
     */
    suspend fun importAllTables(
        databaseBackup: DatabaseBackup,
        strategy: RestoreStrategy = RestoreStrategy.REPLACE_ALL
    ): Result<Unit> = try {

        database.withTransaction {
            Timber.d("Inizio import database - ${databaseBackup.getTotalRecordCount()} record")
            val startTime = System.currentTimeMillis()

            when (strategy) {
                RestoreStrategy.REPLACE_ALL -> {
                    clearAllTablesInOrder()
                    importAllTablesInOrder(databaseBackup)
                }
                RestoreStrategy.MERGE -> {
                    // TODO: Implementazione merge (future)
                    throw UnsupportedOperationException("Merge strategy non ancora implementata")
                }
                RestoreStrategy.SELECTIVE -> {
                    // TODO: Implementazione selettiva (future)
                    throw UnsupportedOperationException("Selective strategy non ancora implementata")
                }
            }

            // Validazione post-import
            val validation = validateImportedData(databaseBackup)
            if (!validation.isValid) {
                throw DatabaseImportException("Validazione post-import fallita: ${validation.errors.joinToString()}")
            }

            val duration = System.currentTimeMillis() - startTime
            Timber.d("Import completato con successo in ${duration}ms")

            // Log finale
            logImportStatistics()
        }

        Result.success(Unit)

    } catch (e: Exception) {
        Timber.e(e, "Errore durante import database")
        Result.failure(DatabaseImportException("Import fallito: ${e.message}", e))
    }

    /**
     * Cancella tutte le tabelle nell'ordine FK inverso
     */
    private suspend fun clearAllTablesInOrder() {
        Timber.d("Pulizia database - eliminazione in ordine FK inverso...")

        // Ordine critico: elimina prima le tabelle con FK, poi quelle referenziate
        try {
            // 1. Associazioni (dipende da checkups + facility_islands)
            checkUpAssociationDao.deleteAll()
            Timber.d("Cleared checkup_island_associations")

            // 2. Foto (dipende da check_items)
            photoDao.deleteAll()
            Timber.d("Cleared photos")

            // 3. Check items (dipende da checkups)
            checkItemDao.deleteAll()
            Timber.d("Cleared check_items")

            // 4. Spare parts (dipende da checkups)
            sparePartDao.deleteAll()
            Timber.d("Cleared spare_parts")

            // 5. CheckUps (dipende da facility_islands via associations)
            checkUpDao.deleteAll()
            Timber.d("Cleared checkups")

            // 6. Contatti (dipende da clients)
            contactDao.deleteAll()
            Timber.d("Cleared contacts")

            // 7. Facility Islands (dipende da facilities)
            facilityIslandDao.deleteAll()
            Timber.d("Cleared facility_islands")

            // 8. Facilities (dipende da clients)
            facilityDao.deleteAll()
            Timber.d("Cleared facilities")

            // 9. Clients (tabella radice)
            clientDao.deleteAll()
            Timber.d("Cleared clients")

            Timber.d("Database completamente pulito")

        } catch (e: Exception) {
            Timber.e(e, "Errore durante pulizia database")
            throw DatabaseImportException("Pulizia database fallita: ${e.message}", e)
        }
    }

    /**
     * Importa tutte le tabelle nell'ordine delle dipendenze FK
     */
    private suspend fun importAllTablesInOrder(backup: DatabaseBackup) {
        Timber.d("Importazione dati nell'ordine FK dependencies...")

        try {
            // 1. Clients (nessuna dipendenza)
            if (backup.clients.isNotEmpty()) {
                clientDao.insertAllFromBackup(backup.clients.map { it.toEntity() })
                Timber.d("Imported ${backup.clients.size} clients")
            }

            // 2. Facilities (dipende da clients)
            if (backup.facilities.isNotEmpty()) {
                facilityDao.insertAllFromBackup(backup.facilities.map { it.toEntity() })
                Timber.d("Imported ${backup.facilities.size} facilities")
            }

            // 3. Contacts (dipende da clients)
            if (backup.contacts.isNotEmpty()) {
                contactDao.insertAllFromBackup(backup.contacts.map { it.toEntity() })
                Timber.d("Imported ${backup.contacts.size} contacts")
            }

            // 4. Facility Islands (dipende da facilities)
            if (backup.facilityIslands.isNotEmpty()) {
                facilityIslandDao.insertAllFromBackup(backup.facilityIslands.map { it.toEntity() })
                Timber.d("Imported ${backup.facilityIslands.size} facility islands")
            }

            // 5. CheckUps (dipende da facility_islands tramite associations)
            if (backup.checkUps.isNotEmpty()) {
                checkUpDao.insertAllFromBackup(backup.checkUps.map { it.toEntity() })
                Timber.d("Imported ${backup.checkUps.size} checkups")
            }

            // 6. Check Items (dipende da checkups)
            if (backup.checkItems.isNotEmpty()) {
                checkItemDao.insertAllFromBackup(backup.checkItems.map { it.toEntity() })
                Timber.d("Imported ${backup.checkItems.size} check items")
            }

            // 7. Photos (dipende da check_items)
            if (backup.photos.isNotEmpty()) {
                photoDao.insertAllFromBackup(backup.photos.map { it.toEntity() })
                Timber.d("Imported ${backup.photos.size} photos")
            }

            // 8. Spare Parts (dipende da checkups)
            if (backup.spareParts.isNotEmpty()) {
                sparePartDao.insertAllFromBackup(backup.spareParts.map { it.toEntity() })
                Timber.d("Imported ${backup.spareParts.size} spare parts")
            }

            // 9. Associations (dipende da checkups + facility_islands)
            if (backup.checkUpAssociations.isNotEmpty()) {
                checkUpAssociationDao.insertAllFromBackup(backup.checkUpAssociations.map { it.toEntity() })
                Timber.d("Imported ${backup.checkUpAssociations.size} checkup associations")
            }

            Timber.d("Import di tutte le tabelle completato con successo")

        } catch (e: Exception) {
            Timber.e(e, "Errore durante import tabelle")
            throw DatabaseImportException("Import tabelle fallito: ${e.message}", e)
        }
    }

    /**
     * Valida i dati importati confrontando con backup originale
     */
    private suspend fun validateImportedData(originalBackup: DatabaseBackup): BackupValidationResult {
        return try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            Timber.d("Validazione post-import...")

            // Verifica conteggi record
            val importedCounts = mapOf(
                "clients" to clientDao.count(),
                "facilities" to facilityDao.count(),
                "contacts" to contactDao.count(),
                "facilityIslands" to facilityIslandDao.count(),
                "checkUps" to checkUpDao.count(),
                "checkItems" to checkItemDao.count(),
                "photos" to photoDao.count(),
                "spareParts" to sparePartDao.count(),
                "associations" to checkUpAssociationDao.count()
            )

            val expectedCounts = mapOf(
                "clients" to originalBackup.clients.size,
                "facilities" to originalBackup.facilities.size,
                "contacts" to originalBackup.contacts.size,
                "facilityIslands" to originalBackup.facilityIslands.size,
                "checkUps" to originalBackup.checkUps.size,
                "checkItems" to originalBackup.checkItems.size,
                "photos" to originalBackup.photos.size,
                "spareParts" to originalBackup.spareParts.size,
                "associations" to originalBackup.checkUpAssociations.size
            )

            for ((table, expectedCount) in expectedCounts) {
                val actualCount = importedCounts[table] ?: 0
                if (actualCount != expectedCount) {
                    errors.add("$table: attesi $expectedCount record, trovati $actualCount")
                } else {
                    Timber.d("✓ $table: $actualCount record importati correttamente")
                }
            }

            // Verifica integrità FK (sample)
            if (originalBackup.checkItems.isNotEmpty()) {
                val orphanedItems = database.query(
                    "SELECT COUNT(*) FROM check_items ci WHERE NOT EXISTS (SELECT 1 FROM checkups c WHERE c.id = ci.checkup_id)",
                    null
                ).use { cursor ->
                    cursor.moveToFirst()
                    cursor.getInt(0)
                }

                if (orphanedItems > 0) {
                    errors.add("Trovati $orphanedItems check items senza checkup parent")
                }
            }

            val totalExpected = expectedCounts.values.sum()
            val totalImported = importedCounts.values.sum()

            Timber.d("Validazione completata - Attesi: $totalExpected, Importati: $totalImported")

            BackupValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore durante validazione post-import")
            BackupValidationResult.invalid(listOf("Errore validazione: ${e.message}"))
        }
    }

    /**
     * Log statistiche finali import
     */
    private suspend fun logImportStatistics() {
        try {
            val stats = mapOf(
                "Clients" to clientDao.count(),
                "Facilities" to facilityDao.count(),
                "Contacts" to contactDao.count(),
                "Facility Islands" to facilityIslandDao.count(),
                "CheckUps" to checkUpDao.count(),
                "Check Items" to checkItemDao.count(),
                "Photos" to photoDao.count(),
                "Spare Parts" to sparePartDao.count(),
                "Associations" to checkUpAssociationDao.count()
            )

            val total = stats.values.sum()
            Timber.d("🎉 DATABASE IMPORT COMPLETATO:")
            Timber.d("📊 Totale record importati: $total")

            for ((name, count) in stats) {
                if (count > 0) {
                    Timber.d("   • $name: $count")
                }
            }

        } catch (e: Exception) {
            Timber.w(e, "Errore nel logging statistiche")
        }
    }
}

/**
 * Eccezione custom per errori di import
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
        checkedAt = checkedAt?.let { Clock.System.now() },
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
        vatNumber = vatNumber,
        fiscalCode = fiscalCode,
        website = website,
        industry = industry,
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

/**
 * Mapping da FacilityBackup a FacilityEntity
 */
fun FacilityBackup.toEntity(): FacilityEntity {
    return FacilityEntity(
        id = id,
        clientId = clientId,
        name = name,
        code = code,
        description = description,
        facilityType = facilityType,
        addressJson = addressJson,
        isPrimary = isPrimary,
        isActive = isActive,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}

/**
 * Mapping da FacilityIslandBackup a FacilityIslandEntity
 */
fun FacilityIslandBackup.toEntity(): FacilityIslandEntity {
    return FacilityIslandEntity(
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

/*
=============================================================================
                            UTILIZZO NELL'APPLICAZIONE
=============================================================================

ESEMPIO - RestoreBackupUseCase:

class RestoreBackupUseCase @Inject constructor(
    private val databaseImporter: DatabaseImporter,
    private val photoArchiver: PhotoArchiver
) {

    suspend operator fun invoke(
        backupPath: String,
        strategy: RestoreStrategy
    ): Flow<RestoreProgress> = flow {

        emit(RestoreProgress.InProgress("Caricamento backup...", 0f))
        val backupData = loadBackupFromPath(backupPath)

        emit(RestoreProgress.InProgress("Ripristino database...", 0.2f))
        val importResult = databaseImporter.importAllTables(
            backupData.database,
            strategy
        )

        importResult.fold(
            onSuccess = {
                emit(RestoreProgress.InProgress("Ripristino foto...", 0.8f))
                // ... photo restore logic
                emit(RestoreProgress.Completed(backupData.metadata.id))
            },
            onFailure = { error ->
                emit(RestoreProgress.Error("Import fallito: ${error.message}", error))
            }
        )
    }
}

=============================================================================
*/