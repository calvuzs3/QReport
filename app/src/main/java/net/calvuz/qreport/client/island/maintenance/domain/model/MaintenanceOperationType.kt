package net.calvuz.qreport.client.island.maintenance.domain.model

import kotlinx.serialization.Serializable
import net.calvuz.qreport.R

/**
 * Predefined categories for maintenance and technical operations on a robotic island.
 *
 * Design rationale:
 *  - Maintained as an enum by the single developer/technician owning the app.
 *  - New types are added via code commit — no in-app CRUD required.
 *  - Use [OTHER] + [MaintenanceLog.customOperationLabel] for rare or one-off operations.
 *    When OTHER appears frequently with the same custom label, promote it to a named value.
 *
 * UI: resolve [labelResId] in the presentation layer for localized display.
 * Analysis: enum name is stored as TEXT in Room; comparisons use the name string directly.
 */
@Serializable
enum class MaintenanceOperationType(val labelResId: Int) {

    // ===== CHECKUP =====
    GENERAL_CHECKUP(R.string.maint_op_general_checkup),

    // ===== SCHEDULED MAINTENANCE =====
    ROUTINE_INSPECTION(R.string.maint_op_routine_inspection),
    OIL_CHANGE(R.string.maint_op_oil_change),
    FILTER_REPLACEMENT(R.string.maint_op_filter_replacement),
    LUBRICATION(R.string.maint_op_lubrication),
    CALIBRATION(R.string.maint_op_calibration),

    // ===== COMPONENT REPLACEMENT =====
    COMPONENT_REPLACEMENT(R.string.maint_op_component_replacement),
    ENCODER_REPLACEMENT(R.string.maint_op_encoder_replacement),
    MOTOR_REPLACEMENT(R.string.maint_op_motor_replacement),
    REDUCER_REPLACEMENT(R.string.maint_op_reducer_replacement),
    SENSOR_REPLACEMENT(R.string.maint_op_sensor_replacement),
    CABLE_REPLACEMENT(R.string.maint_op_cable_replacement),

    // ===== ELECTRICAL / SOFTWARE =====
    ELECTRICAL_REPAIR(R.string.maint_op_electrical_repair),
    SOFTWARE_UPDATE(R.string.maint_op_software_update),
    PARAMETER_TUNING(R.string.maint_op_parameter_tuning),

    // ===== EXTRAORDINARY =====
    EMERGENCY_REPAIR(R.string.maint_op_emergency_repair),
    REVAMPING(R.string.maint_op_revamping),
    INSTALLATION(R.string.maint_op_installation),

    // ===== FALLBACK =====
    OTHER(R.string.maint_op_other);

    companion object {

        /**
         * Safe valueOf that returns [OTHER] instead of throwing on unknown names.
         * Used in the mapper when reading stored enum names from Room.
         */
        fun fromName(name: String): MaintenanceOperationType =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}