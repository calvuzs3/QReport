package net.calvuz.qreport.client.unit.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.calvuz.qreport.client.unit.domain.model.UnitType

/**
 * Single mechanical unit belonging to a robotic island.
 *
 * Examples: individual robot, auxiliary axis, tool rack, magazine, safety fence, etc.
 * An auxiliary axis shared by multiple robots is still a single unit of its island —
 * no many-to-many relationship is needed at this stage.
 */
@Serializable
data class MechanicalUnit(
    val id: String,
    val islandId: String,
    val unitType: UnitType,
    val name: String,               // Required: e.g. "Robot R1", "7° Asse", "Rack Tool A"
    val serialNumber: String? = null,
    val model: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)