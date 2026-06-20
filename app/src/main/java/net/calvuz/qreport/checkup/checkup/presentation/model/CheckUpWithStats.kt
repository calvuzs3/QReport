package net.calvuz.qreport.checkup.checkup.presentation.model

import net.calvuz.qreport.checkup.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.checkup.domain.model.CheckUpSingleStatistics

/**
 * Check-up with stats
 */
data class CheckUpWithStats(
    val checkUp: CheckUp,
    val statistics: CheckUpSingleStatistics
)