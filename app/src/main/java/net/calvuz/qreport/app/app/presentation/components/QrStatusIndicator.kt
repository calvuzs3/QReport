package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.ui.theme.onSuccessContainer
import net.calvuz.qreport.app.app.presentation.ui.theme.successContainer

@Composable
fun QrStatusIndicator(
    modifier: Modifier = Modifier, isActive: Boolean, onRestore: (() -> Unit)? = null
) {
    val (containerColor, contentColor) = if (isActive) {
        MaterialTheme.colorScheme.successContainer to MaterialTheme.colorScheme.onSuccessContainer
    } else {
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    IconButton(
        modifier = modifier.size(16.dp),
        onClick = { if (!isActive) onRestore?.invoke() },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor, contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (isActive) stringResource(R.string.label_active)
            else stringResource(R.string.label_not_active),
        )
    }
}