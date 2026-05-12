package net.calvuz.qreport.client.unit.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qreport.R
import net.calvuz.qreport.client.unit.domain.model.UnitType

/**
 * Add / Edit form for a [MechanicalUnit].
 *
 * The ViewModel determines add vs edit mode based on the presence of "unitId"
 * in the navigation back-stack saved state.
 *
 * @param onNavigateBack Called on successful save or on back press.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicalUnitFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: MechanicalUnitFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val screenLabel = if (viewModel.isEditing) "Modifica unità" else "Nuova unità"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenLabel) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save(onSuccess = onNavigateBack) },
                        enabled = !state.isSaving,
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

            // ── Unit type dropdown ───────────────────────────────────────────
            UnitTypeDropdown(
                selected = state.unitType,
                onSelected = viewModel::onUnitTypeChange
            )

            // ── Name (required) ──────────────────────────────────────────────
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nome *") },
                placeholder = { Text("es. Robot R1, Asse 7°, Rack Tool A") },
                isError = state.showValidation && !state.isNameValid,
                supportingText = {
                    if (state.showValidation && !state.isNameValid)
                        Text("Il nome è obbligatorio", color = MaterialTheme.colorScheme.error)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Serial number (optional) ─────────────────────────────────────
            OutlinedTextField(
                value = state.serialNumber,
                onValueChange = viewModel::onSerialNumberChange,
                label = { Text("Numero seriale") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Model (optional) ─────────────────────────────────────────────
            OutlinedTextField(
                value = state.model,
                onValueChange = viewModel::onModelChange,
                label = { Text("Modello") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Notes (optional) ─────────────────────────────────────────────
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Note") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Save button ──────────────────────────────────────────────────
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
                Text(if (viewModel.isEditing) "Salva modifiche" else "Aggiungi unità")
            }
        }
    }
}

// =============================================================================
// UNIT TYPE DROPDOWN
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitTypeDropdown(
    selected: UnitType,
    onSelected: (UnitType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo unità *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            UnitType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}