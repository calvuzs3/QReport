package net.calvuz.qreport.domain.model.checkup

import kotlinx.serialization.Serializable

/**
 * Tipologie di associazione CheckUp-Isola
 */
@Serializable
enum class AssociationType() {
    STANDARD,
    MULTI_ISLAND,
    COMPARISON,
    MAINTENANCE,
    EMERGENCY
}