package net.calvuz.qreport.checkup.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.checkup.domain.model.spare.SparePart

/**
 * Check-up principale
 * Rappresenta un intero check-up di un'isola robotizzata
 */
@Serializable
data class CheckUp(
    val id: String,
    val header: CheckUpHeader,
    /** Frozen display label of the island type at creation time (e.g. "POLY Move") — never re-queried from the master. */
    val islandType: String,
    val islandTypeId: String? = null,
    val status: CheckUpStatus,
    val checkItems: List<CheckItem> = emptyList(),
    val spareParts: List<SparePart> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null
)