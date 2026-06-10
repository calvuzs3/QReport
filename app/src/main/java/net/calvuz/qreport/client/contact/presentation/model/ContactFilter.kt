package net.calvuz.qreport.client.contact.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.error.presentation.UiText

/** ContactListScreen Filter */
enum class ContactFilter(val labelResId: Int) : QReportFilter {
    ACTIVE(R.string.contacts_list_filter_active), INACTIVE(R.string.contacts_list_filter_inactive), PRIMARY_ONLY(
        R.string.contacts_list_filter_primary_only
    ),
    ALL(R.string.contacts_list_filter_all);

    override fun getDisplayName(): UiText = when (this) {
        ACTIVE -> UiText.StringResources(R.string.contacts_list_filter_active)
        INACTIVE -> UiText.StringResources(R.string.contacts_list_filter_inactive)
        PRIMARY_ONLY -> UiText.StringResources(R.string.contacts_list_filter_primary_only)
        ALL -> UiText.StringResources(R.string.contacts_list_filter_all)
    }
}