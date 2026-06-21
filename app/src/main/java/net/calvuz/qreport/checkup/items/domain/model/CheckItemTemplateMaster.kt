package net.calvuz.qreport.checkup.items.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Domain model for a checklist template master record (the `check_item_templates`
 * table), the master-data replacement for the historical hardcoded
 * `CheckItemModules` object.
 *
 * [moduleTypeId]/[criticalityId] are soft references to `module_types`/
 * `criticality_levels` (see [net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster],
 * [net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster]). Which
 * island types a template surfaces in is no longer decided per-template — it's
 * decided by which island types its module is associated with (see
 * [net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster]'s island-type
 * links).
 */
@Serializable
data class CheckItemTemplateMaster(
    val id: String,
    val moduleTypeId: String,
    val category: String,
    val description: String,
    val criticalityId: String,
    val orderIndex: Int,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)
