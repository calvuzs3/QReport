package net.calvuz.qreport.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * CheckUpAssociationBackup - Backup delle associazioni CheckUp ↔ FacilityIsland
 */
@Serializable
data class CheckUpAssociationBackup(
    val id: String,
    val checkupId: String,
    val islandId: String,
    val associationType: String,
    val notes: String? = null,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant
)