package net.calvuz.qreport.data.backup

import androidx.room.withTransaction
import net.calvuz.qreport.data.local.dao.*
import net.calvuz.qreport.data.local.QReportDatabase
import net.calvuz.qreport.data.local.entity.*
import net.calvuz.qreport.domain.model.backup.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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
    private val facilityDao: FacilityDao,
    private val facilityIslandDao: FacilityIslandDao,
    private val checkUpAssociationDao: CheckUpAssociationDao
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
                    throw UnsupportedOperationException("Merge strategy not implemented yet")
                }
                RestoreStrategy.SELECTIVE -> {
                    // TODO: Implementazione selettiva (future)
                    throw UnsupportedOperationException("Selective strategy not implemented yet")
                }
            }

            // Validazione post-import
            val validation = validateImportedData(databaseBackup)
            if (!validation.isValid) {
                throw DatabaseImportException("Post-import validation failed: ${validation.errors.joinToString()}")
            }

            val duration = System.currentTimeMillis() - startTime
            Timber.v("Import completed in ${duration} ms")

            // Log finale
            logImportStatistics()
        }

        Result.success(Unit)

    } catch (e: Exception) {
        Timber.e(e, "Database import failed")
        Result.failure(DatabaseImportException("Database import failed: ${e.message}", e))
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

            // 7. Facility Islands (dipende da facilities)
            facilityIslandDao.deleteAll()
            Timber.v("Cleared facility_islands")

            // 8. Facilities (dipende da clients)
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

            // 2. Facilities (dipende da clients)
            if (backup.facilities.isNotEmpty()) {
                facilityDao.insertAllFromBackup(backup.facilities.map { it.toEntity() })
                Timber.v("Imported ${backup.facilities.size} facilities")
            }

            // 3. Contacts (dipende da clients)
            if (backup.contacts.isNotEmpty()) {
                contactDao.insertAllFromBackup(backup.contacts.map { it.toEntity() })
                Timber.v("Imported ${backup.contacts.size} contacts")
            }

            // 4. Facility Islands (dipende da facilities)
            if (backup.facilityIslands.isNotEmpty()) {
                facilityIslandDao.insertAllFromBackup(backup.facilityIslands.map { it.toEntity() })
                Timber.v("Imported ${backup.facilityIslands.size} facility islands")
            }

            // 5. CheckUps (dipende da facility_islands tramite associations)
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
            Timber.v("DATABASE IMPORT COMPLETED")
            Timber.v("- imported records: $total")

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