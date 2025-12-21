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
 * FASE 5.2 - DATABASE EXPORTER
 *
 * Gestisce l'export transazionale di tutte le tabelle del database QReport.
 * Converte Entity → Backup models per serializzazione JSON.
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
     * Esporta tutte le tabelle del database in una singola transazione
     *
     * @return DatabaseBackup con tutti i dati convertiti
     */
    suspend fun exportAllTables(): DatabaseBackup = database.withTransaction {

        Timber.d("Inizio export database completo")
        val startTime = System.currentTimeMillis()

        try {
            // Export con ordine ottimizzato per performance
            // Tabelle con meno FK dependencies prima per ridurre lock time

            Timber.d("Export tabelle base...")
            val clients = clientDao.getAllForBackup().map { it.toBackup() }
            Timber.d("Exported ${clients.size} clients")

            val facilities = facilityDao.getAllForBackup().map { it.toBackup() }
            Timber.d("Exported ${facilities.size} facilities")

            val contacts = contactDao.getAllForBackup().map { it.toBackup() }
            Timber.d("Exported ${contacts.size} contacts")

            val facilityIslands = facilityIslandDao.getAllForBackup().map { it.toBackup() }
            Timber.d("Exported ${facilityIslands.size} facility islands")

            Timber.d("Export tabelle checkup...")
            val checkUps = checkUpDao.getAllForBackup().map { it.toBackup() }
            Timber.d("Exported ${checkUps.size} checkups")

            val checkItems = checkItemDao.getAllForBackup().map { it.toBackup() }
            Timber.d("Exported ${checkItems.size} check items")

            val photos = photoDao.getAllForBackup().map { it.toBackup() }
            Timber.d("Exported ${photos.size} photos")

            val spareParts = sparePartDao.getAllForBackup().map { it.toBackup() }
            Timber.d("Exported ${spareParts.size} spare parts")

            Timber.d("Export associazioni...")
            val associations = checkUpAssociationDao.getAllForBackup().map { it.toBackup() }
            Timber.d("Exported ${associations.size} checkup associations")

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

            Timber.d("Export completato in ${duration}ms - $totalRecords record totali")
            Timber.d(buildString {
                append("Breakdown: CheckUps=${checkUps.size}, CheckItems=${checkItems.size}, ")
                append("Photos=${photos.size}, SpareParts=${spareParts.size}, ")
                append("Clients=${clients.size}, Contacts=${contacts.size}, ")
                append("Facilities=${facilities.size}, Islands=${facilityIslands.size}, ")
                append("Associations=${associations.size}")
            })

            databaseBackup

        } catch (e: Exception) {
            Timber.e(e, "Errore durante export database")
            throw DatabaseExportException("Export fallito: ${e.message}", e)
        }
    }

    /**
     * Conta i record totali nel database (per stima prima dell'export)
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
            Timber.d("Conteggio stimato database: $total record (${counts.joinToString()})")
            total

        } catch (e: Exception) {
            Timber.w(e, "Errore nel conteggio record")
            0
        }
    }

    /**
     * Verifica l'integrità del database prima dell'export
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
                warnings.add("$orphanedCheckItems check items potrebbero essere orphaned")
            }

            // Verifica esistenza foto su filesystem
            val photosWithMissingFiles = photoDao.getAllForBackup().count { photo ->
                !java.io.File(photo.filePath).exists()
            }

            if (photosWithMissingFiles > 0) {
                warnings.add("$photosWithMissingFiles foto hanno file mancanti")
            }

            // Verifica size database ragionevole
            val totalRecords = getEstimatedRecordCount()
            if (totalRecords > 100000) {
                warnings.add("Database molto grande ($totalRecords record) - export potrebbe essere lento")
            }

            BackupValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )

        } catch (e: Exception) {
            Timber.e(e, "Errore nella validazione database")
            BackupValidationResult.invalid(listOf("Errore validazione: ${e.message}"))
        }
    }
}

/**
 * Eccezione custom per errori di export
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
        completedAt = completedAt?.let { Clock.System.now() }
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
        checkedAt = checkedAt?.let { Clock.System.now() },
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

/*
=============================================================================
                            UTILIZZO NELL'APPLICAZIONE
=============================================================================

ESEMPIO - Repository:

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val databaseExporter: DatabaseExporter,
    private val photoArchiver: PhotoArchiver
) : BackupRepository {

    override suspend fun createFullBackup(): Flow<BackupProgress> = flow {
        emit(BackupProgress.InProgress("Validazione database...", 0f))

        val validation = databaseExporter.validateDatabaseIntegrity()
        if (!validation.isValid) {
            emit(BackupProgress.Error("Database non valido: ${validation.errors.joinToString()}"))
            return@flow
        }

        emit(BackupProgress.InProgress("Export database...", 0.1f))
        val databaseBackup = databaseExporter.exportAllTables()

        emit(BackupProgress.InProgress("Finalizzazione...", 0.9f))
        // ... salva backup e crea metadata

        emit(BackupProgress.Completed(backupId, backupPath, totalSize))
    }
}

=============================================================================
*/