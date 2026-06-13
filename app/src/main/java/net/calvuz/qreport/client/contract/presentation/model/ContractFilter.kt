package net.calvuz.qreport.client.contract.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.error.presentation.UiText

/** ContractListScreen Filter */
enum class ContractFilter(val labelResId: Int) : QReportFilter {
    ALL(R.string.contracts_list_filter_all),
    ACTIVE(R.string.contracts_list_filter_active),
    INACTIVE(R.string.contracts_list_filter_inactive),
    OUTDATED(R.string.contracts_list_filter_outdated);

    override fun getDisplayName(): UiText = when (this) {
        ALL -> UiText.StringResources(R.string.contracts_list_filter_all)
        ACTIVE -> UiText.StringResources(R.string.contracts_list_filter_active)
        INACTIVE -> UiText.StringResources(R.string.contracts_list_filter_inactive)
        OUTDATED -> UiText.StringResources(R.string.contracts_list_filter_outdated)
    }
}