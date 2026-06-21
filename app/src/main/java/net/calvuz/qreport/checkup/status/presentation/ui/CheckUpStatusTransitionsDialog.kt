package net.calvuz.qreport.checkup.status.presentation.ui

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
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster

/**
 * Checkbox multi-select for which statuses a checkup can move to from [fromStatus] —
 * same pattern as [net.calvuz.qreport.checkup.modules.presentation.ui.ModuleIslandAssociationDialog].
 */
@Composable
fun CheckUpStatusTransitionsDialog(
    fromStatus: CheckUpStatusMaster,
    statuses: List<CheckUpStatusMaster>,
    selectedToStatusIds: List<String>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (toStatusIds: List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(selectedToStatusIds.toSet()) }
    val candidates = statuses.filter { it.id != fromStatus.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.checkup_status_transitions_dialog_title,
                    fromStatus.label
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                candidates.forEach { toStatus ->
                    Row {
                        Checkbox(
                            checked = toStatus.id in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + toStatus.id else selected - toStatus.id
                            }
                        )
                        Text(
                            text = toStatus.label,
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
