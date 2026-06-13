package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.datetime.Instant
import net.calvuz.qreport.R
import net.calvuz.qreport.app.error.presentation.UiText
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianLastModified

data class QrCardFooterData(
    val date: Instant,
    val isActive: Boolean,
    val onRestore: (() -> Unit)? = {},
    val activeString: UiText = UiText.StringResource(R.string.label_active),
    val inactiveString: UiText = UiText.StringResource(R.string.label_not_active),
)

@Suppress("ParamsComparedByRef")@Composable
fun QrCardFooter(data: QrCardFooterData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        QrStatusChip(
            isActive = data.isActive,
            onRestore = data.onRestore,
            activeString = data.activeString,
            inactiveString = data.inactiveString
        )

        Text(
            text = data.date.toItalianLastModified().asString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}