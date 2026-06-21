package net.calvuz.qreport.checkup.modules.presentation.model

import net.calvuz.qreport.checkup.modules.domain.model.ModuleType
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster

/**
 * Resolves the label to display for a checklist module: prefers the live
 * [ModuleTypeMaster] (editable from Settings) over the frozen [ModuleType] enum,
 * same Expand-phase precedence as
 * [net.calvuz.qreport.client.island.presentation.model.resolveIslandTypeDisplay].
 */
fun resolveModuleTypeLabel(
    moduleTypeId: String?,
    legacy: ModuleType,
    masters: List<ModuleTypeMaster>
): String {
    val byId = moduleTypeId?.let { id -> masters.find { it.id == id } }
    val byCode = byId ?: masters.find { it.code == legacy.name }
    return byCode?.label ?: legacy.displayName
}
