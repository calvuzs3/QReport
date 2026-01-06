package net.calvuz.qreport.export.data.model

import net.calvuz.qreport.checkup.domain.model.CheckItem
import net.calvuz.qreport.checkup.domain.model.module.ModuleType

// ============================================================
// UTILITY FUNCTIONS PER LAVORARE CON I MODELLI REALI
// ============================================================

/**
 * Raggruppa CheckItem per ModuleType (basato sui tuoi modelli reali)
 */
fun List<CheckItem>.groupByModuleType(): Map<ModuleType, List<CheckItem>> {
    return this.groupBy { it.moduleType }
        .toSortedMap(compareBy { it.name })
}

/**
 * Estrae tutte le foto da una lista di CheckItem
 */
fun List<CheckItem>.getAllPhotos() = this.flatMap { it.photos }

/**
 * Conta foto per modulo
 */
fun Map<ModuleType, List<CheckItem>>.getPhotoCountByModule(): Map<ModuleType, Int> {
    return this.mapValues { (_, items) -> items.getAllPhotos().size }
}

