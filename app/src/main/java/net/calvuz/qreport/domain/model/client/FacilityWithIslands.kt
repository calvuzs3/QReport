package net.calvuz.qreport.domain.model.client

/**
 * Facility con le sue islands associate
 */
data class FacilityWithIslands(
    val facility: Facility,
    val islands: List<FacilityIsland>
) {

    /**
     * Islands attive
     */
    val activeIslands: List<FacilityIsland>
        get() = islands.filter { it.isActive }

    /**
     * Islands che necessitano manutenzione
     */
    val islandsNeedingMaintenance: List<FacilityIsland>
        get() = activeIslands.filter { it.needsMaintenance() }
}