package net.calvuz.qreport.app.app.presentation.ui.home

import net.calvuz.qreport.checkup.domain.model.CheckUp

/**
 * Dati aggregati per la dashboard
 */
data class DashboardData(
    val recentCheckUps: List<CheckUp>,
    val inProgressCheckUps: List<CheckUp>,
    val draftCheckUps: List<CheckUp>,
    val completedCheckUps: List<CheckUp>
)