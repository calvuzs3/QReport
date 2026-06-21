package net.calvuz.qreport.checkup.items.presentation.ui

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
import net.calvuz.qreport.checkup.items.domain.model.CheckItemTemplateMaster

/**
 * Mirror of [net.calvuz.qreport.client.island.presentation.ui.IslandTypesManagementScreen],
 * sorted by [CheckItemTemplateMaster.orderIndex] like the checklist itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckItemTemplatesManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: CheckItemTemplatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.check_item_template_screen_title)) },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.templates.sortedBy { it.orderIndex }, key = { it.id }) { template ->
                CheckItemTemplateRow(
                    template = template,
                    moduleLabel = uiState.moduleTypes.find { it.id == template.moduleTypeId }?.label ?: template.moduleTypeId,
                    criticalityLabel = uiState.criticalityLevels.find { it.id == template.criticalityId }?.label ?: template.criticalityId,
                    onEdit = { viewModel.onEditClick(template) },
                    onDeactivate = { viewModel.onDeactivate(template.id) },
                    onRestore = { viewModel.onRestore(template.id) }
                )
            }
        }
    }

    if (uiState.isCreatingNew || uiState.editingTemplate != null) {
        CheckItemTemplateFormDialog(
            editingTemplate = uiState.editingTemplate,
            moduleTypes = uiState.moduleTypes,
            criticalityLevels = uiState.criticalityLevels,
            errorMessage = uiState.errorMessage,
            onDismiss = viewModel::onDismissDialog,
            onSave = viewModel::onSave
        )
    }
}

@Composable
private fun CheckItemTemplateRow(
    template: CheckItemTemplateMaster,
    moduleLabel: String,
    criticalityLabel: String,
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
            QrStatusIndicator(isActive = template.isActive, onRestore = onRestore)

            Column(modifier = Modifier.weight(1f)) {
                Text(text = template.description, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${template.category} · $moduleLabel · $criticalityLabel",
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
                if (template.isActive) {
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
