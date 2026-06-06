@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.client.island.maintenance.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportPullToRefresh
import net.calvuz.qreport.app.app.presentation.ui.theme.onSuccessContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.onWarningContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.successContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.warningContainer
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.client.island.maintenance.domain.model.IslandHealthSummary
import net.calvuz.qreport.client.island.maintenance.domain.model.MaintenanceOutcome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandHealthScreen(
    modifier: Modifier = Modifier,
    islandId: String,
    onNavigateBack: () -> Unit,
    viewModel: IslandHealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(islandId) { viewModel.initialize(islandId) }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.maint_screen_health_title))
                    if (uiState.islandName.isNotBlank()) {
                        Text(
                            text = uiState.islandName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.island_detail_action_back)
                    )
                }
            },
            actions = {
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.island_detail_action_more)
                    )
                }
            }
        )

        QReportPullToRefresh(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading && !uiState.hasData ->
                    LoadingState(modifier = Modifier.fillMaxSize())

                uiState.hasData ->
                    IslandHealthContent(
                        summary = uiState.summary!!,
                        modifier = Modifier.fillMaxSize()
                    )

                else -> {
                    // Empty — no logs yet
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.maint_health_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// MAIN CONTENT
// =============================================================================

@Composable
private fun IslandHealthContent(
    summary: IslandHealthSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryHeaderCard(summary)
        PredictionCard(summary)
        HealthIndicatorsCard(summary)
        OperationBreakdownCard(summary)
        if (summary.recurrentComponents.isNotEmpty()) {
            RecurrentComponentsCard(summary)
        }
        summary.emergenciesAfterLastRevamping?.let {
            PostRevampingCard(summary)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// =============================================================================
// CARDS
// =============================================================================

@Composable
private fun SummaryHeaderCard(summary: IslandHealthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.maint_health_total_logs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.totalLogs.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(horizontalAlignment = Alignment.End) {
                    summary.lastLogDate?.let {
                        Text(
                            text = stringResource(R.string.maint_health_last_intervention),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = it.toItalianDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    summary.lastOutcome?.let {
                        OutcomeChip(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionCard(summary: IslandHealthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.maint_health_predicted_next),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()

            if (summary.avgDaysBetweenInterventions != null) {
                HealthDetailRow(
                    label = stringResource(R.string.maint_health_avg_interval),
                    value = stringResource(
                        R.string.maint_health_avg_interval_value,
                        summary.avgDaysBetweenInterventions
                    )
                )
            }

            HealthDetailRow(
                label = stringResource(R.string.maint_health_predicted_next),
                value = summary.predictedNextInterventionDate?.toItalianDate()
                    ?: stringResource(R.string.maint_health_predicted_next_na)
            )

            summary.avgDurationMinutes?.let {
                HealthDetailRow(
                    label = stringResource(R.string.maint_health_avg_duration),
                    value = stringResource(R.string.maint_health_avg_duration_value, it)
                )
            }
        }
    }
}

@Composable
private fun HealthIndicatorsCard(summary: IslandHealthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.island_performance_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()

            // Emergency rate bar
            RateIndicator(
                label = stringResource(R.string.maint_health_emergency_rate),
                rate = summary.emergencyRate,
                highIsGood = false
            )

            // Deferred rate bar
            RateIndicator(
                label = stringResource(R.string.maint_health_deferred_rate),
                rate = summary.deferredRate,
                highIsGood = false
            )
        }
    }
}

@Composable
private fun RateIndicator(
    label: String,
    rate: Float,
    highIsGood: Boolean
) {
    val (barColor, trackColor) = when {
        highIsGood && rate >= 0.8f ->
            MaterialTheme.colorScheme.successContainer to MaterialTheme.colorScheme.onSuccessContainer
        highIsGood && rate >= 0.5f ->
            MaterialTheme.colorScheme.warningContainer to MaterialTheme.colorScheme.onWarningContainer
        !highIsGood && rate <= 0.1f ->
            MaterialTheme.colorScheme.successContainer to MaterialTheme.colorScheme.onSuccessContainer
        !highIsGood && rate <= 0.3f ->
            MaterialTheme.colorScheme.warningContainer to MaterialTheme.colorScheme.onWarningContainer
        else ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "%.0f%%".format(rate * 100),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        LinearProgressIndicator(
            progress = { rate },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = barColor,
            trackColor = trackColor.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun OperationBreakdownCard(summary: IslandHealthSummary) {
    if (summary.logsByOperationType.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.maint_health_top_operations),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()

            val maxCount = summary.logsByOperationType.values.maxOrNull()?.toFloat() ?: 1f

            summary.logsByOperationType.entries
                .sortedByDescending { it.value }
                .take(6)    // Show top 6 to avoid an excessively long card
                .forEach { (type, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(type.labelResId),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(120.dp)
                        )
                        LinearProgressIndicator(
                            progress = { count / maxCount },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                    }
                }
        }
    }
}

@Composable
private fun RecurrentComponentsCard(summary: IslandHealthSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.warningContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.maint_health_recurrent_components),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onWarningContainer
            )
            HorizontalDivider()
            summary.recurrentComponents.forEach { component ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.warningContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Text(
                        text = component,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PostRevampingCard(summary: IslandHealthSummary) {
    val count = summary.emergenciesAfterLastRevamping ?: return
    val isGood = count <= 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGood)
                MaterialTheme.colorScheme.successContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.maint_health_post_revamping),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.maint_health_emergencies_after_revamping, count),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isGood)
                    MaterialTheme.colorScheme.onSuccessContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// =============================================================================
// SHARED HELPERS
// =============================================================================

@Composable
private fun HealthDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OutcomeChip(outcome: MaintenanceOutcome) {
    val (containerColor, contentColor) = when (outcome) {
        MaintenanceOutcome.COMPLETED ->
            MaterialTheme.colorScheme.successContainer to MaterialTheme.colorScheme.onSuccessContainer
        MaintenanceOutcome.PARTIAL ->
            MaterialTheme.colorScheme.warningContainer to MaterialTheme.colorScheme.onWarningContainer
        MaintenanceOutcome.DEFERRED ->
            MaterialTheme.colorScheme.warningContainer to MaterialTheme.colorScheme.onWarningContainer
        MaintenanceOutcome.REQUIRES_PARTS ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(color = containerColor, shape = MaterialTheme.shapes.small) {
        Text(
            text = stringResource(outcome.labelResId),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}