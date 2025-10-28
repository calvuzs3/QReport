package net.calvuz.qreport.domain.model

import kotlinx.serialization.Serializable

/**
 * Stato di avanzamento del check-up
 */
@Serializable
enum class CheckUpStatus(val displayName: String) {
    DRAFT("Bozza"),
    IN_PROGRESS("In Corso"),
    COMPLETED("Completato"),
    EXPORTED("Esportato"),
    ARCHIVED("Archiviato")
}