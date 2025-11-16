package net.calvuz.qreport.presentation.screen.client.client

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.domain.model.client.Contact
import net.calvuz.qreport.domain.model.client.FacilityIsland
import net.calvuz.qreport.domain.model.client.OperationalStatus
import net.calvuz.qreport.domain.usecase.client.client.ClientWithDetails
import net.calvuz.qreport.domain.usecase.client.client.FacilityWithIslands

/**
 * Screen per il dettaglio cliente con 4 tab
 *
 * Features:
 * - Tab: Info, Facilities, Contacts, History
 * - Solo visualizzazione + bottone modifica
 * - ✅ NUOVO: Chiamata diretta contatti con icone telefono
 * - Navigation verso dettagli facility/island (quando implementati)
 * - Gestione stati loading/error/empty
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToFacilityDetail: (String) -> Unit = { }, // Implementazione futura
    onNavigateToIslandDetail: (String) -> Unit = { }, // Implementazione futura
    // Navigation callbacks per contatti
    onNavigateToContactList: (String, String) -> Unit = { _, _ -> }, // (clientId, clientName)
    onNavigateToCreateContact: (String) -> Unit = { }, // (clientId)
    onNavigateToEditContact: (String) -> Unit = { }, // (contactId)
    modifier: Modifier = Modifier,
    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ✅ NUOVO: Funzione per chiamare contatto
    val callContact = { phoneNumber: String ->
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to dialer if no CALL permission
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        }
    }

    // Load client details when screen opens
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
                // Edit button (solo se dati caricati)
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
                    onTabSelected = viewModel::selectTab,
                    onFacilityClick = onNavigateToFacilityDetail,
                    onIslandClick = onNavigateToIslandDetail,
                    onEditContact = onNavigateToEditContact,
                    onCreateContact = { onNavigateToCreateContact(clientId) },
                    onViewAllContacts = {
                        onNavigateToContactList(clientId, uiState.companyName)
                    },
                    // ✅ NUOVO: Callback per chiamate
                    onCallContact = callContact
                )
            }

            else -> {
                EmptyState()
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
    onTabSelected: (ClientDetailTab) -> Unit,
    onFacilityClick: (String) -> Unit,
    onIslandClick: (String) -> Unit,
    onEditContact: (String) -> Unit,
    onCreateContact: () -> Unit,
    onViewAllContacts: () -> Unit,
    // ✅ NUOVO: Callback per chiamate
    onCallContact: (String) -> Unit
) {
    Column {
        // Header con badge status e industry
        if (uiState.statusBadge.isNotBlank() || !uiState.industry.isNullOrBlank()) {
            HeaderSection(
                statusBadge = uiState.statusBadge,
                statusColor = uiState.statusBadgeColor,
                industry = uiState.industry,
                statisticsSummary = uiState.statisticsSummary
            )
        }

        // Tab Row
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ClientDetailTab.values().forEach { tab ->
                val count = when (tab) {
                    ClientDetailTab.INFO -> null
                    ClientDetailTab.FACILITIES -> uiState.facilitiesCount.takeIf { it > 0 }
                    ClientDetailTab.CONTACTS -> uiState.contactsCount.takeIf { it > 0 }
                    ClientDetailTab.HISTORY -> uiState.checkUpsCount.takeIf { it > 0 }
                }

                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(tab.title)
                            count?.let {
                                Badge { Text(it.toString()) }
                            }
                        }
                    }
                )
            }
        }

        // Tab Content
        when (uiState.selectedTab) {
            ClientDetailTab.INFO -> {
                InfoTabContent(
                    clientDetails = uiState.clientDetails!!,
                    modifier = Modifier.weight(1f)
                )
            }

            ClientDetailTab.FACILITIES -> {
                FacilitiesTabContent(
                    facilitiesWithIslands = uiState.facilitiesWithIslands,
                    onFacilityClick = onFacilityClick,
                    onIslandClick = onIslandClick,
                    modifier = Modifier.weight(1f)
                )
            }

            ClientDetailTab.CONTACTS -> {
                ContactsTabContent(
                    contacts = uiState.activeContacts,
                    onViewAllContacts = onViewAllContacts,
                    onCreateContact = onCreateContact,
                    onEditContact = onEditContact,
                    // ✅ NUOVO: Passa il callback per chiamate
                    onCallContact = onCallContact,
                    modifier = Modifier.weight(1f)
                )
            }

            ClientDetailTab.HISTORY -> {
                HistoryTabContent(
                    checkUpsCount = uiState.checkUpsCount,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ContactsTabContent(
    contacts: List<Contact>,
    onViewAllContacts: () -> Unit,
    onCreateContact: () -> Unit,
    onEditContact: (String) -> Unit,
    // ✅ NUOVO: Callback per chiamate
    onCallContact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header con azioni
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Referenti \n(${contacts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add contact button
                IconButton(onClick = onCreateContact) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aggiungi contatto"
                    )
                }

                // View all contacts button
                if (contacts.isNotEmpty()) {
                    TextButton(onClick = onViewAllContacts) {
                        Text("Vedi tutti")
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Content
        if (contacts.isEmpty()) {
            // Empty state with action
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
                        text = "Nessun Referente",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Non ci sono referenti configurati per questo cliente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(onClick = onCreateContact) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aggiungi primo referente")
                    }
                }
            }
        } else {
            // Contacts list (mostra solo i primi 3-4)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = contacts.take(4), // Mostra max 4 contatti
                    key = { it.id }
                ) { contact ->
                    ContactCard(
                        contact = contact,
                        onEdit = { onEditContact(contact.id) },
                        // ✅ NUOVO: Callback per chiamate
                        onCallPhone = if (contact.phone?.isNotBlank() == true) {
                            { onCallContact(contact.phone!!) }
                        } else null,
                        onCallMobile = if (contact.mobilePhone?.isNotBlank() == true) {
                            { onCallContact(contact.mobilePhone!!) }
                        } else null
                    )
                }

                // "View all" card se ci sono più di 4 contatti
                if (contacts.size > 4) {
                    item {
                        Card(
                            onClick = onViewAllContacts,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Vedi tutti i ${contacts.size} referenti",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✅ AGGIORNATO: ContactCard con icone telefono
@Composable
private fun ContactCard(
    contact: Contact,
    onEdit: () -> Unit = { },
    // ✅ NUOVO: Callback per chiamate telefono/cellulare
    onCallPhone: (() -> Unit)? = null,
    onCallMobile: (() -> Unit)? = null
) {
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ NUOVO: Icona telefono fisso
                    if (onCallPhone != null) {
                        IconButton(
                            onClick = onCallPhone,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Chiama telefono: ${contact.phone}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // ✅ NUOVO: Icona cellulare
                    if (onCallMobile != null) {
                        IconButton(
                            onClick = onCallMobile,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = "Chiama cellulare: ${contact.mobilePhone}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Stella per referente primario
                    if (contact.isPrimary) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Referente primario",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Icona modifica
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifica",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            contact.roleDescription.takeIf { it.isNotBlank() }?.let { role ->
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // ✅ MIGLIORATO: Mostra numeri separatamente
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                contact.mobilePhone?.takeIf { it.isNotBlank() }?.let { mobile ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = mobile,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                contact.email?.takeIf { it.isNotBlank() }?.let { email ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// =================================================================
// Il resto delle funzioni rimane uguale (HeaderSection, InfoTabContent,
// FacilitiesTabContent, HistoryTabContent, LoadingState, ErrorState, etc.)
// =================================================================

@Composable
private fun HeaderSection(
    statusBadge: String,
    statusColor: String,
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
            Column(modifier = Modifier.weight(1f)) {
                industry?.let { industryText ->
                    Text(
                        text = industryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (statisticsSummary.isNotBlank()) {
                    Text(
                        text = statisticsSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (statusBadge.isNotBlank()) {
                AssistChip(
                    onClick = { },
                    label = { Text(statusBadge) },
                    enabled = false
                )
            }
        }
    }
}

// InfoTabContent rimane uguale dalla versione originale...
@Composable
private fun InfoTabContent(
    clientDetails: ClientWithDetails,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            InfoCard(
                title = "Informazioni Aziendali",
                icon = Icons.Default.Business
            ) {
                InfoItem(
                    label = "Ragione Sociale",
                    value = clientDetails.client.companyName
                )

                clientDetails.client.vatNumber?.let { vatNumber ->
                    InfoItem(
                        label = "Partita IVA",
                        value = vatNumber
                    )
                }

                clientDetails.client.fiscalCode?.let { fiscalCode ->
                    InfoItem(
                        label = "Codice Fiscale",
                        value = fiscalCode
                    )
                }

                clientDetails.client.industry?.let { industry ->
                    InfoItem(
                        label = "Settore",
                        value = industry
                    )
                }

                clientDetails.client.website?.let { website ->
                    InfoItem(
                        label = "Sito Web",
                        value = website
                    )
                }
            }
        }

        // Sede legale
        clientDetails.client.headquarters?.let { headquarters ->
            item {
                InfoCard(
                    title = "Sede Legale",
                    icon = Icons.Default.LocationOn
                ) {
                    InfoItem(
                        label = "Indirizzo",
                        value = headquarters.toDisplayString()
                    )
                }
            }
        }

        // Statistiche
        item {
            InfoCard(
                title = "Statistiche",
                icon = Icons.Default.Analytics
            ) {
                InfoItem(
                    label = "Stabilimenti",
                    value = "${clientDetails.statistics.facilitiesCount}"
                )

                InfoItem(
                    label = "Isole Robotizzate",
                    value = "${clientDetails.statistics.islandsCount}"
                )

                InfoItem(
                    label = "Referenti",
                    value = "${clientDetails.statistics.contactsCount}"
                )

                InfoItem(
                    label = "Check-up Totali",
                    value = "${clientDetails.statistics.totalCheckUps}"
                )
            }
        }

        clientDetails.client.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            item {
                InfoCard(
                    title = "Note",
                    icon = Icons.Default.Notes
                ) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun FacilitiesTabContent(
    facilitiesWithIslands: List<FacilityWithIslands>,
    onFacilityClick: (String) -> Unit,
    onIslandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (facilitiesWithIslands.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Outlined.Factory,
            message = "Nessuno Stabilimento",
            subMessage = "Non ci sono stabilimenti configurati per questo cliente.",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = facilitiesWithIslands,
                key = { it.facility.id }
            ) { facilityWithIslands ->
                FacilityCard(
                    facilityWithIslands = facilityWithIslands,
                    onFacilityClick = onFacilityClick,
                    onIslandClick = onIslandClick
                )
            }
        }
    }
}

// FacilityCard, IslandItem e le altre funzioni rimangono uguali...

@Composable
private fun FacilityCard(
    facilityWithIslands: FacilityWithIslands,
    onFacilityClick: (String) -> Unit,
    onIslandClick: (String) -> Unit
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
            // Facility header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = facility.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                facility.badgeText?.let { badge ->
                    AssistChip(
                        onClick = { },
                        label = { Text(badge, style = MaterialTheme.typography.labelSmall) },
                        enabled = false
                    )
                }
            }

            // Facility type and address
            Text(
                text = facility.facilityType.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = facility.addressDisplay,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Islands list
            if (facilityWithIslands.islands.isNotEmpty()) {
                Text(
                    text = "Isole Robotizzate (${facilityWithIslands.islands.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                facilityWithIslands.islands.forEach { island ->
                    IslandItem(
                        island = island,
                        onClick = { onIslandClick(island.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IslandItem(
    island: FacilityIsland,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = island.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = island.islandType.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = when (island.operationalStatus) {
                    OperationalStatus.OPERATIONAL -> Icons.Default.CheckCircle
                    OperationalStatus.MAINTENANCE_DUE -> Icons.Default.Warning
                    OperationalStatus.INACTIVE -> Icons.Default.Cancel
                },
                contentDescription = null,
                tint = when (island.operationalStatus) {
                    OperationalStatus.OPERATIONAL -> MaterialTheme.colorScheme.primary
                    OperationalStatus.MAINTENANCE_DUE -> MaterialTheme.colorScheme.error
                    OperationalStatus.INACTIVE -> MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

@Composable
private fun HistoryTabContent(
    checkUpsCount: Int,
    modifier: Modifier = Modifier
) {
    EmptyTabContent(
        icon = Icons.Outlined.History,
        message = "Storico CheckUp",
        subMessage = if (checkUpsCount > 0) {
            "Storico di $checkUpsCount check-up effettuati"
        } else {
            "Nessun check-up ancora effettuato per questo cliente"
        },
        modifier = modifier
    )
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
private fun EmptyTabContent(
    icon: ImageVector,
    message: String,
    subMessage: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (subMessage.isNotBlank()) {
                Text(
                    text = subMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
                text = "Caricamento dettagli cliente...",
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

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Cliente non trovato",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}