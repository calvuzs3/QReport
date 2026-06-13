@file:Suppress("HardCodedStringLiteral")

package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Deactivates a facility, optionally cascading to its islands.
 */
class DeleteFacilityUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val islandRepository: IslandRepository,
    private val getFacilityById: GetFacilityByIdUseCase
) {

    /**
     * Deactivate a facility, optionally cascading to its islands.
     *
     * @param forceDelete if true, deletes even when active islands exist
     * @param forceDeleteOnlyOneActive if true, deletes even if the only one (and so active)
     */
    suspend operator fun invoke(
        facilityId: String,
        forceDelete: Boolean = true,
        forceDeleteOnlyOneActive: Boolean = true    // Delete even if the only one (and so active)
    ): QrResult<Unit, QrError.FacilityError> {

        Timber.v("Deleting facility $facilityId, force=$forceDelete, forceOnlyOneActive=$forceDeleteOnlyOneActive")

        // Check input
        if (facilityId.isBlank()) {
            Timber.d("Facility ID is blank")
            return QrResult.Error(QrError.FacilityError.NotFound())
        }

        // Get data
        val facility = when (val result = getFacilityById(facilityId)) {
            is QrResult.Error -> return QrResult.Error(result.error)
            is QrResult.Success -> result.data
        }

        // Business rule: cannot delete the only active facility for the client
        if (!forceDeleteOnlyOneActive) {
            facilityRepository.getActiveFacilitiesByClient(facility.clientId)
                .fold(onSuccess = { active ->
                    if (active.size == 1 && active.first().id == facilityId) {
                        Timber.d("Cannot delete the only active facility for the client")
                        return QrResult.Error(
                            QrError.FacilityError.CannotDeleteLastFacility()
                        )
                    }
                }, onFailure = {
                    Timber.d(it, "Failed to get active facilities")
                    return QrResult.Error(QrError.FacilityError.LoadError(it.message))
                })
        }

        // Check islands via repository (Facility no longer holds island IDs)
        val islands = islandRepository.getIslandsByFacility(facilityId).getOrElse { emptyList() }
        val activeIslands = islands.filter { it.isActive }

        if (activeIslands.isNotEmpty() && !forceDelete) {
            Timber.d("Cannot delete facility with active islands: ${activeIslands.size}")
            return QrResult.Error(
                QrError.FacilityError.CannotDeleteHasActiveIslands(activeIslands.size.toString())
            )
        }

        // Handle primary: assign to another active facility
        if (facility.isPrimary) {
            facilityRepository.getActiveFacilitiesByClient(facility.clientId)
                .fold(onSuccess = { active ->
                    val next = active.firstOrNull { it.id != facilityId }
                    if (next != null) {
                        facilityRepository.setPrimaryFacility(facility.clientId, next.id)
                    }
                }, onFailure = {
                    Timber.d(
                        it,
                        "Failed to get active facilities {forceDeleteOnlyOneActive=$forceDeleteOnlyOneActive}"
                    )
                })
        }

        // Cascade deactivate islands if forceDelete
        if (forceDelete && activeIslands.isNotEmpty()) facilityRepository.deactivateFacility(
            facilityId
        ).fold(onSuccess = {
            Timber.d("Cascade deleting islands")
            return QrResult.Success(Unit)
        }, onFailure = {
            Timber.d(it, "Failed to cascade delete islands")
            return QrResult.Error(QrError.FacilityError.DeleteError(it.message))
        })

        // Deactivate facility
        facilityRepository.deactivateFacility(facilityId).fold(onSuccess = {
            Timber.d("Successfully deleted facility $facilityId")
            return QrResult.Success(Unit)
        }, onFailure = {
            Timber.d(it, "Failed to delete facility $facilityId")
            return QrResult.Error(QrError.FacilityError.DeleteError(it.message))
        })
    }
}