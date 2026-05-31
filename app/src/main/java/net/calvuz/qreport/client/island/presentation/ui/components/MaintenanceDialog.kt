package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.client.island.domain.model.Island

@Composable
fun MaintenanceDialog(
    island: Island?,
    onDismiss: () -> Unit,
    onConfirm: (resetHours: Boolean, notes: String) -> Unit
) {
    var resetHours by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.maintenance_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.maintenance_dialog_message, island?.serialNumber ?: ""))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = resetHours, onCheckedChange = { resetHours = it })
                    Text(stringResource(R.string.maintenance_dialog_reset_hours))
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.maintenance_dialog_notes_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(resetHours, notes) }) {
                Text(stringResource(R.string.maintenance_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}