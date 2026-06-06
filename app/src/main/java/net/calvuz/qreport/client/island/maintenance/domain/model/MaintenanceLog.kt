package net.calvuz.qreport.client.island.maintenance.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A single maintenance or technical intervention performed on a robotic island.
 *
 * Records are immutable in content once created — no update use case exists.
 * Errors are corrected by creating a follow-up log entry.
 *
 * Component targeting — two parallel optional fields:
 *  - [mechanicalUnitId] FK to a registered [MechanicalUnit]; preferred when available.
 *  - [componentLabel]   free text; used when the component is not yet catalogued.
 * If [mechanicalUnitId] is non-null, [componentLabel] is ignored for analysis purposes.
 *
 * [customOperationLabel] is only meaningful when [operationType] is
 * [MaintenanceOperationType.OTHER] — it provides the specific description for
 * operations not covered by the standard enum.
 */
@Serializable
data class MaintenanceLog(
    val id: String,
    val islandId: String,

    // ===== OPERATION =====
    val operationType: MaintenanceOperationType,
    val customOperationLabel: String? = null,   // only used when operationType == OTHER

    // ===== COMPONENT TARGET =====
    val mechanicalUnitId: String? = null,       // FK → MechanicalUnit (if catalogued)
    val componentLabel: String? = null,         // free text (if not catalogued)

    // ===== DESCRIPTION =====
    val description: String,                    // always required

    // ===== TECHNICIAN SNAPSHOT =====
    // Stored as a snapshot at creation time — not a FK.
    // Changes to TechnicianSettings do not affect past logs.
    val technicianName: String,
    val technicianCompany: String? = null,

    // ===== MACHINE STATE SNAPSHOT =====
    val operatingHoursAtEvent: Int? = null,     // Island.operatingHours at intervention time
    val cycleCountAtEvent: Long? = null,        // Island.cycleCount at intervention time

    // ===== OUTCOME =====
    val outcome: MaintenanceOutcome,
    val durationMinutes: Int? = null,
    val notes: String? = null,

    // ===== META =====
    val performedAt: Instant,                   // actual date/time of the intervention
    val createdAt: Instant,                     // when the record was entered in the app
    val updatedAt: Instant,                     // last write — used for server sync
    val isActive: Boolean = true,               // false = first delete stage
    val isDeleted: Boolean = false              // true = second delete stage / pending purge
) {

    /**
     * Returns the effective display label for the operation type.
     * When [operationType] is OTHER and [customOperationLabel] is provided,
     * the custom label is returned instead of the generic "Other" string.
     *
     * @param typeLabel  localized string for [operationType] resolved by the UI layer
     */
    fun effectiveOperationLabel(typeLabel: String): String =
        if (operationType == MaintenanceOperationType.OTHER &&
            !customOperationLabel.isNullOrBlank()
        ) customOperationLabel else typeLabel

    /**
     * Returns the effective component description for display.
     * Prefers [unitName] (resolved by the UI from the registered MechanicalUnit);
     * falls back to [componentLabel] if no unit FK is set.
     *
     * @param unitName  name of the MechanicalUnit resolved by the UI layer, or null
     */
    fun effectiveComponentLabel(unitName: String?): String? =
        unitName ?: componentLabel

    /**
     * Returns true if this log targets a specific component,
     * either via FK or free text.
     */
    fun hasComponentTarget(): Boolean =
        mechanicalUnitId != null || !componentLabel.isNullOrBlank()
}