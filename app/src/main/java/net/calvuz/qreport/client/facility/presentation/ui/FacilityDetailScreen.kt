package net.calvuz.qreport.client.facility.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import net.calvuz.qreport.app.app.presentation.components.QReportErrorState
import net.calvuz.qreport.app.app.presentation.components.QrLoadingState
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster
import net.calvuz.qreport.client.island.domain.usecase.FacilityOperationalSummary
import net.calvuz.qreport.client.island.presentation.model.IslandPkg
import net.calvuz.qreport.client.island.presentation.ui.components.IslandCard
import net.calvuz.qreport.settings.domain.model.ListViewMode

@Suppress("ParamsComparedByRef")
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
            viewModel.resetDeleteState(); onDeleted()
        }
    }
    LaunchedEffect(facilityId) { viewModel.loadFacilityDetails(facilityId) }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirmation,
            title = { Text(stringResource(R.string.facility_detail_delete_dialog_title)) },
            text = { Text(stringResource(R.string.facility_detail_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteFacility,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.facility_detail_delete_dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirmation) {
                    Text(stringResource(R.string.facility_detail_delete_dialog_cancel))
                }
            })
    }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(title = {
            Text(text = uiState.facilityName.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.facility_detail_title_fallback),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }, navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Default.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.facility_detail_action_back)
                )
            }
        }, actions = {
            if (uiState.hasData) {
                IconButton(
                    onClick = viewModel::showDeleteConfirmation, enabled = !uiState.isDeleting
                ) {
                    if (uiState.isDeleting) CircularProgressIndicator(
                        modifier = Modifier.size(
                            18.dp
                        )
                    )
                    else Icon(
                        Icons.Default.Delete,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = stringResource(R.string.facility_detail_action_delete)
                    )
                }
                IconButton(onClick = { onNavigateToEdit(facilityId) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.facility_detail_action_edit)
                    )
                }
            }
            IconButton(onClick = viewModel::refreshData) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.facility_detail_action_refresh)
                )
            }
        })

        when {
            uiState.isLoading -> QrLoadingState()
            uiState.error != null && !uiState.hasData -> QReportErrorState(
                error = uiState.error!!,
                onRetry = { viewModel.loadFacilityDetails(facilityId) },
                onDismiss = viewModel::dismissError
            )

            uiState.hasData -> FacilityDetailContent(
                uiState = uiState,
                onTabSelected = viewModel::selectTab,
                onIslandClick = onNavigateToIslandDetail,
                onEdit = { onNavigateToEdit(facilityId) },
                onViewAll = { onNavigateToIslandsList(facilityId) },
                onCreateIsland = { onNavigateToCreateIsland(facilityId) },
                onEditIsland = onNavigateToEditIsland,
                onDeleteIsland = viewModel::deleteIsland,
                onMarkMaintenanceComplete = viewModel::markMaintenanceComplete
            )

            else -> {
                EmptyState(
                    textTitle = stringResource(R.string.facility_detail_empty_title),
                    textMessage = stringResource(R.string.facility_detail_empty_message),
                    iconImageVector = Icons.Outlined.PrecisionManufacturing,
                    iconContentDescription = stringResource(R.string.facility_detail_empty_icon_description),
                    iconActionImageVector = Icons.Default.Add,
                    iconActionContentDescription = stringResource(R.string.facility_detail_fab_new_island),
                    textAction = stringResource(R.string.facility_detail_empty_action),
                    onAction = { onNavigateToCreateIsland(facilityId) })
            }
        }
    }
}

// =============================================================================
// DETAIL CONTENT
// =============================================================================

@Suppress("ParamsComparedByRef")
@Composable
private fun FacilityDetailContent(
    uiState: FacilityDetailUiState,
    onTabSelected: (FacilityDetailTab) -> Unit,
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
                    icon = {
                        BadgedBox(badge = {
                            count?.let {
                                Badge {
                                    Text(
                                        text = it.toString(),
                                        color = if (tab == FacilityDetailTab.MAINTENANCE && it > 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }) {
                            Icon(
                                imageVector = when (tab) {
                                    FacilityDetailTab.ISLANDS -> IslandPkg.icon
                                    FacilityDetailTab.MAINTENANCE -> Icons.Default.Build
                                    FacilityDetailTab.INFO -> Icons.Default.Info
                                },
                                contentDescription = stringResource(tab.labelResId)
                            )
                        }
                    })
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
                islands = uiState.islands,
                allIslands = uiState.islands,
                islandTypes = uiState.islandTypes,
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

@Suppress("ParamsComparedByRef", "HardCodedStringLiteral")
@Composable
private fun InfoTabContent(
    facility: Facility,
    @Suppress("unused") operationalSummary: FacilityOperationalSummary?,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoCard(
                    title = stringResource(R.string.facility_detail_info_card_general),
                    icon = Icons.Default.Business
                ) {
                    InfoItem(
                        stringResource(R.string.facility_detail_info_field_name),
                        facility.displayName
                    )
                    facility.code?.let {
                        InfoItem(
                            stringResource(R.string.facility_detail_info_field_code), it
                        )
                    }
                    InfoItem(
                        stringResource(R.string.facility_detail_info_field_type),
                        stringResource(facility.facilityType.labelResId)
                    )
                    facility.notes?.let {
                        InfoItem(
                            stringResource(R.string.facility_detail_info_field_notes), it
                        )
                    }
                    InfoItem(
                        stringResource(R.string.facility_detail_info_field_primary),
                        stringResource(if (facility.isPrimary) R.string.facility_detail_info_primary_yes else R.string.facility_detail_info_primary_no)
                    )
                }
            }
            item {
                InfoCard(
                    title = stringResource(R.string.facility_detail_info_card_address),
                    icon = Icons.Default.LocationOn
                ) {
                    facility.address?.let {
                        InfoItem(
                            stringResource(R.string.facility_detail_info_field_address),
                            it.toDisplayString()
                        )
                    }
                }
            }
            item {
                InfoCard(
                    title = stringResource(R.string.facility_detail_info_card_metadata),
                    icon = Icons.Default.Info
                ) {
                    InfoItem(
                        stringResource(R.string.facility_detail_info_field_created),
                        formatTimestamp(facility.createdAt)
                    )
                    InfoItem(
                        stringResource(R.string.facility_detail_info_field_updated),
                        formatTimestamp(facility.updatedAt)
                    )
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
// TAB: ISLANDS — uses IslandCard COMPACT, no more IslandItem duplicate
// =============================================================================

@Suppress("ParamsComparedByRef")
@Composable
private fun IslandsTabContent(
    islands: List<Island>,
    allIslands: List<Island>,
    islandTypes: List<IslandTypeMaster>,
    onIslandClick: (String) -> Unit,
    onViewAll: () -> Unit,
    onCreateIsland: () -> Unit,
    onEditIsland: (String) -> Unit,
    onDeleteIsland: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.island_tab_count, islands.size, allIslands.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onViewAll) { Text(stringResource(R.string.facility_detail_empty_action)) }

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
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items = islands, key = { it.id }) { island ->
                    IslandCard(
                        island = island,
                        islandTypes = islandTypes,
                        variant = ListViewMode.COMPACT,
                        onClick = { onIslandClick(island.id) },
                        onEdit = { onEditIsland(island.id) },
                        onDelete = { onDeleteIsland(island.id) },
                        showActions = true
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB: MAINTENANCE
// =============================================================================

@Suppress("ParamsComparedByRef")
@Composable
private fun MaintenanceTabContent(
    islandsNeedingMaintenance: List<Island>,
    islandsUnderWarranty: List<Island>,
    onMarkMaintenanceComplete: (String) -> Unit,
    onIslandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier= Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "${stringResource(R.string.island_filter_maintenance_due)} (${islandsNeedingMaintenance.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (islandsNeedingMaintenance.isEmpty()) {
                        Text(
                            stringResource(R.string.island_facility_detail_maintenance_all_ok),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        islandsNeedingMaintenance.forEach { island ->
                            MaintenanceIslandItem(
                                island = island,
                                onMarkComplete = onMarkMaintenanceComplete,
                                onIslandClick = onIslandClick
                            )
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier=Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${stringResource(R.string.island_filter_under_warranty)} (${islandsUnderWarranty.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (islandsUnderWarranty.isEmpty()) {
                        Text(
                            stringResource(R.string.island_facility_detail_warranty_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

@Suppress("ParamsComparedByRef")
@Composable
private fun MaintenanceIslandItem(
    island: Island, onMarkComplete: (String) -> Unit, onIslandClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    island.customName ?: island.serialNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(text = island.nextScheduledMaintenance?.let {
                    stringResource(
                        R.string.facility_detail_maintenance_expiry, formatTimestamp(it)
                    )
                } ?: stringResource(R.string.facility_detail_maintenance_not_scheduled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = { onMarkComplete(island.id) }) { Text(stringResource(R.string.facility_detail_maintenance_complete)) }
                IconButton(onClick = { onIslandClick(island.id) }) {
                    Icon(
                        Icons.Default.ChevronRight, contentDescription = null
                    )
                }
            }
        }
    }
}

@Suppress("ParamsComparedByRef")
@Composable
private fun WarrantyIslandItem(island: Island, onIslandClick: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onIslandClick(island.id) },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    island.customName ?: island.serialNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(text = island.warrantyExpiration?.let {
                    stringResource(
                        R.string.facility_detail_warranty_expiry, formatTimestamp(it)
                    )
                } ?: stringResource(R.string.facility_detail_warranty_not_set),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// =============================================================================
// SHARED COMPOSABLE
// =============================================================================

@Composable
private fun InfoCard(
    title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

// =============================================================================
// HELPERS
// =============================================================================

@Suppress("ParamsComparedByRef")
@Composable
private fun formatTimestamp(timestamp: Instant): String {
    val diffMillis = (Clock.System.now() - timestamp).inWholeMilliseconds
    return when {
        diffMillis < 60_000L -> stringResource(R.string.facility_time_now)
        diffMillis < 3_600_000L -> stringResource(
            R.string.facility_time_minutes_ago, diffMillis / 60_000
        )

        diffMillis < 86_400_000L -> stringResource(
            R.string.facility_time_hours_ago, diffMillis / 3_600_000
        )

        else -> stringResource(R.string.facility_time_days_ago, diffMillis / 86_400_000)
    }
}