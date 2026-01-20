package net.calvuz.qreport.checkup.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.domain.model.CheckUpHeader
import net.calvuz.qreport.client.client.domain.model.ClientInfo
import net.calvuz.qreport.client.island.domain.model.IslandInfo
import net.calvuz.qreport.settings.domain.model.TechnicianInfo
import net.calvuz.qreport.app.app.presentation.components.SectionCard
import net.calvuz.qreport.settings.presentation.ui.TechnicianSettingsViewModel

/**
 * Dialog per l'editing delle informazioni dell'header del check-up
 */
@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHeaderDialog(
    header: CheckUpHeader,
    onDismiss: () -> Unit,
    onConfirm: (CheckUpHeader) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    technicianViewModel: TechnicianSettingsViewModel = hiltViewModel()
) {
    // Client Info State
    var companyName by remember { mutableStateOf(header.clientInfo.companyName) }
    var contactPerson by remember { mutableStateOf(header.clientInfo.contactPerson) }
    var site by remember { mutableStateOf(header.clientInfo.site) }
    var address by remember { mutableStateOf(header.clientInfo.address) }
    var phone by remember { mutableStateOf(header.clientInfo.phone) }
    var email by remember { mutableStateOf(header.clientInfo.email) }

    // Island Info State
    var serialNumber by remember { mutableStateOf(header.islandInfo.serialNumber) }
    var model by remember { mutableStateOf(header.islandInfo.model) }
    var installationDate by remember { mutableStateOf(header.islandInfo.installationDate) }
    var lastMaintenanceDate by remember { mutableStateOf(header.islandInfo.lastMaintenanceDate) }
    var operatingHours by remember { mutableStateOf(header.islandInfo.operatingHours.toString()) }
    var cycleCount by remember { mutableStateOf(header.islandInfo.cycleCount.toString()) }

    // Technician Info State
    var technicianName by remember { mutableStateOf(header.technicianInfo.name) }
    var technicianCompany by remember { mutableStateOf(header.technicianInfo.company) }
    var certification by remember { mutableStateOf(header.technicianInfo.certification) }
    var technicianPhone by remember { mutableStateOf(header.technicianInfo.phone) }
    var technicianEmail by remember { mutableStateOf(header.technicianInfo.email) }

    // ===== TECHNICIAN SETTINGS INTEGRATION =====
    val technicianSettings by technicianViewModel.currentTechnicianInfo.collectAsStateWithLifecycle()
    val hasTechnicianData by technicianViewModel.hasTechnicianData.collectAsStateWithLifecycle()

    // Notes
    var notes by remember { mutableStateOf(header.notes) }

    // ===== AUTO-LoadError TECHNICIAN DATA =====
    var isAutoLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(technicianSettings) {
        // Solo se i campi tecnico sono vuoti E ci sono settings salvate
        if (technicianName.isBlank() &&
            technicianCompany.isBlank() &&
            hasTechnicianData &&
            (technicianSettings.name.isNotBlank() || technicianSettings.company.isNotBlank())
        ) {
            technicianName = technicianSettings.name
            technicianCompany = technicianSettings.company
            certification = technicianSettings.certification
            technicianPhone = technicianSettings.phone
            technicianEmail = technicianSettings.email
            isAutoLoaded = true
        }
    }

    // Validation
    val isValidForm = companyName.isNotBlank() && serialNumber.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.checkup_dialog_edit_header_title
                        ), // "Modifica Informazioni",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.checkup_dialog_edit_header_close) // "Chiudi"
                        )
                    }
                }

                HorizontalDivider()

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // ===== AUTO-LoadError SUCCESS MESSAGE =====
                    if (isAutoLoaded) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.7f
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.checkup_dialog_edit_header_autoload_message), //"Dati tecnico caricati automaticamente dal profilo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Client Information Section
                    SectionCard(
                        title = stringResource(R.string.checkup_dialog_edit_header_section_client), // "Informazioni Cliente",
                        icon = Icons.Default.Business
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = companyName,
                                onValueChange = { companyName = it },
                                label = { Text(stringResource(R.string.checkup_dialog_edit_header_client_company_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = companyName.isBlank()
                            )

                            OutlinedTextField(
                                value = contactPerson,
                                onValueChange = { contactPerson = it },
                                label = { Text(stringResource(R.string.checkup_dialog_edit_header_client_contact_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = site,
                                onValueChange = { site = it },
                                label = { Text(stringResource(R.string.checkup_dialog_edit_header_client_site_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text(stringResource(R.string.checkup_dialog_edit_header_client_address_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text(stringResource(R.string.checkup_dialog_edit_header_client_phone_label)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text(stringResource(R.string.checkup_dialog_edit_header_client_email_label)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // Island Information Section
                    SectionCard(
                        title = stringResource(R.string.checkup_dialog_edit_header_section_island), //"Informazioni Isola",
                        icon = Icons.Default.PrecisionManufacturing
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = serialNumber,
                                onValueChange = { serialNumber = it },
                                label = { Text(stringResource(R.string.checkup_dialog_edit_header_island_serial_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = serialNumber.isBlank()
                            )

                            OutlinedTextField(
                                value = model,
                                onValueChange = { model = it },
                                label = { Text(stringResource(R.string.checkup_dialog_edit_header_island_model_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = installationDate,
                                    onValueChange = { installationDate = it },
                                    label = { Text(stringResource(R.string.checkup_dialog_edit_header_island_installation_label)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    placeholder = { Text(stringResource(R.string.checkup_dialog_edit_header_island_installation_placeholder)) }
                                )

                                OutlinedTextField(
                                    value = lastMaintenanceDate,
                                    onValueChange = { lastMaintenanceDate = it },
                                    label = { Text(stringResource(R.string.checkup_dialog_edit_header_island_maintenance_label)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    placeholder = { Text(stringResource(R.string.checkup_dialog_edit_header_island_maintenance_placeholder)) }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = operatingHours,
                                    onValueChange = { value ->
                                        if (value.all { it.isDigit() } || value.isEmpty()) {
                                            operatingHours = value
                                        }
                                    },
                                    label = { Text(stringResource(R.string.checkup_dialog_edit_header_island_hours_label)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = cycleCount,
                                    onValueChange = { value ->
                                        if (value.all { it.isDigit() } || value.isEmpty()) {
                                            cycleCount = value
                                        }
                                    },
                                    label = { Text(stringResource(R.string.checkup_dialog_edit_header_island_cycles_label)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // Technician Information Section
                    SectionCard(
                        title = stringResource(R.string.checkup_dialog_edit_header_section_technician),
                        icon = Icons.Default.Engineering // .EngineeringOutlined
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            // Load from Profile Button (if data available and not auto-loaded)
                            if (hasTechnicianData && !isAutoLoaded) {
                                OutlinedButton(
                                    onClick = {
                                        technicianName = technicianSettings.name
                                        technicianCompany = technicianSettings.company
                                        certification = technicianSettings.certification
                                        technicianPhone = technicianSettings.phone
                                        technicianEmail = technicianSettings.email
                                        isAutoLoaded = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.checkup_dialog_edit_header_technician_load_profile))
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            OutlinedTextField(
                                value = technicianName,
                                onValueChange = { technicianName = it },
                                label = { Text(stringResource(R.string.checkup_dialog_edit_header_technician_name_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = technicianCompany,
                                onValueChange = { technicianCompany = it },
                                label = { Text(stringResource(R.string.checkup_dialog_edit_header_technician_company_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = certification,
                                onValueChange = { certification = it },
                                label = { Text(stringResource(R.string.checkup_dialog_edit_header_technician_certification_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = technicianPhone,
                                    onValueChange = { technicianPhone = it },
                                    label = { Text(stringResource(R.string.checkup_dialog_edit_header_technician_phone_label)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = technicianEmail,
                                    onValueChange = { technicianEmail = it },
                                    label = { Text(stringResource(R.string.checkup_dialog_edit_header_technician_email_label)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // Notes Section
                    SectionCard(
                        title = stringResource(R.string.checkup_dialog_edit_header_section_notes),
                        icon = Icons.AutoMirrored.Default.Notes
                    ) {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(stringResource(R.string.checkup_dialog_edit_header_notes_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            placeholder = { Text(stringResource(R.string.checkup_dialog_edit_header_notes_placeholder)) }
                        )
                    }
                }

                // Actions
                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text(stringResource(R.string.checkup_dialog_edit_header_action_cancel))
                    }

                    Button(
                        onClick = {
                            val updatedHeader = header.copy(
                                clientInfo = ClientInfo(
                                    companyName = companyName,
                                    contactPerson = contactPerson,
                                    site = site,
                                    address = address,
                                    phone = phone,
                                    email = email
                                ),
                                islandInfo = IslandInfo(
                                    serialNumber = serialNumber,
                                    model = model,
                                    installationDate = installationDate,
                                    lastMaintenanceDate = lastMaintenanceDate,
                                    operatingHours = operatingHours.toIntOrNull() ?: 0,
                                    cycleCount = cycleCount.toLongOrNull() ?: 0L
                                ),
                                technicianInfo = TechnicianInfo(
                                    name = technicianName,
                                    company = technicianCompany,
                                    certification = certification,
                                    phone = technicianPhone,
                                    email = technicianEmail
                                ),
                                notes = notes
                            )
                            onConfirm(updatedHeader)
                        },
                        enabled = isValidForm && !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.checkup_dialog_edit_header_action_save))
                        }
                    }
                }
            }
        }
    }
}