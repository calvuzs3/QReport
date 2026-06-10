package net.calvuz.qreport.client.contract.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder
import net.calvuz.qreport.app.error.presentation.UiText

/** ContractListScreen SortOrder */
enum class ContractSortOrder(val labelResId: Int): QReportSortOrder {
    EXPIRE_RECENT(R.string.contracts_list_sort_expire_recent),
    EXPIRE_OLDEST(R.string.contracts_list_sort_expire_oldest),
    NAME(R.string.contracts_list_sort_name);

    override fun getDisplayName(): UiText = when (this) {
        EXPIRE_RECENT -> UiText.StringResources(R.string.contracts_list_sort_expire_recent)
        EXPIRE_OLDEST -> UiText.StringResources(R.string.contracts_list_sort_expire_oldest)
        NAME -> UiText.StringResources(R.string.contracts_list_sort_name)
    }
}
