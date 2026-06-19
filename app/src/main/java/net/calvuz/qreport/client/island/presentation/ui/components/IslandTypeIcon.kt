package net.calvuz.qreport.client.island.presentation.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.client.island.presentation.model.IslandTypeIconRegistry

@Composable
fun IslandTypeIcon(code: String, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Icon(
            imageVector = IslandTypeIconRegistry.iconFor(code),
            contentDescription = label,
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
