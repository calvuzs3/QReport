package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.model.CheckUp
import net.calvuz.qreport.domain.model.CheckUpStatistics

data class CheckUpWithStats(
    val checkUp: CheckUp,
    val statistics: CheckUpStatistics
)