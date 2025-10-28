package net.calvuz.qreport.presentation.screen.home

/**
 * Statistiche per la dashboard
 */
data class DashboardStatistics(
    val totalCheckUps: Int,
    val activeCheckUps: Int,
    val completedThisWeek: Int,
    val averageCompletionTime: Int // in hours
)