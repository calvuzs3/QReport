package net.calvuz.qreport.client.facility.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.facility.domain.model.FacilityWithIslands
import net.calvuz.qreport.client.facility.domain.model.IslandStatistics
import net.calvuz.qreport.client.facility.domain.repository.FacilityRepository
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Assembles a [FacilityWithIslands] from the facility and its associated islands.
 *
 * Uses the canonical [FacilityWithIslands] from the domain model package.
 * Child collections degrade gracefully — a failure returns an empty island list.
 */
class GetFacilityWithIslandsUseCase @Inject constructor(
    private val facilityRepository: FacilityRepository,
    private val islandRepository: IslandRepository
) {
    suspend operator fun invoke(facilityId: String): QrResult<FacilityWithIslands, QrError.FacilityError> {

        Timber.d("Getting facility $facilityId with islands")

        if (facilityId.isBlank()) {

            return QrResult.Error(QrError.FacilityError.NotFound())
        }

        val facility = facilityRepository.getFacilityById(facilityId).fold(
            onSuccess = { it ?: return QrResult.Error(QrError.FacilityError.NotFound()) },
            onFailure = { return QrResult.Error(QrError.FacilityError.LoadError(it.message)) }
        )

        val islands = islandRepository.getIslandsByFacility(facilityId)
            .getOrElse { emptyList() }
            .sortedWith(
                compareByDescending<Island> { it.isActive }
                    .thenBy { it.islandType.name }
                    .thenBy { (it.customName ?: it.serialNumber).lowercase() }
            )

        return QrResult.Success(
            FacilityWithIslands(
                facility = facility,
                islands = islands,
                statistics = calculateStatistics(islands)
            )
        )
    }

    // -------------------------------------------------------------------------

    private fun calculateStatistics(islands: List<Island>): IslandStatistics {
        if (islands.isEmpty()) return IslandStatistics()
        val active = islands.filter { it.isActive }
        return IslandStatistics(
            totalCount = islands.size,
            activeCount = active.size,
            inactiveCount = islands.size - active.size,
            byType = islands.groupBy { it.islandType }.mapValues { it.value.size },
            totalOperatingHours = islands.sumOf { it.operatingHours },
            totalCycleCount = islands.sumOf { it.cycleCount },
            averageOperatingHours = islands.map { it.operatingHours }.average(),
            maintenanceDueCount = islands.count { it.needsMaintenance() },
            underWarrantyCount = islands.count { it.isUnderWarranty() },
            oldestInstallation = islands.mapNotNull { it.installationDate }.minOrNull(),
            newestInstallation = islands.mapNotNull { it.installationDate }.maxOrNull()
        )
    }
}