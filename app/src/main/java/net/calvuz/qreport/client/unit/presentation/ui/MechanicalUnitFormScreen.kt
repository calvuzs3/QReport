package net.calvuz.qreport.client.unit.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qreport.R
import net.calvuz.qreport.client.unit.domain.model.UnitType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicalUnitFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: MechanicalUnitFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    val context = LocalContext.current

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHost.showSnackbar(it.asString(context)); viewModel.clearError() }
    }

    val titleRes =
        if (viewModel.isEditing) R.string.unit_form_title_edit else R.string.unit_form_title_create

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.unit_form_action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save(onSuccess = onNavigateBack) },
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isValid) Icons.Default.Save else Icons.Outlined.Save,
                                contentDescription = stringResource(R.string.action_save),
                                tint = if (state.isValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UnitTypeDropdown(selected = state.unitType, onSelected = viewModel::onUnitTypeChange)

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.unit_form_field_name)) },
                placeholder = { Text(stringResource(R.string.unit_form_field_name_placeholder)) },
                isError = state.showValidation && !state.isNameValid,
                supportingText = {
                    if (state.showValidation && !state.isNameValid)
                        Text(
                            stringResource(R.string.unit_form_error_name_required),
                            color = MaterialTheme.colorScheme.error
                        )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.serialNumber,
                onValueChange = viewModel::onSerialNumberChange,
                label = { Text(stringResource(R.string.unit_form_field_serial)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.model,
                onValueChange = viewModel::onModelChange,
                label = { Text(stringResource(R.string.unit_form_field_model)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.unit_form_field_notes)) },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.save(onSuccess = onNavigateBack) },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    if (viewModel.isEditing) stringResource(R.string.unit_form_button_save_changes)
                    else stringResource(R.string.unit_form_button_create)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitTypeDropdown(selected: UnitType, onSelected: (UnitType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(selected.labelResId),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.unit_form_field_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            UnitType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.labelResId)) },
                    onClick = { onSelected(type); expanded = false }
                )
            }
        }
    }
}