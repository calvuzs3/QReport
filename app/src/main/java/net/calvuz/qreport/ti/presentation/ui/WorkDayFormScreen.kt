@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.ti.presentation.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Form screen for editing work day details:
 * - Date and technician info
 * - Travel hours (if not remote)
 * - Work hours (morning/afternoon)
 * - Expenses tracking
 *
 * @param interventionId The intervention ID
 * @param workDayIndex Index of work day to edit (null = create new)
 * @param viewModel The form ViewModel (injected or provided by parent)
 */
@Composable
fun WorkDayFormScreen(
    interventionId: String,
    workDayIndex: Int? = null,
    modifier: Modifier = Modifier,
    viewModel: WorkDayFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // Dirty state indicator
        if (state.isDirty) {
            DirtyStateIndicator(
                message = if (state.isNewWorkDay) "Nuova giornata - non salvata" else "Modifiche non salvate",
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
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // ===== BASIC INFO SECTION =====
        BasicInfoSection(
            date = state.date,
            onDateChange = viewModel::updateDate,
            technicianCount = state.technicianCount,
            onTechnicianCountChange = viewModel::updateTechnicianCount,
            technicianInitials = state.technicianInitials,
            onTechnicianInitialsChange = viewModel::updateTechnicianInitials,
            remoteAssistance = state.remoteAssistance,
            onRemoteAssistanceChange = viewModel::updateRemoteAssistance,
            modifier = Modifier.fillMaxWidth()
        )

        // ===== TRAVEL HOURS SECTION (Hidden if remote assistance) =====
        if (!state.remoteAssistance) {
            TravelHoursSection(
                outboundTravelStart = state.outboundTravelStart,
                onOutboundTravelStartChange = viewModel::updateOutboundTravelStart,
                outboundTravelEnd = state.outboundTravelEnd,
                onOutboundTravelEndChange = viewModel::updateOutboundTravelEnd,
                returnTravelStart = state.returnTravelStart,
                onReturnTravelStartChange = viewModel::updateReturnTravelStart,
                returnTravelEnd = state.returnTravelEnd,
                onReturnTravelEndChange = viewModel::updateReturnTravelEnd,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== WORK HOURS SECTION =====
        WorkHoursSection(
            morningStart = state.morningStart,
            onMorningStartChange = viewModel::updateMorningStart,
            morningEnd = state.morningEnd,
            onMorningEndChange = viewModel::updateMorningEnd,
            afternoonStart = state.afternoonStart,
            onAfternoonStartChange = viewModel::updateAfternoonStart,
            afternoonEnd = state.afternoonEnd,
            onAfternoonEndChange = viewModel::updateAfternoonEnd,
            modifier = Modifier.fillMaxWidth()
        )

        // ===== EXPENSES SECTION =====
        ExpensesSection(
            morningPocketMoney = state.morningPocketMoney,
            onMorningPocketMoneyChange = viewModel::updateMorningPocketMoney,
            afternoonPocketMoney = state.afternoonPocketMoney,
            onAfternoonPocketMoneyChange = viewModel::updateAfternoonPocketMoney,
            totalKilometers = state.totalKilometers,
            onTotalKilometersChange = viewModel::updateTotalKilometers,
            flight = state.flight,
            onFlightChange = viewModel::updateFlight,
            rentCar = state.rentCar,
            onRentCarChange = viewModel::updateRentCar,
            transferToAirport = state.transferToAirport,
            onTransferToAirportChange = viewModel::updateTransferToAirport,
            lodging = state.lodging,
            onLodgingChange = viewModel::updateLodging,
            remoteAssistance = state.remoteAssistance,
            modifier = Modifier.fillMaxWidth()
        )

        // Bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicInfoSection(
    date: String,
    onDateChange: (String) -> Unit,
    technicianCount: String,
    onTechnicianCountChange: (String) -> Unit,
    technicianInitials: String,
    onTechnicianInitialsChange: (String) -> Unit,
    remoteAssistance: Boolean,
    onRemoteAssistanceChange: (Boolean) -> Unit,
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
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Informazioni Giornata",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Date picker
            OutlinedTextField(
                value = date,
                onValueChange = onDateChange,
                label = { Text("Data") },
                placeholder = { Text("dd/MM/yyyy") },
                modifier = Modifier.fillMaxWidth()
            )

            // Remote assistance toggle
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (remoteAssistance)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        tint = if (remoteAssistance)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Assistenza Remota",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (remoteAssistance)
                                "I campi viaggio sono nascosti"
                            else
                                "Attiva per nascondere i tempi di viaggio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = remoteAssistance,
                        onCheckedChange = onRemoteAssistanceChange
                    )
                }
            }

            // Technician info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = technicianCount,
                    onValueChange = onTechnicianCountChange,
                    label = { Text("N° Tecnici") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.4f)
                )

                OutlinedTextField(
                    value = technicianInitials,
                    onValueChange = onTechnicianInitialsChange,
                    label = { Text("Sigle Tecnici") },
                    placeholder = { Text("LC, AB, MR") },
                    modifier = Modifier.weight(0.6f)
                )
            }
        }
    }
}

@Composable
private fun TravelHoursSection(
    outboundTravelStart: String,
    onOutboundTravelStartChange: (String) -> Unit,
    outboundTravelEnd: String,
    onOutboundTravelEndChange: (String) -> Unit,
    returnTravelStart: String,
    onReturnTravelStartChange: (String) -> Unit,
    returnTravelEnd: String,
    onReturnTravelEndChange: (String) -> Unit,
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
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Orari di Viaggio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Outbound travel
            Text(
                text = "Viaggio Andata",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeTextField(
                    value = outboundTravelStart,
                    onValueChange = onOutboundTravelStartChange,
                    label = "Inizio",
                    modifier = Modifier.weight(1f)
                )

                TimeTextField(
                    value = outboundTravelEnd,
                    onValueChange = onOutboundTravelEndChange,
                    label = "Fine",
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Return travel
            Text(
                text = "Viaggio Ritorno",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeTextField(
                    value = returnTravelStart,
                    onValueChange = onReturnTravelStartChange,
                    label = "Inizio",
                    modifier = Modifier.weight(1f)
                )

                TimeTextField(
                    value = returnTravelEnd,
                    onValueChange = onReturnTravelEndChange,
                    label = "Fine",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WorkHoursSection(
    morningStart: String,
    onMorningStartChange: (String) -> Unit,
    morningEnd: String,
    onMorningEndChange: (String) -> Unit,
    afternoonStart: String,
    onAfternoonStartChange: (String) -> Unit,
    afternoonEnd: String,
    onAfternoonEndChange: (String) -> Unit,
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
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Orari di Lavoro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Morning work
            Text(
                text = "Lavoro Mattina",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeTextField(
                    value = morningStart,
                    onValueChange = onMorningStartChange,
                    label = "Inizio",
                    modifier = Modifier.weight(1f)
                )

                TimeTextField(
                    value = morningEnd,
                    onValueChange = onMorningEndChange,
                    label = "Fine",
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Afternoon work
            Text(
                text = "Lavoro Pomeriggio",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeTextField(
                    value = afternoonStart,
                    onValueChange = onAfternoonStartChange,
                    label = "Inizio",
                    modifier = Modifier.weight(1f)
                )

                TimeTextField(
                    value = afternoonEnd,
                    onValueChange = onAfternoonEndChange,
                    label = "Fine",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExpensesSection(
    morningPocketMoney: Boolean,
    onMorningPocketMoneyChange: (Boolean) -> Unit,
    afternoonPocketMoney: Boolean,
    onAfternoonPocketMoneyChange: (Boolean) -> Unit,
    totalKilometers: String,
    onTotalKilometersChange: (String) -> Unit,
    flight: Boolean,
    onFlightChange: (Boolean) -> Unit,
    rentCar: Boolean,
    onRentCarChange: (Boolean) -> Unit,
    transferToAirport: Boolean,
    onTransferToAirportChange: (Boolean) -> Unit,
    lodging: Boolean,
    onLodgingChange: (Boolean) -> Unit,
    remoteAssistance: Boolean,
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
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Spese",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Pocket money checkboxes
            Text(
                text = "Indennità di Trasferta",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CheckboxWithLabel(
                    checked = morningPocketMoney,
                    onCheckedChange = onMorningPocketMoneyChange,
                    label = "1/2 Mattina",
                    modifier = Modifier.weight(1f)
                )

                CheckboxWithLabel(
                    checked = afternoonPocketMoney,
                    onCheckedChange = onAfternoonPocketMoneyChange,
                    label = "1/2 Pomeriggio",
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Travel expenses (hidden if remote assistance)
            if (!remoteAssistance) {
                Text(
                    text = "Spese di Viaggio",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Kilometers
                OutlinedTextField(
                    value = totalKilometers,
                    onValueChange = onTotalKilometersChange,
                    label = { Text("Chilometri Totali") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Inserire i km percorsi") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Travel expense checkboxes
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CheckboxWithLabel(
                        checked = flight,
                        onCheckedChange = onFlightChange,
                        label = "Volo"
                    )

                    CheckboxWithLabel(
                        checked = rentCar,
                        onCheckedChange = onRentCarChange,
                        label = "Auto a Noleggio"
                    )

                    CheckboxWithLabel(
                        checked = transferToAirport,
                        onCheckedChange = onTransferToAirportChange,
                        label = "Transfer Aeroporto"
                    )

                    CheckboxWithLabel(
                        checked = lodging,
                        onCheckedChange = onLodgingChange,
                        label = "Pernottamento"
                    )
                }
            } else {
                // Show note for remote assistance
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Spese di viaggio nascoste per assistenza remota",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Filter input to HH:mm format
            val filtered = newValue.filter { it.isDigit() || it == ':' }
            if (filtered.length <= 5) {
                onValueChange(filtered)
            }
        },
        label = { Text(label) },
        placeholder = { Text("HH:mm") },
        modifier = modifier
    )
}

@Composable
private fun CheckboxWithLabel(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
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