package net.calvuz.qreport.presentation.feature.checkup

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
import net.calvuz.qreport.domain.model.island.IslandType

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
        modifier = modifier.fillMaxSize()
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
                    selectedIslandType = uiState.selectedIslandType,
                    onIslandTypeSelected = viewModel::selectIslandType
                )
            }

            // Step 3: Island Info (optional)
            if (uiState.selectedIslandType != null) {
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
    selectedIslandType: IslandType?,
    onIslandTypeSelected: (IslandType) -> Unit,
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
            IslandType.entries.forEach { islandType ->
                IslandTypeOption(
                    islandType = islandType,
                    isSelected = selectedIslandType == islandType,
                    onSelected = { onIslandTypeSelected(islandType) }
                )
            }
        }
    }
}

@Composable
private fun IslandTypeOption(
    islandType: IslandType,
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
                imageVector = islandType.icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = islandType.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = islandType.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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