@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.presentation.screen.client.facilityisland

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
import net.calvuz.qreport.domain.model.island.IslandType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Screen per creazione/modifica isola robotizzata
 *
 * Features:
 * - Form completo con validazioni
 * - Gestione create/edit mode
 * - Selezione tipo isola con descrizioni
 * - Date picker per installazione, garanzia, manutenzione
 * - Validazione campi tecnici (ore, cicli)
 * - Save/cancel con conferma
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityIslandFormScreen(
    facilityId: String,
    facilityName: String,
    islandId: String? = null, // null = create mode
    onNavigateBack: () -> Unit,
    onIslandSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FacilityIslandFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize form
    LaunchedEffect(facilityId, islandId) {
        viewModel.initialize(facilityId, islandId)
    }

    // Handle success
    LaunchedEffect(uiState.savedIslandId) {
        uiState.savedIslandId?.let { savedId ->
            onIslandSaved(savedId)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        if (islandId == null) "Nuova Isola" else "Modifica Isola"
                    )
                    Text(
                        text = facilityName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Indietro")
                }
            },
            actions = {
                TextButton(
                    onClick = viewModel::saveIsland,
                    enabled = uiState.isFormValid && !uiState.isLoading
                ) {
                    Text("Salva")
                }
            }
        )

        // Form content
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading && islandId != null) {
                // Loading existing island
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                FacilityIslandFormContent(
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
private fun FacilityIslandFormContent(
    uiState: FacilityIslandFormUiState,
    onFormEvent: (FacilityIslandFormEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Informazioni Base
        IslandBasicInfoSection(
            uiState = uiState,
            onFormEvent = onFormEvent
        )

        // Informazioni Tecniche
        TechnicalInfoSection(
            uiState = uiState,
            onFormEvent = onFormEvent
        )

        // Date e Manutenzione
        MaintenanceInfoSection(
            uiState = uiState,
            onFormEvent = onFormEvent
        )

        // Configurazione
        ConfigurationSection(
            uiState = uiState,
            onFormEvent = onFormEvent
        )

        // Note
        NotesSection(
            uiState = uiState,
            onFormEvent = onFormEvent
        )

        // Spacer per evitare sovrapposizione con il FAB
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun IslandBasicInfoSection(
    uiState: FacilityIslandFormUiState,
    onFormEvent: (FacilityIslandFormEvent) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Informazioni Base",
                style = MaterialTheme.typography.titleMedium
            )

            // Serial Number (obbligatorio)
            OutlinedTextField(
                value = uiState.serialNumber,
                onValueChange = { onFormEvent(FacilityIslandFormEvent.SerialNumberChanged(it)) },
                label = { Text("Serial Number *") },
                placeholder = { Text("Es. POL-MV-001") },
                isError = uiState.serialNumberError.isNotBlank(),
                supportingText = if (uiState.serialNumberError.isNotBlank()) {
                    { Text(uiState.serialNumberError) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Tipo Isola
            IslandTypeSelector(
                selectedType = uiState.islandType,
                onTypeSelected = { onFormEvent(FacilityIslandFormEvent.IslandTypeChanged(it)) }
            )

            // Modello
            OutlinedTextField(
                value = uiState.model,
                onValueChange = { onFormEvent(FacilityIslandFormEvent.ModelChanged(it)) },
                label = { Text("Modello") },
                placeholder = { Text("Es. POLY-MOVE-2024") },
                isError = uiState.modelError.isNotBlank(),
                supportingText = if (uiState.modelError.isNotBlank()) {
                    { Text(uiState.modelError) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Nome Personalizzato
            OutlinedTextField(
                value = uiState.customName,
                onValueChange = { onFormEvent(FacilityIslandFormEvent.CustomNameChanged(it)) },
                label = { Text("Nome Personalizzato") },
                placeholder = { Text("Es. Isola Produzione A") },
                isError = uiState.customNameError.isNotBlank(),
                supportingText = if (uiState.customNameError.isNotBlank()) {
                    { Text(uiState.customNameError) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Ubicazione
            OutlinedTextField(
                value = uiState.location,
                onValueChange = { onFormEvent(FacilityIslandFormEvent.LocationChanged(it)) },
                label = { Text("Ubicazione") },
                placeholder = { Text("Es. Linea 1 - Postazione A") },
                isError = uiState.locationError.isNotBlank(),
                supportingText = if (uiState.locationError.isNotBlank()) {
                    { Text(uiState.locationError) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun IslandTypeSelector(
    selectedType: IslandType,
    onTypeSelected: (IslandType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedType.displayName,
            onValueChange = { },
            readOnly = true,
            label = { Text("Tipo Isola *") },
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
            IslandType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = getIslandTypeDescription(type),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun TechnicalInfoSection(
    uiState: FacilityIslandFormUiState,
    onFormEvent: (FacilityIslandFormEvent) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Informazioni Tecniche",
                style = MaterialTheme.typography.titleMedium
            )

            // Data Installazione
            DatePickerField(
                label = "Data Installazione",
                value = uiState.installationDate,
                onValueChange = { onFormEvent(FacilityIslandFormEvent.InstallationDateChanged(it)) },
                error = if (uiState.installationDateError.isNotBlank()) uiState.installationDateError else null
            )

            // Scadenza Garanzia
            DatePickerField(
                label = "Scadenza Garanzia",
                value = uiState.warrantyExpiration,
                onValueChange = { onFormEvent(FacilityIslandFormEvent.WarrantyExpirationChanged(it)) },
                error = if (uiState.warrantyExpirationError.isNotBlank()) uiState.warrantyExpirationError else null
            )

            // Ore Operative e Cicli
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ore Operative
                OutlinedTextField(
                    value = uiState.operatingHours,
                    onValueChange = { onFormEvent(FacilityIslandFormEvent.OperatingHoursChanged(it)) },
                    label = { Text("Ore Operative") },
                    placeholder = { Text("0") },
                    isError = uiState.operatingHoursError.isNotBlank(),
                    supportingText = if (uiState.operatingHoursError.isNotBlank()) {
                        { Text(uiState.operatingHoursError) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // Conteggio Cicli
                OutlinedTextField(
                    value = uiState.cycleCount,
                    onValueChange = { onFormEvent(FacilityIslandFormEvent.CycleCountChanged(it)) },
                    label = { Text("Cicli") },
                    placeholder = { Text("0") },
                    isError = uiState.cycleCountError.isNotBlank(),
                    supportingText = if (uiState.cycleCountError.isNotBlank()) {
                        { Text(uiState.cycleCountError) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MaintenanceInfoSection(
    uiState: FacilityIslandFormUiState,
    onFormEvent: (FacilityIslandFormEvent) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Manutenzione",
                style = MaterialTheme.typography.titleMedium
            )

            // Ultima Manutenzione
            DatePickerField(
                label = "Ultima Manutenzione",
                value = uiState.lastMaintenanceDate,
                onValueChange = { onFormEvent(FacilityIslandFormEvent.LastMaintenanceDateChanged(it)) },
                error = if (uiState.lastMaintenanceDateError.isNotBlank()) uiState.lastMaintenanceDateError else null
            )

            // Prossima Manutenzione Programmata
            DatePickerField(
                label = "Prossima Manutenzione",
                value = uiState.nextScheduledMaintenance,
                onValueChange = { onFormEvent(FacilityIslandFormEvent.NextMaintenanceChanged(it)) },
                error = if (uiState.nextMaintenanceError.isNotBlank()) uiState.nextMaintenanceError else null
            )
        }
    }
}

@Composable
private fun ConfigurationSection(
    uiState: FacilityIslandFormUiState,
    onFormEvent: (FacilityIslandFormEvent) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configurazione",
                style = MaterialTheme.typography.titleMedium
            )

            // Stato Attivo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Isola Attiva")
                    Text(
                        text = "Se disabilitata, l'isola non apparirà nelle liste operative",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = uiState.isActive,
                    onCheckedChange = { onFormEvent(FacilityIslandFormEvent.IsActiveChanged(it)) }
                )
            }
        }
    }
}

@Composable
private fun NotesSection(
    uiState: FacilityIslandFormUiState,
    onFormEvent: (FacilityIslandFormEvent) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Note",
                style = MaterialTheme.typography.titleMedium
            )

            // Note
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { onFormEvent(FacilityIslandFormEvent.NotesChanged(it)) },
                label = { Text("Note Aggiuntive") },
                placeholder = { Text("Note tecniche, configurazioni speciali, avvertenze...") },
                isError = uiState.notesError.isNotBlank(),
                supportingText = if (uiState.notesError.isNotBlank()) {
                    { Text(uiState.notesError) }
                } else {
                    { Text("${uiState.notes.length}/1000 caratteri") }
                },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Creazione di un semplice DatePickerField dato che non è fornito
@Composable
private fun DatePickerField(
    label: String,
    value: Instant?,
    onValueChange: (Instant?) -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value?.let {
                it.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            } ?: "",
            onValueChange = { },
            label = { Text(label) },
            placeholder = { Text("Seleziona data") },
            readOnly = true,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error) }
            } else null,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Seleziona data"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (showDatePicker) {
            DatePickerDialog(
                onDateSelected = { selectedDate ->
                    onValueChange(selectedDate)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

@Composable
private fun DatePickerDialog(
    onDateSelected: (Instant?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis?.let {
                        Instant.fromEpochMilliseconds(it)
                    })
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// Helper functions
private fun getIslandTypeDescription(type: IslandType): String {
    return when (type) {
        IslandType.POLY_MOVE -> "Sistema di movimentazione automatizzata"
        IslandType.POLY_CAST -> "Stazione di colata e stampaggio"
        IslandType.POLY_EBT -> "Sistema di test elettrico automatizzato"
        IslandType.POLY_TAG_BLE -> "Stazione di etichettatura Bluetooth"
        IslandType.POLY_TAG_FC -> "Sistema di controllo di flusso con tag"
        IslandType.POLY_TAG_V -> "Stazione di verifica e validazione tag"
        IslandType.POLY_SAMPLE -> "Sistema di campionamento automatico"
    }
}

// Preview helpers
@Composable
private fun FacilityIslandFormScreenPreview() {
    MaterialTheme {
        FacilityIslandFormScreen(
            facilityId = "facility-1",
            facilityName = "Stabilimento Principale",
            islandId = null,
            onNavigateBack = {},
            onIslandSaved = {}
        )
    }
}