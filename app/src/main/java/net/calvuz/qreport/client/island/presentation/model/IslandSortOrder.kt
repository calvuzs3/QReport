package net.calvuz.qreport.client.island.presentation.model

import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

enum class IslandSortOrder(private val label: String): QReportSortOrder {
    CREATED_RECENT("Recenti"),
    CUSTOM_NAME("Nome custom"),
    SERIAL_NUMBER("S/N"),
    TYPE("Tipo"),
    STATUS("Stato"),
    OPERATING_HOURS("Ore Operative"),
    MAINTENANCE_DATE("Data manutenzione");

    override fun getDisplayName(): String = label
}
