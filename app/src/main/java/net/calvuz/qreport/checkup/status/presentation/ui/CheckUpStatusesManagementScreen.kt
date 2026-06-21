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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import net.calvuz.qreport.app.app.presentation.components.QrStatusIndicator
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster

/** Mirror of [net.calvuz.qreport.checkup.criticality.presentation.ui.CriticalityLevelsManagementScreen]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckUpStatusesManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: CheckUpStatusesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkup_status_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_create))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // Extra bottom padding so the FAB doesn't cover the last item
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.statuses, key = { it.id }) { status ->
                CheckUpStatusRow(
                    status = status,
                    onEdit = { viewModel.onEditClick(status) },
                    onDeactivate = { viewModel.onDeactivate(status.id) },
                    onRestore = { viewModel.onRestore(status.id) }
                )
            }
        }
    }

    if (uiState.isCreatingNew || uiState.editingStatus != null) {
        CheckUpStatusFormDialog(
            editingStatus = uiState.editingStatus,
            errorMessage = uiState.errorMessage,
            onDismiss = viewModel::onDismissDialog,
            onSave = viewModel::onSave
        )
    }
}

@Composable
private fun CheckUpStatusRow(
    status: CheckUpStatusMaster,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onRestore: () -> Unit
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
            QrStatusIndicator(isActive = status.isActive, onRestore = onRestore)

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${status.iconEmoji.orEmpty()} ${status.label}".trim(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = buildString {
                        append(status.code)
                        if (status.blocksDeletion) append(" · blocca cancellazione")
                        if (status.marksCompletion) append(" · completamento")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (status.isActive) {
                    IconButton(onClick = onDeactivate, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
