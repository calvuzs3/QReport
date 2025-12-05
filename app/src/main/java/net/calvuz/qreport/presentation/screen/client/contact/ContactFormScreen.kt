@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.presentation.screen.client.contact

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.domain.model.client.ContactMethod

/**
 * Screen per la creazione/modifica di un contatto
 *
 * Features:
 * - Form completo con tutti i campi del modello Contact
 * - Validazioni in tempo reale
 * - Gestione metodi di contatto preferiti
 * - Toggle referente primario
 * - Scroll ottimizzato per form lunghi
 * - Auto-save su cambio configurazione
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFormScreen(
    modifier: Modifier = Modifier,
    clientId: String,
    clientName: String,
    contactId: String? = null, // null = nuovo contatto, non-null = modifica
    onNavigateBack: () -> Unit,
    onContactSaved: (String) -> Unit, // navigateToContactDetail
    viewModel: ContactFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // Initialize form
    LaunchedEffect(clientId, contactId) {
        if (contactId != null) {
            viewModel.initForEdit(contactId)
        } else {
            viewModel.initForCreate(clientId)
        }
    }

    // Handle save completed
    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            val savedContactId = viewModel.getContactId() ?: ""
            onContactSaved(savedContactId)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (uiState.isEditMode) "Modifica Contatto" else "Nuovo Contatto",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = clientName,
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
                // Save button
                TextButton(
                    onClick = viewModel::saveContact,
                    enabled = uiState.canSave && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Salva")
                    }
                }
            }
        )

        // Content
        if (uiState.isLoading) {
            LoadingState()
        } else {
            ContactFormContent(
                uiState = uiState,
                onFirstNameChange = viewModel::updateFirstName,
                onLastNameChange = viewModel::updateLastName,
                onTitleChange = viewModel::updateTitle,
                onRoleChange = viewModel::updateRole,
                onDepartmentChange = viewModel::updateDepartment,
                onEmailChange = viewModel::updateEmail,
                onAlternativeEmailChange = viewModel::updateAlternativeEmail,
                onPhoneChange = viewModel::updatePhone,
                onMobilePhoneChange = viewModel::updateMobilePhone,
                onPreferredContactMethodChange = viewModel::updatePreferredContactMethod,
                onNotesChange = viewModel::updateNotes,
                onIsPrimaryChange = viewModel::updateIsPrimary,
                onSave = viewModel::saveContact,
                focusManager = focusManager
            )
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
private fun ContactFormContent(
    uiState: ContactFormUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAlternativeEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onMobilePhoneChange: (String) -> Unit,
    onPreferredContactMethodChange: (ContactMethod?) -> Unit,
    onNotesChange: (String) -> Unit,
    onIsPrimaryChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error card
        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // General validation error
        uiState.generalValidationError?.let { validationError ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = validationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // ===== DATI ANAGRAFICI =====
        ContactFormSection(title = "Dati Anagrafici") {
            // Nome (obbligatorio)
            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = onFirstNameChange,
                label = { Text("Nome *") },
                isError = uiState.firstNameError != null,
                supportingText = uiState.firstNameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Cognome
            OutlinedTextField(
                value = uiState.lastName,
                onValueChange = onLastNameChange,
                label = { Text("Cognome") },
                isError = uiState.lastNameError != null,
                supportingText = uiState.lastNameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Titolo
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                label = { Text("Titolo") },
                placeholder = { Text("es. Ing., Dott., Dott.ssa") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== RUOLO AZIENDALE =====
        ContactFormSection(title = "Ruolo Aziendale") {
            // Ruolo
            OutlinedTextField(
                value = uiState.role,
                onValueChange = onRoleChange,
                label = { Text("Ruolo") },
                placeholder = { Text("es. Responsabile Produzione, Technical Manager") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Dipartimento
            OutlinedTextField(
                value = uiState.department,
                onValueChange = onDepartmentChange,
                label = { Text("Dipartimento") },
                placeholder = { Text("es. Produzione, Manutenzione, IT") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== CONTATTI =====
        ContactFormSection(title = "Informazioni di Contatto *") {
            // Email
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                placeholder = { Text("nome.cognome@azienda.it") },
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Email alternativa
            OutlinedTextField(
                value = uiState.alternativeEmail,
                onValueChange = onAlternativeEmailChange,
                label = { Text("Email alternativa") },
                placeholder = { Text("email.personale@provider.com") },
                isError = uiState.alternativeEmailError != null,
                supportingText = uiState.alternativeEmailError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Telefono fisso
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = onPhoneChange,
                label = { Text("Telefono") },
                placeholder = { Text("es. 040 1234567") },
                isError = uiState.phoneError != null,
                supportingText = uiState.phoneError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Cellulare
            OutlinedTextField(
                value = uiState.mobilePhone,
                onValueChange = onMobilePhoneChange,
                label = { Text("Cellulare") },
                placeholder = { Text("es. 347 1234567") },
                isError = uiState.mobilePhoneError != null,
                supportingText = uiState.mobilePhoneError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                leadingIcon = {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== PREFERENZE =====
        ContactFormSection(title = "Preferenze di Contatto") {
            // Preferred Contact Method
            ContactMethodDropdown(
                selectedMethod = uiState.preferredContactMethod,
                onMethodSelected = onPreferredContactMethodChange,
                modifier = Modifier.fillMaxWidth()
            )

            // Primary contact toggle
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isPrimary) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (uiState.isPrimary) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Referente Primario",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Il contatto principale per questo cliente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = uiState.isPrimary,
                        onCheckedChange = onIsPrimaryChange
                    )
                }
            }
        }

        // ===== NOTE =====
        ContactFormSection(title = "Note") {
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                label = { Text("Note aggiuntive") },
                placeholder = { Text("Informazioni aggiuntive sul contatto...") },
                minLines = 3,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== SAVE BUTTON =====
        Button(
            onClick = {
                focusManager.clearFocus()
                onSave()
            },
            enabled = uiState.canSave && !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (uiState.isSaving) {
                    "Salvataggio..."
                } else if (uiState.isEditMode) {
                    "Aggiorna Contatto"
                } else {
                    "Crea Contatto"
                }
            )
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ContactFormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun ContactMethodDropdown(
    selectedMethod: ContactMethod?,
    onMethodSelected: (ContactMethod?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedMethod?.displayName ?: "",
            onValueChange = { },
            readOnly = true,
            label = { Text("Metodo di contatto preferito") },
            placeholder = { Text("Seleziona metodo preferito") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Opzione "Nessuna preferenza"
            DropdownMenuItem(
                text = { Text("Nessuna preferenza") },
                onClick = {
                    onMethodSelected(null)
                    expanded = false
                }
            )

            ContactMethod.entries.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method.displayName) },
                    onClick = {
                        onMethodSelected(method)
                        expanded = false
                    }
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
                text = "Caricamento contatto...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}