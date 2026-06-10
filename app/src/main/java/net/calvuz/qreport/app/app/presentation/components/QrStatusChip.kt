package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.calvuz.qreport.app.app.presentation.ui.theme.onSuccessContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.successContainer
import net.calvuz.qreport.app.error.presentation.UiText

@Composable
fun QrStatusChip(
    modifier: Modifier = Modifier, isActive: Boolean, onRestore: (() -> Unit)? = null,
    activeString: UiText,
    inactiveString: UiText
) {
    val (text, containerColor, labelColor) = if (isActive) {
        Triple(
            activeString,
            MaterialTheme.colorScheme.successContainer,
            MaterialTheme.colorScheme.onSuccessContainer
        )
    } else {
        Triple(
            inactiveString,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    AssistChip(
        onClick = { if (!isActive) onRestore?.invoke() },
        label = { Text(text.asString(), style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor, labelColor = labelColor
        ),
        modifier = modifier
    )
}