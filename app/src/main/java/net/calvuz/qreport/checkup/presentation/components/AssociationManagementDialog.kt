package net.calvuz.qreport.checkup.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.domain.model.CheckUpIslandAssociation
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.island.domain.model.Island
import net.calvuz.qreport.app.app.presentation.components.LoadingState
import timber.log.Timber

/**
 * ✅ DIALOG COMPLETO per gestione associazioni CheckUp-Isole
 *
 * Flow: Cliente → Stabilimento → Isola
 * Features:
 * - Step navigation guidata
 * - Rimozione associazione esistente
 * - Visual feedback per ogni step
 */
@Composable
fun AssociationManagementDialog(
    currentAssociations: List<CheckUpIslandAssociation>,
    availableClients: List<Client>,
    availableFacilities: List<Facility>,
    availableIslands: List<Island>,
    selectedClientId: String?,
    selectedFacilityId: String?,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onClientSelected: (String) -> Unit,
    onFacilitySelected: (String) -> Unit,
    onIslandSelected: (String) -> Unit,
    onRemoveAssociation: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                AssociationDialogHeader(
                    currentAssociations = currentAssociations,
                    onDismiss = onDismiss,
                    onRemoveAssociation = onRemoveAssociation
                )

                HorizontalDivider()

                // Content
                if (isLoading) {
                    LoadingState(stringResource(R.string.checkup_component_association_dialog_loading))

                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Step 1: Client Selection
                        item {
                            ClientSelectionStep(
                                clients = availableClients,
                                selectedClientId = selectedClientId,
                                onClientSelected = onClientSelected
                            )
                        }

                        // Step 2: Facility Selection
                        if (selectedClientId != null) {
                            item {
                                FacilitySelectionStep(
                                    facilities = availableFacilities,
                                    selectedFacilityId = selectedFacilityId,
                                    onFacilitySelected = onFacilitySelected
                                )
                            }
                        }

                        // Step 3: Island Selection
                        if (selectedFacilityId != null) {
                            item {
                                IslandSelectionStep(
                                    islands = availableIslands,
                                    onIslandSelected = onIslandSelected
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssociationDialogHeader(
    currentAssociations: List<CheckUpIslandAssociation>,
    onDismiss: () -> Unit,
    onRemoveAssociation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.checkup_component_association_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (currentAssociations.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.checkup_component_association_dialog_associated_to,
                        currentAssociations.first().islandId
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (currentAssociations.isNotEmpty()) {
                TextButton(
                    onClick = onRemoveAssociation,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.checkup_component_association_dialog_remove))
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.checkup_component_association_dialog_close)
                )
            }
        }
    }
}

@Composable
private fun ClientSelectionStep(
    clients: List<Client>,
    selectedClientId: String?,
    onClientSelected: (String) -> Unit
) {
    SelectionStepCard(
        stepNumber = 1,
        title = stringResource(R.string.checkup_component_association_dialog_step_client),
        isCompleted = selectedClientId != null,
        isEmpty = clients.isEmpty()
    ) {
        if (clients.isEmpty()) {
            EmptyStateMessage(
                stringResource(R.string.checkup_component_association_dialog_empty_clients)
            )
        } else {
            clients.forEach { client ->
                SelectionItem(
                    title = client.companyName,
                    subtitle = client.vatNumber ?: "",
                    isSelected = client.id == selectedClientId,
                    onClick = { onClientSelected(client.id) }
                )
            }
        }
    }
}

@Composable
private fun FacilitySelectionStep(
    facilities: List<Facility>,
    selectedFacilityId: String?,
    onFacilitySelected: (String) -> Unit
) {
    SelectionStepCard(
        stepNumber = 2,
        title = stringResource(R.string.checkup_component_association_dialog_step_facility),
        isCompleted = selectedFacilityId != null,
        isEmpty = facilities.isEmpty()
    ) {
        if (facilities.isEmpty()) {
            EmptyStateMessage(stringResource(R.string.checkup_component_association_dialog_empty_facilities))
        } else {
            facilities.forEach { facility ->
                SelectionItem(
                    title = facility.name,
                    subtitle = facility.address.city ?: "",
                    isSelected = facility.id == selectedFacilityId,
                    onClick = { onFacilitySelected(facility.id) }
                )
            }
        }
    }
}

@Composable
private fun IslandSelectionStep(
    islands: List<Island>,
    onIslandSelected: (String) -> Unit
) {
    SelectionStepCard(
        stepNumber = 3,
        title = stringResource(R.string.checkup_component_association_dialog_step_island),
        isCompleted = false,
        isEmpty = islands.isEmpty()
    ) {
        if (islands.isEmpty()) {
            EmptyStateMessage(
                stringResource(R.string.checkup_component_association_dialog_empty_islands)
            )
        } else {
            islands.forEach { island ->
                SelectionItem(
                    title = "${island.islandType.displayName} • ${island.serialNumber}",
                    subtitle =
                        if (island.model != null) {
                            stringResource(
                                R.string.checkup_component_association_dialog_model_prefix,
                                island.model
                            )
                        } else {
                            stringResource(R.string.checkup_component_association_dialog_model_not_specified)
                        },
                    isSelected = false,
                    onClick = { onIslandSelected(island.id) },
                    isClickable = true
                )
            }
        }
    }
}

@Composable
private fun SelectionStepCard(
    stepNumber: Int,
    title: String,
    isCompleted: Boolean,
    isEmpty: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    if (isEmpty) {
        Timber.d("Empty selection step Card ($title)")
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Step indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isCompleted)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Content
            content()
        }
    }
}

@Composable
private fun SelectionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isClickable: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.checkup_component_association_dialog_selected),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (isClickable) {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = stringResource(R.string.checkup_component_association_dialog_not_selected),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}