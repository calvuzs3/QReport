package net.calvuz.qreport.domain.model.checkup

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

/**
 * Tipologie di associazione CheckUp-Isola
 */
@Serializable
enum class AssociationType(val displayName: String, val description: String) {
    STANDARD("Standard", "CheckUp singola isola"),
    MULTI_ISLAND("Multi-Isola", "CheckUp comparativo su più isole"),
    COMPARISON("Confronto", "CheckUp per confronto performance"),
    MAINTENANCE("Manutenzione", "CheckUp post-manutenzione specifica"),
    EMERGENCY("Emergenza", "CheckUp di emergenza per guasto")
}