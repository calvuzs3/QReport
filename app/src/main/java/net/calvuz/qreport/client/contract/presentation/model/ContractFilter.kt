package net.calvuz.qreport.client.contract.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.error.presentation.UiText

/** ContractListScreen Filter */
enum class ContractFilter(val labelResId: Int) : QReportFilter {
    ACTIVE(R.string.contracts_list_filter_active),
    INACTIVE(R.string.contracts_list_filter_inactive),
    ALL(R.string.contracts_list_filter_all);

    override fun getDisplayName(): UiText = when (this) {
        ACTIVE -> UiText.StringResources(R.string.contracts_list_filter_active)
        INACTIVE -> UiText.StringResources(R.string.contracts_list_filter_inactive)
        ALL -> UiText.StringResources(R.string.contracts_list_filter_all)
    }
}