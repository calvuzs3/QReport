package net.calvuz.qreport.client.unit.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.error.presentation.UiText

/**
 * Wrapper for Island data used in selector dropdowns.
 * Implements [net.calvuz.qreport.app.app.presentation.model.QReportFilter] so it works with [QReportSelectorRow].
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
    override fun getDisplayName(): UiText = when (name.isNullOrBlank()) {
        true -> UiText.StringResources(labelResId!!)
        false -> UiText.DynStr(name)
    }

    companion object {
        val ALL = IslandOption(
            id = "",
            name = "",
            labelResId = R.string.island_option_all
        )
    }
}
