package net.calvuz.qreport.checkup.modules.presentation.ui

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
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster

/**
 * Manages which modules apply to which island type — drives which
 * `check_item_templates` get pulled into a new checkup's checklist
 * (see [net.calvuz.qreport.checkup.checkup.domain.usecase.CreateCheckUpUseCase]).
 * Mirror of [net.calvuz.qreport.client.island.presentation.ui.IslandTypesManagementScreen]
 * but editing an association instead of CRUD-ing a master-data row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleIslandAssociationScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModuleIslandAssociationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_island_association_screen_title)) },
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
            items(uiState.islandTypes, key = { it.id }) { islandType ->
                val linkedModuleLabels = uiState.linksByIslandType[islandType.id].orEmpty()
                    .mapNotNull { moduleId -> uiState.moduleTypes.find { it.id == moduleId }?.label }

                IslandTypeModulesRow(
                    islandType = islandType,
                    linkedModuleLabels = linkedModuleLabels,
                    onEdit = { viewModel.onEditClick(islandType) }
                )
            }
        }
    }

    uiState.editingIslandType?.let { islandType ->
        ModuleIslandAssociationDialog(
            islandType = islandType,
            moduleTypes = uiState.moduleTypes,
            selectedModuleTypeIds = uiState.linksByIslandType[islandType.id].orEmpty(),
            errorMessage = uiState.errorMessage,
            onDismiss = viewModel::onDismissDialog,
            onSave = viewModel::onSave
        )
    }
}

@Composable
private fun IslandTypeModulesRow(
    islandType: IslandTypeMaster,
    linkedModuleLabels: List<String>,
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
                Text(text = islandType.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (linkedModuleLabels.isEmpty()) {
                        stringResource(R.string.module_island_association_no_modules)
                    } else {
                        linkedModuleLabels.joinToString(", ")
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
