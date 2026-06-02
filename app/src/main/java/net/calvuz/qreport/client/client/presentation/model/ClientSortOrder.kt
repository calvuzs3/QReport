package net.calvuz.qreport.client.client.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

enum class ClientSortOrder(val labelResId: Int) : QReportSortOrder {
    COMPANY_NAME(R.string.client_sort_company_name),
    CREATED_RECENT(R.string.client_sort_created_recent),
    CREATED_OLDEST(R.string.client_sort_created_oldest),
    FACILITIES_COUNT(R.string.client_sort_facilities_count),
    CHECKUPS_COUNT(R.string.client_sort_checkups_count);

    override fun getDisplayName(): String = name // fallback; UI uses labelResId
}