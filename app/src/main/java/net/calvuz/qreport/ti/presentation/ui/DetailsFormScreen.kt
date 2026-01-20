@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.ti.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
 * Form screen for editing intervention details:
 * - Intervention Description
 * - Materials Used (DDT + 6 fixed items)
 * - External Report
 * - Completion flag
 */
@Composable
fun DetailsFormScreen(
    interventionId: String,
    modifier: Modifier = Modifier,
    viewModel: DetailsFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Note: ViewModel is initialized by parent EditInterventionScreen to avoid double loading
    // LaunchedEffect(interventionId) { viewModel.loadInterventionDetails(interventionId) }

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
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // ===== INTERVENTION DESCRIPTION SECTION =====
        InterventionDescriptionSection(
            description = state.interventionDescription,
            onDescriptionChange = viewModel::updateInterventionDescription,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // ===== MATERIALS USED SECTION =====
        MaterialsUsedSection(
            ddtNumber = state.ddtNumber,
            onDdtNumberChange = viewModel::updateDdtNumber,
            ddtDate = state.ddtDate,
            onDdtDateChange = viewModel::updateDdtDate,
            materialItems = state.materialItems,
            onMaterialItemChange = viewModel::updateMaterialItem,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // ===== EXTERNAL REPORT SECTION =====
        ExternalReportSection(
            reportNumber = state.externalReportNumber,
            onReportNumberChange = viewModel::updateExternalReportNumber,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // ===== COMPLETION SECTION =====
        CompletionSection(
            isComplete = state.isComplete,
            onCompletionChange = viewModel::updateCompletionStatus,
            modifier = Modifier.fillMaxWidth()
        )

        // Bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InterventionDescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
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
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Descrizione Intervento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Descrizione Tecnica") },
            placeholder = { Text("Descrivi l'intervento tecnico effettuato...") },
            minLines = 5,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MaterialsUsedSection(
    ddtNumber: String,
    onDdtNumberChange: (String) -> Unit,
    ddtDate: String,
    onDdtDateChange: (String) -> Unit,
    materialItems: List<MaterialItemState>,
    onMaterialItemChange: (Int, String, String) -> Unit,
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
                imageVector = Icons.Default.Inventory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Materiali Utilizzati",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // DDT Reference
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = ddtNumber,
                onValueChange = onDdtNumberChange,
                label = { Text("Numero DDT") },
                placeholder = { Text("es. DDT001/2024") },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = ddtDate,
                onValueChange = onDdtDateChange,
                label = { Text("Data DDT") },
                placeholder = { Text("dd/MM/yyyy") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        // Material Items (Fixed 6 rows)
        Text(
            text = "Materiali (max 6 righe)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        repeat(6) { index ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Materiale ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (index < materialItems.size) materialItems[index].quantity else "",
                            onValueChange = { newQuantity ->
                                val currentDescription = if (index < materialItems.size) materialItems[index].description else ""
                                onMaterialItemChange(index, newQuantity, currentDescription)
                            },
                            label = { Text("Qtà") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp)
                        )

                        OutlinedTextField(
                            value = if (index < materialItems.size) materialItems[index].description else "",
                            onValueChange = { newDescription ->
                                val currentQuantity = if (index < materialItems.size) materialItems[index].quantity else ""
                                onMaterialItemChange(index, currentQuantity, newDescription)
                            },
                            label = { Text("Descrizione") },
                            placeholder = { Text("Descrivi il materiale...") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalReportSection(
    reportNumber: String,
    onReportNumberChange: (String) -> Unit,
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
                imageVector = Icons.AutoMirrored.Default.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Report Esterno",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = reportNumber,
            onValueChange = onReportNumberChange,
            label = { Text("Numero Report") },
            placeholder = { Text("Numero report ditta esterna") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CompletionSection(
    isComplete: Boolean,
    onCompletionChange: (Boolean) -> Unit,
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
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Stato Completamento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isComplete)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(
                    checked = isComplete,
                    onCheckedChange = onCompletionChange
                )

                Column {
                    Text(
                        text = if (isComplete) "Intervento Completato" else "Intervento in Corso",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isComplete)
                            "L'intervento è stato completato con successo"
                        else
                            "L'intervento è ancora in corso",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Data class for material item state
 */
data class MaterialItemState(
    val quantity: String = "",
    val description: String = ""
)

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