package net.calvuz.qreport.client.island.presentation.ui

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
import net.calvuz.qreport.client.island.domain.model.IslandTypeMaster

/**
 * Create/edit dialog for an island type. A dialog is enough here — the fields
 * are few and flat, no need for a multi-step screen like the Island/Client forms.
 */
@Composable
fun IslandTypeFormDialog(
    editingType: IslandTypeMaster?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (
        code: String,
        label: String,
        description: String?,
        iconName: String?,
        maintenanceIntervalDays: Int,
        sortOrder: Int
    ) -> Unit
) {
    var code by remember { mutableStateOf(editingType?.code ?: "") }
    var label by remember { mutableStateOf(editingType?.label ?: "") }
    var description by remember { mutableStateOf(editingType?.description ?: "") }
    var iconName by remember { mutableStateOf(editingType?.iconName ?: "") }
    var maintenanceIntervalDays by remember {
        mutableStateOf((editingType?.maintenanceIntervalDays ?: 180).toString())
    }
    var sortOrder by remember { mutableStateOf((editingType?.sortOrder ?: 0).toString()) }

    val isValid = code.isNotBlank() && label.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editingType != null) R.string.island_type_dialog_title_edit
                    else R.string.island_type_dialog_title_create
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.island_type_field_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.island_type_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.island_type_field_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = iconName,
                    onValueChange = { iconName = it },
                    label = { Text(stringResource(R.string.island_type_field_icon_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maintenanceIntervalDays,
                    onValueChange = { maintenanceIntervalDays = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.island_type_field_maintenance_interval)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sortOrder,
                    onValueChange = { sortOrder = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.island_type_field_sort_order)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = stringResource(R.string.island_type_dialog_error),
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
                        iconName.trim().ifBlank { null },
                        maintenanceIntervalDays.toIntOrNull() ?: 180,
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
