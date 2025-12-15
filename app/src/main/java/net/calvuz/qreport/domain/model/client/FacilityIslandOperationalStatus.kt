package net.calvuz.qreport.domain.model.client

import kotlinx.serialization.Serializable

/**
 * Stati operativi dell'isola
 */
@Serializable
enum class FacilityIslandOperationalStatus(val displayName: String, val color: String) {
    OPERATIONAL("Operativa", "00B050"),
    MAINTENANCE_DUE("Manutenzione dovuta", "FFC000"),
    INACTIVE("Non attiva", "FF0000")
}