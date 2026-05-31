package net.calvuz.qreport.client.facility.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportErrorState
import net.calvuz.qreport.app.util.SizeUtils.getFormattedCycleCount
import net.calvuz.qreport.app.util.SizeUtils.getFormattedHours
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandOperationalStatus
import net.calvuz.qreport.client.island.domain.usecase.FacilityOperationalSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityDetailScreen(
    modifier: Modifier = Modifier,
    facilityId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToCreateIsland: (String) -> Unit,
    onNavigateToEditIsland: (String) -> Unit,
    onNavigateToIslandDetail: (String) -> Unit,
    onNavigateToIslandsList: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: FacilityDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            viewModel.resetDeleteState()
            onDeleted()
        }
    }

    LaunchedEffect(facilityId) {
        viewModel.loadFacilityDetails(facilityId)
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirmation,
            title = { Text(stringResource(R.string.facility_detail_delete_dialog_title)) },
            text = { Text(stringResource(R.string.facility_detail_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteFacility,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.facility_detail_delete_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirmation) {
                    Text(stringResource(R.string.facility_detail_delete_dialog_cancel))
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Text(
                    text = uiState.facilityName.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.facility_detail_title_fallback),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.facility_detail_action_back)
                    )
                }
            },
            actions = {
                if (uiState.hasData) {
                    IconButton(
                        onClick = viewModel::showDeleteConfirmation,
                        enabled = !uiState.isDeleting
                    ) {
                        if (uiState.isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                tint = MaterialTheme.colorScheme.error,
                                contentDescription = stringResource(R.string.facility_detail_action_delete)
                            )
                        }
                    }
                    IconButton(onClick = { onNavigateToEdit(facilityId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.facility_detail_action_edit)
                        )
                    }
                }
                IconButton(onClick = viewModel::refreshData) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.facility_detail_action_refresh)
                    )
                }
            }
        )

        when {
            uiState.isLoading -> LoadingState()

            uiState.error != null -> QReportErrorState(
                error = uiState.error!!,
                onRetry = viewModel::refreshData,
                onDismiss = viewModel::dismissError
            )

            uiState.hasData -> FacilityDetailContent(
                uiState = uiState,
                onTabSelected = viewModel::selectTab,
                onIslandFilterSelected = viewModel::updateIslandFilter,
                onIslandClick = onNavigateToIslandDetail,
                onEdit = { onNavigateToEdit(facilityId) },
                onViewAll = { onNavigateToIslandsList(facilityId) },
                onCreateIsland = { onNavigateToCreateIsland(facilityId) },
                onEditIsland = onNavigateToEditIsland,
                onDeleteIsland = viewModel::deleteIsland,
                onMarkMaintenanceComplete = viewModel::markMaintenanceComplete
            )

            else -> {
                val (title, message) = when {
                    uiState.selectedIslandFilter != IslandFilter.ALL ->
                        stringResource(R.string.facility_detail_empty_filtered_title) to
                                stringResource(R.string.facility_detail_empty_filtered_message,
                                    stringResource(uiState.selectedIslandFilter.labelResId()))
                    else ->
                        stringResource(R.string.facility_detail_empty_title) to
                                stringResource(R.string.facility_detail_empty_message)
                }
                EmptyState(
                    textTitle = title,
                    textMessage = message,
                    iconImageVector = Icons.Outlined.PrecisionManufacturing,
                    iconContentDescription = stringResource(R.string.facility_detail_empty_icon_description),
                    iconActionImageVector = Icons.Default.Add,
                    iconActionContentDescription = stringResource(R.string.facility_detail_fab_new_island),
                    textAction = stringResource(R.string.facility_detail_empty_action),
                    onAction = { onNavigateToCreateIsland(facilityId) }
                )
            }
        }
    }
}

// =============================================================================
// DETAIL CONTENT
// =============================================================================

@Composable
private fun FacilityDetailContent(
    uiState: FacilityDetailUiState,
    onTabSelected: (FacilityDetailTab) -> Unit,
    onIslandFilterSelected: (IslandFilter) -> Unit,
    onEdit: (String) -> Unit,
    onIslandClick: (String) -> Unit,
    onViewAll: () -> Unit,
    onCreateIsland: () -> Unit,
    onEditIsland: (String) -> Unit,
    onDeleteIsland: (String) -> Unit,
    onMarkMaintenanceComplete: (String) -> Unit
) {
    Column {
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FacilityDetailTab.entries.forEach { tab ->
                val count = when (tab) {
                    FacilityDetailTab.INFO -> null
                    FacilityDetailTab.ISLANDS -> uiState.islandsCount.takeIf { it > 0 }
                    FacilityDetailTab.MAINTENANCE -> uiState.maintenanceIssuesCount.takeIf { it > 0 }
                }
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(stringResource(tab.labelResId))
                            count?.let {
                                Badge {
                                    Text(
                                        text = it.toString(),
                                        color = if (tab == FacilityDetailTab.MAINTENANCE && it > 0)
                                            MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        when (uiState.selectedTab) {
            FacilityDetailTab.INFO -> InfoTabContent(
                facility = uiState.facility!!,
                operationalSummary = uiState.operationalSummary,
                modifier = Modifier.weight(1f),
                onEdit = onEdit
            )
            FacilityDetailTab.ISLANDS -> IslandsTabContent(
                islands = uiState.filteredIslands,
                allIslands = uiState.islands,
                selectedFilter = uiState.selectedIslandFilter,
                onFilterSelected = onIslandFilterSelected,
                onIslandClick = onIslandClick,
                onViewAll = onViewAll,
                onCreateIsland = onCreateIsland,
                onEditIsland = onEditIsland,
                onDeleteIsland = onDeleteIsland,
                modifier = Modifier.weight(1f)
            )
            FacilityDetailTab.MAINTENANCE -> MaintenanceTabContent(
                islandsNeedingMaintenance = uiState.islandsNeedingMaintenance,
                islandsUnderWarranty = uiState.islandsUnderWarranty,
                onMarkMaintenanceComplete = onMarkMaintenanceComplete,
                onIslandClick = onIslandClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =============================================================================
// TAB: INFO
// =============================================================================

@Composable
private fun InfoTabContent(
    facility: Facility,
    operationalSummary: FacilityOperationalSummary?,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.facility_detail_tab_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = { onEdit(facility.id) }) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.facility_detail_action_edit))
            }
        }

        LazyColumn(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoCard(title = stringResource(R.string.facility_detail_info_card_general), icon = Icons.Default.Business) {
                    InfoItem(stringResource(R.string.facility_detail_info_field_name), facility.displayName)
                    facility.code?.let { InfoItem(stringResource(R.string.facility_detail_info_field_code), it) }
                    InfoItem(stringResource(R.string.facility_detail_info_field_type), stringResource(facility.facilityType.labelResId))
                    facility.notes?.let { InfoItem(stringResource(R.string.facility_detail_info_field_notes), it) }
                    InfoItem(
                        stringResource(R.string.facility_detail_info_field_primary),
                        stringResource(if (facility.isPrimary) R.string.facility_detail_info_primary_yes else R.string.facility_detail_info_primary_no)
                    )
                }
            }
            item {
                InfoCard(title = stringResource(R.string.facility_detail_info_card_address), icon = Icons.Default.LocationOn) {
                    facility.address?.let {
                        InfoItem(stringResource(R.string.facility_detail_info_field_address), it.toDisplayString())
                    }
                }
            }
            item {
                InfoCard(title = stringResource(R.string.facility_detail_info_card_metadata), icon = Icons.Default.Info) {
                    InfoItem(stringResource(R.string.facility_detail_info_field_created), formatTimestamp(facility.createdAt))
                    InfoItem(stringResource(R.string.facility_detail_info_field_updated), formatTimestamp(facility.updatedAt))
                    InfoItem(
                        stringResource(R.string.facility_detail_info_field_status),
                        stringResource(if (facility.isActive) R.string.facility_detail_info_status_active else R.string.facility_detail_info_status_inactive)
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB: ISLANDS
// =============================================================================

@Composable
private fun IslandsTabContent(
    islands: List<Island>,
    allIslands: List<Island>,
    selectedFilter: IslandFilter,
    onFilterSelected: (IslandFilter) -> Unit,
    onIslandClick: (String) -> Unit,
    onViewAll: () -> Unit,
    onCreateIsland: () -> Unit,
    onEditIsland: (String) -> Unit,
    onDeleteIsland: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Isole (${islands.size}/${allIslands.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (islands.isNotEmpty()) {
                    OutlinedButton(onClick = onViewAll) {
                        Text(stringResource(R.string.facility_detail_empty_action))
                    }
                }
                Button(onClick = onCreateIsland) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.facility_detail_fab_new_island))
                }
            }
        }

        if (islands.isEmpty()) {
            EmptyState(
                textTitle = stringResource(R.string.facility_detail_empty_title),
                textMessage = stringResource(R.string.facility_detail_empty_message),
                iconImageVector = Icons.Outlined.PrecisionManufacturing,
                iconContentDescription = stringResource(R.string.facility_detail_empty_icon_description),
                iconActionImageVector = Icons.Default.Add,
                iconActionContentDescription = stringResource(R.string.facility_detail_fab_new_island),
                textAction = stringResource(R.string.facility_detail_empty_action),
                onAction = onCreateIsland
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = islands, key = { it.id }) { island ->
                    IslandItem(
                        island = island,
                        onIslandClick = onIslandClick,
                        onEditIsland = onEditIsland,
                        onDeleteIsland = onDeleteIsland
                    )
                }
            }
        }
    }
}

@Composable
private fun IslandItem(
    island: Island,
    onIslandClick: (String) -> Unit,
    onEditIsland: (String) -> Unit,
    onDeleteIsland: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), onClick = { onIslandClick(island.id) }) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(island.islandType.labelResId) ,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = island.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(text = "S/N: ${island.serialNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (island.operatingHours > 0 || island.cycleCount > 0) {
                    Text(
                        text = "${island.operatingHours.getFormattedHours()} • ${island.cycleCount.getFormattedCycleCount()} cicli",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (island.islandOperationalStatus) {
                        IslandOperationalStatus.OPERATIONAL -> Icons.Default.CheckCircle
                        IslandOperationalStatus.MAINTENANCE_DUE -> Icons.Default.Warning
                        IslandOperationalStatus.INACTIVE -> Icons.Default.Cancel
                    },
                    contentDescription = null,
                    tint = when (island.islandOperationalStatus) {
                        IslandOperationalStatus.OPERATIONAL -> MaterialTheme.colorScheme.primary
                        IslandOperationalStatus.MAINTENANCE_DUE -> MaterialTheme.colorScheme.error
                        IslandOperationalStatus.INACTIVE -> MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(20.dp)
                )
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.island_delete_dialog_confirm), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = { onEditIsland(island.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.facility_detail_action_edit), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showDeleteDialog) {
        IslandDeleteDialog(
            islandName = island.displayName,
            onConfirm = { onDeleteIsland(island.id); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// =============================================================================
// TAB: MAINTENANCE
// =============================================================================

@Composable
private fun MaintenanceTabContent(
    islandsNeedingMaintenance: List<Island>,
    islandsUnderWarranty: List<Island>,
    onMarkMaintenanceComplete: (String) -> Unit,
    onIslandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("${stringResource(R.string.island_filter_needs_maintenance)} (${islandsNeedingMaintenance.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (islandsNeedingMaintenance.isEmpty()) {
                        Text("Tutte le isole sono in regola", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        islandsNeedingMaintenance.forEach { island ->
                            MaintenanceIslandItem(island = island, onMarkComplete = onMarkMaintenanceComplete, onIslandClick = onIslandClick)
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("${stringResource(R.string.island_filter_under_warranty)} (${islandsUnderWarranty.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (islandsUnderWarranty.isEmpty()) {
                        Text("Nessuna isola attualmente in garanzia", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        islandsUnderWarranty.forEach { island ->
                            WarrantyIslandItem(island = island, onIslandClick = onIslandClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaintenanceIslandItem(island: Island, onMarkComplete: (String) -> Unit, onIslandClick: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(island.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    text = island.nextScheduledMaintenance?.let {
                        stringResource(R.string.facility_detail_maintenance_expiry, formatTimestamp(it))
                    } ?: stringResource(R.string.facility_detail_maintenance_not_scheduled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = { onMarkComplete(island.id) }) {
                    Text(stringResource(R.string.facility_detail_maintenance_complete))
                }
                IconButton(onClick = { onIslandClick(island.id) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun WarrantyIslandItem(island: Island, onIslandClick: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), onClick = { onIslandClick(island.id) }, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(island.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    text = island.warrantyExpiration?.let {
                        stringResource(R.string.facility_detail_warranty_expiry, formatTimestamp(it))
                    } ?: stringResource(R.string.facility_detail_warranty_not_set),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

// =============================================================================
// DIALOGS
// =============================================================================

@Composable
private fun IslandDeleteDialog(islandName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.island_delete_dialog_title)) },
        text = { Text(stringResource(R.string.island_delete_dialog_message, islandName)) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.island_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.island_delete_dialog_cancel)) }
        }
    )
}

// =============================================================================
// SHARED COMPOSABLES
// =============================================================================

@Composable
private fun InfoCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
        Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// =============================================================================
// HELPERS
// =============================================================================

@Composable
private fun formatTimestamp(timestamp: Instant): String {
    val diffMillis = (Clock.System.now() - timestamp).inWholeMilliseconds
    return when {
        diffMillis < 60_000L -> stringResource(R.string.facility_time_now)
        diffMillis < 3_600_000L -> stringResource(R.string.facility_time_minutes_ago, diffMillis / 60_000)
        diffMillis < 86_400_000L -> stringResource(R.string.facility_time_hours_ago, diffMillis / 3_600_000)
        else -> stringResource(R.string.facility_time_days_ago, diffMillis / 86_400_000)
    }
}