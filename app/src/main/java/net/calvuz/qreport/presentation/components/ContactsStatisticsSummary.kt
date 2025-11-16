package net.calvuz.qreport.presentation.screen.client.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.domain.model.client.ContactStatistics

/**
 * Componente per mostrare statistiche riassuntive dei contatti
 * Design semplificato uguale alla scheda Facilities
 */
@Composable
fun ContactsStatisticsSummary(
    statistics: ContactStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Contatti totali
            StatItem(
                icon = Icons.Default.Person,
                value = statistics.totalContacts.toString(),
                label = "Contatti"
            )

            // Contatti primari
            StatItem(
                icon = Icons.Default.Star,
                value = statistics.primaryContacts.toString(),
                label = "Primari",
                color = if (statistics.primaryContacts > 0)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Contatti raggiungibili (con telefono o email)
            StatItem(
                icon = Icons.Default.Phone,
                value = (statistics.totalContacts - statistics.contactsWithoutContact).toString(),
                label = "Raggiungibili",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}