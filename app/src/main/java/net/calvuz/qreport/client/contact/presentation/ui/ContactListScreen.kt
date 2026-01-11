package net.calvuz.qreport.client.contact.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.ActiveFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.list.QrListItemCard.QrListItemCardVariant
import net.calvuz.qreport.app.app.presentation.components.selection.BatchAction
import net.calvuz.qreport.app.app.presentation.components.selection.BatchActionHandler
import net.calvuz.qreport.app.app.presentation.components.selection.BatchActionSets
import net.calvuz.qreport.app.app.presentation.components.selection.BatchDeleteConfirmationDialog
import net.calvuz.qreport.app.app.presentation.components.selection.FloatingSelectionIndicator
import net.calvuz.qreport.app.app.presentation.components.selection.SelectableListItem
import net.calvuz.qreport.app.app.presentation.components.selection.SelectionBottomSheet
import net.calvuz.qreport.app.app.presentation.components.selection.SelectionManager
import net.calvuz.qreport.app.app.presentation.components.selection.rememberSelectionManager
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.client.contact.presentation.ui.components.ContactCard
import timber.log.Timber


@Composable
fun ContactFilter.getDisplayName(): UiText {
    return when (this) {
        ContactFilter.ACTIVE -> (UiText.StringResource(R.string.contacts_list_filter_active))
        ContactFilter.INACTIVE -> (UiText.StringResource(R.string.contacts_list_filter_inactive))
        ContactFilter.PRIMARY_ONLY -> (UiText.StringResource(R.string.contacts_list_filter_primary_only))
        ContactFilter.ALL -> (UiText.StringResource(R.string.contacts_list_filter_all))
    }
}

@Composable
fun ContactSortOrder.getDisplayName(): UiText {
    return when (this) {
        ContactSortOrder.NAME -> (UiText.StringResource(R.string.contacts_list_sort_name))
        ContactSortOrder.CREATED_RECENT -> (UiText.StringResource(R.string.contacts_list_sort_created_recent))
        ContactSortOrder.CREATED_OLDEST -> (UiText.StringResource(R.string.contacts_list_sort_created_oldest))
    }
}

/**
 * Screen per la lista contatti di un cliente
 *
 * Features:
 * - Lista contatti con informazioni principali
 * - Search per nome, ruolo, email, telefono
 * - Operazioni: aggiungi, modifica, elimina, imposta primario
 * - Multi-selection e bulk delete
 * - Pull to refresh
 * - Stati loading/error/empty
 * - Indicazione referente primario
 */
@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    clientId: String,
    clientName: String,
    onNavigateBack: () -> Unit,
    onNavigateToCreateContact: (String) -> Unit,
    onNavigateToEditContact: (String) -> Unit,
    onNavigateToContactDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Selection manager for multi-select functionality
    val selectionManager = rememberSelectionManager<String>()
    val selectionState by selectionManager.selectionState.collectAsState()

    // Bottom sheet state
    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Batch delete dialog state
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // Load contacts when screen opens
    LaunchedEffect(clientId) {
        viewModel.initializeForClient(clientId)
    }

    // Clear selection when navigating away or data changes significantly
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            selectionManager.clearSelection()
        }
    }

    // Close bottom sheet when selection is cleared
    LaunchedEffect(selectionState.isInSelectionMode) {
        if (!selectionState.isInSelectionMode) {
            showBottomSheet = false
        }
    }

    // ✅ CREATE BATCH ACTION HANDLER
    val batchActionHandler = createContactBatchActionHandler(
        viewModel = viewModel,
        onNavigateToEditContact = onNavigateToEditContact,
        onShowBatchDeleteDialog = { showBatchDeleteDialog = true },
        contactsWithStats = uiState.filteredContacts,
        selectionManager = selectionManager
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectionState.isInSelectionMode) {
                                stringResource(
                                    R.string.selection_summary,
                                    selectionState.selectedCount
                                )
                            } else {
                                stringResource(R.string.contact_screen_list_title)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!selectionState.isInSelectionMode) {
                            Text(
                                text = clientName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectionState.isInSelectionMode) {
                                selectionManager.clearSelection()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selectionState.isInSelectionMode) {
                                Icons.Default.Close
                            } else {
                                Icons.Default.ArrowBackIosNew
                            },
                            contentDescription = if (selectionState.isInSelectionMode) {
                                stringResource(R.string.clear_selection)
                            } else {
                                stringResource(R.string.action_back)
                            }
                        )
                    }
                },
                actions = {
                    if (!selectionState.isInSelectionMode) {
                        var showFilterMenu by remember { mutableStateOf(false) }
                        var showSortMenu by remember { mutableStateOf(false) }

                        // Sort button
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.Sort,
                                contentDescription = stringResource(R.string.label_ordering)
                            )
                        }

                        // Filter button
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.label_filtering)
                            )
                        }

                        // Sort menu
                        ContactSortMenu(
                            expanded = showSortMenu,
                            selectedSort = uiState.selectedSortOrder,
                            onSortSelected = viewModel::updateSortOrder,
                            onDismiss = { showSortMenu = false }
                        )

                        // Filter menu
                        ContactFilterMenu(
                            expanded = showFilterMenu,
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelected = viewModel::updateFilter,
                            onDismiss = { showFilterMenu = false }
                        )
                    }
                }
            )

            if (!selectionState.isInSelectionMode) {
                // Search bar
                QReportSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    placeholder = stringResource(R.string.contacts_search_hint),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Filter chips
                ActiveFiltersChipRow(
                    selectedFilter = uiState.selectedFilter.getDisplayName().asString(),
                    selectedSort = uiState.selectedSortOrder.getDisplayName().asString(),
                    onClearFilter = { viewModel.updateFilter(ContactFilter.ACTIVE) },
                    onClearSort = { viewModel.updateSortOrder(ContactSortOrder.CREATED_RECENT) },
                    avoidFilter = ContactFilter.ACTIVE.getDisplayName().asString(),
                    avoidSort = ContactSortOrder.CREATED_RECENT.getDisplayName().asString(),
                )
            }

            // Content with Pull to Refresh
            val pullToRefreshState = rememberPullToRefreshState()

            // Reset refresh state when not refreshing
            LaunchedEffect(uiState.isRefreshing) {
                if (!uiState.isRefreshing && pullToRefreshState.isRefreshing) {
                    pullToRefreshState.endRefresh()
                }
            }

            // Handle pull to refresh
            LaunchedEffect(pullToRefreshState.isRefreshing) {
                if (pullToRefreshState.isRefreshing) {
                    viewModel.refresh()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                val currentError = uiState.error

                // Content based on state
                when {
                    uiState.isLoading -> {
                        LoadingState()
                    }

                    uiState.filteredContacts.isEmpty() -> {
                        val (title, message) = when {
                            uiState.contacts.isEmpty() ->
                                stringResource(R.string.contact_screen_list_empty_title) to stringResource(
                                    R.string.contact_screen_list_empty_message
                                )

                            uiState.selectedFilter != ContactFilter.ACTIVE ->
                                stringResource(R.string.contact_screen_list_empty_no_results_title) to
                                        stringResource(
                                            R.string.contact_screen_list_empty_no_result_message,
                                            uiState.selectedFilter.getDisplayName().asString()
                                        )

                            else -> stringResource(R.string.contact_screen_list_empty_error_title) to
                                    stringResource(R.string.checkup_screen_list_empty_error_message)
                        }
                        EmptyState(
                            textTitle = title,
                            textMessage = message,
                            iconImageVector = Icons.Outlined.AssignmentTurnedIn,
                            iconContentDescription = title,
                            iconActionImageVector = Icons.Default.Add,
                            iconActionContentDescription = stringResource(R.string.contacts_list_action_add),  //contact_screen_list_empty_action_add),
                            textAction = stringResource(R.string.contacts_list_action_add), //contact_screen_list_empty_action_add),
                            onAction = { onNavigateToCreateContact(clientId) }
                        )
                    }

                    else -> {
                        // ✅ CONTACTS LIST WITH SELECTION
                        ContactListWithSelection(
                            contactsWithStats = uiState.filteredContacts,
                            selectionManager = selectionManager,
                            onNavigateToDetail = onNavigateToContactDetail,
                            onNavigateToEdit = onNavigateToEditContact,
                            onDeleteContact = viewModel::deleteContact,
                            onSetPrimaryContact = viewModel::setPrimaryContact,
                            isSettingPrimary = uiState.isSettingPrimary,
                            isDeletingContact = uiState.isDeletingContact,
                        )
                    }
                }

                // Pull to refresh indicator
                if (pullToRefreshState.isRefreshing || uiState.isLoading) {
                    PullToRefreshContainer(
                        state = pullToRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                // Add contact button (only when not in selection mode)
                if (!selectionState.isInSelectionMode && !uiState.isLoading && uiState.error == null) {
                    FloatingActionButton(
                        onClick = { onNavigateToCreateContact(clientId) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.contacts_list_action_add)
                        )
                    }
                }
            }
        }

        // Floating selection indicator (appears above the list, doesn't cover it)
        FloatingSelectionIndicator(
            selectionState = selectionState,
            onOpenBottomSheet = {
                showBottomSheet = true
            },
            onClearSelection = {
                selectionManager.clearSelection()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )


        // ✅ BOTTOM SHEET FOR BATCH ACTIONS
        if (showBottomSheet) {
            SelectionBottomSheet(
                selectionState = selectionState,
                availableActions = BatchActionSets.contactActions,
                batchActionHandler = batchActionHandler,
                onDismiss = { showBottomSheet = false },
                sheetState = bottomSheetState,
            )
        }

        // ✅ BATCH DELETE CONFIRMATION DIALOG
        BatchDeleteConfirmationDialog(
            isVisible = showBatchDeleteDialog,
            selectedItems = selectionState.selectedItems,
            batchActionHandler = batchActionHandler,
            onConfirm = {
                viewModel.bulkDeleteContacts(selectionState.selectedItems.toList())
                selectionManager.clearSelection()
                showBatchDeleteDialog = false
            },
            onDismiss = { showBatchDeleteDialog = false }
        )
    }
}

@Composable
private fun ContactListWithSelection(
    contactsWithStats: List<ContactWithStats>,
    selectionManager: SelectionManager<String>,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleteContact: (String) -> Unit,
    onSetPrimaryContact: (String) -> Unit,
    isSettingPrimary: String?,
    isDeletingContact: String?,
    modifier: Modifier = Modifier
) {
    val selectionState by selectionManager.selectionState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = contactsWithStats,
            key = { it.contact.id }
        ) { contactWithStats ->
            if (selectionState.isInSelectionMode) {
                // ✅ SELECTION MODE: Use SelectableListItem
                SelectableListItem(
                    item = contactWithStats.contact.id,
                    selectionManager = selectionManager,
                    onNavigateToItem = { onNavigateToDetail(it) }
                ) { contactId ->
                    // Find the contact data
                    val contact =
                        contactsWithStats.find { it.contact.id == contactId }?.contact
                    if (contact != null) {
                        ContactCard(
                            contact = contact,
                            onClick = {}, // Handled by SelectableListItem
                            onSetPrimary = null,  // Disabled in selection mode
                            onEdit = null,        // Disabled in selection mode
                            onDelete = null,      // Disabled in selection mode
                            variant = QrListItemCardVariant.FULL,
                            isSettingPrimary = false  // ✅ FIXED: No TODO, disabled in selection mode
                        )
                    }
                }
            } else {
                // ✅ NORMAL MODE: Full functionality
                ContactCard(
                    contact = contactWithStats.contact,
                    onClick = { onNavigateToDetail(contactWithStats.contact.id) },
                    onEdit = { onNavigateToEdit(contactWithStats.contact.id) },
                    onDelete = { onDeleteContact(contactWithStats.contact.id) },
                    onSetPrimary = { onSetPrimaryContact(contactWithStats.contact.id) },
                    isSettingPrimary = isSettingPrimary == contactWithStats.contact.id,
//                    isDeletingContact = isDeletingContact == contactWithStats.contact.id,
                    variant = QrListItemCardVariant.FULL
                )
            }
        }
    }
}

//        // ✅ BULK DELETE PROGRESS INDICATOR
//        if (uiState.isBulkDeleting) {
//            Card(
//                modifier = Modifier
//                    .align(Alignment.Center)
//                    .padding(32.dp),
//                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(24.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    CircularProgressIndicator()
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Text(
//                        text = stringResource(
//                            R.string.contacts_list_bulk_delete_progress,
//                            uiState.bulkDeleteProgress,
//                            uiState.bulkDeleteTotal
//                        ),
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                }
//            }
//        }



/**
 * ✅ CORRECTED: Creates batch action handler for CONTACTS (not contracts)
 */
@Composable
private fun createContactBatchActionHandler(
    viewModel: ContactListViewModel,
    onNavigateToEditContact: (String) -> Unit,
    onShowBatchDeleteDialog: () -> Unit,
    contactsWithStats: List<ContactWithStats>,
    selectionManager: SelectionManager<String>
): BatchActionHandler<String> {
    return remember {
        object : BatchActionHandler<String> {
            override fun onBatchAction(action: BatchAction, selectedItems: Set<String>) {
                Timber.d("🔥 Batch action: $action for ${selectedItems.size} contacts")  // ✅ Fixed comment

                when (action) {
                    BatchAction.SelectAll -> {
                        val allContactIds =
                            contactsWithStats.map { it.contact.id }  // ✅ Fixed comment
                        selectionManager.selectAll(allContactIds)
                    }

                    BatchAction.Edit -> {
                        // ✅ ONLY SINGLE EDIT ALLOWED
                        if (selectedItems.size == 1) {
                            onNavigateToEditContact(selectedItems.first())
                            selectionManager.clearSelection()
                        } else {
                            Timber.w("Edit action requires exactly one contact, got ${selectedItems.size}")
                        }
                    }

                    BatchAction.Delete -> {
                        onShowBatchDeleteDialog()
                    }

//                    BatchAction.ContactBatchAction.SetPrimary -> {
//                        // ✅ ONLY SINGLE SET PRIMARY ALLOWED
//                        if (selectedItems.size == 1) {
//                            viewModel.setPrimaryContact(selectedItems.first())
//                            selectionManager.clearSelection()
//                        } else {
//                            Timber.w("Set primary action requires exactly one contact, got ${selectedItems.size}")
//                        }
//                    }

                    BatchAction.Export -> {
                        // TODO: Implement contact export
                        Timber.d("Batch export for ${selectedItems.size} contacts")  // ✅ Fixed comment
                        selectionManager.clearSelection()
                    }

                    else -> {
                        Timber.w("Unsupported batch action: $action")
                    }
                }
            }

            override fun isBatchActionAvailable(
                action: BatchAction,
                selectedItems: Set<String>
            ): Boolean {
                return when (action) {
//                    // ✅ SET PRIMARY: Only available for single selection and non-primary contacts
//                    BatchAction.ContactBatchAction.SetPrimary -> {
//                        selectedItems.size == 1 && selectedItems.all { contactId ->
//                            val contact =
//                                contactsWithStats.find { it.contact.id == contactId }?.contact
//                            contact != null && !contact.isPrimary  // Only non-primary contacts can be set as primary
//                        }
//                    }

                    // ✅ EDIT: Only available for single selection
                    BatchAction.Edit -> {
                        selectedItems.size == 1
                    }

                    // ✅ SELECT ALL: Available if not all items are selected
                    BatchAction.SelectAll -> {
                        selectedItems.size < contactsWithStats.size
                    }

                    // ✅ DELETE: Available for any selection
                    BatchAction.Delete -> {
                        selectedItems.isNotEmpty()
                    }

                    // ✅ EXPORT: Available for any selection
                    BatchAction.Export -> {
                        selectedItems.isNotEmpty()
                    }

                    else -> false
                }
            }

            override fun getBatchDeleteConfirmationMessage(selectedItems: Set<String>): UiText {
                return UiText.StringResources(  // ✅ Fixed: contacts instead of contracts
                    R.string.contacts_list_bulk_delete_confirmation,
                    selectedItems.size
                )
            }
        }
    }
}

@Composable
private fun ContactSortMenu(
    expanded: Boolean,
    selectedSort: ContactSortOrder,
    onSortSelected: (ContactSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        ContactSortOrder.entries.forEach { sortOrder ->
            DropdownMenuItem(
                text = { Text((sortOrder.getDisplayName().asString())) },
                onClick = {
                    onSortSelected(sortOrder)
                    onDismiss()
                },
                leadingIcon = if (selectedSort == sortOrder) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

@Composable
private fun ContactFilterMenu(
    expanded: Boolean,
    selectedFilter: ContactFilter,
    onFilterSelected: (ContactFilter) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        ContactFilter.entries.forEach { filter ->
            DropdownMenuItem(
                text = { Text((filter.getDisplayName().asString())) },
                onClick = {
                    onFilterSelected(filter)
                    onDismiss()
                },
                leadingIcon = if (selectedFilter == filter) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}
