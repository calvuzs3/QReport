package net.calvuz.qreport.app.app.presentation.ui.home.model

import net.calvuz.qreport.checkup.domain.model.CheckUp

/**
 * Dashboard Checkup Data
 */
data class DashboardCheckupData(
    val recentCheckUps: List<CheckUp>,
    val inProgressCheckUps: List<CheckUp>,
    val draftCheckUps: List<CheckUp>,
    val completedCheckUps: List<CheckUp>
)