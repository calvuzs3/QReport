package net.calvuz.qreport.client.island.presentation.ui.components

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter

/**
 * Wrapper for Island data used in selector dropdowns.
 * Implements [QReportFilter] so it works with [QReportSelectorRow].
 *
 * [labelResId] is non-null only for sentinel values (e.g. ALL).
 * The UI checks [labelResId] first and uses stringResource() if set,
 * otherwise falls back to [name].
 */
data class IslandOption(
    val id: String,
    val name: String,
    val labelResId: Int? = null
) : QReportFilter {
    override fun getDisplayName(): String = name

    companion object {
        val ALL = IslandOption(
            id = "",
            name = "",
            labelResId = R.string.island_option_all
        )
    }
}