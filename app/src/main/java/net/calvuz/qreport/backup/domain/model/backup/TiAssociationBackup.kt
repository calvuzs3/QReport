package net.calvuz.qreport.backup.domain.model.backup

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class TiAssociationBackup(
    val id: String,
    val interventionId: String,
    val islandId: String,
    val associationType: String,
    val notes: String? = null,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    val syncedAt: Long? = null,
    val isDeleted: Boolean = false
)
