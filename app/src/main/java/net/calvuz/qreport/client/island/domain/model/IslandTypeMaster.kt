package net.calvuz.qreport.client.island.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Domain model for a user-managed island type master record (the `island_types` table).
 *
 * Distinct from the legacy IslandType enum: this is the source of truth for
 * display (label/icon) and maintenance interval going forward, and supports
 * custom types created from Settings.
 *
 * [isDeleted] is a data-layer/sync concern, never surfaced here — same convention
 * as [net.calvuz.qreport.client.client.domain.model.Client].
 */
@Serializable
data class IslandTypeMaster(
    val id: String,
    val code: String,
    val label: String,
    val description: String? = null,
    val iconName: String? = null,
    val maintenanceIntervalDays: Int = 180,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)
