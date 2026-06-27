package net.calvuz.qreport.checkup.modules.presentation.model

import net.calvuz.qreport.checkup.modules.domain.model.ModuleType
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster

/**
 * Resolves the display label for a module given its key (= moduleTypeId when present,
 * otherwise moduleType.name). Lookup order: master by ID → master by code → enum
 * displayName → raw key (handles custom types from sync with no enum counterpart).
 */
fun resolveModuleTypeLabel(
    moduleKey: String,
    masters: List<ModuleTypeMaster>
): String {
    val byId = masters.find { it.id == moduleKey }
    val byCode = byId ?: masters.find { it.code == moduleKey }
    val enumFallback = ModuleType.entries.find { it.name == moduleKey }?.displayName
    return byCode?.label ?: enumFallback ?: moduleKey
}
