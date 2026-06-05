package net.calvuz.qreport.client.client.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder
import net.calvuz.qreport.app.error.presentation.UiText

enum class ClientSortOrder(val labelResId: Int) : QReportSortOrder {
    COMPANY_NAME(R.string.client_sort_company_name),
    CREATED_RECENT(R.string.client_sort_created_recent),
    CREATED_OLDEST(R.string.client_sort_created_oldest),
    FACILITIES_COUNT(R.string.client_sort_facilities_count),
    CHECKUPS_COUNT(R.string.client_sort_checkups_count);

    override fun getDisplayName(): UiText = when (this) {
        COMPANY_NAME -> UiText.StringResources(R.string.client_sort_company_name)
        CREATED_RECENT -> UiText.StringResources(R.string.client_sort_created_recent)
        CREATED_OLDEST -> UiText.StringResources(R.string.client_sort_created_oldest)
        FACILITIES_COUNT -> UiText.StringResources(R.string.client_sort_facilities_count)
        CHECKUPS_COUNT -> UiText.StringResources(R.string.client_sort_checkups_count)
    }
}