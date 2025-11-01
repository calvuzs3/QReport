package net.calvuz.qreport.presentation.screen.home

import net.calvuz.qreport.domain.model.checkup.CheckUp

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