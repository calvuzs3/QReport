package net.calvuz.qreport.client.unit.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

enum class UnitSortOrder: QReportSortOrder {
    CREATED_RECENT,    NAME,    BY_TYPE,    BY_SERIAL;

    override fun getDisplayName(): String = when (this) {
        CREATED_RECENT->"Recenti"
        NAME->"Nome custom"
        BY_TYPE -> "Per Tipo"
        BY_SERIAL -> "Per Seriale"
    }
}
