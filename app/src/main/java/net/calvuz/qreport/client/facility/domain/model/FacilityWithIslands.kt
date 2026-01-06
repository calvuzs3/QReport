package net.calvuz.qreport.client.facility.domain.model

import net.calvuz.qreport.client.island.domain.model.Island

/**
 * Facility con le sue islands associate
 */
data class FacilityWithIslands(
    val facility: Facility,
    val islands: List<Island>
) {

    /**
     * Islands attive
     */
    val activeIslands: List<Island>
        get() = islands.filter { it.isActive }

    /**
     * Islands che necessitano manutenzione
     */
    val islandsNeedingMaintenance: List<Island>
        get() = activeIslands.filter { it.needsMaintenance() }
}