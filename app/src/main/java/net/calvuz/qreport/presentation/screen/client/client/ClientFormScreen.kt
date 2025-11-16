package net.calvuz.qreport.presentation.screen.client.client

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen per la creazione/modifica di un cliente
 *
 * Features:
 * - Dual mode: Create (clientId = null) / Edit (clientId != null)
 * - Form validation real-time
 * - Sezioni organizzate: Dati Aziendali, Indirizzo, Note
 * - Loading states e error handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormScreen(
    onNavigateBack: () -> Unit,
    onNavigateToClientDetail: (String) -> Unit,
    clientId: String? = null, // null = create, non-null = edit
    modifier: Modifier = Modifier,
    viewModel: ClientFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Initialize for edit mode
    LaunchedEffect(clientId) {
        clientId?.let { id ->
            viewModel.loadClientForEdit(id)
        }
    }

    // Handle navigation when client is saved
    LaunchedEffect(uiState.savedClientId) {
        uiState.savedClientId?.let { savedId ->
            onNavigateToClientDetail(savedId)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(if (clientId == null) "Nuovo Cliente" else "Modifica Cliente")
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Indietro"
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
                        vatNumber = uiState.vatNumber,
                        fiscalCode = uiState.fiscalCode,
                        industry = uiState.industry,
                        website = uiState.website,
                        onCompanyNameChange = viewModel::updateCompanyName,
                        onVatNumberChange = viewModel::updateVatNumber,
                        onFiscalCodeChange = viewModel::updateFiscalCode,
                        onIndustryChange = viewModel::updateIndustry,
                        onWebsiteChange = viewModel::updateWebsite,
                        errors = uiState.fieldErrors
                    )
                }

                // Section 2: Indirizzo Sede Legale
                item {
                    AddressSection(
                        street = uiState.street,
                        streetNumber = uiState.streetNumber,
                        city = uiState.city,
                        province = uiState.province,
                        region = uiState.region,
                        postalCode = uiState.postalCode,
                        onStreetChange = viewModel::updateStreet,
                        onStreetNumberChange = viewModel::updateStreetNumber,
                        onCityChange = viewModel::updateCity,
                        onProvinceChange = viewModel::updateProvince,
                        onRegionChange = viewModel::updateRegion,
                        onPostalCodeChange = viewModel::updatePostalCode
                    )
                }

                // Section 3: Note aggiuntive
                item {
                    NotesSection(
                        notes = uiState.notes,
                        onNotesChange = viewModel::updateNotes
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
    vatNumber: String,
    fiscalCode: String,
    industry: String,
    website: String,
    onCompanyNameChange: (String) -> Unit,
    onVatNumberChange: (String) -> Unit,
    onFiscalCodeChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onWebsiteChange: (String) -> Unit,
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
                singleLine = true,
                isError = errors.containsKey("companyName"),
                supportingText = errors["companyName"]?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            // VAT Number
            OutlinedTextField(
                value = vatNumber,
                onValueChange = onVatNumberChange,
                label = { Text("Partita IVA") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("00000000000") },
                isError = errors.containsKey("vatNumber"),
                supportingText = errors["vatNumber"]?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            // Fiscal Code
            OutlinedTextField(
                value = fiscalCode,
                onValueChange = onFiscalCodeChange,
                label = { Text("Codice Fiscale") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("RSSMRA80A01H501X") },
                isError = errors.containsKey("fiscalCode"),
                supportingText = errors["fiscalCode"]?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )

            // Industry
            OutlinedTextField(
                value = industry,
                onValueChange = onIndustryChange,
                label = { Text("Settore") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Automotive, Metalmeccanico, etc.") }
            )

            // Website
            OutlinedTextField(
                value = website,
                onValueChange = onWebsiteChange,
                label = { Text("Sito Web") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                placeholder = { Text("www.azienda.it") },
                isError = errors.containsKey("website"),
                supportingText = errors["website"]?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            )
        }
    }
}

@Composable
private fun AddressSection(
    street: String,
    streetNumber: String,
    city: String,
    province: String,
    region: String,
    postalCode: String,
    onStreetChange: (String) -> Unit,
    onStreetNumberChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onProvinceChange: (String) -> Unit,
    onRegionChange: (String) -> Unit,
    onPostalCodeChange: (String) -> Unit,
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
                text = "Indirizzo Sede Legale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Street and number row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = street,
                    onValueChange = onStreetChange,
                    label = { Text("Via/Corso") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    placeholder = { Text("Via Roma") }
                )

                OutlinedTextField(
                    value = streetNumber,
                    onValueChange = onStreetNumberChange,
                    label = { Text("N.") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("123") }
                )
            }

            // City and postal code row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("Città") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    placeholder = { Text("Milano") }
                )

                OutlinedTextField(
                    value = postalCode,
                    onValueChange = onPostalCodeChange,
                    label = { Text("CAP") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("20100") }
                )
            }

            // Province and region row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = province,
                    onValueChange = onProvinceChange,
                    label = { Text("Provincia") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("MI") }
                )

                OutlinedTextField(
                    value = region,
                    onValueChange = onRegionChange,
                    label = { Text("Regione") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Lombardia") }
                )
            }
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
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Note Aggiuntive",
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
                placeholder = { Text("Informazioni aggiuntive sul cliente...") }
            )
        }
    }
}