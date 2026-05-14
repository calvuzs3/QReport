package net.calvuz.qreport.client.island.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportFilter

enum class IslandFilter: QReportFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    MAINTENANCE_DUE,
    UNDER_WARRANTY,
    HIGH_OPERATING_HOURS,
    BY_TYPE;

    override fun getDisplayName(): String = when(this) {
        ALL -> "Tutti"
        ACTIVE->"Attivi"
        INACTIVE->"Inattivi"
        MAINTENANCE_DUE->"Rich.Manutenzione"
        UNDER_WARRANTY->"In garanzia"
        HIGH_OPERATING_HOURS->"Alte ORE funz."
        BY_TYPE->"Per tipo"
    }
}