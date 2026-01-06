package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * SparePartBackup - Backup dei ricambi
 */
@Serializable
data class SparePartBackup(
    val id: String,
    val checkUpId: String,
    val partNumber: String,
    val description: String,
    val quantity: Int,
    val urgency: String,
    val category: String,
    val estimatedCost: Double?,
    val notes: String,
    val supplierInfo: String,
    @Contextual val addedAt: Instant
)