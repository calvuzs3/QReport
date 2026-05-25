package net.calvuz.qreport.client.unit.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportFilter

enum class MechanicalUnitFilter(private val label: String) : QReportFilter {
    ALL("Tutte"),
    ACTIVE("Attive"),
    INACTIVE("Non attive"),
    ROBOT("Solo Robot");

    override fun getDisplayName(): String = label
}
