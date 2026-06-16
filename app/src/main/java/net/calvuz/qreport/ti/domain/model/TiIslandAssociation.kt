package net.calvuz.qreport.ti.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class TiAssociationType {
    STANDARD,
    MULTI_ISLAND
}

@Serializable
data class TiIslandAssociation(
    val id: String,
    val interventionId: String,
    val islandId: String,
    val associationType: TiAssociationType,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncedAt: Instant? = null,
    val isDeleted: Boolean = false
)
