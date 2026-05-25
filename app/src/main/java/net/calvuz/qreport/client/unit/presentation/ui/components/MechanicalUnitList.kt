package net.calvuz.qreport.client.unit.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.settings.domain.model.ListViewMode

@Composable
fun MechanicalUnitListContent(
    units: List<MechanicalUnit>,
    variant: ListViewMode,
    onUnitClick: (String) -> Unit,
    onUnitDelete: (unitId: String, unitName: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = units,
            key = { it.id }
        ) { unit ->
            MechanicalUnitCard(
                unit = unit,
                onClick = { onUnitClick(unit.id) },
                onEdit = { onUnitClick(unit.id) },
                onDelete = { onUnitDelete(unit.id, unit.name) },
                variant = variant
            )
        }
    }
}