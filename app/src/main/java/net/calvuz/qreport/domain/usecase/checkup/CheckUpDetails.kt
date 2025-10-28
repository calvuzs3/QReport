package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.model.CheckItem
import net.calvuz.qreport.domain.model.CheckUp
import net.calvuz.qreport.domain.model.CheckUpProgress
import net.calvuz.qreport.domain.model.CheckUpStatistics
import net.calvuz.qreport.domain.model.SparePart

/**
 * Dati completi di un check-up con statistiche e progresso
 */
data class CheckUpDetails(
    val checkUp: CheckUp,
    val checkItems: List<CheckItem>,
    val spareParts: List<SparePart>,
    val statistics: CheckUpStatistics,
    val progress: CheckUpProgress
)