package net.calvuz.qreport.client.client.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.app.app.presentation.components.EmptyTabContent
import net.calvuz.qreport.app.app.presentation.components.QReportErrorState
import net.calvuz.qreport.app.app.presentation.components.QrLoadingState
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianCreatedAt
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDateTime
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianLastModified
import net.calvuz.qreport.client.client.presentation.model.ClientWithDetails
import net.calvuz.qreport.client.contact.domain.model.Contact
import net.calvuz.qreport.client.contact.domain.model.ContactStatistics
import net.calvuz.qreport.client.contact.presentation.model.ContactPkg
import net.calvuz.qreport.client.contact.presentation.ui.components.ContactCard
import net.calvuz.qreport.client.contact.presentation.ui.components.ContactsStatisticsSummary
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.domain.model.ContractStatistics
import net.calvuz.qreport.client.contract.presentation.model.ContractPkg
import net.calvuz.qreport.client.contract.presentation.ui.components.ContractListItem
import net.calvuz.qreport.client.contract.presentation.ui.components.ContractsStatisticsSummary
import net.calvuz.qreport.client.facility.domain.model.FacilityWithIslands
import net.calvuz.qreport.client.facility.presentation.model.FacilityPkg
import net.calvuz.qreport.client.facility.presentation.ui.components.FacilityCard
import net.calvuz.qreport.client.facility.presentation.ui.components.FacilityStatisticsSummary
import net.calvuz.qreport.client.island.presentation.ui.components.IslandCard
import net.calvuz.qreport.settings.domain.model.ListViewMode

@Suppress("ParamsComparedByRef")
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
                        R.string.client_detail_delete_dialog_message, uiState.companyName
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteClient, colors = ButtonDefaults.textButtonColors(
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
            })
    }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(title = {
            Text(text = uiState.companyName.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.client_detail_title_fallback),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }, navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.client_detail_action_back)
                )
            }
        }, actions = {
            if (uiState.hasData) {
                IconButton(
                    onClick = viewModel::showDeleteConfirmation, enabled = !uiState.isDeleting
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
        })

        when {
            uiState.isLoading -> QrLoadingState()

            uiState.error != null -> QReportErrorState(
                error = uiState.error!!,
                onRetry = viewModel::refreshData,
                onDismiss = viewModel::dismissError
            )

            uiState.hasData -> ClientDetailContent(
                uiState = uiState,
                onTabSelected = viewModel::selectTab,
                onEdit = { onNavigateToEdit(clientId) },
                onFacilityClick = { facilityId ->
                    onNavigateToFacilityDetail(
                        clientId,
                        facilityId
                    )
                },
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
                onCreateCheckUp = { onNavigateToCreateCheckUp(clientId) })

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

@Suppress("ParamsComparedByRef")
@Composable
private fun ClientDetailContent(
    uiState: ClientDetailUiState,
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
                                ClientDetailTab.FACILITIES -> FacilityPkg.icon
                                ClientDetailTab.CONTACTS -> ContactPkg.icon
                                ClientDetailTab.CONTRACTS -> ContractPkg.icon
                                ClientDetailTab.INFO -> Icons.Default.Info
                            }, contentDescription = when (tab) {
                                ClientDetailTab.FACILITIES -> stringResource(FacilityPkg.titleResId)
                                ClientDetailTab.CONTACTS -> stringResource(ContactPkg.titleResId)
                                ClientDetailTab.CONTRACTS -> stringResource(ContractPkg.titleResId)
                                ClientDetailTab.INFO -> stringResource(R.string.client_detail_info_title)
                            }
                        )
                    })
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

@Suppress("ParamsComparedByRef")
@Composable
private fun InfoTabContent(
    modifier: Modifier = Modifier, clientDetails: ClientWithDetails, onEdit: () -> Unit = {}
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
                text = stringResource(R.string.client_detail_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.client_detail_info_action_edit))
            }
        }

        LazyColumn(
            modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    DoubleInfoItem(
                        label = stringResource(R.string.client_detail_info_field_created),
                        valueStart = (clientDetails.client.createdAt.toItalianDateTime()),
                        valueEnd = (clientDetails.client.createdAt.toItalianCreatedAt())
                    )
                    DoubleInfoItem(
                        label = stringResource(R.string.client_detail_info_field_updated),
                        valueStart = (clientDetails.client.updatedAt.toItalianDateTime()),
                        valueEnd = (clientDetails.client.createdAt.toItalianLastModified())
                    )
                    DoubleInfoItem(
                        label = stringResource(R.string.client_detail_info_field_status),
                        valueStart = "",
                        valueEnd = if (clientDetails.client.isActive) stringResource(R.string.client_detail_info_status_active)
                        else stringResource(R.string.client_detail_info_status_inactive)
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB: FACILITIES
// =============================================================================

@Suppress("ParamsComparedByRef")
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.client_detail_facilities_header,
                    facilitiesWithIslands.size
                ), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onManageFacilities) {
                    Text(stringResource(R.string.client_detail_facilities_view_all))
                }
                Button(onClick = onCreateFacility) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_new))
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
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(facilitiesWithIslands) { facilityWithIslands ->
                    val facility = facilityWithIslands.facility
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Facility — COMPACT with edit action
                        FacilityCard(
                            facility = facility,
                            variant = ListViewMode.COMPACT,
                            onClick = { onFacilityClick(facility.id) },
                            onEdit = { onEditFacility(facility.id) },
                            onRestore = {},
                            showActions = true
                        )
                        // Islands preview — MINIMAL, max 5, indented
                        if (facilityWithIslands.islands.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.client_detail_facility_islands_header,
                                    facilityWithIslands.islands.size
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                            )
                            facilityWithIslands.islands.take(5).forEach { island ->
                                IslandCard(
                                    island = island,
                                    variant = ListViewMode.MINIMAL,
                                    onClick = { onIslandClick(facility.id, island.id) },
                                    showActions = false,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                            if (facilityWithIslands.islands.size > 5) {
                                TextButton(
                                    onClick = { onFacilityClick(facility.id) },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.client_detail_facility_islands_more,
                                            facilityWithIslands.islands.size - 5
                                        )
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.client_detail_facility_no_islands),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// TAB: CONTACTS
// =============================================================================

@Suppress("ParamsComparedByRef")
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
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
                    ContactCard(
                        contact = contact,
                        variant = ListViewMode.COMPACT,
                        onClick = { onContactClick(contact.id) },
                        onEdit = { onEditContact(contact.id) },
                        showActions = true
                    )
                }
                if (contacts.size > 5) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
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
                                    text = stringResource(
                                        R.string.client_detail_contacts_more,
                                        contacts.size - 5
                                    ),
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
// TAB: CONTRACTS
// =============================================================================

@Suppress("ParamsComparedByRef")
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
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
                            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
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
                                    text = stringResource(
                                        R.string.client_detail_contracts_more,
                                        contracts.size - 5
                                    ),
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
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DoubleInfoItem(label: String, valueStart: String, valueEnd: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = valueStart, style = MaterialTheme.typography.bodyMedium)
            Text(text = valueEnd, style = MaterialTheme.typography.bodyMedium)
        }
    }
}