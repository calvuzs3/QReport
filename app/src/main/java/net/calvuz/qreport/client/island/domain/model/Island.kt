package net.calvuz.qreport.client.island.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Robotic Island belonging to a [net.calvuz.qreport.client.facility.domain.model.Facility].
 *
 * Pure domain: no UI strings, no color codes, no Compose dependencies.
 * Maintenance status text is a presentation concern — the UI resolves
 * [islandOperationalStatus] and [daysToNextMaintenance] into localized strings.
 */
@Serializable
data class Island(
    val id: String,
    val facilityId: String,

    // ===== COMMISSIONING =====
    val commissioningNumber: String? = null,
    val customName: String? = null,

    // ===== ISLAND TYPE =====
    /** Frozen display label of the island type (e.g. "POLY Move") — kept for backward compat, superseded by [islandTypeId]. */
    val islandType: String,
    /** FK to the island_types master data table — nullable until every island has been re-saved through the new picker. */
    val islandTypeId: String? = null,

    // ===== TECHNICAL DETAILS =====
    val modelNumber: String? = null,
    val serialNumber: String,
    val installationDate: Instant? = null,
    val warrantyExpiration: Instant? = null,

    // ===== MAINTENANCE =====
    val operatingHours: Int = 0,
    val cycleCount: Long = 0L,
    val lastMaintenanceDate: Instant? = null,
    val nextScheduledMaintenance: Instant? = null,

    // ===== CONFIGURATION =====
    val location: String? = null,
    val notes: String? = null,

    // ===== META =====
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
) {

    /**
     * Returns true if scheduled maintenance is overdue or due today.
     */
    fun needsMaintenance(): Boolean =
        nextScheduledMaintenance?.let { it <= Clock.System.now() } ?: false

    /**
     * Returns true if the island is currently under warranty.
     */
    fun isUnderWarranty(): Boolean =
        warrantyExpiration?.let { it > Clock.System.now() } ?: false

    /**
     * Computed operational status — pure domain logic, no strings.
     * The UI resolves [IslandOperationalStatus.labelResId] for display.
     */
    val islandOperationalStatus: IslandOperationalStatus
        get() = when {
            !isActive -> IslandOperationalStatus.INACTIVE
            needsMaintenance() -> IslandOperationalStatus.MAINTENANCE_DUE
            else -> IslandOperationalStatus.OPERATIONAL
        }

    /**
     * Days until the next scheduled maintenance.
     * Positive = days remaining, negative = days overdue, null = not scheduled.
     */
    fun daysToNextMaintenance(): Long? =
        nextScheduledMaintenance?.let { (it - Clock.System.now()).inWholeDays }
}