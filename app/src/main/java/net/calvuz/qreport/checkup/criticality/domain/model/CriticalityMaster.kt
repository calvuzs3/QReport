package net.calvuz.qreport.checkup.criticality.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Domain model for a user-managed checklist criticality level master record
 * (the `criticality_levels` table). Distinct from the legacy [CriticalityLevel]
 * enum: this is the source of truth for display (label/color/icon) going
 * forward, and supports custom criticality levels created from Settings.
 */
@Serializable
data class CriticalityMaster(
    val id: String,
    val code: String,
    val label: String,
    val priority: Int,
    val colorHex: String,
    val iconEmoji: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)
