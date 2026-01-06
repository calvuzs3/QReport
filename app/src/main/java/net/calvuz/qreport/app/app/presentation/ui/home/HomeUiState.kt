package net.calvuz.qreport.app.app.presentation.ui.home

import net.calvuz.qreport.checkup.domain.model.CheckUp

/**
 * Stato UI per la Home Screen
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val isCreatingCheckUp: Boolean = false,
    val dashboardStats: DashboardStatistics? = null,
    val recentCheckUps: List<CheckUp> = emptyList(),
    val inProgressCheckUps: List<CheckUp> = emptyList(),
    val selectedCheckUpId: String? = null,
    val quickCreatedCheckUpId: String? = null,
    val showQuickCreateSuccess: Boolean = false,
    val error: String? = null
)