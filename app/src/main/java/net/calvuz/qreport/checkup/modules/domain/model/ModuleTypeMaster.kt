package net.calvuz.qreport.checkup.modules.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Domain model for a user-managed checklist module type master record
 * (the `module_types` table). Distinct from the legacy [ModuleType] enum: this
 * is the source of truth for display (label/icon) going forward, and supports
 * custom module types created from Settings.
 */
@Serializable
data class ModuleTypeMaster(
    val id: String,
    val code: String,
    val label: String,
    val description: String? = null,
    val iconName: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)
