package net.calvuz.qreport.checkup.checkup.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.checkup.items.domain.model.CheckItem

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
    /** Status code — soft-references [net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster]. */
    val status: String,
    val checkItems: List<CheckItem> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null
)