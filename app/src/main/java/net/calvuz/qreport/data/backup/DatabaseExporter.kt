package net.calvuz.qreport.data.backup

import androidx.room.withTransaction
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.data.local.dao.*
import net.calvuz.qreport.data.local.QReportDatabase
import net.calvuz.qreport.domain.model.backup.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transaction export of all db tables
 * - conversion Entity → Backup for JSON serialization
 */

@Singleton
class DatabaseExporter @Inject constructor(
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
     * Export of all tables in a single transaction
     *
     * @return DatabaseBackup with all data converted
     */
    suspend fun exportAllTables(): DatabaseBackup = database.withTransaction {

        Timber.v("Database export begin")
        val startTime = System.currentTimeMillis()

        try {
            // Export with optimized order
            // less FK dependencies first to reduce lock time

            val clients = clientDao.getAllForBackup().map { it.toBackup() }
            Timber.v("Exported ${clients.size} clients")

            val facilities = facilityDao.getAllForBackup().map { it.toBackup() }
            Timber.v("Exported ${facilities.size} facilities")

            val contacts = contactDao.getAllForBackup().map { it.toBackup() }
            Timber.v("Exported ${contacts.size} contacts")

            val facilityIslands = facilityIslandDao.getAllForBackup().map { it.toBackup() }
            Timber.v("Exported ${facilityIslands.size} facility islands")

            val checkUps = checkUpDao.getAllForBackup().map { it.toBackup() }
            Timber.v("Exported ${checkUps.size} checkups")

            val checkItems = checkItemDao.getAllForBackup().map { it.toBackup() }
            Timber.v("Exported ${checkItems.size} check items")

            val photos = photoDao.getAllForBackup().map { it.toBackup() }
            Timber.v("Exported ${photos.size} photos")

            val spareParts = sparePartDao.getAllForBackup().map { it.toBackup() }
            Timber.v("Exported ${spareParts.size} spare parts")

            val associations = checkUpAssociationDao.getAllForBackup().map { it.toBackup() }
            Timber.v("Exported ${associations.size} checkup associations")

            val databaseBackup = DatabaseBackup(
                // Core entities
                checkUps = checkUps,
                checkItems = checkItems,
                photos = photos,
                spareParts = spareParts,

                // Client entities
                clients = clients,
                contacts = contacts,
                facilities = facilities,
                facilityIslands = facilityIslands,

                // Associations
                checkUpAssociations = associations,

                // Metadata
                exportedAt = Clock.System.now()
            )

            val duration = System.currentTimeMillis() - startTime
            val totalRecords = databaseBackup.getTotalRecordCount()

            Timber.v("Export completed in ${duration} ms - $totalRecords records")
            Timber.v(buildString {
                append("Breakdown: CheckUps=${checkUps.size}, CheckItems=${checkItems.size}, ")
                append("Photos=${photos.size}, SpareParts=${spareParts.size}, ")
                append("Clients=${clients.size}, Contacts=${contacts.size}, ")
                append("Facilities=${facilities.size}, Islands=${facilityIslands.size}, ")
                append("Associations=${associations.size}")
            })

            databaseBackup

        } catch (e: Exception) {
            Timber.e(e, "Database export failed")
            throw DatabaseExportException("Export fallito: ${e.message}", e)
        }
    }

    /**
     * Count total records in db (estimation)
     */
    suspend fun getEstimatedRecordCount(): Int = database.withTransaction {
        try {
            val counts = listOf(
                checkUpDao.count(),
                checkItemDao.count(),
                photoDao.count(),
                sparePartDao.count(),
                clientDao.count(),
                contactDao.count(),
                facilityDao.count(),
                facilityIslandDao.count(),
                checkUpAssociationDao.count()
            )

            val total = counts.sum()
            Timber.v("Database count estimate: $total records (${counts.joinToString()})")
            total

        } catch (e: Exception) {
            Timber.w(e, "Database count estimation failed")
            0
        }
    }

    /**
     * Database integrity check before expor
     */
    suspend fun validateDatabaseIntegrity(): BackupValidationResult {
        return try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            // Verifica FK consistency
            val orphanedCheckItems = checkItemDao.count() -
                    database.query("SELECT COUNT(*) FROM check_items ci WHERE EXISTS (SELECT 1 FROM checkups c WHERE c.id = ci.checkup_id)", null).use {
                        it.moveToFirst()
                        it.getInt(0)
                    }

            if (orphanedCheckItems > 0) {
                warnings.add("$orphanedCheckItems possibly orphaned check items")
            }

            // Check for photo existance on filesystem
            val photosWithMissingFiles = photoDao.getAllForBackup().count { photo ->
                !java.io.File(photo.filePath).exists()
            }

            if (photosWithMissingFiles > 0) {
                warnings.add("$photosWithMissingFiles photos have missing files")
            }

            // Check fr db size
            val totalRecords = getEstimatedRecordCount()
            if (totalRecords > 100000) {
                warnings.add("Database is big ($totalRecords record) - the export could be slow")
            }

            BackupValidationResult(
                isValid = true,
                errors = errors,
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Database integrity validation failed")
            BackupValidationResult.invalid(listOf("Errore validazione: ${e.message}"))
        }
    }
}

/**
 * Custom exception for export errors
 */
class DatabaseExportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

// ===== EXTENSION FUNCTIONS PER MAPPING ENTITY → BACKUP =====

/**
 * Mapping da CheckUpEntity a CheckUpBackup ✔️
 */
fun net.calvuz.qreport.data.local.entity.CheckUpEntity.toBackup(): CheckUpBackup {
    return CheckUpBackup(
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
 * Mapping da CheckItemEntity a CheckItemBackup ✔️
 */
fun net.calvuz.qreport.data.local.entity.CheckItemEntity.toBackup(): CheckItemBackup {
    return CheckItemBackup(
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
 * Mapping da PhotoEntity a PhotoBackup ✔️
 */
fun net.calvuz.qreport.data.local.entity.PhotoEntity.toBackup(): PhotoBackup {
    return PhotoBackup(
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
 * Mapping da SparePartEntity a SparePartBackup ✔️
 */
fun net.calvuz.qreport.data.local.entity.SparePartEntity.toBackup(): SparePartBackup {
    return SparePartBackup(
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
 * Mapping da ClientEntity a ClientBackup ✔️
 */
fun net.calvuz.qreport.data.local.entity.ClientEntity.toBackup(): ClientBackup {
    return ClientBackup(
        id = id,
        companyName = companyName,
        vatNumber = vatNumber,
        fiscalCode = fiscalCode,
        website = website,
        industry = industry,
        notes = notes,
        headquartersJson = headquartersJson,
        isActive = isActive,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}

/**
 * Mapping da ContactEntity a ContactBackup ✔️
 */
fun net.calvuz.qreport.data.local.entity.ContactEntity.toBackup(): ContactBackup {
    return ContactBackup(
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
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}

/**
 * Mapping da FacilityEntity a FacilityBackup ✔️
 */
fun net.calvuz.qreport.data.local.entity.FacilityEntity.toBackup(): FacilityBackup {
    return FacilityBackup(
        id = id,
        clientId = clientId,
        name = name,
        code = code,
        description = description,
        facilityType = facilityType,
        addressJson = addressJson,
        isPrimary = isPrimary,
        isActive = isActive,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}

/**
 * Mapping da FacilityIslandEntity a FacilityIslandBackup ✔️
 */
fun net.calvuz.qreport.data.local.entity.FacilityIslandEntity.toBackup(): FacilityIslandBackup {
    return FacilityIslandBackup(
        id = id,
        facilityId = facilityId,
        islandType = islandType,
        serialNumber = serialNumber,
        model = model,
        installationDate = installationDate?.let { Instant.fromEpochMilliseconds(it) },
        warrantyExpiration = warrantyExpiration?.let { Instant.fromEpochMilliseconds(it) },
        isActive = isActive,
        operatingHours = operatingHours,
        cycleCount = cycleCount,
        lastMaintenanceDate = lastMaintenanceDate?.let { Instant.fromEpochMilliseconds(it) },
        nextScheduledMaintenance = nextScheduledMaintenance?.let { Instant.fromEpochMilliseconds(it) },
        customName = customName,
        location = location,
        notes = notes,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}

/**
 * Mapping da CheckUpIslandAssociationEntity a CheckUpAssociationBackup ✔️
 */
fun net.calvuz.qreport.data.local.entity.CheckUpIslandAssociationEntity.toBackup(): CheckUpAssociationBackup {
    return CheckUpAssociationBackup(
        id = id,
        checkupId = checkupId,
        islandId = islandId,
        associationType = associationType,
        notes = notes,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}