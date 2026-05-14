package net.calvuz.qreport.client.client.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

enum class ClientSortOrder: QReportSortOrder {
    COMPANY_NAME, CREATED_RECENT, CREATED_OLDEST, FACILITIES_COUNT, CHECKUPS_COUNT;

    override fun getDisplayName(): String = when (this) {
        COMPANY_NAME -> "Nome Azienda"
        CREATED_RECENT -> "Più Recenti"
        CREATED_OLDEST -> "Meno Recenti"
        FACILITIES_COUNT -> "Stabilimenti"
        CHECKUPS_COUNT -> "Check-up"
    }
}
