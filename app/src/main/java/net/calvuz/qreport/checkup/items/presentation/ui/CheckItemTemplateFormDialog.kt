@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.checkup.items.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster
import net.calvuz.qreport.checkup.items.domain.model.CheckItemTemplateMaster
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster

/**
 * Create/edit dialog for a checklist template — like
 * [net.calvuz.qreport.client.island.presentation.ui.IslandTypeFormDialog] but with
 * dropdowns for module/criticality (sourced from their own master data). Which
 * island types a template applies to is now decided at the module level — see
 * [net.calvuz.qreport.checkup.modules.presentation.ui.ModuleIslandAssociationScreen].
 */
@Composable
fun CheckItemTemplateFormDialog(
    editingTemplate: CheckItemTemplateMaster?,
    moduleTypes: List<ModuleTypeMaster>,
    criticalityLevels: List<CriticalityMaster>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (
        category: String,
        description: String,
        moduleTypeId: String,
        criticalityId: String,
        orderIndex: Int
    ) -> Unit
) {
    var category by remember { mutableStateOf(editingTemplate?.category ?: "") }
    var description by remember { mutableStateOf(editingTemplate?.description ?: "") }
    var orderIndex by remember { mutableStateOf((editingTemplate?.orderIndex ?: 0).toString()) }
    var selectedModuleTypeId by remember {
        mutableStateOf(editingTemplate?.moduleTypeId ?: moduleTypes.firstOrNull()?.id)
    }
    var selectedCriticalityId by remember {
        mutableStateOf(editingTemplate?.criticalityId ?: criticalityLevels.firstOrNull()?.id)
    }

    var moduleMenuExpanded by remember { mutableStateOf(false) }
    var criticalityMenuExpanded by remember { mutableStateOf(false) }

    val isValid = category.isNotBlank() && description.isNotBlank() &&
        selectedModuleTypeId != null && selectedCriticalityId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editingTemplate != null) R.string.check_item_template_dialog_title_edit
                    else R.string.check_item_template_dialog_title_create
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.check_item_template_field_category)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.check_item_template_field_description)) },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = moduleMenuExpanded,
                    onExpandedChange = { moduleMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = moduleTypes.find { it.id == selectedModuleTypeId }?.label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.check_item_template_field_module_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = moduleMenuExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = moduleMenuExpanded,
                        onDismissRequest = { moduleMenuExpanded = false }
                    ) {
                        moduleTypes.forEach { module ->
                            DropdownMenuItem(
                                text = { Text(module.label) },
                                onClick = {
                                    selectedModuleTypeId = module.id
                                    moduleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = criticalityMenuExpanded,
                    onExpandedChange = { criticalityMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = criticalityLevels.find { it.id == selectedCriticalityId }?.label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.check_item_template_field_criticality)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = criticalityMenuExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = criticalityMenuExpanded,
                        onDismissRequest = { criticalityMenuExpanded = false }
                    ) {
                        criticalityLevels.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.label) },
                                onClick = {
                                    selectedCriticalityId = level.id
                                    criticalityMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = orderIndex,
                    onValueChange = { orderIndex = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.check_item_template_field_order_index)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = stringResource(R.string.check_item_template_dialog_error),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onSave(
                        category.trim(),
                        description.trim(),
                        selectedModuleTypeId!!,
                        selectedCriticalityId!!,
                        orderIndex.toIntOrNull() ?: 0
                    )
                }
            ) {
                Text(stringResource(if (editingTemplate != null) R.string.action_update else R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
