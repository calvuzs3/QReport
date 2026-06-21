package net.calvuz.qreport.checkup.items.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.checkup.items.domain.model.CheckItemStatus
import net.calvuz.qreport.checkup.items.presentation.model.CheckItemStatusExt.getColor
import net.calvuz.qreport.checkup.items.presentation.model.CheckItemStatusExt.getDisplayName
import net.calvuz.qreport.checkup.items.presentation.model.CheckItemStatusExt.getIconVector


@Composable
fun CheckupItemStatusChip(
    modifier: Modifier = Modifier,
    status: CheckItemStatus,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val colors = AssistChipDefaults.assistChipColors(
        containerColor = status.getColor(),
        labelColor = Color.White,
        leadingIconContentColor = Color.White
    )

    AssistChip(
        onClick = onClick ?: {},
        label = { Text(status.getDisplayName(context)) },
        leadingIcon = {
            Icon(
                imageVector = status.getIconVector(),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = colors,
        modifier = modifier
    )
}