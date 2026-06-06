package net.calvuz.qreport.client.island.maintenance.domain.model

import kotlinx.serialization.Serializable
import net.calvuz.qreport.R

/**
 * Result of a maintenance intervention.
 *
 * Drives the deferred/emergency rate metrics in [IslandHealthSummary]:
 *  - [DEFERRED] and [REQUIRES_PARTS] contribute to the deferred rate.
 *  - Outcome stored as TEXT (enum name) in Room.
 *
 * UI: resolve [labelResId] in the presentation layer for localized display.
 */
@Serializable
enum class MaintenanceOutcome(val labelResId: Int) {

    /** Intervention fully completed — issue resolved. */
    COMPLETED(R.string.maint_outcome_completed),

    /** Intervention partially completed — follow-up required. */
    PARTIAL(R.string.maint_outcome_partial),

    /** Intervention postponed — will be rescheduled. */
    DEFERRED(R.string.maint_outcome_deferred),

    /** Intervention blocked — waiting for spare parts. */
    REQUIRES_PARTS(R.string.maint_outcome_requires_parts);

    companion object {

        /**
         * Safe valueOf that returns [COMPLETED] instead of throwing on unknown names.
         * Used in the mapper when reading stored enum names from Room.
         */
        fun fromName(name: String): MaintenanceOutcome =
            entries.firstOrNull { it.name == name } ?: COMPLETED
    }
}