package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.calvuz.qreport.app.app.presentation.model.QReportFilter

/**
 * Generic dropdown menu for any [QReportFilter] type.
 *
 * Meant to be anchored inside a [Box] alongside its trigger element.
 * For a self-contained trigger + dropdown, use [QReportSelectorRow] instead.
 *
 * Replaces the old QReportClientMenu.
 */
@Composable
fun <T> QReportSelectorMenu(
    expanded: Boolean,
    entries: List<T>,
    selectedFilter: T,
    onFilterSelected: (T) -> Unit,
    onDismiss: () -> Unit
) where T : QReportFilter {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        entries.forEach { entry ->
            DropdownMenuItem(
                text = { Text(entry.getDisplayName()) },
                onClick = { onFilterSelected(entry); onDismiss() },
                leadingIcon = if (selectedFilter == entry) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}
