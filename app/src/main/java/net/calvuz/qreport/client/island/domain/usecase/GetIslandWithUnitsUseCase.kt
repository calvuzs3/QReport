package net.calvuz.qreport.client.island.domain.usecase

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.repository.IslandRepository
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.domain.model.UnitType
import net.calvuz.qreport.client.unit.domain.repository.MechanicalUnitRepository
import javax.inject.Inject

/**
 * Assembles an [IslandWithUnits] pairing an island with its mechanical units.
 *
 * Units degrade gracefully — a failure returns an empty list and the use
 * case still succeeds, since the island data itself is the critical part.
 */
class GetIslandWithUnitsUseCase @Inject constructor(
    private val repository: IslandRepository,
    private val unitRepository: MechanicalUnitRepository
) {
    suspend operator fun invoke(islandId: String): QrResult<IslandWithUnits, QrError.IslandError> {
        if (islandId.isBlank()) {
            return QrResult.Error(QrError.IslandError.NotFound())
        }

        val island = repository.getIslandById(islandId).fold(
            onSuccess = { it ?: return QrResult.Error(QrError.IslandError.NotFound()) },
            onFailure = { return QrResult.Error(QrError.IslandError.LoadError(it.message)) }
        )

        val units = unitRepository.getUnitsByIsland(islandId)
            .getOrElse { emptyList() }
            .sortedWith(
                compareByDescending<MechanicalUnit> { it.isActive }
                    .thenBy { it.unitType.name }
            )

        return QrResult.Success(
            IslandWithUnits(
                island = island,
                units = units,
                statistics = calculateStatistics(units)
            )
        )
    }

    // -------------------------------------------------------------------------

    private fun calculateStatistics(units: List<MechanicalUnit>): UnitStatistics {
        if (units.isEmpty()) return UnitStatistics()
        return UnitStatistics(
            totalCount = units.size,
            activeCount = units.count { it.isActive },
            inactiveCount = units.count { !it.isActive },
            byType = units.groupBy { it.unitType }.mapValues { it.value.size }
        )
    }
}

// =============================================================================
// DATA CLASSES
// =============================================================================

data class IslandWithUnits(
    val island: Island,
    val units: List<MechanicalUnit>,
    val statistics: UnitStatistics
) {
    val hasUnits: Boolean get() = units.isNotEmpty()
    val displayName: String get() = island.displayName
    val unitsCount: Int get() = units.size
}

data class UnitStatistics(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val inactiveCount: Int = 0,
    val byType: Map<UnitType, Int> = emptyMap()
) {
    val hasActiveUnits: Boolean get() = activeCount > 0
    val hasInactiveUnits: Boolean get() = inactiveCount > 0
}

data class UnitSearchCriteria(
    val unitType: Set<UnitType> = emptySet(),
    val activeOnly: Boolean = false
)