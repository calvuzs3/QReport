package net.calvuz.qreport.presentation.feature.checkup.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.presentation.feature.checkup.model.CheckUpStatusExt.getDisplayName


@Composable
fun CheckupStatusChip(
    status: CheckUpStatus,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (text, containerColor) = when (status) {
        CheckUpStatus.DRAFT -> status.getDisplayName(context) to MaterialTheme.colorScheme.surfaceVariant
        CheckUpStatus.IN_PROGRESS -> status.getDisplayName(context) to MaterialTheme.colorScheme.primaryContainer
        CheckUpStatus.COMPLETED -> status.getDisplayName(context) to MaterialTheme.colorScheme.tertiaryContainer
        CheckUpStatus.EXPORTED -> status.getDisplayName(context) to MaterialTheme.colorScheme.secondaryContainer
        CheckUpStatus.ARCHIVED -> status.getDisplayName(context) to MaterialTheme.colorScheme.outline
    }

    AssistChip(
        onClick = { },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor
        ),
        modifier = modifier
    )
}