package net.calvuz.qreport.client.contact.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
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
import net.calvuz.qreport.app.app.presentation.components.simple_selection.DeleteConfirmationDialog
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectableItem
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionAction
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionTopBar
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SimpleSelectionActionHandler
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SimpleSelectionManager
import net.calvuz.qreport.app.app.presentation.components.simple_selection.rememberSimpleSelectionManager
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.presentation.ui.components.ContactCard
import androidx.compose.material.icons.filled.Star
import timber.log.Timber

// Contact-specific custom action ID
private const val ACTION_SET_PRIMARY = "set_primary"

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
 * Screen for client contact list
 *
 * Features:
 * - Contact list with main info
 * - Search by name, role, email, phone
 * - Operations: add, edit, delete, set primary
 * - Multi-selection with Gmail-style TopBar
 * - Pull to refresh
 * - Loading/error/empty states
 * - Primary contact indicator
 */
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

    // Simple selection manager
    val selectionManager = rememberSimpleSelectionManager<Contact>()
    val selectionState by selectionManager.selectionState.collectAsState()

    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load contacts when screen opens
    LaunchedEffect(clientId) {
        viewModel.initializeForClient(clientId)
    }

    // Clear selection when loading
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            selectionManager.clearSelection()
        }
    }

    // Action handler for contacts
    val actionHandler = remember(uiState.filteredContacts) {
        ContactActionHandler(
            onEdit = { contacts ->
                if (contacts.size == 1) {
                    onNavigateToEditContact(contacts.first().id)
                    selectionManager.clearSelection()
                }
            },
            onDelete = {
                showDeleteDialog = true
            },
            onSetPrimary = { contacts ->
                if (contacts.size == 1) {
                    viewModel.setPrimaryContact(contacts.first().id)
                    selectionManager.clearSelection()
                }
            },
            onSelectAll = {
                selectionManager.selectAll(uiState.filteredContacts.map { it.contact })
            },
            onPerformDelete = { contacts ->
                viewModel.bulkDeleteContacts(contacts.map { it.id })
                selectionManager.clearSelection()
            }
        )
    }

    // Define actions
    val setPrimaryAction = SelectionAction.Custom(
        icon = Icons.Default.Star,
        label = stringResource(R.string.action_set_as_primary),
        isDestructive = false,
        actionId = ACTION_SET_PRIMARY
    )

    val primaryActions = listOf(
        SelectionAction.Edit,
        setPrimaryAction,
        SelectionAction.Delete
    )

    val secondaryActions = listOf(
        SelectionAction.SelectAll
    )

    // Pull to refresh state
    val pullToRefreshState = rememberPullToRefreshState()

    // Handle pull to refresh
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Selection-aware TopBar
            SelectionTopBar(
                normalTopBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(R.string.contact_screen_list_title),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = clientName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBackIosNew,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        },
                        actions = {
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
                    )
                },
                selectionManager = selectionManager,
                primaryActions = primaryActions,
                secondaryActions = secondaryActions,
                actionHandler = actionHandler
            )

            // Search bar and filters (hidden in selection mode)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
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
                            iconActionContentDescription = stringResource(R.string.contacts_list_action_add),
                            textAction = stringResource(R.string.contacts_list_action_add),
                            onAction = { onNavigateToCreateContact(clientId) }
                        )
                    }

                    else -> {
                        ContactListWithSelection(
                            contactsWithStats = uiState.filteredContacts,
                            selectionManager = selectionManager,
                            onNavigateToDetail = onNavigateToContactDetail,
                            onNavigateToEdit = onNavigateToEditContact,
                            onDeleteContact = viewModel::deleteContact,
                            onSetPrimaryContact = viewModel::setPrimaryContact,
                            isSettingPrimary = uiState.isSettingPrimary,
                            modifier = Modifier.fillMaxSize()
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

                // FAB (hidden in selection mode)
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

        // Delete confirmation dialog
        DeleteConfirmationDialog(
            isVisible = showDeleteDialog,
            selectedItems = selectionState.selectedItems,
            actionHandler = actionHandler,
            onConfirm = {
                actionHandler.performDelete(selectionState.selectedItems)
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}

/**
 * Contact list with selection support
 */
@Composable
private fun ContactListWithSelection(
    contactsWithStats: List<ContactWithStats>,
    selectionManager: SimpleSelectionManager<Contact>,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleteContact: (String) -> Unit,
    onSetPrimaryContact: (String) -> Unit,
    isSettingPrimary: String?,
    modifier: Modifier = Modifier
) {
    val selectionState by selectionManager.selectionState.collectAsState()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = contactsWithStats,
            key = { it.contact.id }
        ) { contactWithStats ->
            SelectableItem(
                item = contactWithStats.contact,
                selectionManager = selectionManager,
                onNormalClick = { contact ->
                    onNavigateToDetail(contact.id)
                }
            ) { isSelected ->
                ContactCard(
                    modifier = Modifier.fillMaxWidth(),
                    contact = contactWithStats.contact,
                    onClick = { },
                    showActions = !selectionState.isInSelectionMode,
                    onEdit = if (!selectionState.isInSelectionMode) {
                        { onNavigateToEdit(contactWithStats.contact.id) }
                    } else null,
                    onDelete = if (!selectionState.isInSelectionMode) {
                        { onDeleteContact(contactWithStats.contact.id) }
                    } else null,
                    onSetPrimary = if (!selectionState.isInSelectionMode) {
                        { onSetPrimaryContact(contactWithStats.contact.id) }
                    } else null,
                    isSettingPrimary = isSettingPrimary == contactWithStats.contact.id,
                    isSelected = isSelected,
                    variant = QrListItemCardVariant.FULL
                )
            }
        }
    }
}

/**
 * Contact specific action handler
 */
class ContactActionHandler(
    private val onEdit: (Set<Contact>) -> Unit,
    private val onDelete: () -> Unit,
    private val onSetPrimary: (Set<Contact>) -> Unit,
    private val onSelectAll: () -> Unit,
    private val onPerformDelete: (Set<Contact>) -> Unit
) : SimpleSelectionActionHandler<Contact> {

    override fun onActionClick(action: SelectionAction, selectedItems: Set<Contact>) {
        when (action) {
            SelectionAction.Edit -> onEdit(selectedItems)
            SelectionAction.Delete -> onDelete()
            SelectionAction.SelectAll -> onSelectAll()
            is SelectionAction.Custom -> {
                when (action.actionId) {
                    ACTION_SET_PRIMARY -> onSetPrimary(selectedItems)
                    else -> Timber.d("Custom action: ${action.actionId}")
                }
            }
            else -> {
                Timber.w("Unhandled action: $action")
            }
        }
    }

    override fun isActionEnabled(action: SelectionAction, selectedItems: Set<Contact>): Boolean {
        return when (action) {
            // Edit: only single selection
            SelectionAction.Edit -> selectedItems.size == 1

            // Delete: any selection
            SelectionAction.Delete -> selectedItems.isNotEmpty()

            // SelectAll: always available
            SelectionAction.SelectAll -> true

            // Custom actions
            is SelectionAction.Custom -> {
                when (action.actionId) {
                    // SetPrimary: only single selection and contact must not be primary
                    ACTION_SET_PRIMARY -> {
                        selectedItems.size == 1 && selectedItems.all { !it.isPrimary && it.isActive }
                    }
                    else -> true
                }
            }

            else -> false
        }
    }

    override fun getDeleteConfirmationMessage(selectedItems: Set<Contact>): String {
        return when (selectedItems.size) {
            1 -> "Eliminare il contatto ${selectedItems.first().fullName}?"
            else -> "Eliminare ${selectedItems.size} contatti?"
        }
    }

    fun performDelete(selectedItems: Set<Contact>) {
        onPerformDelete(selectedItems)
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