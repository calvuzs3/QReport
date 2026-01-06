package net.calvuz.qreport.checkup.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Associazione tra CheckUp e Isola Robotizzata
 * Supporta associazioni multiple e tipologie diverse
 */
@Serializable
data class CheckUpIslandAssociation(
    val id: String,
    val checkupId: String,
    val islandId: String,
    val associationType: AssociationType = AssociationType.STANDARD,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

