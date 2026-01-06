package net.calvuz.qreport.checkup.domain.usecase

import net.calvuz.qreport.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.domain.model.CheckUpSingleStatistics

data class CheckUpWithStats(
    val checkUp: CheckUp,
    val statistics: CheckUpSingleStatistics
)