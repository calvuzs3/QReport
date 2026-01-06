package net.calvuz.qreport.checkup.domain.model.spare

import kotlinx.serialization.Serializable

/**
 * Enum per urgenza delle parti di ricambio
 */
@Serializable
enum class SparePartUrgency(val displayName: String, val priority: Int) {
    IMMEDIATE("Immediata", 4),
    HIGH("Alta", 3),
    MEDIUM("Media", 2),
    LOW("Bassa", 1);
}