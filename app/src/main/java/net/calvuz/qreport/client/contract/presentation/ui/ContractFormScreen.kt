package net.calvuz.qreport.client.contract.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import net.calvuz.qreport.app.app.presentation.components.QrDatePickerField
import net.calvuz.qreport.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractFormScreen(
    modifier: Modifier = Modifier,
    clientId: String,
    clientName: String,
    contractId: String? = null, // null == new()
    onNavigateBack: () -> Unit,
    onContractSaved: (String) -> Unit,
    viewModel: ContractFormViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // Initialize form
    LaunchedEffect(clientId, contractId) {
        viewModel.init(clientId, contractId)
    }

    // Handle save completed
    LaunchedEffect(uiState.saveCompleted, uiState.savedContractId) {
        if (uiState.saveCompleted && !uiState.savedContractId.isNullOrBlank()) {
            onContractSaved(uiState.savedContractId!!)
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
                        text = if (uiState.isEditMode) stringResource(R.string.contracts_screen_form_title_edit)
                            else stringResource(R.string.contracts_screen_form_title_new),
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
                IconButton(
                    onClick = { viewModel.saveContract() },
                    enabled = uiState.canSave && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (uiState.canSave) Icons.Default.Save else Icons.Outlined.Save,
                            contentDescription = stringResource(R.string.action_save),
                            tint = if (uiState.canSave) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        // Content
        if (uiState.isLoading) {
            LoadingState()
        } else {
            ContractFormContent(
                uiState = uiState,
                onFormEvent = viewModel::onFormEvent,

                onSave = viewModel::saveContract,
                focusManager = focusManager
            )
        }

        // Error handling
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Could show snackbar or other error handling
            }
        }
    }
}


@Composable
private fun ContractFormContent(
    uiState: ContractFormUiState,
    onFormEvent: (ContractFormEvent) -> Unit,

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

        // ValidationError
        ContractFormSection(title = stringResource(R.string.contracts_screen_form_section_contract)) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { onFormEvent(ContractFormEvent.NameChanged(it)) },
                label = { Text(stringResource(R.string.contracts_screen_form_field_name)) },
                placeholder = { Text(stringResource(R.string.contracts_screen_form_field_name_placeholder)) },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it.asString()) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { onFormEvent(ContractFormEvent.DescriptionChanged(it)) },
                label = { Text(stringResource(R.string.contracts_screen_form_field_description) ) },
                placeholder = { Text(stringResource(R.string.contracts_screen_form_field_description_placeholder)) },
                isError = uiState.descriptionError != null,
                supportingText = uiState.descriptionError?.let { { Text(it.asString()) } },
                minLines = 2,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            QrDatePickerField(
                label = stringResource(R.string.contracts_screen_form_field_start_date),
                value = uiState.startDate,
                onValueChange = { onFormEvent(ContractFormEvent.StartDateChanged(it)) },
                error = uiState.startDateError?.asString()
            )

            QrDatePickerField(
                label = stringResource(R.string.contracts_screen_form_field_end_date),
                value = uiState.endDate,
                onValueChange = { onFormEvent(ContractFormEvent.EndDateChanged(it)) },
                error = uiState.endDateError?.asString()
            )
        }
        ContractFormSection(title = stringResource(R.string.contracts_screen_form_section_options)) {

            // Priority Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.contracts_screen_form_field_priority))
                Switch(
                    checked = uiState.hasPriority,
                    onCheckedChange = { onFormEvent(ContractFormEvent.HasPriorityChanged(it)) }
                )
            }

            // Remote Assistance Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.contracts_screen_form_field_remote_assistance))
                Switch(
                    checked = uiState.hasRemoteAssistance,
                    onCheckedChange = { onFormEvent(ContractFormEvent.HasRemoteAssistanceChanged(it)) }
                )
            }

            // Maintenance Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.contracts_screen_form_field_maintenance))
                Switch(
                    checked = uiState.hasMaintenance,
                    onCheckedChange = { onFormEvent(ContractFormEvent.HasMaintenanceChanged(it)) }
                )
            }
        }

        // ===== SAVE BUTTON =====
        IconButton(
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
            Icon(
                imageVector = if (uiState.canSave) Icons.Default.Save else Icons.Outlined.Save,
                contentDescription = stringResource(R.string.action_save),
                tint = if (uiState.canSave) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (uiState.isSaving) {
                    stringResource(R.string.label_saving)
                } else if (uiState.isEditMode) {
                    stringResource(R.string.action_update)
                } else {
                    stringResource(R.string.action_save)
                }
            )
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(32.dp))

    }

}


@Composable
private fun ContractFormSection(
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