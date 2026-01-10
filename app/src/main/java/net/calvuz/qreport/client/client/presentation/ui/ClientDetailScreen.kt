package net.calvuz.qreport.client.client.presentation.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactStatistics
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandOperationalStatus
import net.calvuz.qreport.client.contact.presentation.ui.components.ContactsStatisticsSummary
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.client.client.domain.model.ClientWithDetails
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.model.ContractStatistics
import net.calvuz.qreport.client.contract.presentation.ui.components.ContractListItem
import net.calvuz.qreport.client.contract.presentation.ui.components.ContractsStatisticsSummary
import net.calvuz.qreport.client.facility.domain.model.FacilityWithIslands
import net.calvuz.qreport.app.app.presentation.components.EmptyTabContent
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.client.facility.presentation.ui.components.FacilityStatisticsSummary
import net.calvuz.qreport.app.util.callContact
import timber.log.Timber

/**
 * Screen per il dettaglio cliente con 4 tab - VERSIONE FINALE CON GESTIONE FACILITY
 *
 * Features:
 * - Tab: Info, Facilities, Contacts, History
 * - ✅ Gestione completa facilities (CRUD) nel tab dedicato
 * - ✅ UI consistency: "Stabilimenti" e "Nuovo" button
 * - ✅ Colori consistenti tra tab
 * - Chiamata diretta contatti con icone telefono
 * - Navigation verso gestione facility complete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    modifier: Modifier = Modifier,
    clientId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleteClient: () -> Unit,

    // Facility navigation callbacks
    onNavigateToFacilityList: (String) -> Unit, // Navigate to full facility list
    onNavigateToCreateFacility: (String) -> Unit, // Create new facility
    onNavigateToEditFacility: (String, String) -> Unit, // Edit facility (clientId, facilityId)
    onNavigateToFacilityDetail: (String, String) -> Unit = { _, _ -> }, // Facility detail
    onNavigateToIslandDetail: (String, String) -> Unit = { _, _ -> }, // Island detail

    // Contacts navigation callbacks
    onNavigateToContactList: (String, String) -> Unit = { _, _ -> }, // (clientId, clientName)
    onNavigateToCreateContact: (String, String) -> Unit = { _, _ -> }, // (clientId)
    onNavigateToEditContact: (String) -> Unit = { }, // (contactId)
    onNavigateToContactDetail: (String) -> Unit = { }, // (contactId)

    // Check-up navigation callbacks
    onNavigateToCreateCheckUp: (String) -> Unit = { }, // (clientId)

    // Contracts navigation callbacks
    onNavigateToContractList: (String, String) -> Unit = { _, _ -> },    //clientId
    onNavigateToCreateContract: (String, String) -> Unit = { _, _ -> }, // (clientId, clientName)
    onNavigateToEditContract: (String) -> Unit = { }, // (contractId)


    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clientName = uiState.companyName


    // ✅ Handle delete success - Navigate back automatically
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            viewModel.resetDeleteState()
            onDeleteClient()  // Navigate back to client list
        }
    }

    // ✅ Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirmation,
            title = { Text("Elimina Cliente") },
            text = {
                Text("Sei sicuro di voler eliminare ${uiState.companyName}? Questa azione non può essere annullata.")
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteClient,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirmation) {
                    Text("Annulla")
                }
            }
        )
    }

    // ✅ Load client details when screen opens
    LaunchedEffect(clientId) {
        viewModel.loadClientDetails(clientId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = uiState.companyName.takeIf { it.isNotBlank() } ?: "Cliente",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                // Delete button
                if (uiState.hasData) {
                    IconButton(
                        onClick = viewModel::showDeleteConfirmation,  // Show confirmation dialog
                        enabled = !uiState.isDeleting
                    ) {
                        if (uiState.isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                tint = MaterialTheme.colorScheme.error,
                                contentDescription = "Elimina cliente"
                            )
                        }
                    }
                }

                // Edit button
                if (uiState.hasData) {
                    IconButton(onClick = { onNavigateToEdit(clientId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica cliente"
                        )
                    }
                }

                // Refresh button
                IconButton(onClick = viewModel::refreshData) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Aggiorna"
                    )
                }
            }
        )

        when {
            uiState.isLoading -> {
                LoadingState()
            }

            uiState.error != null -> {
                ErrorState(
                    error = uiState.error!!,
                    onRetry = viewModel::refreshData,
                    onDismiss = viewModel::dismissError
                )
            }

            uiState.hasData -> {
                ClientDetailContent(
                    uiState = uiState,
                    clientId = clientId,
                    clientName = clientName,
                    onTabSelected = viewModel::selectTab,

                    // Facility management callbacks
                    onFacilityClick = { facilityId ->
                        onNavigateToFacilityDetail(
                            clientId,
                            facilityId
                        )
                    },
                    onIslandClick = onNavigateToIslandDetail,
                    onManageFacilities = { onNavigateToFacilityList(clientId) },
                    onCreateFacility = { onNavigateToCreateFacility(clientId) },
                    onEditFacility = { facilityId ->
                        onNavigateToEditFacility(
                            clientId,
                            facilityId
                        )
                    },

                    // Contact callbacks
                    onContactClick = onNavigateToContactDetail,
                    onEditContact = onNavigateToEditContact,
                    onCreateContact = { onNavigateToCreateContact(clientId, clientName) },
                    onViewAllContacts = {
                        onNavigateToContactList(clientId, uiState.companyName)
                    },

                    // Contracts callbacks
                    onCreateContract = { onNavigateToCreateContract(clientId, clientName) },
                    onEditContract = onNavigateToEditContract,
                    onViewAllContracts = { onNavigateToContractList(clientId, clientName) },

                    // Check-up callbacks
                    onCreateCheckUp = { onNavigateToCreateCheckUp(clientId) }
                )
            }

            else -> {
                EmptyState(
                    textTitle = "Nessun cliente trovato",
                    textMessage = "Aggiungi un cliente per iniziare",
                    iconImageVector = Icons.Default.Person,
                    iconContentDescription = "Nessun cliente trovato",
//                    iconActionImageVector = Icons.Default.Add,
//                    iconActionContentDescription = "Aggiungi un cliente per iniziare",
//                    textAction = "Nuovo Cliente",
//                    onAction = viewModel::createClient,
                )
            }
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Could show snackbar or other error handling
        }
    }
}

@Composable
private fun ClientDetailContent(
    uiState: ClientDetailUiState,
    clientId: String,
    clientName: String,
    onTabSelected: (ClientDetailTab) -> Unit,

    // Facility callbacks
    onFacilityClick: (String) -> Unit,
    onIslandClick: (String, String) -> Unit,
    onManageFacilities: () -> Unit,
    onCreateFacility: () -> Unit,
    onEditFacility: (String) -> Unit,

    // Contact callbacks
    onContactClick: (String) -> Unit,
    onEditContact: (String) -> Unit,
    onCreateContact: () -> Unit,
    onViewAllContacts: () -> Unit,

    // Check-Up callbacks
    onCreateCheckUp: () -> Unit,

    // Contracts
    onCreateContract: () -> Unit,
    onEditContract: (String) -> Unit,
    onViewAllContracts: () -> Unit,
) {
    Column {
        // Header con badge status e industry
        if (uiState.statusBadge.isNotBlank() || !uiState.industry.isNullOrBlank()) {
            HeaderSection(
                statusBadge = uiState.statusBadge,
                statusColor = uiState.statusBadgeColor.toLongOrNull() ?: 0xFF00B050L,
                industry = uiState.industry,
                statisticsSummary = uiState.statisticsSummary
            )
        }

        // Tab Row
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            // Tab Info
            Tab(
                selected = uiState.selectedTab == ClientDetailTab.INFO,
                onClick = { onTabSelected(ClientDetailTab.INFO) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info"
                    )
                },
                text = { Text("Info") },
            )

            // Tab Facilities
            Tab(
                selected = uiState.selectedTab == ClientDetailTab.FACILITIES,
                onClick = { onTabSelected(ClientDetailTab.FACILITIES) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Factory,
                        contentDescription = "Stabilimenti"
                    )
                },
                text = {
                    Text("Stabilim")
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Text("Stab")
//                        uiState.facilitiesCount.takeIf { it > 0 }?.let { count ->
//                            Badge { Text(count.toString()) }
//                        }
//                    }
                },
            )

            // Tab Contacts
            Tab(
                selected = uiState.selectedTab == ClientDetailTab.CONTACTS,
                onClick = { onTabSelected(ClientDetailTab.CONTACTS) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = "Contatti",
                    )
                },
                text = {
                    Text("Contatti")
//                    uiState.contactsCount.takeIf { it > 0 }?.let { count ->
//                        Badge { Text(count.toString()) }
//                    }
//                    Row(
////                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Text("Contatti")
//                        uiState.contactsCount.takeIf { it > 0 }?.let { count ->
//                            Badge { Text(count.toString()) }
//                        }
//                    }
                },
            )

            // Tab Contracts
            Tab(
                selected = uiState.selectedTab == ClientDetailTab.CONTRACTS,
                onClick = { onTabSelected(ClientDetailTab.CONTRACTS) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.AssignmentTurnedIn,
                        contentDescription = "Contratti"
                    )
                },
                text = {
                    Text("Contratti")
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Text("Storico")
//                        uiState.checkUpsCount.takeIf { it > 0 }?.let { count ->
//                            Badge { Text(count.toString()) }
//                        }
//                    }
                }
            )
        }

        // Tab Content
        when (uiState.selectedTab) {
            ClientDetailTab.INFO -> {
                Timber.w("INFO {${uiState.clientDetails}}")
                InfoTabContent(
                    clientDetails = uiState.clientDetails!!,
                    modifier = Modifier.weight(1f)
                )
            }

            ClientDetailTab.FACILITIES -> {
                // ✅ Tab Facilities con gestione completa
                FacilitiesTabContentWithManagement(
                    facilitiesWithIslands = uiState.facilitiesWithIslands,
                    onFacilityClick = onFacilityClick,
                    onIslandClick = onIslandClick,
                    onManageFacilities = onManageFacilities,
                    onCreateFacility = onCreateFacility,
                    onEditFacility = onEditFacility,
                    modifier = Modifier.weight(1f)
                )
            }

            ClientDetailTab.CONTACTS -> {
                // ✅ Tab Contacts con statistiche dettagliate
                ContactsTabContent(
                    contacts = uiState.contacts,
                    contactStatistics = uiState.contactStatistics, // ← STATISTICHE AGGIUNTE
                    onViewAllContacts = onViewAllContacts,
                    onContactClick = onContactClick,
                    onCreateContact = onCreateContact,
                    onEditContact = onEditContact,
                    modifier = Modifier.weight(1f)
                )
            }

            ClientDetailTab.CONTRACTS -> {
                ContractsTabContent(
                    contracts = uiState.contracts,
                    contractStatistics = uiState.contractStatistics, // ← STATISTICHE AGGIUNTE
                    onViewAllContracts = onViewAllContracts,
                    onContractClick = { },
                    onCreateContract = onCreateContract,
                    onEditContract = onEditContract
                )
            }
        }
    }
}

// TAB Info
@Composable
private fun InfoTabContent(
    clientDetails: ClientWithDetails,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard(
                title = "Informazioni Generali",
                icon = Icons.Default.Business
            ) {
                InfoItem(
                    label = "Ragione Sociale",
                    value = clientDetails.client.companyName
                )

                clientDetails.client.vatNumber?.let {
                    InfoItem(
                        label = "Partita IVA",
                        value = it
                    )
                }

                clientDetails.client.industry?.let {
                    InfoItem(
                        label = "Settore",
                        value = it
                    )
                }
            }
        }

        item {
            InfoCard(
                title = "Sede Legale",
                icon = Icons.Default.LocationOn
            ) {
                clientDetails.client.headquarters?.let { address ->
                    InfoItem(
                        label = "Indirizzo",
                        value = address.toDisplayString()
                    )
                }
            }
        }

        item {
            InfoCard(
                title = "Metadati",
                icon = Icons.Default.Info
            ) {
                InfoItem(
                    label = "Creato",
                    value = formatTimestamp(clientDetails.client.createdAt)
                )
                InfoItem(
                    label = "Ultima modifica",
                    value = formatTimestamp(clientDetails.client.updatedAt)
                )
                InfoItem(
                    label = "Stato",
                    value = if (clientDetails.client.isActive) "Attivo" else "Inattivo"
                )
            }
        }
    }
}

// TAB Facilities
@Composable
private fun FacilitiesTabContentWithManagement(
    facilitiesWithIslands: List<FacilityWithIslands>,
    onFacilityClick: (String) -> Unit,
    onIslandClick: (String, String) -> Unit,
    onManageFacilities: () -> Unit,
    onCreateFacility: () -> Unit,
    onEditFacility: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // ✅ Header con azioni gestione - UI CORRETTA come richiesto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ CORRETTO: Solo "Stabilimenti" invece di "Gestione Stabilimenti"
            Text(
                text = "Stabilimenti (${facilitiesWithIslands.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Vedi tutti
                if (facilitiesWithIslands.isNotEmpty()) {
                    OutlinedButton(onClick = onManageFacilities) {
                        Text("Vedi tutti")
                    }
                }

                // Nuovo
                Button(onClick = onCreateFacility) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuovo")
                }
            }
        }

        // Stats
        if (facilitiesWithIslands.isNotEmpty()) {
            FacilityStatisticsSummary(
                statistics = facilitiesWithIslands,
            )
        }

        // Lista facility esistenti
        if (facilitiesWithIslands.isEmpty()) {
            EmptyTabContent(
                icon = Icons.Outlined.Factory,
                message = "Nessuno Stabilimento",
                subMessage = "Aggiungi uno Stabilimento per questo Cliente",
                onButtonClick = onCreateFacility,
                onButtonMessage = "Nuovo Stabilimento"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(facilitiesWithIslands) { facilityWithIslands ->
                    FacilityItemWithActions(
                        facilityWithIslands = facilityWithIslands,
                        onFacilityClick = onFacilityClick,
                        onIslandClick = onIslandClick,
                        onEditFacility = onEditFacility
                    )
                }
            }
        }
    }
}

// TAB Clients
@Composable
private fun ContactsTabContent(
    modifier: Modifier = Modifier,
    contacts: List<Contact>,
    contactStatistics: ContactStatistics? = null,
    onViewAllContacts: () -> Unit,
    onContactClick: (String) -> Unit,
    onCreateContact: () -> Unit,
    onEditContact: (String) -> Unit,
) {
    Column(modifier = modifier) {
        // ✅ Header con stesso stile di Facilities per consistency
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Contatti (${contacts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Vedi tutti
                if (contacts.isNotEmpty()) {
                    OutlinedButton(onClick = onViewAllContacts) {
                        Text("Vedi tutti")
                    }
                }

                // Nuovo
                Button(onClick = onCreateContact) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuovo")
                }
            }
        }

        // Stats
        if (contacts.isNotEmpty()) {
            contactStatistics?.let { stats ->
                ContactsStatisticsSummary(
                    statistics = stats
                )
            }
        }

        // ✅ CORRETTO: Logica if/else per contatti vuoti o con contenuto
        if (contacts.isEmpty()) {
            EmptyTabContent(
                icon = Icons.Outlined.Person,
                message = "Nessun Contatto",
                subMessage = "Aggiungi un contatto per questo cliente",
                onButtonClick = onCreateContact,
                onButtonMessage = "Nuovo Contatto"
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts.take(5)) { contact ->
                    ContactItem(
                        // ← USA ContactItem ESISTENTE
                        contact = contact,
                        onContactClick = onContactClick,
                        onEditContact = onEditContact,
                    )
                }

                // Se ci sono più di 5 contatti, mostra indicatore
                if (contacts.size > 5) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "... e altri ${contacts.size - 5} contatti",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB Contracts *
@Composable
private fun ContractsTabContent(
    modifier: Modifier = Modifier,
    contracts: List<Contract>,
    contractStatistics: ContractStatistics? = null,
    onViewAllContracts: () -> Unit,
    onContractClick: (String) -> Unit,
    onCreateContract: () -> Unit,
    onEditContract: (String) -> Unit,
) {
    Column(modifier = modifier) {
        // ✅ Header con stesso stile di Facilities per consistency
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Contratti (${contracts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Vedi tutti
                if (contracts.isNotEmpty()) {
                    OutlinedButton(onClick = onViewAllContracts) {
                        Text("Vedi tutti")
                    }
                }

                // Nuovo
                Button(onClick = onCreateContract) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuovo")
                }
            }
        }

        // Stats
        if (contracts.isNotEmpty()) {
            contractStatistics?.let { stats ->
                ContractsStatisticsSummary(
                    statistics = stats
                )
            }
        }

        // ✅ CORRETTO: Logica if/else per contatti vuoti o con contenuto
        if (contracts.isEmpty()) {
            EmptyTabContent(
                icon = Icons.Outlined.AssignmentTurnedIn,
                message = "Nessun Contratto",
                subMessage = "Aggiungi un contratto per questo cliente",
                onButtonClick = onCreateContract,
                onButtonMessage = "Nuovo Contratto"
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contracts.take(5)) { contract ->
                    ContractListItem(
                        // ← USA ContactItem ESISTENTE
                        contract = contract,
                        onContractClick = onContractClick,
                        onEditContract = onEditContract,
                    )
                }

                // Se ci sono più di 5 contatti, mostra indicatore
                if (contracts.size > 5) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "... e altri ${contracts.size - 5} contratti",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✅ Facility item con azioni
@Composable
private fun FacilityItemWithActions(
    facilityWithIslands: FacilityWithIslands,
    onFacilityClick: (String) -> Unit,
    onIslandClick: (String, String) -> Unit,
    onEditFacility: (String) -> Unit
) {
    val facility = facilityWithIslands.facility

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = { onFacilityClick(facility.id) } // ✅ Tutta la card è ora cliccabile
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header facility con azioni
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Primary badge
                    if (facility.isPrimary) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Primario",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = facility.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${facility.facilityType.displayName} • ${facility.address.city ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Solo azione edit (rimuovo la freccia destra)
                IconButton(
                    onClick = { onEditFacility(facility.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifica",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Isole associate (se presenti)
            if (facilityWithIslands.islands.isNotEmpty()) {
                Text(
                    text = "Isole (${facilityWithIslands.islands.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                facilityWithIslands.islands.take(3).forEach { island ->
                    IslandItemCompact(
                        island = island,
                        onClick = { onIslandClick(facilityWithIslands.facility.id, island.id) }
                    )
                }

                if (facilityWithIslands.islands.size > 3) {
                    TextButton(
                        onClick = { onFacilityClick(facility.id) }
                    ) {
                        Text("Vedi altre ${facilityWithIslands.islands.size - 3} isole")
                    }
                }
            } else {
                Text(
                    text = "Nessuna isola associata",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ✅ Island item compatto
@Composable
private fun IslandItemCompact(
    island: Island,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = island.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = island.islandType.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = when (island.islandOperationalStatus) {
                    IslandOperationalStatus.OPERATIONAL -> Icons.Default.CheckCircle
                    IslandOperationalStatus.MAINTENANCE_DUE -> Icons.Default.Warning
                    IslandOperationalStatus.INACTIVE -> Icons.Default.Cancel
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when (island.islandOperationalStatus) {
                    IslandOperationalStatus.OPERATIONAL -> MaterialTheme.colorScheme.primary
                    IslandOperationalStatus.MAINTENANCE_DUE -> MaterialTheme.colorScheme.error
                    IslandOperationalStatus.INACTIVE -> MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

@Composable
private fun ContactItem(
    contact: Contact,
    onContactClick: (String) -> Unit,
    onEditContact: (String) -> Unit,
) {
    val context = LocalContext.current

    Card(
        onClick = { onContactClick(contact.id) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (contact.isPrimary) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Primario",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = contact.fullName,   // .displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                contact.role?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                contact.email?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                contact.phone?.let { phone ->
                    IconButton(
                        onClick = {
                            val result = callContact(context, phone)
                            if (!result) {
                                Toast.makeText(
                                    context,
                                    "Errore di chiamata",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Chiama",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = { onEditContact(contact.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifica",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Resto delle funzioni esistenti dal file originale...
@Composable
private fun HeaderSection(
    statusBadge: String,
    statusColor: Long,
    industry: String?,
    statisticsSummary: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status badge
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                if (statusBadge.isNotBlank()) {
                    AssistChip(
                        onClick = { },
                        label = { Text(statusBadge) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(statusColor)
                        ),
                    )
                }
            }

            // Industry + Statistics
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.End
            ) {
                industry?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = statisticsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
private fun InfoItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
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
                text = error,
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

// Helper functions
private fun formatTimestamp(timestamp: Instant): String {
    val now = Clock.System.now()
    val updated = timestamp
    val diffMillis = (now - updated).inWholeMilliseconds

    return when {
        diffMillis < 60000 -> "Ora"
        diffMillis < 3600000 -> "${diffMillis / 60000} min fa"
        diffMillis < 86400000 -> "${diffMillis / 3600000}h fa"
        else -> "${diffMillis / 86400000} giorni fa"
    }
}