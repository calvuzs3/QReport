package net.calvuz.qreport.checkup.status.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster

/**
 * Create/edit dialog for a checkup status — mirror of
 * [net.calvuz.qreport.checkup.criticality.presentation.ui.CriticalityLevelFormDialog],
 * with [blocksDeletion]/[marksCompletion] switches replacing the business rules that
 * used to be hardcoded `when` branches on the old `CheckUpStatus` enum.
 */
@Composable
fun CheckUpStatusFormDialog(
    editingStatus: CheckUpStatusMaster?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (
        code: String,
        label: String,
        colorHex: String,
        iconEmoji: String?,
        sortOrder: Int,
        blocksDeletion: Boolean,
        marksCompletion: Boolean
    ) -> Unit
) {
    var code by remember { mutableStateOf(editingStatus?.code ?: "") }
    var label by remember { mutableStateOf(editingStatus?.label ?: "") }
    var colorHex by remember { mutableStateOf(editingStatus?.colorHex ?: "#9E9E9E") }
    var iconEmoji by remember { mutableStateOf(editingStatus?.iconEmoji ?: "") }
    var sortOrder by remember { mutableStateOf((editingStatus?.sortOrder ?: 0).toString()) }
    var blocksDeletion by remember { mutableStateOf(editingStatus?.blocksDeletion ?: false) }
    var marksCompletion by remember { mutableStateOf(editingStatus?.marksCompletion ?: false) }

    val isValid = code.isNotBlank() && label.isNotBlank() && colorHex.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editingStatus != null) R.string.checkup_status_dialog_title_edit
                    else R.string.checkup_status_dialog_title_create
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.checkup_status_field_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.checkup_status_field_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text(stringResource(R.string.checkup_status_field_color_hex)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = iconEmoji,
                    onValueChange = { iconEmoji = it },
                    label = { Text(stringResource(R.string.checkup_status_field_icon_emoji)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sortOrder,
                    onValueChange = { sortOrder = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.checkup_status_field_sort_order)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.checkup_status_field_blocks_deletion),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = blocksDeletion, onCheckedChange = { blocksDeletion = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.checkup_status_field_marks_completion),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = marksCompletion, onCheckedChange = { marksCompletion = it })
                }
                if (errorMessage != null) {
                    Text(
                        text = stringResource(R.string.checkup_status_dialog_error),
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
                        colorHex.trim(),
                        iconEmoji.trim().ifBlank { null },
                        sortOrder.toIntOrNull() ?: 0,
                        blocksDeletion,
                        marksCompletion
                    )
                }
            ) {
                Text(stringResource(if (editingStatus != null) R.string.action_update else R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
