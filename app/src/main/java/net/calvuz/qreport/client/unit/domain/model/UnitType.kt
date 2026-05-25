package net.calvuz.qreport.client.unit.domain.model

import kotlinx.serialization.Serializable

/**
 * Type of physical unit that can belong to a robotic island.
 */
@Serializable
enum class UnitType(val displayName: String) {
    ROBOT("Robot"),
    AXIS("Asse aggiuntivo"),
    SAFETY("Sistema di sicurezza"),
    ELECTRICAL_PANEL("Quadro elettrico"),
    PNEUMATIC_PANEL("Quadro pneumatico"),
    STATION("Stazione"),
    MAGAZINE("Magazzino"),
    TOOL_RACK("Rack cambio tool"),
    OTHER("Altro")
}