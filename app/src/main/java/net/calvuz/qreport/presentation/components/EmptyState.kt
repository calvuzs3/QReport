package net.calvuz.qreport.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    iconImageVector: ImageVector,
    iconContentDescription: String? = null,
    searchQuery: String? = null,
    textFilter: String? = null,
    iconActionImageVector: ImageVector? = null,
    iconActionContentDescription: String? = null,
    textAction: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val (title, message) = when {
            searchQuery != null && searchQuery.isNotEmpty() -> "Nessun risultato" to "Non ci sono elementi che corrispondono alla ricerca '$searchQuery'"
            textFilter != null -> "Nessun risultato" to "Non ci sono elementi con filtro '$textFilter'"
            else -> "Nessun risultato" to "Non hai ancora aggiunto nessuno elemento"
        }

        Icon(
            imageVector = iconImageVector,
            contentDescription = iconContentDescription,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.align(alignment = Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (iconActionImageVector != null && iconContentDescription!=null && onAction != null && textAction != null) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onAction) {
                Icon(
                    imageVector = iconActionImageVector,
                    contentDescription = iconActionContentDescription,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(textAction)
            }
        }
    }
}