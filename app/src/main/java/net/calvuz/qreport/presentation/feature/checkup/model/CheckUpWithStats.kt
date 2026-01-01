package net.calvuz.qreport.presentation.feature.checkup.model

import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpSingleStatistics

/**
 * Check-up with stats
 */
data class CheckUpWithStats(
    val checkUp: CheckUp,
    val statistics: CheckUpSingleStatistics
)