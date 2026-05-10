package net.calvuz.qreport.client.unit.domain.model

import kotlinx.serialization.Serializable

/**
 * Type of physical unit that can belong to a robotic island.
 */
@Serializable
enum class UnitType(val displayName: String) {
    ROBOT("Robot"),
    AXIS("Asse aggiuntivo (7°/8°/9°)"),
    SAFETY("Sistema di sicurezza"),
    ELECTRICAL_PANEL("Quadro elettrico"),
    STATION("Stazione (etichettatura, collaudo, ...)"),
    MAGAZINE("Magazzino (carico/scarico)"),
    TOOL_RACK("Rack cambio tool"),
    OTHER("Altro")
}