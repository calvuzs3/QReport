package net.calvuz.qreport.client.unit.domain.model

import kotlinx.serialization.Serializable
import net.calvuz.qreport.R

/**
 * Type of physical unit that can belong to a robotic island.
 *
 * [labelResId] is resolved via stringResource() in the presentation layer.
 * No display strings live in the domain model.
 */
@Serializable
enum class UnitType(val labelResId: Int) {
    ROBOT(R.string.unit_type_robot),
    AXIS(R.string.unit_type_axis),
    SAFETY(R.string.unit_type_safety),
    ELECTRICAL_PANEL(R.string.unit_type_electrical_panel),
    PNEUMATIC_PANEL(R.string.unit_type_pneumatic_panel),
    STATION(R.string.unit_type_station),
    MAGAZINE(R.string.unit_type_magazine),
    TOOL_RACK(R.string.unit_type_tool_rack),
    OTHER(R.string.unit_type_other)
}