package net.calvuz.qreport.presentation.screen.home

import net.calvuz.qreport.domain.model.checkup.CheckUp

/**
 * Dati aggregati per la dashboard
 */
data class DashboardData(
    val recentCheckUps: List<CheckUp>,
    val inProgressCheckUps: List<CheckUp>,
    val draftCheckUps: List<CheckUp>,
    val completedCheckUps: List<CheckUp>
)