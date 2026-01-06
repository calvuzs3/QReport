package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * CheckItemBackup - Backup dei CheckItem
 */
@Serializable
data class CheckItemBackup(
    val id: String,
    val checkUpId: String,
    val moduleType: String,
    val itemCode: String,
    val description: String,
    val status: String,
    val criticality: String,
    val notes: String,
    @Contextual val checkedAt: Instant?,
    val orderIndex: Int,
)