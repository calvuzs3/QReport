package net.calvuz.qreport.checkup.checkup.domain.usecase

import net.calvuz.qreport.checkup.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpSingleStatistics

data class CheckUpWithStats(
    val checkUp: CheckUp,
    val statistics: CheckUpSingleStatistics
)