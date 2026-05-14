package net.calvuz.qreport.client.facility.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

enum class FacilitySortOrder: QReportSortOrder {
    NAME, CREATED_RECENT, CREATED_OLDEST, ISLANDS_COUNT, TYPE;

    override fun getDisplayName(): String = when( this) {
        NAME -> "Nome"
        CREATED_RECENT -> "Più Recenti"
        CREATED_OLDEST -> "Meno Recenti"
        ISLANDS_COUNT -> "Numero Isole"
        TYPE -> "Tipo"
    }
}