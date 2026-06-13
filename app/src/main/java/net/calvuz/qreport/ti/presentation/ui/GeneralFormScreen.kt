@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.ti.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.ti.domain.model.WorkLocationType

/**
 * Screen for creating or editing TechnicalIntervention
 * Handles both modes: Create (interventionId = null) and Edit (interventionId != null)
 *
 * UPDATED: Unified card styling matching WorkDayFormScreen and SignaturesFormScreen
 */
@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralFormScreen(
    modifier: Modifier = Modifier,
    interventionId: String? = null,
    viewModel: GeneralFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // Form content
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Dirty state indicator
        if (state.isDirty) {
            DirtyStateIndicator(
                message = stringResource(R.string.intervention_general_unsaved_changes),
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

        // Edit mode indicator
        if (state.isEditMode) {
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
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.intervention_form_edit_mode_badge),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
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

        // ===== ROBOT DATA SECTION =====
        RobotDataSection(
            serialNumber = state.serialNumber,
            onSerialNumberChange = viewModel::updateSerialNumber,
            hoursOfDuty = state.hoursOfDuty,
            onHoursOfDutyChange = viewModel::updateHoursOfDuty,
            isEditMode = state.isEditMode
        )

        // ===== WORK LOCATION SECTION =====
        WorkLocationSection(
            workLocation = state.workLocation,
            onWorkLocationChange = viewModel::updateWorkLocation,
            customLocation = state.customLocation,
            onCustomLocationChange = viewModel::updateCustomLocation
        )

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
    modifier: Modifier = Modifier,
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
    isEditMode: Boolean = false
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    text = stringResource(R.string.intervention_form_section_customer),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isEditMode) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "(${stringResource(R.string.intervention_form_immutable_in_edit)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Customer name (required)
            OutlinedTextField(
                value = customerName,
                onValueChange = onCustomerNameChange,
                label = { Text(stringResource(R.string.intervention_form_customer_name_label)) },
                isError = customerName.isBlank(),
                supportingText = if (customerName.isBlank()) {
                    { Text(stringResource(R.string.err_field_required), color = MaterialTheme.colorScheme.error) }
                } else null,
                enabled = !isEditMode,
                modifier = Modifier.fillMaxWidth()
            )

            // Customer contact (optional)
            OutlinedTextField(
                value = customerContact,
                onValueChange = onCustomerContactChange,
                label = { Text(stringResource(R.string.intervention_form_customer_contact_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Ticket number (required)
            OutlinedTextField(
                value = ticketNumber,
                onValueChange = onTicketNumberChange,
                label = { Text(stringResource(R.string.intervention_form_ticket_number_label)) },
                isError = ticketNumber.isBlank(),
                supportingText = if (ticketNumber.isBlank()) {
                    { Text(stringResource(R.string.err_field_required), color = MaterialTheme.colorScheme.error) }
                } else null,
                enabled = !isEditMode,
                modifier = Modifier.fillMaxWidth()
            )

            // Customer order number (optional)
            OutlinedTextField(
                value = customerOrderNumber,
                onValueChange = onCustomerOrderNumberChange,
                label = { Text(stringResource(R.string.intervention_general_customer_order_number_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Notes (optional)
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.intervention_form_notes_label)) },
                placeholder = { Text(stringResource(R.string.intervention_general_notes_placeholder)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RobotDataSection(
    modifier: Modifier = Modifier,
    serialNumber: String,
    onSerialNumberChange: (String) -> Unit,
    hoursOfDuty: String,
    onHoursOfDutyChange: (String) -> Unit,
    isEditMode: Boolean = false
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    text = stringResource(R.string.intervention_form_section_robot),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isEditMode) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "(${stringResource(R.string.intervention_form_immutable_in_edit)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Serial number (required)
            OutlinedTextField(
                value = serialNumber,
                onValueChange = onSerialNumberChange,
                label = { Text(stringResource(R.string.intervention_form_serial_number_label)) },
                isError = serialNumber.isBlank(),
                supportingText = if (serialNumber.isBlank()) {
                    { Text(stringResource(R.string.err_field_required), color = MaterialTheme.colorScheme.error) }
                } else null,
                enabled = !isEditMode,
                modifier = Modifier.fillMaxWidth()
            )

            // Hours of duty (editable in both modes - can be updated)
            OutlinedTextField(
                value = hoursOfDuty,
                onValueChange = onHoursOfDutyChange,
                label = { Text(stringResource(R.string.intervention_form_hours_of_duty_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = hoursOfDuty.isBlank() || hoursOfDuty.toIntOrNull() == null,
                supportingText = if (hoursOfDuty.isBlank()) {
                    { Text(stringResource(R.string.err_field_required), color = MaterialTheme.colorScheme.error) }
                } else if (hoursOfDuty.toIntOrNull() == null) {
                    { Text(stringResource(R.string.err_invalid_number), color = MaterialTheme.colorScheme.error) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
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
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.intervention_form_section_work_location),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Work location dropdown
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = workLocation.displayName.asString(),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.intervention_form_work_location_label)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    WorkLocationType.entries.forEach { locationType ->
                        DropdownMenuItem(
                            text = { Text(locationType.displayName.asString()) },
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
                    label = { Text(stringResource(R.string.intervention_form_custom_location_label)) },
                    placeholder = { Text(stringResource(R.string.intervention_form_custom_location_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TechniciansSection(
    technicians: String,
    onTechniciansChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.intervention_form_section_technicians),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = technicians,
                onValueChange = onTechniciansChange,
                label = { Text(stringResource(R.string.intervention_form_technicians_label)) },
                placeholder = { Text(stringResource(R.string.intervention_form_technicians_placeholder)) },
                supportingText = { Text(stringResource(R.string.intervention_form_technicians_supporting)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
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
                text = stringResource(R.string.msg_saving),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Extension for WorkLocationType display names
 */
private val WorkLocationType.displayName: UiText
    get() = when (this) {
        WorkLocationType.CLIENT_SITE -> UiText.StringResource(R.string.work_location_client_site)
        WorkLocationType.OUR_SITE -> UiText.StringResource(R.string.work_location_our_site)
        WorkLocationType.OTHER -> UiText.StringResource(R.string.work_location_other)
    }