package net.calvuz.qreport.client.client.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.client.client.presentation.ui.components.FormAddressSection
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onClientSaved: (String, String) -> Unit,
    clientId: String? = null, // null = create, non-null = edit
    viewModel: ClientFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize for edit mode
    LaunchedEffect(clientId) {
        clientId?.let { id ->
            viewModel.loadClientForEdit(id)
        }
    }

    // Handle save completed
    LaunchedEffect(uiState.saveCompleted, uiState.savedClientId) {
        if (uiState.saveCompleted && !uiState.savedClientId.isNullOrBlank()) {
            Timber.d("Client saved ID: ${uiState.savedClientId}")
            Timber.d("Client saved NAME: ${uiState.savedClientName}")

            onClientSaved(uiState.savedClientId!!, uiState.savedClientName!!)
            viewModel.resetSaveCompleted()
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
                Column {
                    Text(
                        text = if (uiState.isEditMode) "Modifica Cliente" else "Nuovo Cliente",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = uiState.companyName,
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
                IconButton(
                    onClick = viewModel::saveClient,
                    enabled = uiState.canSave && !uiState.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Salva",
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
            // Loading state while loading client for edit
            if (uiState.isLoading && clientId != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                // Section 1: Dati Aziendali
                item {
                    CompanyDataSection(
                        companyName = uiState.companyName,
                        onCompanyNameChange = viewModel::updateCompanyName,
                        errors = uiState.fieldErrors
                    )
                }

                // Section 2: Note aggiuntive
                item {
                    NotesSection(
                        notes = uiState.notes,
                        onNotesChange = viewModel::updateNotes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Section 3: Indirizzo
                item {
                    FormAddressSection(
                        street = uiState.street,
                        streetNumber = uiState.streetNumber,
                        city = uiState.city,
                        province = uiState.province,
                        postalCode = uiState.postalCode,
                        country = uiState.country,
                        onStreetChange = viewModel::updateStreet,
                        onStreetNumberChange = viewModel::updateStreetNumber,
                        onCityChange = viewModel::updateCity,
                        onProvinceChange = viewModel::updateProvince,
                        onPostalCodeChange = viewModel::updatePostalCode,
                        onCountryChange = viewModel::updateCountry
                    )
                }

                // Action Buttons
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
                            Text("Annulla")
                        }

                        Button(
                            onClick = viewModel::saveClient,
                            enabled = uiState.canSave && !uiState.isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(if (clientId == null) "Crea Cliente" else "Salva Modifiche")
                            }
                        }
                    }
                }
            }
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or dialog
        }
    }
}

@Composable
private fun CompanyDataSection(
    companyName: String,
    onCompanyNameChange: (String) -> Unit,
    errors: Map<String, String>,
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
                text = "Dati Aziendali",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Company Name - Required
            OutlinedTextField(
                value = companyName,
                onValueChange = onCompanyNameChange,
                label = { Text("Ragione Sociale *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Characters
                ),
                singleLine = true,
                isError = errors.containsKey("companyName"),
                supportingText = errors["companyName"]?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Note",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                placeholder = { Text("note aggiuntive...") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences
                ),
            )
        }
    }
}