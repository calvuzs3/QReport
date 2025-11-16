package net.calvuz.qreport.presentation.screen.client.contact

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.presentation.components.QReportSearchBar

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    clientId: String,
    clientName: String,
    onNavigateBack: () -> Unit,
    onNavigateToCreateContact: (String) -> Unit,
    onNavigateToEditContact: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load contacts when screen opens
    LaunchedEffect(clientId) {
        viewModel.loadContacts(clientId)
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
                // Filter toggle
                IconButton(onClick = viewModel::toggleActiveFilter) {
                    Icon(
                        imageVector = if (uiState.showActiveOnly) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (uiState.showActiveOnly) {
                            "Mostra tutti i contatti"
                        } else {
                            "Mostra solo contatti attivi"
                        }
                    )
                }

                // Refresh button
                IconButton(onClick = viewModel::refreshContacts) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Aggiorna"
                    )
                }
            }
        )

        // Search bar
        QReportSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            placeholder = "Cerca contatti per nome, ruolo, email...",
            modifier = Modifier.padding(16.dp)
        )

        // Stats row
        if (uiState.hasContacts) {
            ContactStatsRow(
                stats = uiState.contactStats,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Content with Pull to Refresh
        val pullToRefreshState = rememberPullToRefreshState()

        // Handle pull to refresh
        LaunchedEffect(pullToRefreshState.isRefreshing) {
            if (pullToRefreshState.isRefreshing) {
                viewModel.refreshContacts()
            }
        }

        // Reset refresh state when not refreshing
        LaunchedEffect(uiState.isLoading) {
            if (!uiState.isLoading && pullToRefreshState.isRefreshing) {
                pullToRefreshState.endRefresh()
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

                uiState.isEmpty -> {
                    EmptyState(
                        onCreateFirst = { onNavigateToCreateContact(clientId) }
                    )
                }

                else -> {
                    ContactListContent(
                        contacts = uiState.filteredContacts,
                        isSearchActive = uiState.isSearchActive,
                        searchQuery = uiState.searchQuery,
                        onContactClick = onNavigateToEditContact,
                        onDeleteContact = viewModel::deleteContact,
                        onSetPrimaryContact = viewModel::setPrimaryContact,
                        isDeletingContact = uiState.isDeletingContact,
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
                    contentDescription = "Nuovo Contatto"
                )
            }
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Additional error handling if needed
        }
    }
}

@Composable
private fun ContactStatsRow(
    stats: ContactStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stats.totalContacts} contatt${if (stats.totalContacts == 1) "o" else "i"} attiv${if (stats.totalContacts == 1) "o" else "i"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (stats.hasPrimaryContact) {
                AssistChip(
                    onClick = { },
                    label = { Text("Referente primario impostato") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            } else {
                AssistChip(
                    onClick = { },
                    label = { Text("Nessun referente primario") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun ContactListContent(
    contacts: List<Contact>,
    isSearchActive: Boolean,
    searchQuery: String,
    onContactClick: (String) -> Unit,
    onDeleteContact: (String) -> Unit,
    onSetPrimaryContact: (String) -> Unit,
    isDeletingContact: String?,
    isSettingPrimary: String?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search results info
        if (isSearchActive) {
            item {
                Text(
                    text = "Trovati ${contacts.size} risultat${if (contacts.size == 1) "o" else "i"} per \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        items(
            items = contacts,
            key = { it.id }
        ) { contact ->
            ContactCard(
                contact = contact,
                onClick = { onContactClick(contact.id) },
                onDelete = { onDeleteContact(contact.id) },
                onSetPrimary = { onSetPrimaryContact(contact.id) },
                isDeleting = isDeletingContact == contact.id,
                isSettingPrimary = isSettingPrimary == contact.id
            )
        }
    }
}

@Composable
private fun ContactCard(
    contact: Contact,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSetPrimary: () -> Unit,
    isDeleting: Boolean,
    isSettingPrimary: Boolean
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = contact.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (contact.isPrimary) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Referente primario",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (!contact.isActive) {
                            AssistChip(
                                onClick = { },
                                label = { Text("Inattivo", style = MaterialTheme.typography.labelSmall) },
                                enabled = false
                            )
                        }
                    }

                    contact.roleDescription.takeIf { it.isNotBlank() }?.let { role ->
                        Text(
                            text = role,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Actions
                Row {
                    if (!contact.isPrimary && contact.isActive) {
                        IconButton(
                            onClick = onSetPrimary,
                            enabled = !isSettingPrimary
                        ) {
                            if (isSettingPrimary) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = "Imposta come primario",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !isDeleting && contact.isActive
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Contact info
            contact.primaryContact?.let { contactInfo ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            contactInfo.contains("@") -> Icons.Default.Email
                            else -> Icons.Default.Phone
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = contactInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina Contatto") },
            text = {
                Text("Sei sicuro di voler eliminare il contatto '${contact.fullName}'?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Caricamento contatti...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    // Usa una variabile locale per evitare problemi di smart cast
    val errorMessage = error.takeIf { it.isNotBlank() } ?: "Errore sconosciuto"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Errore",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Chiudi")
                }

                Button(onClick = onRetry) {
                    Text("Riprova")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onCreateFirst: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Contacts,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Nessun contatto",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Non hai ancora aggiunto nessun contatto per questo cliente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onCreateFirst) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi primo contatto")
            }
        }
    }
}