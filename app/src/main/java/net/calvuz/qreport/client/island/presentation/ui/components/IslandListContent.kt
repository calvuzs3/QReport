package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.client.island.presentation.ui.IslandWithStats
import net.calvuz.qreport.settings.domain.model.ListViewMode

@Suppress("ParamsComparedByRef")
@Composable
fun IslandListContent(
    islands: List<IslandWithStats>,
    variant: ListViewMode,
    onIslandClick: (String) -> Unit,
    onIslandDelete: (String) -> Unit,
    onIslandRestore: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = islands, key = { it.island.id }) { islandWithStats ->
            IslandCard(
                island = islandWithStats.island,
                onClick = { onIslandClick(islandWithStats.island.id) },
                onDelete = if (islandWithStats.island.isActive) {
                    { onIslandDelete(islandWithStats.island.id) }
                } else null,
                onRestore = if (!islandWithStats.island.isActive) {
                    { onIslandRestore(islandWithStats.island.id) }
                } else null,
                variant = variant)
        }
    }
}