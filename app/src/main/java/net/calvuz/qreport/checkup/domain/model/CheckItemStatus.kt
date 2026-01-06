package net.calvuz.qreport.checkup.domain.model

import kotlinx.serialization.Serializable

/**
 * Stato di controllo di un singolo item
 */
@Serializable
enum class CheckItemStatus() {
    PENDING(),
    OK(),
    NOK(),
    NA()
}