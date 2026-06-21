package net.calvuz.qreport.checkup.checkup.presentation.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.calvuz.qreport.app.util.ColorUtils.toComposeColor
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster

/**
 * @param statusMaster The resolved master row for the checkup's status code,
 * looked up by the caller (e.g. `statusMasters.find { it.id == checkup.status }`).
 * Null if the status was deactivated/removed after the checkup was created.
 */
@Composable
fun CheckupStatusChip(
    statusMaster: CheckUpStatusMaster?,
    modifier: Modifier = Modifier
) {
    val text = statusMaster?.label ?: "?"
    val containerColor = statusMaster?.colorHex?.toComposeColor() ?: MaterialTheme.colorScheme.surfaceVariant

    AssistChip(
        onClick = { },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor
        ),
        modifier = modifier
    )
}