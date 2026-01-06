package net.calvuz.qreport.checkup.domain.model

import kotlinx.serialization.Serializable

/**
 * Stato di avanzamento del check-up
 */
@Serializable
enum class CheckUpStatus() {
    DRAFT,
    IN_PROGRESS,
    COMPLETED,
    EXPORTED,
    ARCHIVED
}