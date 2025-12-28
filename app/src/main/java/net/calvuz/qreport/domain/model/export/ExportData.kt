package net.calvuz.qreport.domain.model.export

import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckItem
import net.calvuz.qreport.domain.model.checkup.CheckUpSingleStatistics
import net.calvuz.qreport.domain.model.checkup.CheckUpProgress
import net.calvuz.qreport.domain.model.module.ModuleType

/**
 * Modello principale per dati di export - VERSIONE CORRETTA
 *
 * AGGIORNATO per utilizzare SOLO i modelli domain esistenti:
 * - CheckUp (contiene già header con ClientInfo, TechnicianInfo, IslandInfo)
 * - CheckItem (contiene già Photo)
 * - CheckUpSingleStatistics e CheckUpProgress per metadati
 * - Raggruppamento per ModuleType invece di "sections" artificiali
 */
data class ExportData(
    /**
     * Checkup principale - contiene già TUTTO:
     * - header (ClientInfo, TechnicianInfo, IslandInfo)
     * - checkItems con Photo
     * - spareParts
     * - date e status
     */
    val checkup: CheckUp,

    /**
     * Items raggruppati per modulo (basato sui modelli reali)
     * Sostituisce le "sections" artificiali
     */
    val itemsByModule: Map<ModuleType, List<CheckItem>>,

    /**
     * Statistiche del checkup (modello reale esistente)
     */
    val statistics: CheckUpSingleStatistics,

    /**
     * Progresso compilazione (modello reale esistente)
     */
    val progress: CheckUpProgress,

    /**
     * Metadati per l'export (solo info tecniche di generazione)
     */
    val exportMetadata: ExportTechnicalMetadata
)


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

