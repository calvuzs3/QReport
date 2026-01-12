@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.presentation.screen.intervention.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
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
import net.calvuz.qreport.ti.presentation.ui.CreateInterventionViewModel

/**
 * Screen for creating new TechnicalIntervention
 * Focus on Customer Section + Robot Data Section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInterventionScreen(
    onNavigateBack: () -> Unit,
    onInterventionCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateInterventionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    val e = state.errorMessage

    // Handle side effects
    LaunchedEffect(state.isSuccess) {
        val interventionId = state.createdInterventionId
        if (state.isSuccess && interventionId != null) {
            onInterventionCreated(interventionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuovo Intervento Tecnico") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.createIntervention() },
                        enabled = state.canCreate && !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("CREA")
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Error display
            if (e != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = e.asString(),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
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
                onNotesChange = viewModel::updateNotes
            )

            HorizontalDivider()

            // ===== ROBOT DATA SECTION =====
            RobotDataSection(
                serialNumber = state.serialNumber,
                onSerialNumberChange = viewModel::updateSerialNumber,
                hoursOfDuty = state.hoursOfDuty,
                onHoursOfDutyChange = viewModel::updateHoursOfDuty
            )

            HorizontalDivider()

            // ===== WORK LOCATION SECTION (Simple) =====
            WorkLocationSection(
                workLocation = state.workLocation,
                onWorkLocationChange = viewModel::updateWorkLocation,
                customLocation = state.customLocation,
                onCustomLocationChange = viewModel::updateCustomLocation
            )

            HorizontalDivider()

            // ===== TECHNICIANS SECTION (Simple) =====
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
            modifier = Modifier.fillMaxWidth()
        )

        // Notes (optional)
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
            modifier = Modifier.fillMaxWidth()
        )

        // Hours of duty (required)
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
 * Extension for WorkLocationType display names
 */
private val WorkLocationType.displayName: String
    get() = when (this) {
        WorkLocationType.CLIENT_SITE -> "Sede Cliente"
        WorkLocationType.OUR_SITE -> "Nostra Sede"
        WorkLocationType.OTHER -> "Altro"
    }