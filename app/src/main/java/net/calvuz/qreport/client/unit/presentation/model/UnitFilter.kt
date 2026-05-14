package net.calvuz.qreport.client.unit.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportFilter

enum class UnitFilter : QReportFilter {
    ALL, ACTIVE, INACTIVE;

    override fun getDisplayName(): String = when (this) {
        ALL -> "Tutti"
        ACTIVE -> "Attivi"
        INACTIVE -> "Inattivi"
    }
}