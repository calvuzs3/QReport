package net.calvuz.qreport.checkup.modules.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import net.calvuz.qreport.checkup.modules.domain.model.ModuleTypeMaster

/**
 * Create/edit dialog for a checklist module type — mirror of
 * [net.calvuz.qreport.client.island.presentation.ui.IslandTypeFormDialog].
 */
@Composable
fun ModuleTypeFormDialog(
    editingType: ModuleTypeMaster?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (
        code: String,
        label: String,
        description: String?,
        sortOrder: Int
    ) -> Unit
) {
    var code by remember { mutableStateOf(editingType?.code ?: "") }
    var label by remember { mutableStateOf(editingType?.label ?: "") }
    var description by remember { mutableStateOf(editingType?.description ?: "") }
    var sortOrder by remember { mutableStateOf((editingType?.sortOrder ?: 0).toString()) }

    val isValid = code.isNotBlank() && label.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editingType != null) R.string.module_type_dialog_title_edit
                    else R.string.module_type_dialog_title_create
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.module_type_field_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.module_type_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.module_type_field_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sortOrder,
                    onValueChange = { sortOrder = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.module_type_field_sort_order)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = stringResource(R.string.module_type_dialog_error),
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
                        code.trim(),
                        label.trim(),
                        description.trim().ifBlank { null },
                        sortOrder.toIntOrNull() ?: 0
                    )
                }
            ) {
                Text(stringResource(if (editingType != null) R.string.action_update else R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
