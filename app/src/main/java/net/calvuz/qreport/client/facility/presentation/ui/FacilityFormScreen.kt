@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.client.facility.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.client.facility.domain.model.FacilityType

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
    clientId: String,
    facilityId: String? = null, // null = create mode
    onNavigateBack: () -> Unit,
    onFacilitySaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FacilityFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize form
    LaunchedEffect(clientId, facilityId) {
        viewModel.initialize(clientId, facilityId)
    }

    // Handle success
    LaunchedEffect(uiState.savedFacilityId) {
        uiState.savedFacilityId?.let { savedId ->
            onFacilitySaved(savedId)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
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

        // Tipo stabilimento
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
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

        // Sezione Indirizzo
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Indirizzo Stabilimento",
                    style = MaterialTheme.typography.titleSmall
                )

                // Via
                OutlinedTextField(
                    value = uiState.street,
                    onValueChange = { onFormEvent(FacilityFormEvent.StreetChanged(it)) },
                    label = { Text("Via/Indirizzo") },
                    placeholder = { Text("Es. Via Roma 123") },
                    isError = uiState.streetError != null,
                    supportingText = uiState.streetError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Città e CAP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.city,
                        onValueChange = { onFormEvent(FacilityFormEvent.CityChanged(it)) },
                        label = { Text("Città *") },
                        placeholder = { Text("Milano") },
                        isError = uiState.cityError != null,
                        supportingText = uiState.cityError?.let { { Text(it) } },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.postalCode,
                        onValueChange = { onFormEvent(FacilityFormEvent.PostalCodeChanged(it)) },
                        label = { Text("CAP") },
                        placeholder = { Text("20100") },
                        modifier = Modifier.weight(0.6f)
                    )
                }

                // Provincia e Paese
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.province,
                        onValueChange = { onFormEvent(FacilityFormEvent.ProvinceChanged(it)) },
                        label = { Text("Provincia") },
                        placeholder = { Text("MI") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.country,
                        onValueChange = { onFormEvent(FacilityFormEvent.CountryChanged(it)) },
                        label = { Text("Paese") },
                        placeholder = { Text("Italia") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Descrizione
        OutlinedTextField(
            value = uiState.description,
            onValueChange = { onFormEvent(FacilityFormEvent.DescriptionChanged(it)) },
            label = { Text("Descrizione") },
            placeholder = { Text("Descrizione opzionale dello stabilimento...") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        // Opzioni aggiuntive
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Opzioni",
                    style = MaterialTheme.typography.titleSmall
                )

                // Stabilimento primario
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

                // Stato attivo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stabilimento Attivo")
                        Text(
                            text = "Se disabilitato, lo stabilimento non sarà visibile nelle liste",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = uiState.isActive,
                        onCheckedChange = { onFormEvent(FacilityFormEvent.ActiveChanged(it)) }
                    )
                }
            }
        }

        // Spacer per FAB
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// Preview helpers
@Composable
private fun FacilityFormScreenPreview() {
    MaterialTheme {
        FacilityFormScreen(
            clientId = "client-1",
            facilityId = null,
            onNavigateBack = {},
            onFacilitySaved = {}
        )
    }
}