package net.calvuz.qreport.client.facility.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.app.app.domain.model.Address

/**
 * Domain model for a Facility (production site belonging to a [Client]).
 *
 * Pure domain: no UI strings, no color codes, no badge models.
 * Presentation concerns (labels, colors) live in the presentation layer.
 *
 * Child relationships (islands) are loaded separately by the repository
 */
@Serializable
data class Facility(
    val id: String,
    val clientId: String,

    // ===== DATA =====
    val name: String,
    val code: String? = null,
    val notes: String? = null,
    val facilityType: FacilityType = FacilityType.PRODUCTION,

    // ===== ADDRESS =====
    val address: Address? = null,

    // ===== META =====
    val isPrimary: Boolean = false,     // Main facility for the client
    val isActive: Boolean = true,       // false = soft-deleted
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /** Display name shown in lists and headers. */
    val displayName: String
        get() = if (code.isNullOrBlank()) name else "$name ($code)"

    /** Formatted address string for display, null if no address is set. */
    val addressDisplay: String?
        get() = address?.toDisplayString()

    /** Returns true if the minimum required data is present. */
    fun isComplete(): Boolean = name.isNotBlank()
}