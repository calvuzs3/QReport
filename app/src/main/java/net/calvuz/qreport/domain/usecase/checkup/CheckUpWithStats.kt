package net.calvuz.qreport.domain.usecase.checkup

import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpStatistics

data class CheckUpWithStats(
    val checkUp: CheckUp,
    val statistics: CheckUpStatistics
)