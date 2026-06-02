package net.calvuz.qreport.client.facility.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.client.facility.domain.model.FacilityType
import net.calvuz.qreport.client.facility.presentation.model.icon

/**
 * Filter chip for [FacilityType] — used in the facility list filter row.
 *
 * Mirrors the island type chip pattern so both lists are visually consistent.
 *
 * Usage in a filter row:
 * ```
 * FacilityType.entries.forEach { type ->
 *     FacilityTypeChip(
 *         type = type,
 *         selected = selectedType == type,
 *         onSelected = { onTypeSelected(type) }
 *     )
 * }
 * ```
 */
@Composable
fun FacilityTypeChip(
    type: FacilityType,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onSelected,
        label = { Text(stringResource(type.labelResId), style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(
                imageVector = type.icon(),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = modifier
    )
}