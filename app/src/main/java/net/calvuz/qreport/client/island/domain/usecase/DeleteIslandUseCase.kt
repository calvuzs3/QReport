package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Soft-deletes a robotic island.
 *
 * Business rules checked unless [ force] = true:
 * - Island must not have overdue maintenance
 *
 * The warning about being the last island in the facility is informational
 * only and does not block deletion.
 */
class DeleteIslandUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val checkIslandExists: CheckIslandExistsUseCase
) {
    suspend operator fun invoke(
        islandId: String,
        force: Boolean = false
    ): QrResult<Unit, QrError.IslandError> {

        Timber.d("Delete island")

        // Check input
        if (islandId.isBlank()) {
            Timber.d("Island id is blank")
            return QrResult.Error(QrError.IslandError.NotFound())
        }

        // Check island exists
        val island = when (val r = checkIslandExists(islandId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // Block deletion if maintenance is overdue (unless forced)
        if (!force && island.needsMaintenance()) {
            Timber.d("Error in deleting island due to maintenance date")
            return QrResult.Error(QrError.IslandError.CannotDeleteMaintenanceOverdue())
        }

        when {

            // ── Stage 1: island is still active ──────────────────────────────
            island.isActive -> {

                islandRepository.deactivateIsland(islandId).fold(
                    onSuccess = {
                        Timber.d("Successfully deactivated island: $Unit")
                        return QrResult.Success(Unit) },
                    onFailure = {
                        Timber.d("Error in deactivating island: ${it.message}")
                        return QrResult.Error(QrError.IslandError.DeleteError(it.message)) }
                )
            }

            // ── Already deleted ───────────────────────────────────────────────
            else -> {
                Timber.d("Error in deleting island: not found")
                return QrResult.Error(QrError.IslandError.NotFound())
            }
        }
    }

    /** Returns the number of islands that would remain in the facility after deletion. */
    suspend fun countRemainingAfterDeletion(islandId: String): Result<Int> {
        val island = when (val r = checkIslandExists(islandId)) {
            is QrResult.Error -> return Result.success(0)
            is QrResult.Success -> r.data
        }
        return islandRepository.getIslandsCountByFacility(island.facilityId)
            .map { it - 1 }
    }

    /** Deletes all islands for a facility (used internally by DeleteFacilityUseCase cascade). */
    suspend fun deleteAllForFacility(facilityId: String): Result<Int> {
        if (facilityId.isBlank()) return Result.failure(IllegalArgumentException("Facility ID is required"))
        val islands = islandRepository.getIslandsByFacility(facilityId).getOrElse { return Result.failure(it) }
        var deleted = 0
        islands.forEach { island ->
            islandRepository.deleteIsland(island)
                .onSuccess { deleted++ }
                .onFailure { return Result.failure(it) }
        }
        return Result.success(deleted)
    }
}