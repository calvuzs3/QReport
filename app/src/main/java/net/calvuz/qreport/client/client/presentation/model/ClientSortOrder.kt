package net.calvuz.qreport.client.client.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

enum class ClientSortOrder: QReportSortOrder {
    COMPANY_NAME, CREATED_RECENT, CREATED_OLDEST, FACILITIES_COUNT, CHECKUPS_COUNT
}

// Extension
fun ClientSortOrder.getDisplayName(): String {
    return when (this) {
        ClientSortOrder.COMPANY_NAME -> "Nome Azienda"
        ClientSortOrder.CREATED_RECENT -> "Più Recenti"
        ClientSortOrder.CREATED_OLDEST -> "Meno Recenti"
        ClientSortOrder.FACILITIES_COUNT -> "Stabilimenti"
        ClientSortOrder.CHECKUPS_COUNT -> "Check-up"
    }
}