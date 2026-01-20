@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.ti.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qreport.ti.domain.model.WorkLocationType

/**
 * Screen for creating or editing TechnicalIntervention
 * Handles both modes: Create (interventionId = null) and Edit (interventionId != null)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralFormScreen(
    interventionId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: GeneralFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // Note: ViewModel is initialized by parent EditInterventionScreen to avoid double loading
    // LaunchedEffect(interventionId) { viewModel.loadInterventionDetails(interventionId) }


    // Form content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // Dirty state indicator
        if (state.isDirty) {
            DirtyStateIndicator(
                message = "Modifiche non salvate",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Auto-save indicator
        if (state.isSaving) {
            AutoSaveIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Error display
        state.errorMessage?.let { errorMessage ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage.asString(),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Modalità Modifica",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }


        // ===== CUSTOMER SECTION =====
        CustomerDataSection(
            customerName = state.customerName,
            onCustomerNameChange = viewModel::updateCustomerName,
            customerContact = state.customerContact,
            onCustomerContactChange = viewModel::updateCustomerContact,
            ticketNumber = state.ticketNumber,
            onTicketNumberChange = viewModel::updateTicketNumber,
            customerOrderNumber = state.customerOrderNumber,
            onCustomerOrderNumberChange = viewModel::updateCustomerOrderNumber,
            notes = state.notes,
            onNotesChange = viewModel::updateNotes,
            isEditMode = state.isEditMode
        )

        HorizontalDivider()

        // ===== ROBOT DATA SECTION =====
        RobotDataSection(
            serialNumber = state.serialNumber,
            onSerialNumberChange = viewModel::updateSerialNumber,
            hoursOfDuty = state.hoursOfDuty.toString(),
            onHoursOfDutyChange = viewModel::updateHoursOfDuty,
            isEditMode = state.isEditMode
        )

        HorizontalDivider()

        // ===== WORK LOCATION SECTION =====
        WorkLocationSection(
            workLocation = state.workLocation,
            onWorkLocationChange = viewModel::updateWorkLocation,
            customLocation = state.customLocation,
            onCustomLocationChange = viewModel::updateCustomLocation
        )

        HorizontalDivider()

        // ===== TECHNICIANS SECTION =====
        TechniciansSection(
            technicians = state.technicians,
            onTechniciansChange = viewModel::updateTechnicians
        )

        // Spacer for bottom navigation
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CustomerDataSection(
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    customerContact: String,
    onCustomerContactChange: (String) -> Unit,
    ticketNumber: String,
    onTicketNumberChange: (String) -> Unit,
    customerOrderNumber: String,
    onCustomerOrderNumberChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Dati Cliente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (isEditMode) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "(Immutabili in modifica)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Customer name (required)
        OutlinedTextField(
            value = customerName,
            onValueChange = onCustomerNameChange,
            label = { Text("Nome Cliente *") },
            isError = customerName.isBlank(),
            supportingText = if (customerName.isBlank()) {
                { Text("Campo obbligatorio", color = MaterialTheme.colorScheme.error) }
            } else null,
            enabled = !isEditMode, // Read-only in edit mode for fiscal compliance
            modifier = Modifier.fillMaxWidth()
        )

        // Customer contact (optional)
        OutlinedTextField(
            value = customerContact,
            onValueChange = onCustomerContactChange,
            label = { Text("Persona di Riferimento") },
            modifier = Modifier.fillMaxWidth()
        )

        // Ticket number (required)
        OutlinedTextField(
            value = ticketNumber,
            onValueChange = onTicketNumberChange,
            label = { Text("Numero Commessa/Ticket *") },
            isError = ticketNumber.isBlank(),
            supportingText = if (ticketNumber.isBlank()) {
                { Text("Campo obbligatorio", color = MaterialTheme.colorScheme.error) }
            } else null,
            enabled = !isEditMode, // Read-only in edit mode for fiscal compliance
            modifier = Modifier.fillMaxWidth()
        )

        // Customer order number (required)
        OutlinedTextField(
            value = customerOrderNumber,
            onValueChange = onCustomerOrderNumberChange,
            label = { Text("Numero Ordine Cliente *") },
            isError = customerOrderNumber.isBlank(),
            supportingText = if (customerOrderNumber.isBlank()) {
                { Text("Campo obbligatorio", color = MaterialTheme.colorScheme.error) }
            } else null,
            enabled = !isEditMode, // Read-only in edit mode for fiscal compliance
            modifier = Modifier.fillMaxWidth()
        )

        // Notes (always editable)
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Note") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RobotDataSection(
    serialNumber: String,
    onSerialNumberChange: (String) -> Unit,
    hoursOfDuty: String,
    onHoursOfDutyChange: (String) -> Unit,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PrecisionManufacturing,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Dati Robot",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (isEditMode) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "(Immutabili in modifica)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Serial number (required)
        OutlinedTextField(
            value = serialNumber,
            onValueChange = onSerialNumberChange,
            label = { Text("Serial Number *") },
            isError = serialNumber.isBlank(),
            supportingText = if (serialNumber.isBlank()) {
                { Text("Campo obbligatorio", color = MaterialTheme.colorScheme.error) }
            } else null,
            enabled = !isEditMode, // Read-only in edit mode for fiscal compliance
            modifier = Modifier.fillMaxWidth()
        )

        // Hours of duty (editable in both modes - can be updated)
        OutlinedTextField(
            value = hoursOfDuty,
            onValueChange = onHoursOfDutyChange,
            label = { Text("Ore di Servizio *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = hoursOfDuty.isBlank() || hoursOfDuty.toIntOrNull() == null,
            supportingText = if (hoursOfDuty.isBlank()) {
                { Text("Campo obbligatorio", color = MaterialTheme.colorScheme.error) }
            } else if (hoursOfDuty.toIntOrNull() == null) {
                { Text("Inserire un numero valido", color = MaterialTheme.colorScheme.error) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WorkLocationSection(
    workLocation: WorkLocationType,
    onWorkLocationChange: (WorkLocationType) -> Unit,
    customLocation: String,
    onCustomLocationChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Lavoro svolto presso",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        // Work location dropdown
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = workLocation.displayName,
                onValueChange = { },
                readOnly = true,
                label = { Text("Sede di Lavoro") },
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
                WorkLocationType.values().forEach { locationType ->
                    DropdownMenuItem(
                        text = { Text(locationType.displayName) },
                        onClick = {
                            onWorkLocationChange(locationType)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Custom location field (only when OTHER is selected)
        if (workLocation == WorkLocationType.OTHER) {
            OutlinedTextField(
                value = customLocation,
                onValueChange = onCustomLocationChange,
                label = { Text("Specifica Sede") },
                placeholder = { Text("Inserisci sede personalizzata") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TechniciansSection(
    technicians: String,
    onTechniciansChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tecnici (max 6)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = technicians,
            onValueChange = onTechniciansChange,
            label = { Text("Nomi Tecnici") },
            placeholder = { Text("Inserisci nomi separati da virgola") },
            supportingText = { Text("Massimo 6 tecnici, separati da virgola") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}


/**
 * Dirty state indicator component
 */
@Composable
private fun DirtyStateIndicator(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Auto-save indicator component
 */
@Composable
private fun AutoSaveIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Salvataggio in corso...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Extension for WorkLocationType display names
 */
private val WorkLocationType.displayName: String
    get() = when (this) {
        WorkLocationType.CLIENT_SITE -> "Sede Cliente"
        WorkLocationType.OUR_SITE -> "Nostra Sede"
        WorkLocationType.OTHER -> "Altro"
    }