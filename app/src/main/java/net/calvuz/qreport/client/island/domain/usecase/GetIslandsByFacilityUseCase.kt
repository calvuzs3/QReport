package net.calvuz.qreport.client.island.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.client.island.domain.repository.IslandTypeMasterRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns islands for a given facility, sorted by type then name/serial.
 *
 * Secondary methods (maintenance, warranty, summary) follow the same
 * QrResult pattern. The reactive [observeIslandsByFacility] stays on Flow.
 */
class GetIslandsByFacilityUseCase @Inject constructor(
    private val islandRepository: IslandRepository,
    private val islandTypeMasterRepository: IslandTypeMasterRepository,
) {

    @Suppress("HardCodedStringLiteral")
    suspend operator fun invoke(facilityId: String): QrResult<List<Island>, QrError.IslandError> {

        Timber.d("Get islands for facility $facilityId")

        // Check input
        if (facilityId.isBlank()) {
            Timber.d("Facility id is blank")
            return QrResult.Error(QrError.IslandError.FacilityNotFound())
        }

        // Get
        islandRepository.getIslandsByFacility(facilityId).fold(
            onSuccess = { islands ->
                Timber.d("Loaded islands for facility $facilityId: ${islands.size}")
                return QrResult.Success(islands.sortedByTypeThenName(loadIslandTypeLabels()))
            },
            onFailure = {
                Timber.d(it, "Failed to load islands for facility $facilityId")
                return QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }

    @Suppress("HardCodedStringLiteral")
    suspend fun getActive(facilityId: String): QrResult<List<Island>, QrError.IslandError> {
        Timber.v("Getting active islands for facility $facilityId")

        return if (facilityId.isBlank()) {
            Timber.d("Facility id is blank")
            QrResult.Error(QrError.IslandError.FacilityNotFound())
        } else {
            islandRepository.getActiveIslandsByFacility(facilityId).fold(
                onSuccess = { islands ->
                    Timber.d("Loaded active islands for facility $facilityId: ${islands.size}")
                    QrResult.Success(islands.sortedByTypeThenName(loadIslandTypeLabels()))
                },
                onFailure = {
                    Timber.d(it, "Failed to load active islands for facility $facilityId")
                    QrResult.Error(QrError.IslandError.LoadError(it.message))
                })
        }
    }

    fun observeIslandsByFacility(facilityId: String): Flow<List<Island>> =
        islandRepository.getAllActiveIslandsByFacilityFlow(facilityId)
            .map { islands -> islands.sortedByTypeThenName(emptyMap()) }

    suspend fun getIslandsDueMaintenance(
        facilityId: String,
        currentTime: Instant = Clock.System.now()
    ): Result<List<Island>> {
        val maintenance = islandRepository.getIslandsRequiringMaintenance(currentTime)
            .getOrElse { return Result.failure(it) }
        return Result.success(
            maintenance.filter { it.facilityId == facilityId }
                .sortedBy { it.nextScheduledMaintenance }
        )
    }

    suspend fun getIslandsUnderWarranty(
        facilityId: String,
        currentTime: Instant = Clock.System.now()
    ): Result<List<Island>> {
        val warranty = islandRepository.getIslandsUnderWarranty(currentTime)
            .getOrElse { return Result.failure(it) }
        return Result.success(
            warranty.filter { it.facilityId == facilityId }
                .sortedBy { it.warrantyExpiration }
        )
    }

    suspend fun getIslandsCount(facilityId: String): QrResult<Int, QrError.IslandError> {
        if (facilityId.isBlank()) return QrResult.Error(QrError.IslandError.FacilityNotFound())
        return islandRepository.getIslandsCountByFacility(facilityId).fold(
            onSuccess = { QrResult.Success(it) },
            onFailure = { QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )
    }

    suspend fun getFacilityOperationalSummary(facilityId: String): Result<FacilityOperationalSummary> {
        if (facilityId.isBlank()) return Result.failure(IllegalArgumentException("Facility ID required"))
        val islands = islandRepository.getIslandsByFacility(facilityId)
            .getOrElse { return Result.failure(it) }
        val now = Clock.System.now()
        return Result.success(
            FacilityOperationalSummary(
                facilityId = facilityId,
                totalIslands = islands.size,
                activeIslands = islands.count { it.isActive },
                islandsByType = islands.groupBy { it.islandType }.mapValues { it.value.size },
                totalOperatingHours = islands.sumOf { it.operatingHours },
                totalCycles = islands.sumOf { it.cycleCount },
                islandsUnderWarranty = islands.count { it.warrantyExpiration?.let { exp -> exp > now } == true },
                islandsDueMaintenance = islands.count { it.needsMaintenance() },
                averageOperatingHours = if (islands.isNotEmpty()) islands.map { it.operatingHours }.average().toInt() else 0
            )
        )
    }

    // -------------------------------------------------------------------------

    /** Island type labels keyed by id, for sorting — falls back silently if the master list can't be loaded. */
    private suspend fun loadIslandTypeLabels(): Map<String, String> =
        islandTypeMasterRepository.getIslandTypes().getOrNull()?.associate { it.id to it.label } ?: emptyMap()

    private fun List<Island>.sortedByTypeThenName(typeLabelsById: Map<String, String>): List<Island> =
        sortedWith(
            compareBy<Island> { typeLabelsById[it.islandTypeId] ?: it.islandType }
                .thenBy { it.customName?.lowercase() ?: it.serialNumber.lowercase() }
        )
}

data class FacilityOperationalSummary(
    val facilityId: String,
    val totalIslands: Int,
    val activeIslands: Int,
    val islandsByType: Map<String, Int>,
    val totalOperatingHours: Int,
    val totalCycles: Long,
    val islandsUnderWarranty: Int,
    val islandsDueMaintenance: Int,
    val averageOperatingHours: Int
)