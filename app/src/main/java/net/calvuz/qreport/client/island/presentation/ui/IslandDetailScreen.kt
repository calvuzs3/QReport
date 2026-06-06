@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.client.island.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Clock
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.QReportErrorState
import net.calvuz.qreport.app.app.presentation.components.QReportPullToRefresh
import net.calvuz.qreport.client.island.presentation.model.IslandPkg
import net.calvuz.qreport.client.island.presentation.ui.components.InfoTabContent
import net.calvuz.qreport.client.island.presentation.ui.components.MaintenanceDialog
import net.calvuz.qreport.client.island.presentation.ui.components.MaintenanceTabContent
import net.calvuz.qreport.client.island.presentation.ui.components.UnitsTabContent
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandDetailScreen(
    modifier: Modifier = Modifier,
    facilityId: String,
    islandId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (facilityId: String, islandId: String) -> Unit,
    onNavigateToCreateUnit: (islandId: String) -> Unit,
    onNavigateToUnitList: (islandId: String) -> Unit,
    onNavigateToEditUnit: (unitId: String) -> Unit,
    onIslandDeleted: () -> Unit = {},
    onNavigateToCreateMaintenanceLog: (islandId: String) -> Unit,
    onNavigateToIslandHealth: (IslandId: String) -> Unit,

    viewModel: IslandDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMaintenanceDialog by remember { mutableStateOf(false) }

    val onDetailEvent: (IslandDetailEvent) -> Unit = viewModel::onDetailEvent

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            viewModel.resetDeleteState(); onIslandDeleted()
        }
    }
    LaunchedEffect(islandId) { onDetailEvent(IslandDetailEvent.LoadIsland(islandId)) }

    if (uiState.showDeleteConfirmation) {
        QReportConfirmDeleteDialog(
            objectName = stringResource(R.string.island_detail_object_name),
            objectDesc = uiState.islandName,
            onConfirm = { onDetailEvent(IslandDetailEvent.DeleteIsland(force = false)) },
            onDismiss = viewModel::hideDeleteConfirmation
        )
    }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(title = {
            Column {
                Text(text = uiState.islandName.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.island_detail_title_fallback),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge)
            }
        }, navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Default.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.island_detail_action_back)
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
                        contentDescription = stringResource(R.string.island_detail_action_delete)
                    )
                }
                if (uiState.island?.needsMaintenance() == true) {
                    IconButton(
                        onClick = { showMaintenanceDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = stringResource(R.string.island_detail_action_maintenance)
                        )
                    }
                }
                IconButton(onClick = { onNavigateToEdit(facilityId, islandId) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.island_detail_action_edit)
                    )
                }
                var showMoreMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.island_detail_action_more)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.island_detail_menu_record_maintenance)) },
                        onClick = { showMaintenanceDialog = true; showMoreMenu = false },
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) })
                }
            }
        })

        QReportPullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onDetailEvent(IslandDetailEvent.Refresh) },
            modifier = Modifier.fillMaxSize()
        ) {

            when {
                uiState.isLoading -> LoadingState(
                    modifier = Modifier.fillMaxSize(),
                    message = stringResource(R.string.island_detail_loading)
                )

                uiState.error != null && !uiState.hasData -> QReportErrorState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    error = uiState.error!!,
                    onRetry = { onDetailEvent(IslandDetailEvent.Refresh) },
                    onDismiss = { onDetailEvent(IslandDetailEvent.DismissError) })

                uiState.hasData -> IslandDetailContent(
                    uiState = uiState,
                    onTabSelected = viewModel::selectTab,
                    onEdit = { onNavigateToEdit(facilityId, islandId) },
                    onCreateUnit = { onNavigateToCreateUnit(islandId) },
                    onEditUnit = onNavigateToEditUnit,
                    onDeleteUnit = { onDetailEvent(IslandDetailEvent.DeleteUnit(it)) },
                    onMaintenanceAction = { showMaintenanceDialog = true },
                    onNavigateToUnitList = onNavigateToUnitList,
                    onDismissError = { onDetailEvent(IslandDetailEvent.DismissError) },
                    onAddLog = { onNavigateToCreateMaintenanceLog(islandId) },
                    onNavigateToHealth = { onNavigateToIslandHealth(islandId) },
                )

                else -> EmptyState(
                    textTitle = stringResource(R.string.island_detail_empty_title),
                    textMessage = stringResource(R.string.island_detail_empty_message),
                    iconImageVector = IslandPkg.icon
                )
            }
        }
    }

    if (showMaintenanceDialog) {
        MaintenanceDialog(
            island = uiState.island,
            onDismiss = { showMaintenanceDialog = false },
            onConfirm = { resetHours, notes ->
                viewModel.recordMaintenance(
                    maintenanceDate = Clock.System.now(),
                    resetOperatingHours = resetHours,
                    notes = notes.takeIf { it.isNotBlank() })
                showMaintenanceDialog = false
            })
    }
}

@Composable
private fun IslandDetailContent(
    uiState: FacilityIslandDetailUiState,
    onTabSelected: (IslandDetailTab) -> Unit,
    onEdit: () -> Unit,
    onCreateUnit: () -> Unit,
    onEditUnit: (String) -> Unit,
    onDeleteUnit: (MechanicalUnit) -> Unit,
    onMaintenanceAction: () -> Unit,
    onNavigateToUnitList: (String) -> Unit,
    onDismissError: () -> Unit,
    onAddLog: () -> Unit,
    onNavigateToHealth: () -> Unit,
) {
    Column {
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            IslandDetailTab.entries.forEach { tab ->
                val badge = when (tab) {
                    IslandDetailTab.UNITS -> uiState.unitsCount.takeIf { it > 0 }
                    else -> null
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
                            badge?.let { Badge { Text(it.toString()) } }
                        }
                    })
            }
        }

        when (uiState.selectedTab) {
            IslandDetailTab.INFO -> InfoTabContent(
                island = uiState.island!!,
                statistics = uiState.statistics,
                error = uiState.error,
                onDismissError = onDismissError,
                onEdit = onEdit,
                modifier = Modifier.weight(1f)
            )

            IslandDetailTab.UNITS -> UnitsTabContent(
                units = uiState.units,
                isLoading = uiState.isLoadingUnits,
                onCreateUnit = onCreateUnit,
                onEditUnit = onEditUnit,
                onDeleteUnit = onDeleteUnit,
                onViewAll = { onNavigateToUnitList(uiState.island?.id ?: "") },
                modifier = Modifier.weight(1f)
            )

            IslandDetailTab.MAINTENANCE -> MaintenanceTabContent(
                island = uiState.island!!,
                statistics = uiState.statistics,
                onMaintenanceAction = onMaintenanceAction,
                logs = uiState.logs,
                isLoadingLogs = uiState.isLoadingLogs,
                availableUnits = uiState.units,
                onAddLog = onAddLog,
                onNavigateToHealth = onNavigateToHealth,

                modifier = Modifier.weight(1f)
            )
        }
    }
}