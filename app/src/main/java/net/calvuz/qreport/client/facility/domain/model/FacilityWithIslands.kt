package net.calvuz.qreport.client.facility.domain.model

import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandType
import kotlinx.datetime.Instant

/**
 * Canonical model pairing a [Facility] with its associated [Island] list
 * and aggregate [IslandStatistics].
 *
 * This is the single definition used across all features (client detail,
 * facility detail, checkup creation). It replaces the local definition
 * that was previously duplicated inside [GetFacilityWithIslandsUseCase].
 */
data class FacilityWithIslands(
    val facility: Facility,
    val islands: List<Island>,
    val statistics: IslandStatistics = IslandStatistics()
) {
    val hasIslands: Boolean get() = islands.isNotEmpty()
    val displayName: String get() = facility.displayName
    val islandCount: Int get() = islands.size
    val activeIslandCount: Int get() = statistics.activeCount

    /** Islands that need maintenance, used by the client detail screen. */
    val islandsNeedingMaintenance: List<Island>
        get() = islands.filter { it.needsMaintenance() }
}

/**
 * Aggregate statistics computed from a [FacilityWithIslands.islands] list.
 */
data class IslandStatistics(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val inactiveCount: Int = 0,
    val byType: Map<IslandType, Int> = emptyMap(),
    val totalOperatingHours: Int = 0,
    val totalCycleCount: Long = 0L,
    val averageOperatingHours: Double = 0.0,
    val maintenanceDueCount: Int = 0,
    val underWarrantyCount: Int = 0,
    val oldestInstallation: Instant? = null,
    val newestInstallation: Instant? = null
) {
    val hasActiveIslands: Boolean get() = activeCount > 0
    val maintenanceRate: Float
        get() = if (totalCount > 0) maintenanceDueCount.toFloat() / totalCount else 0f
    val warrantyRate: Float
        get() = if (totalCount > 0) underWarrantyCount.toFloat() / totalCount else 0f
}