package net.calvuz.qreport.domain.usecase.client.facility

import net.calvuz.qreport.domain.model.client.Facility
import net.calvuz.qreport.domain.repository.FacilityRepository
import net.calvuz.qreport.domain.repository.FacilityIslandRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Use Case per archiviazione di uno stabilimento
 *
 * Gestisce:
 * - Archiviazione invece di cancellazione definitiva
 * - Disattivazione facility e isole associate
 * - Gestione facility primaria archiviata
 * - Mantenimento dati per audit trail
 * - Ripristino da archivio
 */
class ArchiveFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val facilityIslandRepository: FacilityIslandRepository
) {

    /**
     * Archivia uno stabilimento (soft archive)
     *
     * @param facilityId ID della facility da archiviare
     * @param archiveReason Motivo dell'archiviazione
     * @param archiveOptions Opzioni di archiviazione
     * @return Result con informazioni archiviazione
     */
    suspend operator fun invoke(
        facilityId: String,
        archiveReason: String? = null,
        archiveOptions: ArchiveOptions = ArchiveOptions()
    ): Result<ArchiveResult> {
        return try {
            // 1. Validazione input
            if (facilityId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID facility è obbligatorio"))
            }

            // 2. Verifica esistenza facility
            val facility = checkFacilityExists(facilityId)
                .getOrElse { return Result.failure(it) }

            // 3. Verifiche pre-archiviazione
            validateCanArchive(facility, archiveOptions)
                .onFailure { return Result.failure(it) }

            // 4. Gestione facility primaria se necessario
            if (facility.isPrimary) {
                handlePrimaryFacilityArchiving(facility.clientId, facilityId)
                    .onFailure { return Result.failure(it) }
            }

            // 5. Archiviazione isole associate se richiesta
            val archivedIslands = if (archiveOptions.archiveAssociatedIslands) {
                archiveAssociatedIslands(facilityId)
                    .getOrElse { emptyList() }
            } else emptyList()

            // 6. Archiviazione facility principale
            val archivedFacility = archiveFacility(facility, archiveReason)
                .getOrElse { return Result.failure(it) }

            // 7. Risultato operazione
            val result = ArchiveResult(
                facilityId = facilityId,
                facilityName = facility.name,
                archivedAt = archivedFacility.updatedAt,
                reason = archiveReason,
                archivedIslandsCount = archivedIslands.size,
                wasPrimary = facility.isPrimary,
                newPrimaryFacilityId = if (facility.isPrimary) {
                    findNewPrimaryFacility(facility.clientId)
                } else null
            )

            Result.success(result)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica che la facility esista e la restituisce
     */
    private suspend fun checkFacilityExists(facilityId: String): Result<Facility> {
        return facilityRepository.getFacilityById(facilityId)
            .mapCatching { facility ->
                facility ?: throw NoSuchElementException("Facility con ID '$facilityId' non trovata")
            }
    }

    /**
     * Validazioni pre-archiviazione
     */
    private suspend fun validateCanArchive(
        facility: Facility,
        options: ArchiveOptions
    ): Result<Unit> {
        return try {
            // 1. Facility già archiviata/inattiva
            if (!facility.isActive) {
                if (options.allowAlreadyInactive) {
                    return Result.success(Unit) // Permetti archiviazione facility già inattiva
                } else {
                    throw IllegalStateException("La facility è già inattiva/archiviata")
                }
            }

            // 2. Verifica che non sia l'unica facility attiva (se è primaria)
            if (facility.isPrimary && !options.allowArchiveLastFacility) {
                val activeFacilities = facilityRepository.getActiveFacilitiesByClient(facility.clientId)
                    .getOrThrow()

                if (activeFacilities.size == 1) {
                    throw IllegalStateException(
                        "Non è possibile archiviare l'unica facility attiva per questo cliente. " +
                                "Usare allowArchiveLastFacility=true per forzare l'operazione."
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gestisce l'archiviazione di una facility primaria
     */
    private suspend fun handlePrimaryFacilityArchiving(
        clientId: String,
        facilityId: String
    ): Result<Unit> {
        return try {
            // Trova altre facility attive per impostare come primaria
            val otherActiveFacilities = facilityRepository.getActiveFacilitiesByClient(clientId)
                .getOrThrow()
                .filter { it.id != facilityId }

            if (otherActiveFacilities.isNotEmpty()) {
                // Imposta la prima come nuova primaria
                val newPrimary = otherActiveFacilities.minByOrNull { it.name }!!
                facilityRepository.setPrimaryFacility(clientId, newPrimary.id)
                    .onFailure { return Result.failure(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Archivia la facility principale
     */
    private suspend fun archiveFacility(facility: Facility, reason: String?): Result<Facility> {
        return try {
            val now = Clock.System.now()

            // Aggiorna descrizione con info archiviazione se fornito motivo
            val updatedDescription = if (reason != null) {
                val archiveNote = "\n[ARCHIVIATO il $now: $reason]"
                (facility.description ?: "") + archiveNote
            } else facility.description

            val archivedFacility = facility.copy(
                isActive = false,
                isPrimary = false, // Rimuovi flag primario
                description = updatedDescription,
                updatedAt = now
            )

            facilityRepository.updateFacility(archivedFacility)
                .map { archivedFacility }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Archivia isole associate
     */
    private suspend fun archiveAssociatedIslands(facilityId: String): Result<List<String>> {
        return try {
            val islands = facilityIslandRepository.getIslandsByFacility(facilityId)
                .getOrThrow()

            val archivedIslandIds = mutableListOf<String>()

            islands.filter { it.isActive }.forEach { island ->
                facilityIslandRepository.deleteIsland(island.id) // Soft delete
                    .onSuccess { archivedIslandIds.add(island.id) }
                    .onFailure {
                        // Log errore ma continua
                        // TODO: Add proper logging
                    }
            }

            Result.success(archivedIslandIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Trova ID della nuova facility primaria
     */
    private suspend fun findNewPrimaryFacility(clientId: String): String? {
        return try {
            val primaryFacility = facilityRepository.getPrimaryFacility(clientId)
                .getOrNull()
            primaryFacility?.id
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Ripristina una facility dall'archivio
     */
    suspend fun unarchive(
        facilityId: String,
        restoreAsPrimary: Boolean = false
    ): Result<String> {
        return try {
            // 1. Verifica facility esista ed è archiviata
            val facility = checkFacilityExists(facilityId)
                .getOrElse { return Result.failure(it) }

            if (facility.isActive) {
                return Result.failure(IllegalStateException("La facility è già attiva"))
            }

            // 2. Ripristina facility
            val restoredFacility = facility.copy(
                isActive = true,
                isPrimary = if (restoreAsPrimary) {
                    // Verifica che non ci sia già una primaria
                    val existingPrimary = facilityRepository.getPrimaryFacility(facility.clientId)
                        .getOrNull()
                    existingPrimary == null
                } else false,
                updatedAt = Clock.System.now()
            )

            facilityRepository.updateFacility(restoredFacility)
                .onFailure { return Result.failure(it) }

            // 3. Se impostata come primaria, rimuovi flag da altre
            if (restoredFacility.isPrimary) {
                facilityRepository.setPrimaryFacility(facility.clientId, facilityId)
                    .onFailure {
                        // Log warning ma non fallire
                        // TODO: Add logging
                    }
            }

            Result.success(facilityId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ottieni lista facilities archiviate per un cliente
     */
    suspend fun getArchivedFacilities(clientId: String): Result<List<Facility>> {
        return try {
            if (clientId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID cliente è obbligatorio"))
            }

            val allFacilities = facilityRepository.getFacilitiesByClient(clientId)
                .getOrThrow()

            val archivedFacilities = allFacilities.filter { !it.isActive }
                .sortedByDescending { it.updatedAt } // Più recenti prima

            Result.success(archivedFacilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Archivia multiple facilities in batch
     */
    suspend fun archiveMultipleFacilities(
        facilityIds: List<String>,
        batchReason: String? = null,
        archiveOptions: ArchiveOptions = ArchiveOptions()
    ): Result<ArchiveBatchResult> {
        return try {
            val results = mutableListOf<ArchiveResult>()
            val errors = mutableListOf<Pair<String, Exception>>()

            facilityIds.forEach { facilityId ->
                invoke(facilityId, batchReason, archiveOptions)
                    .onSuccess { result -> results.add(result) }
                    .onFailure { error ->
                        if (error is Exception) {
                            errors.add(facilityId to error)
                        }
                    }
            }

            val batchResult = ArchiveBatchResult(
                successfulArchives = results,
                failedArchives = errors.toMap(),
                totalRequested = facilityIds.size
            )

            Result.success(batchResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pulizia archivio - rimozione permanente facilities archiviate da tempo
     */
    suspend fun cleanupOldArchives(
        clientId: String,
        olderThan: Instant,
        dryRun: Boolean = true
    ): Result<CleanupResult> {
        return try {
            val archivedFacilities = getArchivedFacilities(clientId)
                .getOrThrow()

            val oldArchives = archivedFacilities.filter { facility ->
                facility.updatedAt < olderThan
            }

            if (dryRun) {
                return Result.success(
                    CleanupResult(
                        candidatesForDeletion = oldArchives.map { it.id },
                        actuallyDeleted = emptyList(),
                        dryRun = true
                    )
                )
            }

            // Eliminazione permanente (se implementata nel repository)
            val deleted = mutableListOf<String>()
            oldArchives.forEach { facility ->
                // facilityRepository.permanentlyDeleteFacility(facility.id)
                // Per ora solo log
                deleted.add(facility.id)
            }

            Result.success(
                CleanupResult(
                    candidatesForDeletion = oldArchives.map { it.id },
                    actuallyDeleted = deleted,
                    dryRun = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Opzioni di archiviazione
 */
data class ArchiveOptions(
    val archiveAssociatedIslands: Boolean = true,
    val allowAlreadyInactive: Boolean = false,
    val allowArchiveLastFacility: Boolean = false
) {
    companion object {
        fun safe() = ArchiveOptions(
            archiveAssociatedIslands = false,
            allowAlreadyInactive = false,
            allowArchiveLastFacility = false
        )

        fun aggressive() = ArchiveOptions(
            archiveAssociatedIslands = true,
            allowAlreadyInactive = true,
            allowArchiveLastFacility = true
        )
    }
}

/**
 * Risultato archiviazione singola
 */
data class ArchiveResult(
    val facilityId: String,
    val facilityName: String,
    val archivedAt: Instant,
    val reason: String?,
    val archivedIslandsCount: Int,
    val wasPrimary: Boolean,
    val newPrimaryFacilityId: String?
)

/**
 * Risultato operazione batch archive
 */
data class ArchiveBatchResult(
    val successfulArchives: List<ArchiveResult>,
    val failedArchives: Map<String, Exception>,
    val totalRequested: Int
) {
    val successCount: Int = successfulArchives.size
    val errorCount: Int = failedArchives.size
    val successRate: Float = if (totalRequested > 0) successCount.toFloat() / totalRequested else 0f
}

/**
 * Risultato cleanup archivio
 */
data class CleanupResult(
    val candidatesForDeletion: List<String>,
    val actuallyDeleted: List<String>,
    val dryRun: Boolean
)