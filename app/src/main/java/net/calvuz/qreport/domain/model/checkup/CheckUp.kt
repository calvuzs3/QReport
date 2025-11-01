package net.calvuz.qreport.domain.model.checkup

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.domain.model.spare.SparePart

/**
 * Check-up principale
 * Rappresenta un intero check-up di un'isola robotizzata
 */
@Serializable
data class CheckUp(
    val id: String,
    val header: CheckUpHeader,
    val islandType: IslandType,
    val status: CheckUpStatus,
    val checkItems: List<CheckItem> = emptyList(),
    val spareParts: List<SparePart> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null
)