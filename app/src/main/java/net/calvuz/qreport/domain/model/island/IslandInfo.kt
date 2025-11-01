package net.calvuz.qreport.domain.model.island

import kotlinx.serialization.Serializable

/**
 * Informazioni isola robotizzata
 */
@Serializable
data class IslandInfo(
    val serialNumber: String,
    val model: String = "",
    val installationDate: String = "",
    val lastMaintenanceDate: String = "",
    val operatingHours: Int = 0,
    val cycleCount: Long = 0L
)