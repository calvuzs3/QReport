package net.calvuz.qreport.checkup.presentation.model

import net.calvuz.qreport.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.domain.model.CheckUpSingleStatistics

/**
 * Check-up with stats
 */
data class CheckUpWithStats(
    val checkUp: CheckUp,
    val statistics: CheckUpSingleStatistics
)