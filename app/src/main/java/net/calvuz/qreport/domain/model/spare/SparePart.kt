package net.calvuz.qreport.domain.model.spare

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Parte di ricambio consigliata
 */
@Serializable
data class SparePart(
    val id: String,
    val checkUpId: String,
    val partNumber: String,
    val description: String,
    val quantity: Int,
    val urgency: SparePartUrgency,
    val category: SparePartCategory,
    val estimatedCost: Double? = null,
    val notes: String = "",
    val supplierInfo: String = "",
    val addedAt: Instant
)