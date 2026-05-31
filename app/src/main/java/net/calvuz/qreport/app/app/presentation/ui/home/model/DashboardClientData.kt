package net.calvuz.qreport.app.app.presentation.ui.home.model

import net.calvuz.qreport.client.client.domain.model.Client

/**
 * Dashboard Checkup Data
 */
data class DashboardClientData(
    val recentClient: List<Client>
)