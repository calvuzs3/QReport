package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.presentation.ui.components.MechanicalUnitCard
import net.calvuz.qreport.settings.domain.model.ListViewMode

/**
 * Tab content showing the mechanical units of an island.
 *
 * Uses [MechanicalUnitCard] (COMPACT variant) so the appearance is consistent
 * with the standalone unit list screen. Delete confirmation is handled inside
 * the card — no local pending-delete state needed here.
 */
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
    Column(modifier = modifier.fillMaxSize()) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

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
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 16.dp, bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(units, key = { it.id }) { unit ->
                        MechanicalUnitCard(
                            unit = unit,
                            variant = ListViewMode.COMPACT,
                            onClick = { onEditUnit(unit.id) },
                            onEdit = { onEditUnit(unit.id) },
                            onDelete = { onDeleteUnit(unit) },
                            showActions = true
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
}