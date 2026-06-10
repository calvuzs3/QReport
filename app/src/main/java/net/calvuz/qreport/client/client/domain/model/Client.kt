package net.calvuz.qreport.client.client.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.app.app.domain.model.Address

/**
 * Domain model for a Client (customer company).
 *
 * Pure domain: no UI types, no color codes, no child collections.
 * Child entities (facilities, contacts, contracts) are loaded separately
 * by the repository and composed into richer models (e.g. ClientWithDetails)
 * only when the use case requires them.
 *
 * Soft-delete is handled via [isActive]: inactive clients are excluded from
 * normal queries but retained in the database for historical reference.
 */
@Serializable
data class Client(

    val id: String,

    // ===== DATA =====
    val companyName: String, val notes: String? = null,

    // ===== LOCALIZATION =====
    val headquarters: Address? = null,  // Serialized as JSON in the DB

    // ===== METADATA =====
    val isActive: Boolean = true,       // false = soft-deleted
    val createdAt: Instant, val updatedAt: Instant
) {
    /** Convenience alias used throughout the UI. */
    val displayName: String get() = companyName
}

