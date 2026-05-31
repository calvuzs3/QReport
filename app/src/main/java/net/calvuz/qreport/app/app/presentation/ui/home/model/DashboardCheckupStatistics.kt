package net.calvuz.qreport.app.app.presentation.ui.home.model

/**
 * Dashboard Checkup statistics
 */
data class DashboardCheckupStatistics(
    val totalCheckUps: Int,
    val activeCheckUps: Int,
    val completedThisWeek: Int,
    val averageCompletionTime: Int // in hours
)