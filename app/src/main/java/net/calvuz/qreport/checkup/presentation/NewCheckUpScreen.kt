package net.calvuz.qreport.checkup.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.presentation.components.ClientFacilityIslandSelectorDialog
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster
import net.calvuz.qreport.client.island.presentation.model.IslandTypeIconRegistry

/**
 * Screen per la creazione guidata di un nuovo check-up
 *
 * Flusso:
 * 1. Inserimento info cliente
 * 2. Selezione tipo isola
 * 3. Info opzionali isola
 * 4. Creazione con template
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCheckUpScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCheckUpDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewCheckUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle navigation when check-up is created
    LaunchedEffect(uiState.createdCheckUpId) {
        uiState.createdCheckUpId?.let { checkUpId ->
            onNavigateToCheckUpDetail(checkUpId)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text(stringResource(R.string.checkup_screen_new_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.checkup_screen_new_back)
                    )
                }
            }
        )

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step 0: Link to existing client/facility/island (optional)
            item {
                SourceSelectionSection(
                    selectedIslandTypeMaster = uiState.selectedIslandTypeMaster,
                    isLinked = uiState.selectedIslandId != null,
                    clientName = uiState.clientName,
                    site = uiState.site,
                    onLinkSource = viewModel::openSourceSelectionDialog,
                    onUnlinkSource = viewModel::clearLinkedSource
                )
            }

            // Step 1: Client Info
            item {
                ClientInfoSection(
                    clientName = uiState.clientName,
                    contactPerson = uiState.contactPerson,
                    site = uiState.site,
                    onClientNameChange = viewModel::updateClientName,
                    onContactPersonChange = viewModel::updateContactPerson,
                    onSiteChange = viewModel::updateSite
                )
            }

            // Step 2: Island Type Selection
            item {
                IslandTypeSection(
                    islandTypes = uiState.islandTypes,
                    selectedIslandTypeMaster = uiState.selectedIslandTypeMaster,
                    onIslandTypeSelected = viewModel::selectIslandType
                )
            }

            // Step 3: Island Info (optional)
            if (uiState.selectedIslandTypeMaster != null) {
                item {
                    IslandInfoSection(
                        serialNumber = uiState.serialNumber,
                        model = uiState.model,
                        onSerialNumberChange = viewModel::updateSerialNumber,
                        onModelChange = viewModel::updateModel
                    )
                }
            }

            // Step 4: Action Buttons
            item {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.checkup_screen_new_action_cancel))
                    }

                    Button(
                        onClick = viewModel::createCheckUp,
                        enabled = uiState.canCreate && !uiState.isCreating,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.checkup_screen_new_action_create))
                        }
                    }
                }
            }
        }
    }

    // Error handling
//    if (uiState.error != null ) {
//        // TODO: handle errors
//        LaunchedEffect(error) {
//            // Show snackbar or dialog
//        }
//    }

    // Source selection dialog (Cliente -> Stabilimento -> Isola)
    if (uiState.showSourceSelectionDialog) {
        ClientFacilityIslandSelectorDialog(
            availableClients = uiState.availableClients,
            availableFacilities = uiState.availableFacilities,
            availableIslands = uiState.availableIslands,
            selectedClientId = uiState.selectedClientId,
            selectedFacilityId = uiState.selectedFacilityId,
            isLoading = uiState.isLoadingSelection,
            onDismiss = viewModel::dismissSourceSelectionDialog,
            onClientSelected = viewModel::onClientSelectedForSource,
            onFacilitySelected = viewModel::onFacilitySelectedForSource,
            onIslandSelected = viewModel::onIslandSelectedForSource
        )
    }
}

@Composable
private fun SourceSelectionSection(
    selectedIslandTypeMaster: IslandTypeMaster?,
    isLinked: Boolean,
    clientName: String,
    site: String,
    onLinkSource: () -> Unit,
    onUnlinkSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.checkup_screen_new_section_source_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (isLinked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(
                            R.string.checkup_screen_new_linked_banner,
                            clientName,
                            site,
                            selectedIslandTypeMaster?.label ?: ""
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                TextButton(onClick = onUnlinkSource) {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.checkup_screen_new_unlink_source))
                }
            } else {
                OutlinedButton(
                    onClick = onLinkSource,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.checkup_screen_new_link_source_button))
                }
            }
        }
    }
}

@Composable
private fun ClientInfoSection(
    clientName: String,
    contactPerson: String,
    site: String,
    onClientNameChange: (String) -> Unit,
    onContactPersonChange: (String) -> Unit,
    onSiteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.checkup_screen_new_section_client_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = clientName,
                onValueChange = onClientNameChange,
                label = { Text(stringResource(R.string.checkup_screen_new_client_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = contactPerson,
                onValueChange = onContactPersonChange,
                label = { Text(stringResource(R.string.checkup_screen_new_contact_person_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = site,
                onValueChange = onSiteChange,
                label = { Text(stringResource(R.string.checkup_screen_new_site_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun IslandTypeSection(
    islandTypes: List<IslandTypeMaster>,
    selectedIslandTypeMaster: IslandTypeMaster?,
    onIslandTypeSelected: (IslandTypeMaster) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.checkup_screen_new_section_island_type_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(R.string.checkup_screen_new_island_type_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Island type options
            islandTypes.forEach { islandType ->
                IslandTypeOption(
                    islandType = islandType,
                    isSelected = selectedIslandTypeMaster?.id == islandType.id,
                    onSelected = { onIslandTypeSelected(islandType) }
                )
            }
        }
    }
}

@Composable
private fun IslandTypeOption(
    islandType: IslandTypeMaster,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp
            )
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = IslandTypeIconRegistry.iconFor(islandType.label),
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = islandType.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                islandType.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = null
            )
        }
    }
}

@Composable
private fun IslandInfoSection(
    serialNumber: String,
    model: String,
    onSerialNumberChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.checkup_screen_new_section_island_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = serialNumber,
                onValueChange = onSerialNumberChange,
                label = { Text(stringResource(R.string.checkup_screen_new_serial_number_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = model,
                onValueChange = onModelChange,
                label = { Text(stringResource(R.string.checkup_screen_new_model_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}