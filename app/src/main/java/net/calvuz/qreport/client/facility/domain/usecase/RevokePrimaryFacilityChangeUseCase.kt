package net.calvuz.qreport.client.facility.domain.usecase

import kotlinx.datetime.Clock
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Removes the primary flag from the current primary facility of a client
 * before a new one is promoted.
 *
 * Kept on [Result] because it is an internal helper called only by
 * [NewFacilityUseCase] and [UpdateFacilityUseCase], which handle the
 * error wrapping themselves.
 */
class RevokePrimaryFacilityChangeUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository
) {
    suspend operator fun invoke(clientId: String): Result<Unit> {

        Timber.v("Revoking primary facility change for client $clientId")

        return try {
            val existing = facilityRepository.getPrimaryFacility(clientId)
                .getOrElse { return Result.success(Unit) } // No primary exists — nothing to do

            if (existing != null) {
                val updated = existing.copy(isPrimary = false, updatedAt = Clock.System.now())
                facilityRepository.updateFacility(updated).onFailure { return Result.failure(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to revoke primary facility change for client $clientId")
            Result.failure(e)
        }
    }
}