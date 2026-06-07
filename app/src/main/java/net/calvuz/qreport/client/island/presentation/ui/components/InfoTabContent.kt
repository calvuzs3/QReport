package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.usecase.SingleIslandStatistics

@Composable
fun InfoTabContent(
    island: Island,
    statistics: SingleIslandStatistics?,
    error: UiText?,              // UiText instead of String
    onEdit: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {


        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Inline error card — dismissed without retrying (stats are supplementary)
            error?.let { uiError ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiError.asString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismissError) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_dismiss),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            item { IslandBasicInfoCard(island = island) }
            statistics?.let { item { OperationalStatsCard(statistics = it) } }
            item { WarrantyStatusCard(island = island, statistics = statistics) }
            item { PerformanceMetricsCard(statistics = statistics) }
        }
    }
}