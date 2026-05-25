package net.calvuz.qreport.client.island.presentation.ui.components

import net.calvuz.qreport.app.app.presentation.model.QReportFilter

/**
 * Wrapper for Facility data used in the island list selector dropdown.
 * Implements [net.calvuz.qreport.app.app.presentation.model.QReportFilter] so it works with [QReportSelectorRow].
 *
 * @param id Facility ID, empty string for the "no facility selected" sentinel.
 * @param name Display name shown in the dropdown.
 */
data class IslandOption(
    val id: String,
    val name: String
) : QReportFilter {

    override fun getDisplayName(): String = name

    companion object {
        /** Shown before a facility is selected, or when no islands are scoped. */
        val ALL = IslandOption(id = "", name = "Tutte le Isole")
    }
}