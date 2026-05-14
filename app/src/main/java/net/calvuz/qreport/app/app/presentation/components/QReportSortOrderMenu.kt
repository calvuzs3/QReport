package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

@Composable
fun <T> QReportSortOrderMenu(
    expanded: Boolean,
    entries: List<T>,
    selectedSortOrder: T,
    onSortOrderSelected: (T) -> Unit,
    onDismiss: () -> Unit
) where T : QReportSortOrder {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        entries.forEach { filter ->
            DropdownMenuItem(
                text = { Text(filter.getDisplayName()) },
                onClick = { onSortOrderSelected(filter); onDismiss() },
                leadingIcon = if (selectedSortOrder == filter) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}