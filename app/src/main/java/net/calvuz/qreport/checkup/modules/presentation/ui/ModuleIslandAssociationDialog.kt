package net.calvuz.qreport.checkup.modules.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster

/**
 * Checkbox multi-select for which modules apply to one island type — same
 * checkbox-list pattern previously used (per-template) in
 * [net.calvuz.qreport.checkup.items.presentation.ui.CheckItemTemplateFormDialog],
 * now applied at the module level instead.
 */
@Composable
fun ModuleIslandAssociationDialog(
    islandType: IslandTypeMaster,
    moduleTypes: List<ModuleTypeMaster>,
    selectedModuleTypeIds: List<String>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (moduleTypeIds: List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(selectedModuleTypeIds.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.module_island_association_dialog_title,
                    islandType.label
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moduleTypes.forEach { module ->
                    Row {
                        Checkbox(
                            checked = module.id in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + module.id else selected - module.id
                            }
                        )
                        Text(
                            text = module.label,
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected.toList()) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
