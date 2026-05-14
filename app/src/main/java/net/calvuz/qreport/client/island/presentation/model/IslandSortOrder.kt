package net.calvuz.qreport.client.island.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

enum class IslandSortOrder: QReportSortOrder {
    CREATED_RECENT,
    CUSTOM_NAME,
    SERIAL_NUMBER,
    TYPE,
    STATUS,
    OPERATING_HOURS,
    MAINTENANCE_DATE;

    override fun getDisplayName(): String = when (this) {
        SERIAL_NUMBER->"S/N"
        TYPE->"Tipo"
        STATUS->"Stato"
        OPERATING_HOURS->"Ore Operative"
        MAINTENANCE_DATE->"Data manutenzione"
        CREATED_RECENT->"Recenti"
        CUSTOM_NAME->"Nome custom"
    }
}
