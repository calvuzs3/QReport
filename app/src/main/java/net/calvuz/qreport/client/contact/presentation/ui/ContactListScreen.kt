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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.QrLoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportSearchBar
import net.calvuz.qreport.app.app.presentation.components.simple_selection.DeleteConfirmationDialog
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectableItem
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionAction
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SelectionTopBar
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SimpleSelectionActionHandler
import net.calvuz.qreport.app.app.presentation.components.simple_selection.SimpleSelectionManager
import net.calvuz.qreport.app.app.presentation.components.simple_selection.rememberSimpleSelectionManager
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.presentation.ui.components.ContactCard
import androidx.compose.material.icons.filled.Star
import net.calvuz.qreport.app.app.presentation.components.QReportFiltersChipRow
import net.calvuz.qreport.app.app.presentation.components.QReportPullToRefresh
import net.calvuz.qreport.app.app.presentation.components.QReportSelectorRow
import net.calvuz.qreport.client.client.presentation.model.ClientPkg
import net.calvuz.qreport.client.contact.presentation.model.ContactFilter
import net.calvuz.qreport.client.contact.presentation.model.ContactPkg
import net.calvuz.qreport.client.contact.presentation.model.ContactSortOrder
import net.calvuz.qreport.client.facility.presentation.model.ClientOption
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.settings.presentation.model.getCardVariantDescription
import net.calvuz.qreport.settings.presentation.model.getCardVariantIcon
import timber.log.Timber

// Contact-specific custom action ID
private const val ACTION_SET_PRIMARY = "set_primary"

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
@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    modifier: Modifier = Modifier,
    clientId: String? = null,
    clientName: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToCreateContact: (String) -> Unit,
    onNavigateToEditContact: (String) -> Unit,
    onNavigateToContactDetail: (String) -> Unit,
    viewModel: ContactListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val selectionManager = rememberSimpleSelectionManager<Contact>()
    val selectionState by selectionManager.selectionState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(clientId) {
        if (!clientId.isNullOrBlank()) viewModel.initializeForClient(clientId)
        else viewModel.initialize()
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
                                    text = uiState.selectedClient
                                        .takeIf { it != ClientOption.ALL }
                                        ?.companyName
                                        ?: clientName.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.contact_screen_list_subtitle_all),
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

                            // View mode toggle button
                            IconButton(onClick = viewModel::cycleCardVariant) {
                                Icon(
                                    imageVector = uiState.cardVariant.getCardVariantIcon(),
                                    contentDescription = uiState.cardVariant.getCardVariantDescription()
                                )
                            }

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
                QReportSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    placeholder = stringResource(R.string.contacts_search_hint),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                QReportSelectorRow(
                    entries = uiState.availableClients,
                    selectedItem = uiState.selectedClient,
                    onItemSelected = viewModel::updateSelectedClient,
                    icon = ClientPkg.icon,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                QReportFiltersChipRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    selectedFilter = uiState.selectedFilter,
                    avoidFilter = ContactPkg.selectedFilter,
                    onClearFilter = { viewModel.updateFilter(ContactPkg.selectedFilter) },
                    selectedSort = uiState.selectedSortOrder,
                    avoidSort = ContactPkg.selectedSortOrder,
                    onClearSort = { viewModel.updateSortOrder(ContactSortOrder.CREATED_RECENT) },
                )
            }

            // Content with Pull to Refresh
            QReportPullToRefresh(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {

                // Content based on state
                when {
                    uiState.isLoading -> {
                        QrLoadingState()
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
                            onAction = { onNavigateToCreateContact(uiState.clientId) }
                        )
                    }

                    else -> {
                        ContactListWithSelection(
                            contactsWithStats = uiState.filteredContacts,
                            variant = uiState.cardVariant,
                            isSettingPrimary = uiState.isSettingPrimary,
                            selectionManager = selectionManager,
                            onNavigateToDetail = onNavigateToContactDetail,
                            onNavigateToEdit = onNavigateToEditContact,
                            onDeleteContact = viewModel::deleteContact,
                            onRestoreContact = viewModel::restoreContact,
                            onSetPrimaryContact = viewModel::setPrimaryContact,
                            modifier = Modifier.fillMaxSize()
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
@Suppress("ParamsComparedByRef")
@Composable
private fun ContactListWithSelection(
    modifier: Modifier = Modifier,
    contactsWithStats: List<ContactWithStats>,
    selectionManager: SimpleSelectionManager<Contact>,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleteContact: (String) -> Unit,
    onRestoreContact: (String) -> Unit,
    onSetPrimaryContact: (String) -> Unit,
    isSettingPrimary: String?,
    variant: ListViewMode = ListViewMode.FULL
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
                    onRestore = if (!selectionState.isInSelectionMode) {
                        { onRestoreContact(contactWithStats.contact.id) }
                    } else null,
                    onSetPrimary = if (!selectionState.isInSelectionMode) {
                        { onSetPrimaryContact(contactWithStats.contact.id) }
                    } else null,
                    isSettingPrimary = isSettingPrimary == contactWithStats.contact.id,
                    isSelected = isSelected,
                    variant = variant
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