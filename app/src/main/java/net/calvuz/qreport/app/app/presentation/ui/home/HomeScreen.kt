@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.app.app.presentation.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.ui.theme.onSuccessContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.onWarningContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.success
import net.calvuz.qreport.app.app.presentation.ui.theme.successContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.warningContainer
import net.calvuz.qreport.checkup.domain.model.CheckUp
import net.calvuz.qreport.checkup.presentation.components.CheckupStatusChip
import net.calvuz.qreport.checkup.presentation.model.CheckupPkg
import net.calvuz.qreport.client.client.presentation.model.ClientPkg
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.presentation.model.icon

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToClients: () -> Unit,
    onNavigateToCheckUps: () -> Unit,
    // facilityId="" loads all islands — IslandListScreen handles blank id as "no filter"
    onNavigateToIslands: () -> Unit,
    onNavigateToTechnicalInterventions: () -> Unit,
    onNavigateToNewCheckUp: () -> Unit,
    onNavigateToCheckUpDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(uiState.selectedCheckUpId) {
        uiState.selectedCheckUpId?.let { viewModel.navigateToCheckUp(it); viewModel.clearSelectedCheckUp() }
    }

    Column(modifier = modifier.fillMaxSize()) {

        HomeHeader(
            onRefresh = viewModel::refresh,
            isLoading = uiState.isLoading,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        HorizontalDivider()

        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── CHECK-UP ─────────────────────────────────────────────────
                item {
                    DashboardSectionCard(
                        tileTitle = stringResource(R.string.home_section_checkup),
                        tileIcon = CheckupPkg.icon,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onTileClick = onNavigateToCheckUps,
                        chips = {
                            val stats = uiState.checkupStats
                            if (stats != null) {
                                StatChip(stats.totalCheckUps.toString(), stringResource(R.string.home_checkup_stat_total), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                                StatChip(stats.activeCheckUps.toString(), stringResource(R.string.home_checkup_stat_active), MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
                                StatChip(stats.completedThisWeek.toString(), stringResource(R.string.home_checkup_stat_week), MaterialTheme.colorScheme.successContainer, MaterialTheme.colorScheme.onSuccessContainer)
                            }
                        }
                    ) {
                        if (uiState.recentCheckUps.isEmpty()) {
                            PreviewEmptyRow(stringResource(R.string.home_checkup_empty))
                        } else {
                            uiState.recentCheckUps.take(3).forEach { checkUp ->
                                CheckUpPreviewRow(checkUp = checkUp, onClick = { viewModel.navigateToCheckUp(checkUp.id) })
                            }
                        }
                    }
                }

                // ── CLIENTI ──────────────────────────────────────────────────
                item {
                    DashboardSectionCard(
                        tileTitle = stringResource(R.string.home_section_clients),
                        tileIcon = ClientPkg.icon,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onTileClick = onNavigateToClients,
                        chips = {
                            val stats = uiState.clientStats
                            if (stats != null) {
                                StatChip(stats.totalClient.toString(), stringResource(R.string.home_clients_stat_total), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                                StatChip(stats.activeClient.toString(), stringResource(R.string.home_clients_stat_active), MaterialTheme.colorScheme.successContainer, MaterialTheme.colorScheme.onSuccessContainer)
                            }
                        }
                    ) {
                        // No preview list for clients — the stats are sufficient at a glance
                        if (uiState.clientStats == null || uiState.clientStats.totalClient == 0) {
                            PreviewEmptyRow(stringResource(R.string.home_clients_empty))
                        }
                    }
                }

                // ── ISOLE ─────────────────────────────────────────────────────
                item {
                    val islandWarning = uiState.islandStats.maintenanceSoon > 0
                    DashboardSectionCard(
                        tileTitle = stringResource(R.string.home_section_islands),
                        tileIcon = Icons.Default.PrecisionManufacturing,
                        accentColor = if (islandWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.success,
                        onTileClick = onNavigateToIslands,
                        chips = {
                            with(uiState.islandStats) {
                                StatChip(total.toString(), stringResource(R.string.home_islands_stat_total), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                                StatChip(operational.toString(), stringResource(R.string.home_islands_stat_operational), MaterialTheme.colorScheme.successContainer, MaterialTheme.colorScheme.onSuccessContainer)
                                if (maintenanceSoon > 0) {
                                    StatChip(maintenanceSoon.toString(), stringResource(R.string.home_islands_stat_maintenance), MaterialTheme.colorScheme.warningContainer, MaterialTheme.colorScheme.onWarningContainer)
                                }
                            }
                        }
                    ) {
                        if (uiState.recentIslands.isEmpty()) {
                            PreviewEmptyRow(stringResource(R.string.home_islands_empty))
                        } else {
                            uiState.recentIslands.forEach { island ->
                                IslandPreviewRow(island = island, onClick = onNavigateToIslands)
                            }
                        }
                    }
                }

                // ── ACCESSO RAPIDO ────────────────────────────────────────────
                item {
                    Text(
                        text = stringResource(R.string.home_section_management),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NavTile(modifier = Modifier.weight(1f), title = stringResource(ClientPkg.titleResId), icon = ClientPkg.icon, onClick = onNavigateToClients, isHighlighted = true)
                        //NavTile(modifier = Modifier.weight(1f), title = stringResource(CheckupPkg.titleResId), icon = CheckupPkg.icon, onClick = onNavigateToCheckUps)
                        NavTile(modifier = Modifier.weight(1f), title = (CheckupPkg.title), icon = CheckupPkg.icon, onClick = onNavigateToCheckUps)
                        NavTile(modifier = Modifier.weight(1f), title = stringResource(R.string.home_nav_interventions_title), icon = Icons.Default.Workspaces, onClick = onNavigateToTechnicalInterventions)
                    }
                }
            }
        }
    }
}

// =============================================================================
// HEADER
// =============================================================================

@Composable
private fun HomeHeader(onRefresh: () -> Unit, isLoading: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = "QReport", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRefresh, enabled = !isLoading) {
            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.home_action_refresh))
        }
    }
}

// =============================================================================
// SECTION CARD — tile header + chips + preview list
// =============================================================================

/**
 * Dashboard section card — uniform appearance across all sections.
 *
 * The tile header always uses [MaterialTheme.colorScheme.surfaceContainerHigh]
 * so all sections look consistent in both light and dark theme.
 * [accentColor] is applied only to the icon, giving each section
 * its distinct identity without overwhelming color variation.
 *
 * ┌─────────────────────────────────────┐
 * │ [Icon  Title            Apri →    ] │  ← tile: surfaceContainerHigh
 * │  [chip][chip][chip]                 │
 * │ ─────────────────────────────────── │
 * │  preview row 1                      │
 * └─────────────────────────────────────┘
 */
@Composable
private fun DashboardSectionCard(
    tileTitle: String,
    tileIcon: ImageVector,
    accentColor: Color,            // used for icon tint only
    onTileClick: () -> Unit,
    chips: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val tileContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val tileContentColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Tile header ──────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                onClick = onTileClick,
                color = tileContainerColor,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(imageVector = tileIcon, contentDescription = null, modifier = Modifier.size(26.dp), tint = accentColor)
                        Text(text = tileTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tileContentColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.home_action_open), style = MaterialTheme.typography.labelMedium, color = tileContentColor.copy(alpha = 0.6f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = tileContentColor.copy(alpha = 0.6f))
                    }
                }
            }

            // ── Chips + preview ──────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { chips() }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
            }
        }
    }
}

// =============================================================================
// STAT CHIP
// =============================================================================

@Composable
private fun StatChip(value: String, label: String, containerColor: Color, contentColor: Color) {
    Surface(shape = MaterialTheme.shapes.small, color = containerColor) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = contentColor)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.85f))
        }
    }
}

// =============================================================================
// PREVIEW ROWS
// =============================================================================

@Composable
private fun PreviewEmptyRow(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun CheckUpPreviewRow(checkUp: CheckUp, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = checkUp.header.clientInfo.companyName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = checkUp.updatedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CheckupStatusChip(status = checkUp.status)
        }
    }
}

@Composable
private fun IslandPreviewRow(island: Island, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = onClick, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = island.islandType.icon(), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(text = island.customName ?: island.serialNumber , style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = stringResource(island.islandType.labelResId), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                imageVector = if (island.needsMaintenance()) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (island.needsMaintenance()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.success,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// =============================================================================
// NAV TILES (bottom row)
// =============================================================================

@Composable
private fun NavTile(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, isHighlighted: Boolean = false) {
    val containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Card(modifier = modifier.height(72.dp), colors = CardDefaults.cardColors(containerColor = containerColor), onClick = onClick) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(22.dp), tint = contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}