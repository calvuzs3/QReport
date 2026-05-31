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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityFormScreen(
    modifier: Modifier = Modifier,
    clientId: String,
    facilityId: String? = null,
    onNavigateBack: () -> Unit,
    onFacilitySaved: (String) -> Unit,
    viewModel: FacilityFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(clientId, facilityId) {
        viewModel.initialize(clientId, facilityId)
    }

    LaunchedEffect(uiState.saveCompleted, uiState.savedFacilityId) {
        val savedId = uiState.savedFacilityId
        if (uiState.saveCompleted && savedId != null) {
            Timber.d("Facility saved ID: $savedId name: ${uiState.name}")
            onFacilitySaved(savedId)
            viewModel.resetSaveCompleted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    if (facilityId == null)
                        stringResource(R.string.facility_form_title_create)
                    else
                        stringResource(R.string.facility_form_title_edit)
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.facility_form_action_back)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = viewModel::saveFacility,
                    enabled = uiState.isFormValid && !uiState.isLoading
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (uiState.isFormValid) Icons.Default.Save else Icons.Outlined.Save,
                            contentDescription = stringResource(R.string.facility_form_action_save),
                            tint = if (uiState.isFormValid)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = viewModel::saveFacility,
                    enabled = uiState.isFormValid && !uiState.isLoading
                ) {
                    Text(stringResource(R.string.facility_form_button_save_text))
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading && facilityId != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                FacilityFormContent(
                    uiState = uiState,
                    onFormEvent = viewModel::onFormEvent
                )
            }

            uiState.error?.let {
                LaunchedEffect(it) { viewModel.dismissError() }
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
        // Name
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onFormEvent(FacilityFormEvent.NameChanged(it)) },
            label = { Text(stringResource(R.string.facility_form_field_name)) },
            placeholder = { Text(stringResource(R.string.facility_form_field_name_placeholder)) },
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { error ->
                { Text(error.asString()) }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Code
        OutlinedTextField(
            value = uiState.code,
            onValueChange = { onFormEvent(FacilityFormEvent.CodeChanged(it)) },
            label = { Text(stringResource(R.string.facility_form_field_code)) },
            placeholder = { Text(stringResource(R.string.facility_form_field_code_placeholder)) },
            isError = uiState.codeError != null,
            supportingText = uiState.codeError?.let { error ->
                { Text(error.asString()) }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Notes
        OutlinedTextField(
            value = uiState.notes,
            onValueChange = { onFormEvent(FacilityFormEvent.NotesChanged(it)) },
            label = { Text(stringResource(R.string.facility_form_field_notes)) },
            placeholder = { Text(stringResource(R.string.facility_form_field_notes_placeholder)) },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        // Facility Type dropdown
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = stringResource(uiState.facilityType.labelResId),
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.facility_form_field_type)) },
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
                                Text(stringResource(type.labelResId))
                                Text(
                                    text = stringResource(type.descriptionResId),
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

        // Address
        FormAddressSection(
            street = uiState.street,
            streetNumber = uiState.streetNumber,
            city = uiState.city,
            province = uiState.province,
            postalCode = uiState.postalCode,
            country = uiState.country,
            onStreetChange = { onFormEvent(FacilityFormEvent.StreetChanged(it)) },
            onStreetNumberChange = { onFormEvent(FacilityFormEvent.StreetNumberChanged(it)) },
            onCityChange = { onFormEvent(FacilityFormEvent.CityChanged(it)) },
            onProvinceChange = { onFormEvent(FacilityFormEvent.ProvinceChanged(it)) },
            onPostalCodeChange = { onFormEvent(FacilityFormEvent.PostalCodeChanged(it)) },
            onCountryChange = { onFormEvent(FacilityFormEvent.CountryChanged(it)) }
        )

        // Options
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.facility_form_section_options),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.facility_form_option_primary_title))
                        Text(
                            text = stringResource(R.string.facility_form_option_primary_desc),
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

        Spacer(modifier = Modifier.height(80.dp))
    }
}