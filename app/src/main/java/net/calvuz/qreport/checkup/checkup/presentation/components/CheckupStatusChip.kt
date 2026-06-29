package net.calvuz.qreport.checkup.checkup.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.calvuz.qreport.app.util.ColorUtils.toComposeColor
import net.calvuz.qreport.checkup.status.domain.model.CheckUpStatusMaster

/**
 * Full chip: emoji + label + background color. For FULL cards and detail screens.
 *
 * @param statusMaster The resolved master row, looked up by the caller.
 * Null if the status was deactivated/removed after the checkup was created.
 */
@Suppress("ParamsComparedByRef")
@Composable
fun CheckupStatusChip(
    statusMaster: CheckUpStatusMaster?,
    modifier: Modifier = Modifier
) {
    val label = buildString {
        statusMaster?.iconEmoji?.let { append("$it ") }
        append(statusMaster?.label ?: "?")
    }
    val containerColor = statusMaster?.colorHex?.toComposeColor()
        ?: MaterialTheme.colorScheme.surfaceVariant
    // Black or white text depending on background luminance (arbitrary hex colors need this)
    val labelColor = if (containerColor.luminance() > 0.3f) Color(0xDD000000) else Color(0xDDFFFFFF)

    AssistChip(
        onClick = { },
        label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
        ),
        modifier = modifier
    )
}

/**
 * Compact dot: colored circle with the status emoji inside. For COMPACT/MINIMAL cards.
 * Analogous to [net.calvuz.qreport.app.app.presentation.components.QrStatusIndicator]
 * but multi-state instead of binary active/inactive.
 */
@Suppress("ParamsComparedByRef")
@Composable
fun CheckupStatusDot(
    statusMaster: CheckUpStatusMaster?,
    modifier: Modifier = Modifier
) {
    val bgColor = statusMaster?.colorHex?.toComposeColor()
        ?: MaterialTheme.colorScheme.surfaceVariant
    val emoji = statusMaster?.iconEmoji

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) {
            Text(text = emoji, fontSize = 14.sp, lineHeight = 16.sp)
        }
    }
}