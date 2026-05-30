package net.calvuz.qreport.client.client.presentation.ui

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
import net.calvuz.qreport.app.app.presentation.components.EmptyTabContent
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QReportErrorState
import net.calvuz.qreport.app.util.callContact
import net.calvuz.qreport.client.client.presentation.model.ClientWithDetails
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactStatistics
import net.calvuz.qreport.client.contact.presentation.ui.components.ContactsStatisticsSummary
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.model.ContractStatistics
import net.calvuz.qreport.client.contract.presentation.ui.components.ContractListItem
import net.calvuz.qreport.client.contract.presentation.ui.components.ContractsStatisticsSummary
import net.calvuz.qreport.client.facility.domain.model.FacilityWithIslands
import net.calvuz.qreport.client.facility.presentation.ui.components.FacilityStatisticsSummary
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.client.island.domain.model.IslandOperationalStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    modifier: Modifier = Modifier,
    clientId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleteClient: () -> Unit,
    onNavigateToFacilityList: (String) -> Unit,
    onNavigateToCreateFacility: (String) -> Unit,
    onNavigateToEditFacility: (String, String) -> Unit,
    onNavigateToFacilityDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToIslandDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToContactList: (String, String) -> Unit = { _, _ -> },
    onNavigateToCreateContact: (String, String) -> Unit = { _, _ -> },
    onNavigateToEditContact: (String) -> Unit = { },
    onNavigateToContactDetail: (String) -> Unit = { },
    onNavigateToCreateCheckUp: (String) -> Unit = { },
    onNavigateToContractList: (String, String) -> Unit = { _, _ -> },
    onNavigateToCreateContract: (String, String) -> Unit = { _, _ -> },
    onNavigateToEditContract: (String) -> Unit = { },
    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clientName = uiState.companyName

    // Handle delete success — navigate back automatically
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            viewModel.resetDeleteState()
            onDeleteClient()
        }
    }

    // Load client details on entry
    LaunchedEffect(clientId) {
        viewModel.loadClientDetails(clientId)
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirmation,
            title = { Text(stringResource(R.string.client_detail_delete_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.client_detail_delete_dialog_message,
                        uiState.companyName
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteClient,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.client_detail_delete_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirmation) {
                    Text(stringResource(R.string.client_detail_delete_dialog_cancel))
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Text(
                    text = uiState.companyName.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.client_detail_title_fallback),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.client_detail_action_back)
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
                                contentDescription = stringResource(R.string.client_detail_action_delete)
                            )
                        }
                    }
                    IconButton(onClick = { onNavigateToEdit(clientId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.client_detail_action_edit)
                        )
                    }
                }
                IconButton(onClick = viewModel::refreshData) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.client_detail_action_refresh)
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

            uiState.hasData -> ClientDetailContent(
                uiState = uiState,
                clientId = clientId,
                clientName = clientName,
                onTabSelected = viewModel::selectTab,
                onEdit = { onNavigateToEdit(clientId) },
                onFacilityClick = { facilityId -> onNavigateToFacilityDetail(clientId, facilityId) },
                onIslandClick = onNavigateToIslandDetail,
                onManageFacilities = { onNavigateToFacilityList(clientId) },
                onCreateFacility = { onNavigateToCreateFacility(clientId) },
                onEditFacility = { facilityId -> onNavigateToEditFacility(clientId, facilityId) },
                onContactClick = onNavigateToContactDetail,
                onEditContact = onNavigateToEditContact,
                onCreateContact = { onNavigateToCreateContact(clientId, clientName) },
                onViewAllContacts = { onNavigateToContactList(clientId, uiState.companyName) },
                onCreateContract = { onNavigateToCreateContract(clientId, clientName) },
                onEditContract = onNavigateToEditContract,
                onViewAllContracts = { onNavigateToContractList(clientId, clientName) },
                onCreateCheckUp = { onNavigateToCreateCheckUp(clientId) }
            )

            else -> EmptyState(
                textTitle = stringResource(R.string.client_detail_empty_title),
                textMessage = stringResource(R.string.client_detail_empty_message),
                iconImageVector = Icons.Default.Person,
                iconContentDescription = stringResource(R.string.client_detail_empty_title)
            )
        }
    }
}

// =============================================================================
// DETAIL CONTENT
// =============================================================================

@Composable
private fun ClientDetailContent(
    uiState: ClientDetailUiState,
    clientId: String,
    clientName: String,
    onTabSelected: (ClientDetailTab) -> Unit,
    onEdit: () -> Unit,
    onFacilityClick: (String) -> Unit,
    onIslandClick: (String, String) -> Unit,
    onManageFacilities: () -> Unit,
    onCreateFacility: () -> Unit,
    onEditFacility: (String) -> Unit,
    onContactClick: (String) -> Unit,
    onEditContact: (String) -> Unit,
    onCreateContact: () -> Unit,
    onViewAllContacts: () -> Unit,
    onCreateCheckUp: () -> Unit,
    onCreateContract: () -> Unit,
    onEditContract: (String) -> Unit,
    onViewAllContracts: () -> Unit,
) {
    Column {
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ClientDetailTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = when (tab) {
                                ClientDetailTab.FACILITIES -> Icons.Default.Factory
                                ClientDetailTab.CONTACTS -> Icons.Default.Contacts
                                ClientDetailTab.CONTRACTS -> Icons.Outlined.AssignmentTurnedIn
                                ClientDetailTab.INFO -> Icons.Default.Info
                            },
                            contentDescription = stringResource(tab.labelResId)
                        )
                    }
                )
            }
        }

        when (uiState.selectedTab) {
            ClientDetailTab.INFO -> InfoTabContent(
                modifier = Modifier.weight(1f),
                clientDetails = uiState.clientDetails!!,
                onEdit = onEdit
            )
            ClientDetailTab.FACILITIES -> FacilitiesTabContent(
                facilitiesWithIslands = uiState.facilitiesWithIslands,
                onFacilityClick = onFacilityClick,
                onIslandClick = onIslandClick,
                onManageFacilities = onManageFacilities,
                onCreateFacility = onCreateFacility,
                onEditFacility = onEditFacility,
                modifier = Modifier.weight(1f)
            )
            ClientDetailTab.CONTACTS -> ContactsTabContent(
                contacts = uiState.contacts,
                contactStatistics = uiState.contactStatistics,
                onViewAllContacts = onViewAllContacts,
                onContactClick = onContactClick,
                onCreateContact = onCreateContact,
                onEditContact = onEditContact,
                modifier = Modifier.weight(1f)
            )
            ClientDetailTab.CONTRACTS -> ContractsTabContent(
                contracts = uiState.contracts,
                contractStatistics = uiState.contractStatistics,
                onViewAllContracts = onViewAllContracts,
                onContractClick = { },
                onCreateContract = onCreateContract,
                onEditContract = onEditContract
            )
        }
    }
}

// =============================================================================
// TAB: INFO
// =============================================================================

@Composable
private fun InfoTabContent(
    modifier: Modifier = Modifier,
    clientDetails: ClientWithDetails,
    onEdit: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.client_detail_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.client_detail_info_action_edit))
            }
        }

        LazyColumn(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoCard(
                    title = stringResource(R.string.client_detail_info_card_general),
                    icon = Icons.Default.Business
                ) {
                    InfoItem(
                        label = stringResource(R.string.client_detail_info_field_company_name),
                        value = clientDetails.client.companyName
                    )
                    clientDetails.client.notes?.let {
                        InfoItem(
                            label = stringResource(R.string.client_detail_info_field_notes),
                            value = it
                        )
                    }
                }
            }

            item {
                InfoCard(
                    title = stringResource(R.string.client_detail_info_card_headquarters),
                    icon = Icons.Default.LocationOn
                ) {
                    clientDetails.client.headquarters?.let { address ->
                        InfoItem(
                            label = stringResource(R.string.client_detail_info_field_address),
                            value = address.toDisplayString()
                        )
                    }
                }
            }

            item {
                InfoCard(
                    title = stringResource(R.string.client_detail_info_card_metadata),
                    icon = Icons.Default.Info
                ) {
                    InfoItem(
                        label = stringResource(R.string.client_detail_info_field_created),
                        value = formatTimestamp(clientDetails.client.createdAt)
                    )
                    InfoItem(
                        label = stringResource(R.string.client_detail_info_field_updated),
                        value = formatTimestamp(clientDetails.client.updatedAt)
                    )
                    InfoItem(
                        label = stringResource(R.string.client_detail_info_field_status),
                        value = if (clientDetails.client.isActive)
                            stringResource(R.string.client_detail_info_status_active)
                        else
                            stringResource(R.string.client_detail_info_status_inactive)
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB: FACILITIES
// =============================================================================

@Composable
private fun FacilitiesTabContent(
    facilitiesWithIslands: List<FacilityWithIslands>,
    onFacilityClick: (String) -> Unit,
    onIslandClick: (String, String) -> Unit,
    onManageFacilities: () -> Unit,
    onCreateFacility: () -> Unit,
    onEditFacility: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.client_detail_facilities_header, facilitiesWithIslands.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (facilitiesWithIslands.isNotEmpty()) {
                    OutlinedButton(onClick = onManageFacilities) {
                        Text(stringResource(R.string.client_detail_facilities_view_all))
                    }
                }
                Button(onClick = onCreateFacility) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.client_detail_facilities_new))
                }
            }
        }

        if (facilitiesWithIslands.isNotEmpty()) {
            FacilityStatisticsSummary(statistics = facilitiesWithIslands)
        }

        if (facilitiesWithIslands.isEmpty()) {
            EmptyTabContent(
                icon = Icons.Outlined.Factory,
                message = stringResource(R.string.client_detail_facilities_empty_title),
                subMessage = stringResource(R.string.client_detail_facilities_empty_message),
                onButtonClick = onCreateFacility,
                onButtonMessage = stringResource(R.string.client_detail_facilities_empty_action)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
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

@Composable
private fun FacilityItemWithActions(
    facilityWithIslands: FacilityWithIslands,
    onFacilityClick: (String) -> Unit,
    onIslandClick: (String, String) -> Unit,
    onEditFacility: (String) -> Unit
) {
    val facility = facilityWithIslands.facility

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onFacilityClick(facility.id) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                    if (facility.isPrimary) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = stringResource(R.string.client_detail_facility_primary_badge),
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
                            text = "${facility.facilityType.displayName} • ${facility.address?.city ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { onEditFacility(facility.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.client_detail_facility_action_edit),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (facilityWithIslands.islands.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.client_detail_facility_islands_header, facilityWithIslands.islands.size),
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
                    TextButton(onClick = { onFacilityClick(facility.id) }) {
                        Text(
                            stringResource(
                                R.string.client_detail_facility_islands_more,
                                facilityWithIslands.islands.size - 3
                            )
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.client_detail_facility_no_islands),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IslandItemCompact(island: Island, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
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

// =============================================================================
// TAB: CONTACTS
// =============================================================================

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
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.client_detail_contacts_header, contacts.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (contacts.isNotEmpty()) {
                    OutlinedButton(onClick = onViewAllContacts) {
                        Text(stringResource(R.string.client_detail_contacts_view_all))
                    }
                }
                Button(onClick = onCreateContact) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.client_detail_contacts_new))
                }
            }
        }

        if (contacts.isNotEmpty()) {
            contactStatistics?.let { ContactsStatisticsSummary(statistics = it) }
        }

        if (contacts.isEmpty()) {
            EmptyTabContent(
                icon = Icons.Outlined.Person,
                message = stringResource(R.string.client_detail_contacts_empty_title),
                subMessage = stringResource(R.string.client_detail_contacts_empty_message),
                onButtonClick = onCreateContact,
                onButtonMessage = stringResource(R.string.client_detail_contacts_empty_action)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts.take(5)) { contact ->
                    ContactItem(
                        contact = contact,
                        onContactClick = onContactClick,
                        onEditContact = onEditContact
                    )
                }
                if (contacts.size > 5) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.client_detail_contacts_more, contacts.size - 5),
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
                            contentDescription = stringResource(R.string.client_detail_contact_primary_badge),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = contact.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                contact.role?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                contact.email?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                contact.phone?.let { phone ->
                    val callErrorMsg = stringResource(R.string.client_detail_contact_call_error)
                    IconButton(
                        onClick = {
                            if (!callContact(context, phone)) {
                                Toast.makeText(context, callErrorMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = stringResource(R.string.client_detail_contact_action_call),
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
                        contentDescription = stringResource(R.string.client_detail_contact_action_edit),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB: CONTRACTS
// =============================================================================

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
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.client_detail_contracts_header, contracts.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (contracts.isNotEmpty()) {
                    OutlinedButton(onClick = onViewAllContracts) {
                        Text(stringResource(R.string.client_detail_contracts_view_all))
                    }
                }
                Button(onClick = onCreateContract) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.client_detail_contracts_new))
                }
            }
        }

        if (contracts.isNotEmpty()) {
            contractStatistics?.let { ContractsStatisticsSummary(statistics = it) }
        }

        if (contracts.isEmpty()) {
            EmptyTabContent(
                icon = Icons.Outlined.AssignmentTurnedIn,
                message = stringResource(R.string.client_detail_contracts_empty_title),
                subMessage = stringResource(R.string.client_detail_contracts_empty_message),
                onButtonClick = onCreateContract,
                onButtonMessage = stringResource(R.string.client_detail_contracts_empty_action)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contracts.take(5)) { contract ->
                    ContractListItem(
                        contract = contract,
                        onContractClick = onContractClick,
                        onEditContract = onEditContract
                    )
                }
                if (contracts.size > 5) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.client_detail_contracts_more, contracts.size - 5),
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

// =============================================================================
// SHARED COMPOSABLE
// =============================================================================

@Composable
private fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

// =============================================================================
// HELPERS
// =============================================================================

@Composable
private fun formatTimestamp(timestamp: Instant): String {
    val diffMillis = (Clock.System.now() - timestamp).inWholeMilliseconds
    return when {
        diffMillis < 60_000L -> stringResource(R.string.client_detail_time_now)
        diffMillis < 3_600_000L -> stringResource(R.string.client_detail_time_minutes_ago, diffMillis / 60_000)
        diffMillis < 86_400_000L -> stringResource(R.string.client_detail_time_hours_ago, diffMillis / 3_600_000)
        else -> stringResource(R.string.client_detail_time_days_ago, diffMillis / 86_400_000)
    }
}