@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("HardCodedStringLiteral", "ASSIGNED_VALUE_IS_NEVER_READ")
package net.calvuz.qreport.ti.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.checkup.checkup.presentation.components.ClientFacilityIslandSelectorDialog
import net.calvuz.qreport.ti.domain.model.WorkLocationType

/**
 * Screen for creating a new TechnicalIntervention
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalInterventionFormScreen(
    modifier: Modifier = Modifier,
    viewModel: TechnicalInterventionFormViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onInterventionSaved: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    val handleBackPress = {
        if (state.hasUnsavedData) {
            showUnsavedChangesDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Handle side effects
    LaunchedEffect(state.isSuccess) {
        val savedId = state.savedInterventionId
        if (state.isSuccess && savedId != null) {
            onInterventionSaved(savedId)
        }
    }

    BackHandler(onBack = handleBackPress)

    if (state.showSourceSelectionDialog) {
        ClientFacilityIslandSelectorDialog(
            availableClients = state.availableClients,
            availableFacilities = state.availableFacilities,
            availableIslands = state.availableIslands,
            selectedClientId = state.selectedClientId,
            selectedFacilityId = state.selectedFacilityId,
            isLoading = state.isLoadingSelection,
            onDismiss = viewModel::dismissSourceSelectionDialog,
            onClientSelected = viewModel::onClientSelectedForSource,
            onFacilitySelected = viewModel::onFacilitySelectedForSource,
            onIslandSelected = viewModel::onIslandSelectedForSource
        )
    }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text(stringResource(R.string.intervention_general_unsaved_changes)) },
            text = { Text(stringResource(R.string.intervention_form_unsaved_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedChangesDialog = false
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.intervention_form_unsaved_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) {
                    Text(stringResource(R.string.intervention_form_unsaved_keep_editing))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.interventions_create_new))
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveIntervention() },
                        enabled = state.canSave && !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.action_create).uppercase())
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

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

            // ===== SOURCE SELECTION SECTION =====
            TiSourceSelectionSection(
                isLinked = state.isLinked,
                linkedClientName = state.linkedClientName,
                linkedIslandLabel = state.linkedIslandLabel,
                onLinkSource = viewModel::openSourceSelectionDialog,
                onUnlinkSource = viewModel::clearLinkedSource
            )

            HorizontalDivider()

            // ===== CUSTOMER SECTION =====
            CustomerDataSection(
                isLinked = state.isLinked,
                customerName = state.customerName,
                onCustomerNameChange = viewModel::updateCustomerName,
                customerContact = state.customerContact,
                onCustomerContactChange = viewModel::updateCustomerContact,
                ticketNumber = state.ticketNumber,
                onTicketNumberChange = viewModel::updateTicketNumber,
                customerOrderNumber = state.customerOrderNumber,
                onCustomerOrderNumberChange = viewModel::updateCustomerOrderNumber,
                notes = state.notes,
                onNotesChange = viewModel::updateNotes
            )

            HorizontalDivider()

            // ===== ROBOT DATA SECTION =====
            if (!state.isLinked) {
                RobotDataSection(
                    serialNumber = state.serialNumber,
                    onSerialNumberChange = viewModel::updateSerialNumber,
                    hoursOfDuty = state.hoursOfDuty,
                    onHoursOfDutyChange = viewModel::updateHoursOfDuty
                )

                HorizontalDivider()
            }

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
}

@Composable
private fun TiSourceSelectionSection(
    isLinked: Boolean,
    linkedClientName: String?,
    linkedIslandLabel: String?,
    onLinkSource: () -> Unit,
    onUnlinkSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.intervention_form_section_source),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (isLinked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(
                        R.string.intervention_form_linked_banner,
                        linkedClientName ?: "",
                        linkedIslandLabel ?: ""
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            TextButton(onClick = onUnlinkSource) {
                Icon(
                    imageVector = Icons.Default.LinkOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.intervention_form_unlink_source))
            }
        } else {
            OutlinedButton(
                onClick = onLinkSource,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.intervention_form_link_source_button))
            }
        }
    }
}

@Composable
private fun CustomerDataSection(
    modifier: Modifier = Modifier,
    isLinked: Boolean = false,
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    customerContact: String,
    onCustomerContactChange: (String) -> Unit,
    ticketNumber: String,
    onTicketNumberChange: (String) -> Unit,
    customerOrderNumber: String,
    onCustomerOrderNumberChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
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
                text = stringResource(R.string.intervention_form_section_customer),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Customer name — hidden when linked (snapshot comes from Client entity)
        if (!isLinked) {
            OutlinedTextField(
                value = customerName,
                onValueChange = onCustomerNameChange,
                label = { Text(stringResource(R.string.intervention_form_customer_name_label)) },
                isError = customerName.isBlank(),
                supportingText = if (customerName.isBlank()) {
                    { Text(stringResource(R.string.err_field_required), color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            // Customer contact (optional)
            OutlinedTextField(
                value = customerContact,
                onValueChange = onCustomerContactChange,
                label = { Text(stringResource(R.string.intervention_form_customer_contact_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Ticket number (required)
        OutlinedTextField(
            value = ticketNumber,
            onValueChange = onTicketNumberChange,
            label = { Text(stringResource(R.string.intervention_form_ticket_number_label)) },
            isError = ticketNumber.isBlank(),
            supportingText = if (ticketNumber.isBlank()) {
                { Text(stringResource(R.string.err_field_required), color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        // Customer order number (required)
        OutlinedTextField(
            value = customerOrderNumber,
            onValueChange = onCustomerOrderNumberChange,
            label = { Text(stringResource(R.string.intervention_form_customer_order_number_label)) },
            isError = customerOrderNumber.isBlank(),
            supportingText = if (customerOrderNumber.isBlank()) {
                { Text(stringResource(R.string.err_field_required), color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        // Notes (always editable)
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.intervention_form_notes_label)) },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RobotDataSection(
    modifier: Modifier = Modifier,
    serialNumber: String,
    onSerialNumberChange: (String) -> Unit,
    hoursOfDuty: String,
    onHoursOfDutyChange: (String) -> Unit
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
                text = stringResource(R.string.intervention_form_section_robot),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
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
            modifier = Modifier.fillMaxWidth()
        )

        // Hours of duty (always editable)
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
            text = stringResource(R.string.intervention_form_section_work_location),
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
            text = stringResource(R.string.intervention_form_section_technicians),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = technicians,
            onValueChange = onTechniciansChange,
            label = { Text(stringResource(R.string.intervention_form_technicians_label)) },
            placeholder = { Text(stringResource(R.string.intervention_form_technicians_placeholder)) },
            supportingText = { Text(stringResource(R.string.intervention_form_technicians_supporting)) },
            modifier = Modifier.fillMaxWidth()
        )
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