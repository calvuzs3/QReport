@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.client.facility.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.client.client.presentation.ui.components.FormAddressSection
import net.calvuz.qreport.client.facility.domain.model.FacilityType
import timber.log.Timber

/**
 * Screen per creazione/modifica stabilimento
 *
 * Features:
 * - ValidationError completo con validazioni
 * - Gestione create/edit mode
 * - Selezione tipo facility
 * - Input indirizzo strutturato
 * - Gestione facility primaria
 * - Save/cancel con conferma
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityFormScreen(
    modifier: Modifier = Modifier,
    clientId: String,
    facilityId: String? = null, // null = create mode
    onNavigateBack: () -> Unit,
    onFacilitySaved: (String) -> Unit,
    viewModel: FacilityFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize form
    LaunchedEffect(clientId, facilityId) {
        viewModel.initialize(clientId, facilityId)
    }

    // Handle success
    LaunchedEffect(uiState.saveCompleted, uiState.savedFacilityId) {
        uiState.savedFacilityId?.let { savedId ->
            Timber.d("Facility saved ID: ${uiState.savedFacilityId}")
            Timber.d("Facility saved NAME: ${uiState.name}")

            onFacilitySaved(savedId)
            viewModel::resetSaveCompleted
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
//            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    if (facilityId == null) "Nuovo Stabilimento" else "Modifica Stabilimento"
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Indietro")
                }
            },
            actions = {
                // Save button
                IconButton(
                    onClick = { viewModel::saveFacility },
                    enabled = uiState.isFormValid && !uiState.isLoading
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (uiState.isFormValid) Icons.Default.Save else Icons.Outlined.Save,
                            contentDescription = stringResource(R.string.action_save),
                            tint = if (uiState.isFormValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = viewModel::saveFacility,
                    enabled = uiState.isFormValid && !uiState.isLoading
                ) {
                    Text("Salva")
                }
            }
        )

        // ValidationError content
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading && facilityId != null) {
                // Loading existing facility
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                FacilityFormContent(
                    uiState = uiState,
                    onFormEvent = viewModel::onFormEvent
                )
            }

            // Error snackbar
            uiState.error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    // Show snackbar or handle error
                    viewModel.dismissError()
                }
            }
        }
    }
}

@Composable
private fun FacilityFormContent(
    uiState: FacilityFormUiState,
    onFormEvent: (FacilityFormEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nome stabilimento
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onFormEvent(FacilityFormEvent.NameChanged(it)) },
            label = { Text("Nome Stabilimento *") },
            placeholder = { Text("Es. Stabilimento Principale") },
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Codice interno (opzionale)
        OutlinedTextField(
            value = uiState.code,
            onValueChange = { onFormEvent(FacilityFormEvent.CodeChanged(it)) },
            label = { Text("Codice Interno") },
            placeholder = { Text("Es. FAB01") },
            isError = uiState.codeError != null,
            supportingText = uiState.codeError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Notes
        OutlinedTextField(
            value = uiState.notes,
            onValueChange = { onFormEvent(FacilityFormEvent.NotesChanged(it)) },
            label = { Text("Note") },
            placeholder = { Text("note aggiuntive...") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        // Tipo stabilimento
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            // Name
            OutlinedTextField(
                value = uiState.facilityType.displayName,
                onValueChange = { },
                readOnly = true,
                label = { Text("Tipo Stabilimento *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            // Type
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                FacilityType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(type.displayName)
                                Text(
                                    text = type.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onFormEvent(FacilityFormEvent.TypeChanged(type))
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        // Address Section
        FormAddressSection(
            street = uiState.street,
            streetNumber = uiState.streetNumber,
            city = uiState.city,
            province = uiState.province,
            postalCode = uiState.postalCode,
            country = uiState.country,
            onStreetChange = { onFormEvent(FacilityFormEvent.StreetChanged(it)) }, //viewModel::updateStreet,
            onStreetNumberChange = { onFormEvent(FacilityFormEvent.StreetNumberChanged(it)) }, //viewModel::updateStreetNumber,
            onCityChange = { onFormEvent(FacilityFormEvent.CityChanged(it)) }, //viewModel::updateCity,
            onProvinceChange = { onFormEvent(FacilityFormEvent.ProvinceChanged(it)) }, //viewModel::updateProvince,
            onPostalCodeChange = { onFormEvent(FacilityFormEvent.PostalCodeChanged(it)) }, //viewModel::updatePostalCode,
            onCountryChange = { onFormEvent(FacilityFormEvent.CountryChanged(it)) }, //viewModel::updateCountry
        )

        // Meta
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Opzioni",
                    style = MaterialTheme.typography.titleSmall
                )

                // Primary Facility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stabilimento Primario")
                        Text(
                            text = "Imposta come stabilimento principale per il cliente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = uiState.isPrimary,
                        onCheckedChange = { onFormEvent(FacilityFormEvent.PrimaryChanged(it)) }
                    )
                }
            }
        }

        // Spacer per FAB
        Spacer(modifier = Modifier.height(80.dp))
    }
}