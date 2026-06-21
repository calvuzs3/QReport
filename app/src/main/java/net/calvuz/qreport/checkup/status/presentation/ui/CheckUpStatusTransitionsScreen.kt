package net.calvuz.qreport.checkup.status.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster

/**
 * Manages which statuses a checkup can move to from each status — drives the
 * transition check in [net.calvuz.qreport.checkup.checkup.domain.usecase.UpdateCheckUpStatusUseCase].
 * Mirror of [net.calvuz.qreport.checkup.modules.presentation.ui.ModuleIslandAssociationScreen]
 * but editing the status workflow graph instead of a module↔island-type association.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckUpStatusTransitionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CheckUpStatusTransitionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkup_status_transitions_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.statuses, key = { it.id }) { status ->
                val toLabels = uiState.transitionsByStatusId[status.id].orEmpty()
                    .mapNotNull { toId -> uiState.statuses.find { it.id == toId }?.label }

                StatusTransitionsRow(
                    status = status,
                    toLabels = toLabels,
                    onEdit = { viewModel.onEditClick(status) }
                )
            }
        }
    }

    uiState.editingFromStatus?.let { fromStatus ->
        CheckUpStatusTransitionsDialog(
            fromStatus = fromStatus,
            statuses = uiState.statuses,
            selectedToStatusIds = uiState.transitionsByStatusId[fromStatus.id].orEmpty(),
            errorMessage = uiState.errorMessage,
            onDismiss = viewModel::onDismissDialog,
            onSave = viewModel::onSave
        )
    }
}

@Composable
private fun StatusTransitionsRow(
    status: CheckUpStatusMaster,
    toLabels: List<String>,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = status.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (toLabels.isEmpty()) {
                        stringResource(R.string.checkup_status_transitions_no_transitions)
                    } else {
                        toLabels.joinToString(", ")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
