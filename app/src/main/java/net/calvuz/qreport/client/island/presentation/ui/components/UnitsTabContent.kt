package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.ConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit

@Composable
fun UnitsTabContent(
    units: List<MechanicalUnit>,
    isLoading: Boolean,
    onCreateUnit: () -> Unit,
    onEditUnit: (String) -> Unit,
    onDeleteUnit: (MechanicalUnit) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<MechanicalUnit?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.island_units_tab_title, units.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (units.isNotEmpty()) {
                    OutlinedButton(onClick = onViewAll) { Text(stringResource(R.string.island_units_view_all)) }
                }
                Button(onClick = onCreateUnit) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.island_units_new))
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                units.isEmpty() -> EmptyState(
                    textTitle = stringResource(R.string.island_units_empty_title),
                    textMessage = stringResource(R.string.island_units_empty_message),
                    iconImageVector = Icons.Outlined.PrecisionManufacturing,
                    iconContentDescription = stringResource(R.string.island_units_empty_icon_description),
                    iconActionImageVector = Icons.Default.Add,
                    iconActionContentDescription = stringResource(R.string.island_units_new),
                    textAction = stringResource(R.string.island_units_new),
                    onAction = onCreateUnit
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(units, key = { it.id }) { unit ->
                        MechanicalUnitListItem(
                            unit = unit,
                            onEditClick = { onEditUnit(unit.id) },
                            onDeleteClick = { pendingDelete = unit }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = onCreateUnit,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.island_units_new))
            }
        }
    }

    pendingDelete?.let { unit ->
        ConfirmDeleteDialog(
            objectName = stringResource(R.string.island_units_object_name),
            objectDesc = unit.name,
            onConfirm = { onDeleteUnit(unit); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun MechanicalUnitListItem(
    unit: MechanicalUnit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // unit.unitType.displayName — resolved at presentation layer
                Text(
                    text = unit.unitType.displayName,
                    //text = stringResource(unit.unitType.displayName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = unit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val subtitle = listOfNotNull(unit.serialNumber, unit.model).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                unit.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Row {
                IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit)) }
                IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}