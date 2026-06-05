package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Soft-deletes a facility, optionally cascading to its islands.
 */
class DeleteFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val islandRepository: IslandRepository,
    private val checkFacilityExists: CheckFacilityExistsUseCase
) {

    /**
     * Soft-deletes a facility, optionally cascading to its islands.
     *
     * @param forceDelete if true, deletes even when active islands exist
     */
    suspend operator fun invoke(
        facilityId: String,
        forceDelete: Boolean = false
    ): QrResult<Unit, QrError.FacilityError> {

        Timber.d("Deleting facility $facilityId")

        // Check input
        if (facilityId.isBlank()) {
            Timber.d("Facility ID is blank")
            return QrResult.Error(QrError.FacilityError.NotFound())
        }

        // 1. Verify exists
        val facility = when (val result = checkFacilityExists(facilityId)) {
            is QrResult.Error -> return QrResult.Error(result.error)
            is QrResult.Success -> result.data
        }

        // 2. Business rule: cannot delete the only active facility for the client
        facilityRepository.getActiveFacilitiesByClient(facility.clientId).fold(
            onSuccess = { active ->
                if (active.size == 1 && active.first().id == facilityId) {
                    return QrResult.Error(
                        QrError.FacilityError.CannotDeleteLastFacility()
                    )
                }
            },
            onFailure = {
                return QrResult.Error(QrError.FacilityError.LoadError(it.message))
            }
        )

        // 3. Check islands via repository (Facility no longer holds island IDs)
        val islands = islandRepository.getIslandsByFacility(facilityId).getOrElse { emptyList() }
        val activeIslands = islands.filter { it.isActive }

        if (activeIslands.isNotEmpty() && !forceDelete) {
            Timber.d("Cannot delete facility with active islands: ${activeIslands.size}")
            return QrResult.Error(
                QrError.FacilityError.CannotDeleteHasActiveIslands(activeIslands.size.toString())
            )
        }

        // 4. Handle primary: assign to another active facility
        if (facility.isPrimary) {
            facilityRepository.getActiveFacilitiesByClient(facility.clientId).fold(
                onSuccess = { active ->
                    val next = active.firstOrNull { it.id != facilityId }
                    if (next != null) {
                        facilityRepository.setPrimaryFacility(facility.clientId, next.id)
                    }
                },
                onFailure = { /* log only, non-fatal */ }
            )
        }

        // 5. Cascade deactivate islands if forceDelete
        if (forceDelete && activeIslands.isNotEmpty())return facilityRepository.deactivateFacility(facilityId).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.FacilityError.DeleteError(it.message)) }
        )

        // 6. Soft-delete facility
        return facilityRepository.deactivateFacility(facilityId).fold(
            onSuccess = { QrResult.Success(Unit) },
            onFailure = { QrResult.Error(QrError.FacilityError.DeleteError(it.message)) }
        )
    }
}