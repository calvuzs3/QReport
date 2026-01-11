@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.client.contact.presentation.ui

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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.client.contact.domain.model.ContactMethod
import timber.log.Timber
import net.calvuz.qreport.R

/**
 * Screen per la creazione/modifica di un contatto
 *
 * Features:
 * - ValidationError completo con tutti i campi del modello Contact
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
    contactId: String? = null, // null = new, non-null = edit
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
    LaunchedEffect(uiState.saveCompleted, uiState.savedContactId) {
        if (uiState.saveCompleted && !uiState.savedContactId.isNullOrBlank()) {
            Timber.d("Contact saved ID: ${uiState.savedContactId}")

            onContactSaved(uiState.savedContactId!!)
            viewModel.resetSaveCompleted()
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
                        text = if (uiState.isEditMode) stringResource(R.string.contact_form_title_edit) else stringResource(R.string.contact_form_title_new),
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
                        contentDescription = stringResource(R.string.action_back)
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
                        Text(stringResource(R.string.action_save))
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
    focusManager: FocusManager
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
                        text = error.asString(),
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
                        text = validationError.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // ===== DATI ANAGRAFICI =====
        ContactFormSection(title = stringResource(R.string.contact_form_section_data)) {
            // Nome (obbligatorio)
            OutlinedTextField(
                value = uiState.firstName,
                onValueChange = onFirstNameChange,
                label = { Text(stringResource(R.string.contact_form_field_name)) },
                placeholder = { Text(stringResource(R.string.contact_form_field_name_placeholder))},
                isError = uiState.firstNameError != null,
                supportingText = uiState.firstNameError?.let { { Text(it.asString()) } },
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
                label = { Text(stringResource(R.string.contact_form_field_surname) )},
                isError = uiState.lastNameError != null,
                supportingText = uiState.lastNameError?.let { { Text(it.asString()) } },
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
                label = { Text(stringResource(R.string.contact_form_field_title)) },
                placeholder = { Text(stringResource(R.string.contact_form_field_title_placeholder)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== RUOLO AZIENDALE =====
        ContactFormSection(title = stringResource(R.string.contact_form_section_role)) {
            // Ruolo
            OutlinedTextField(
                value = uiState.role,
                onValueChange = onRoleChange,
                label = { Text(stringResource(R.string.contact_form_field_role)) },
                placeholder = { Text(stringResource(R.string.contact_form_field_role_placeholder) )},
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
                label = { Text(stringResource(R.string.contact_form_field_department)) },
                placeholder = { Text(stringResource(R.string.contact_form_field_department_placholder) )},
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== CONTATTI =====
        ContactFormSection(title = stringResource(R.string.contact_form_section_contact_info)) {
            // Email
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.contact_form_field_email)) },
                placeholder = { Text(stringResource(R.string.contact_form_field_email_placeholder)) },
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { { Text(it.asString()) } },
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
                label = { Text(stringResource(R.string.contact_form_field_alternative_email)) },
                placeholder = { Text(stringResource(R.string.contact_form_field_email_placeholder) )},
                isError = uiState.alternativeEmailError != null,
                supportingText = uiState.alternativeEmailError?.let { { Text(it.asString()) } },
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
                label = { Text(stringResource(R.string.contact_form_field_phone)) },
                placeholder = { Text(stringResource(R.string.contact_form_field_phone_placeholder)) },
                isError = uiState.phoneError != null,
                supportingText = uiState.phoneError?.let { { Text(it.asString()) } },
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
                label = { Text(stringResource(R.string.contact_form_field_mobile)) },
                placeholder = { Text(stringResource(R.string.contact_form_field_phone_placeholder)) },
                isError = uiState.mobilePhoneError != null,
                supportingText = uiState.mobilePhoneError?.let { { Text(it.asString()) } },
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
        ContactFormSection(title = stringResource(R.string.contact_form_section_contact_pref)) {
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
                            text = stringResource(R.string.contact_primary_contact),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.contact_primary_contact_desc),
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
        ContactFormSection(title = stringResource(R.string.contact_form_section_note)) {
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.contact_form_field_notes)) },
                placeholder = { Text(stringResource(R.string.contact_form_field_notes_placeholder)) },
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
                    stringResource(R.string.label_saving)
                } else if (uiState.isEditMode) {
                    stringResource(R.string.label_updating)
                } else {
                    stringResource(R.string.label_creating)
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
            label = { Text(stringResource(R.string.contact_form_field_preferred_contact_method)) },
            placeholder = { Text(stringResource(R.string.contact_form_field_preferred_contact_method_placholder)) },
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
                text = { Text(stringResource(R.string.contact_form_field_preferred_contact_method_empty)) },
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