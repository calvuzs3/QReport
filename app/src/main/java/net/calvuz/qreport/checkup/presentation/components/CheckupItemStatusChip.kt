package net.calvuz.qreport.checkup.presentation.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import net.calvuz.qreport.checkup.domain.model.CheckItemStatus
import net.calvuz.qreport.checkup.presentation.model.CheckItemStatusExt.getColor
import net.calvuz.qreport.checkup.presentation.model.CheckItemStatusExt.getDisplayName


@Composable
fun CheckupItemStatusChip(
    modifier: Modifier = Modifier,
    status: CheckItemStatus,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val colors = AssistChipDefaults.assistChipColors(
        containerColor = status.getColor(),
        labelColor = Color.White
    )

    if (onClick != null) {
        AssistChip(
            onClick = onClick,
            label = { Text(status.getDisplayName(context)) },
            colors = colors,
            modifier = modifier
        )
    } else {
        AssistChip(
            onClick = { },
            label = { Text(status.getDisplayName(context)) },
            colors = colors,
            modifier = modifier
        )
    }
}