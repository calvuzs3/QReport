package net.calvuz.qreport.client.contact.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.outlined.Contacts
import net.calvuz.qreport.R

object ContactPkg {
    val titleResId: Int = R.string.contact_pkg_title
    val descriptionResId: Int = R.string.contact_pkg_description
    val icon = Icons.Default.Contacts
    val iconUnselected = Icons.Outlined.Contacts
    val selectedFilter: ContactFilter = ContactFilter.ACTIVE
    val selectedSortOrder: ContactSortOrder = ContactSortOrder.CREATED_RECENT
}