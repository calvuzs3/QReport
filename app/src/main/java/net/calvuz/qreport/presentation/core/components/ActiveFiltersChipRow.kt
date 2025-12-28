package net.calvuz.qreport.presentation.core.components

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
import androidx.compose.ui.unit.dp

@Composable
fun ActiveFiltersChipRow(
    modifier: Modifier = Modifier,
    selectedFilter: String? = null,
    avoidFilter: String? = null,
    selectedSort: String? = null,
    avoidSort: String? = null,
    onClearFilter: () -> Unit,
    onClearSort: () -> Unit,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        if (selectedFilter != null) {
            if (avoidFilter != null && avoidFilter != selectedFilter) {
                item {
                    FilterChip(
                        selected = true,
                        onClick = onClearFilter,
                        label = { Text("Filtro: $selectedFilter") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Rimuovi filtro",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        if (selectedSort != null) {
            if (avoidSort != null && avoidSort != selectedSort) {
                item {
                    FilterChip(
                        selected = true,
                        onClick = onClearSort,
                        label = { Text("Ordine: $selectedSort") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Rimuovi ordinamento",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}
