package net.calvuz.qreport.client.unit.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

enum class MechanicalUnitSortOrder(private val label: String) : QReportSortOrder {
    CREATED_RECENT("Recenti"),
    NAME("Nome custom"),
    BY_TYPE("Per Tipo"),
    BY_SERIAL("Per Seriale");

    override fun getDisplayName(): String = label
}

