package net.calvuz.qreport.domain.model.checkup

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