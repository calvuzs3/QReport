package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.app.app.presentation.model.QReportFilter

/**
 * Self-contained selector row for any [QReportFilter] type.
 *
 * Combines a tappable trigger card with [QReportSelectorMenu], managing the
 * expanded state internally. The caller only needs to provide the data and
 * react to selection — no [showMenu] variable needed in the screen.
 *
 * Usage example:
 * ```
 * QReportSelectorRow(
 *     entries = uiState.availableClients,
 *     selectedItem = uiState.selectedClient,
 *     onItemSelected = viewModel::updateSelectedClient,
 *     icon = Icons.Default.Business,
 *     modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
 * )
 * ```
 *
 * @param entries   Full list of options shown in the dropdown.
 * @param selectedItem  Currently selected option (shown in the trigger row).
 * @param onItemSelected Called when the user picks an entry.
 * @param modifier  Applied to the outer [Box].
 * @param icon      Leading icon in the trigger row. Defaults to [Icons.Default.FilterList].
 */
@Composable
fun <T : QReportFilter> QReportSelectorRow(
    entries: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.FilterList
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {

        // Trigger card — tapping opens the dropdown
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedItem.getDisplayName(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }

        // Dropdown anchored to the card above
        QReportSelectorMenu(
            expanded = expanded,
            entries = entries,
            selectedFilter = selectedItem,
            onFilterSelected = {
                onItemSelected(it)
                expanded = false
            },
            onDismiss = { expanded = false }
        )
    }
}

