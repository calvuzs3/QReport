package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.model.QReportFilter
import net.calvuz.qreport.app.app.presentation.model.QReportSortOrder

@Composable
fun QReportFiltersChipRow(
    modifier: Modifier = Modifier,
    selectedFilter: QReportFilter,
    avoidFilter: QReportFilter,
    selectedSort: QReportSortOrder,
    avoidSort: QReportSortOrder,
    onClearFilter: () -> Unit,
    onClearSort: () -> Unit,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        if (avoidFilter != selectedFilter) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearFilter,
                    label = { Text("Filtro: ${selectedFilter.getDisplayName().asString()}") }, // 👈
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.filter_chip_row_remove_filter),
                            modifier = Modifier.size(18.dp))
                    }
                )
            }
        }

        if (avoidSort != selectedSort) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearSort,
                    label = { Text("Ordine: ${selectedSort.getDisplayName().asString()}") }, // 👈
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.filter_chip_row_remove_sort),
                            modifier = Modifier.size(18.dp))
                    }
                )
            }
        }
    }
}