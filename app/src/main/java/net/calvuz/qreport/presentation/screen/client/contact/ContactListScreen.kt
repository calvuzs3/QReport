package net.calvuz.qreport.presentation.screen.client.contact

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.presentation.components.EmptyState
import net.calvuz.qreport.presentation.components.ErrorState
import net.calvuz.qreport.presentation.components.LoadingState
import net.calvuz.qreport.presentation.components.client.ContactCard
import net.calvuz.qreport.presentation.components.QReportSearchBar
import net.calvuz.qreport.presentation.components.client.ContactCardVariant

/**
 * Screen per la lista contatti di un cliente
 *
 * Features:
 * - Lista contatti con informazioni principali
 * - Search per nome, ruolo, email, telefono
 * - Operazioni: aggiungi, modifica, elimina, imposta primario
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

    // Load contacts when screen opens
    LaunchedEffect(clientId) {
        viewModel.initializeForClient(clientId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Contatti",
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
                        contentDescription = "Indietro"
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
                        contentDescription = "Ordinamento"
                    )
                }

                // Filter button
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtri"
                    )
                }

                // Sort menu
                ContactSortMenu(
                    expanded = showSortMenu,
                    selectedSort = uiState.sortOrder,
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

        // Search bar
        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = "Ricerca Contatti",
            modifier = Modifier.padding(16.dp)
        )

        // Filter chips
        if (uiState.selectedFilter != ContactFilter.ALL || uiState.sortOrder != ContactSortOrder.NAME) {
            ActiveFiltersChipRow(
                selectedFilter = uiState.selectedFilter,
                selectedSort = uiState.sortOrder,
                onClearFilter = { viewModel.updateFilter(ContactFilter.ALL) },
                onClearSort = { viewModel.updateSortOrder(ContactSortOrder.NAME) },
                modifier = Modifier.padding(horizontal = 16.dp)
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
                viewModel.refreshContacts()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            // Store error in local variable to avoid smart cast issues
            val currentError = uiState.error

            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                currentError != null -> {
                    ErrorState(
                        error = currentError,
                        onRetry = viewModel::refreshContacts,
                        onDismiss = viewModel::dismissError
                    )
                }

                uiState.filteredContacts.isEmpty() -> {
                    val (title, message) = when {
                        uiState.contacts.isEmpty() -> "Nessun Contatto" to "Non ci sono ancora Contatti per questo Cliente"
                        uiState.selectedFilter != ContactFilter.ALL -> "Nessun risultato" to "Non ci sono Contatti che corrispondono al filtro '${getContactFilterDisplayName(uiState.selectedFilter)}'"
                        else -> "Lista vuota" to "Errore nel caricamento dati"
                    }
                    EmptyState(
                        textTitle = title,
                        textMessage = message,
                        iconImageVector = Icons.Outlined.Contacts,
                        iconContentDescription = "Nessun Contatto",
                        iconActionImageVector = Icons.Default.Add,
                        iconActionContentDescription = "Nuovo contatto",
                        textAction = "Nuovo Contatto",
                        onAction = { onNavigateToCreateContact(clientId) }
                    )
                }

                else -> {
                    ContactListContent(
                        contacts = uiState.filteredContacts,
                        onContactClick = onNavigateToContactDetail,
                        onContactEdit = onNavigateToEditContact,
                        onContactDelete = viewModel::deleteContact,
                        onSetPrimaryContact = viewModel::setPrimaryContact,
                        isSettingPrimary = uiState.isSettingPrimary
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

            // FAB per nuovo contatto
            FloatingActionButton(
                onClick = { onNavigateToCreateContact(clientId) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuovo contatto"
                )
            }
        }
    }
}

@Composable
private fun ContactListContent(
    contacts: List<ContactWithStats>,
    onContactEdit: (String) -> Unit,
    onContactClick: (String) -> Unit,
    onContactDelete: (String) -> Unit,
    onSetPrimaryContact: (String) -> Unit,
    isSettingPrimary: String?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = contacts,
            key = { it.contact.id }
        ) { contactWithStats ->
            ContactCard(
                contact = contactWithStats.contact,
                onClick = { onContactClick(contactWithStats.contact.id) },
                onEdit = { onContactEdit(contactWithStats.contact.id) },
                onDelete = { onContactDelete(contactWithStats.contact.id) },
                onSetPrimary = { onSetPrimaryContact(contactWithStats.contact.id) },
                isSettingPrimary = isSettingPrimary == contactWithStats.contact.id,
                variant = ContactCardVariant.FULL
            )
        }
    }
}


@Composable
private fun ActiveFiltersChipRow(
    selectedFilter: ContactFilter,
    selectedSort: ContactSortOrder,
    onClearFilter: () -> Unit,
    onClearSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        if (selectedFilter != ContactFilter.ALL) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearFilter,
                    label = { Text("Filtro: ${getContactFilterDisplayName(selectedFilter)}") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Rimuovi filtro",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        if (selectedSort != ContactSortOrder.NAME) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearSort,
                    label = { Text("Ordine: ${getContactSortDisplayName(selectedSort)}") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Rimuovi ordinamento",
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                text = { Text(getContactSortDisplayName(sortOrder)) },
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

// Helper function for Sorting display names
private fun getContactSortDisplayName(sortOrder: ContactSortOrder): String {
    return when (sortOrder) {
        ContactSortOrder.NAME -> "Nome"
        ContactSortOrder.CREATED_RECENT -> "Più Recenti"
        ContactSortOrder.CREATED_OLDEST -> "Meno Recenti"
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
                text = { Text(getContactFilterDisplayName(filter)) },
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

// Helper function for Filtering display names
private fun getContactFilterDisplayName(filter: ContactFilter): String {
    return when (filter) {
        ContactFilter.ALL -> "Tutti"
        ContactFilter.ACTIVE -> "Attivi"
        ContactFilter.INACTIVE -> "Inattivi"
        ContactFilter.PRIMARY_ONLY -> "Solo Primario"
    }
}