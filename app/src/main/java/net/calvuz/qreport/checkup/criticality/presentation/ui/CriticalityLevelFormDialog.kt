package net.calvuz.qreport.checkup.criticality.presentation.ui

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
import net.calvuz.qreport.checkup.criticality.domain.model.CriticalityMaster

/**
 * Create/edit dialog for a checklist criticality level — mirror of
 * [net.calvuz.qreport.client.island.presentation.ui.IslandTypeFormDialog].
 */
@Composable
fun CriticalityLevelFormDialog(
    editingLevel: CriticalityMaster?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (
        code: String,
        label: String,
        priority: Int,
        colorHex: String,
        iconEmoji: String?,
        sortOrder: Int
    ) -> Unit
) {
    var code by remember { mutableStateOf(editingLevel?.code ?: "") }
    var label by remember { mutableStateOf(editingLevel?.label ?: "") }
    var priority by remember { mutableStateOf((editingLevel?.priority ?: 0).toString()) }
    var colorHex by remember { mutableStateOf(editingLevel?.colorHex ?: "#9E9E9E") }
    var iconEmoji by remember { mutableStateOf(editingLevel?.iconEmoji ?: "") }
    var sortOrder by remember { mutableStateOf((editingLevel?.sortOrder ?: 0).toString()) }

    val isValid = code.isNotBlank() && label.isNotBlank() && colorHex.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editingLevel != null) R.string.criticality_level_dialog_title_edit
                    else R.string.criticality_level_dialog_title_create
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.criticality_level_field_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.criticality_level_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.criticality_level_field_priority)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text(stringResource(R.string.criticality_level_field_color_hex)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = iconEmoji,
                    onValueChange = { iconEmoji = it },
                    label = { Text(stringResource(R.string.criticality_level_field_icon_emoji)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sortOrder,
                    onValueChange = { sortOrder = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.criticality_level_field_sort_order)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = stringResource(R.string.criticality_level_dialog_error),
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
                        priority.toIntOrNull() ?: 0,
                        colorHex.trim(),
                        iconEmoji.trim().ifBlank { null },
                        sortOrder.toIntOrNull() ?: 0
                    )
                }
            ) {
                Text(stringResource(if (editingLevel != null) R.string.action_update else R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
