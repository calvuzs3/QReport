package net.calvuz.qreport.app.app.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun StatusIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isActive) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Icon(
        imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
        contentDescription = if (isActive) "Attivo" else "Inattivo",
        tint = color,
        modifier = modifier.size(16.dp)
    )
}