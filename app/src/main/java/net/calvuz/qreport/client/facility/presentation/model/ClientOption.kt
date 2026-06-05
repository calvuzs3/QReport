package net.calvuz.qreport.client.facility.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.error.presentation.UiText

/**
 * Wrapper for Client data used in filter dropdowns.
 * Implements [net.calvuz.qreport.app.app.presentation.model.QReportFilter] so it can be used with [QReportSelectorRow].
 *
 * [labelResId] is non-null only for sentinel values (e.g. ALL).
 * The UI resolves the label via stringResource() when set, otherwise
 * falls back to [companyName].
 */
data class ClientOption(
    val id: String,
    val companyName: String,
    val labelResId: Int? = null
) : QReportFilter {

    override fun getDisplayName(): UiText = when (companyName.isNullOrBlank()) {
        true -> UiText.StringResources(labelResId!!)
        false -> UiText.DynStr(companyName)
    }

    companion object {
        val ALL = ClientOption(
            id = "",
            companyName = "",
            labelResId = R.string.client_option_all
        )
    }
}