package net.calvuz.qreport.checkup.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.QrLoadingState
import net.calvuz.qreport.client.client.domain.model.Client
import net.calvuz.qreport.client.facility.domain.model.Facility
import net.calvuz.qreport.client.island.domain.model.Island

/**
 * Dialog per la selezione guidata Cliente -> Stabilimento -> Isola,
 * usato per pre-compilare e collegare un nuovo check-up.
 */
@Composable
fun ClientFacilityIslandSelectorDialog(
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
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.checkup_component_source_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.checkup_component_association_dialog_close)
                        )
                    }
                }

                HorizontalDivider()

                if (isLoading) {
                    QrLoadingState(message = stringResource(R.string.checkup_component_association_dialog_loading))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ClientSelectionStep(
                                clients = availableClients,
                                selectedClientId = selectedClientId,
                                onClientSelected = onClientSelected
                            )
                        }

                        if (selectedClientId != null) {
                            item {
                                FacilitySelectionStep(
                                    facilities = availableFacilities,
                                    selectedFacilityId = selectedFacilityId,
                                    onFacilitySelected = onFacilitySelected
                                )
                            }
                        }

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
