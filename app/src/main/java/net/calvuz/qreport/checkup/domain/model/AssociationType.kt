package net.calvuz.qreport.checkup.domain.model

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