package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Soft-deletes a robotic island by setting [isActive] = false.
 *
 * The repository implementation also sets isDeleted=true as a sync marker —
 * this detail is transparent to the domain layer.
 *
 * Cascade to child MechanicalUnits is handled inside the repository @Transaction.
 *
 * Business rules checked unless [force] = true:
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

        Timber.d("Delete island: $islandId")

        if (islandId.isBlank()) {
            return QrResult.Error(QrError.IslandError.NotFound())
        }

        val island = when (val r = checkIslandExists(islandId)) {
            is QrResult.Error -> return QrResult.Error(r.error)
            is QrResult.Success -> r.data
        }

        // Block deletion if maintenance is overdue (unless forced)
        if (!force && island.needsMaintenance()) {
            Timber.d("Deletion blocked: maintenance overdue on island $islandId")
            return QrResult.Error(QrError.IslandError.CannotDeleteMaintenanceOverdue())
        }

        return islandRepository.deactivateIsland(islandId).fold(
            onSuccess = {
                Timber.d("Island deactivating: $islandId")
                QrResult.Success(Unit)
            },
            onFailure = {
                Timber.e("Failed to deactivating island: ${it.message}")
                QrResult.Error(QrError.IslandError.DeleteError(it.message))
            }
        )
    }

    // -------------------------------------------------------------------------


    /**
     * Deletes all islands belonging to [facilityId].
     * Used by the backup/restore system — not part of the normal delete flow.
     */
    suspend fun deleteAllForFacility(facilityId: String): Result<Int> {
        if (facilityId.isBlank())
            return Result.failure(IllegalArgumentException("Facility ID is required"))

        val islands = islandRepository.getIslandsByFacility(facilityId)
            .getOrElse { return Result.failure(it) }

        var deleted = 0
        islands.forEach { island ->
            islandRepository.deleteIsland(island)
                .onSuccess { deleted++ }
                .onFailure { return Result.failure(it) }
        }
        return Result.success(deleted)
    }
}