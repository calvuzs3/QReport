package net.calvuz.qreport.client.contact.presentation.model

import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder
import net.calvuz.qreport.app.error.presentation.UiText


/** ContactListScreen SortOrder */
enum class ContactSortOrder(val labelResId: Int): QReportSortOrder {
    CREATED_RECENT(R.string.contacts_list_sort_created_recent),
    CREATED_OLDEST(R.string.contacts_list_sort_created_oldest),
    NAME(R.string.contacts_list_sort_name);

    override fun getDisplayName(): UiText = when (this) {
        CREATED_RECENT -> UiText.StringResources(R.string.contacts_list_sort_created_recent)
        CREATED_OLDEST -> UiText.StringResources(R.string.contacts_list_sort_created_oldest)
        NAME -> UiText.StringResources(R.string.contacts_list_sort_name)
    }
}