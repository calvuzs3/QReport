package net.calvuz.qreport.client.island.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.error.presentation.UiText

/**
 * Wrapper for Facility data used in the island list selector dropdown.
 * Implements [net.calvuz.qreport.app.app.presentation.model.QReportFilter] so it works with [QReportSelectorRow].
 *
 * [labelResId] is resolved at runtime via stringResource() in the composable,
 * following the same pattern as [FacilityDetailTab] and [ClientDetailTab].
 *
 * @param id    Facility ID, empty string for the ALL sentinel.
 * @param name  Display name for real facilities (ignored for ALL sentinel).
 */
data class FacilityOption(
    val id: String,
    val name: String,
    val labelResId: Int? = null   // non-null only for sentinel values
) : QReportFilter {

    // For real facilities the name comes from DB; for sentinels it is resolved via labelResId.
    override fun getDisplayName(): UiText = when (name.isNullOrBlank()) {
        true -> UiText.StringResources(labelResId!!)
        false -> UiText.DynStr(name)
    }

    companion object {
        /** Sentinel shown before a facility is selected, or when all are in scope. */
        val ALL = FacilityOption(
            id = "",
            name = "",                              // resolved via labelResId in UI
            labelResId = R.string.facility_option_all
        )
    }
}