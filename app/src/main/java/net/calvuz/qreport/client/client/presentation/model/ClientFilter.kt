package net.calvuz.qreport.client.client.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter

enum class ClientFilter(val labelResId: Int) : QReportFilter {
    ALL(R.string.client_filter_all),
    ACTIVE(R.string.client_filter_active),
    INACTIVE(R.string.client_filter_inactive),
    WITH_FACILITIES(R.string.client_filter_with_facilities),
    WITH_ISLANDS(R.string.client_filter_with_islands),
    WITH_CONTACTS(R.string.client_filter_with_contacts),
    WITH_CONTRACTS(R.string.client_filter_with_contracts);

    override fun getDisplayName(): String = name // fallback; UI uses labelResId
}