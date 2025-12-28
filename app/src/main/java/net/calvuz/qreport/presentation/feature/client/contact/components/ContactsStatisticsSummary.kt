package net.calvuz.qreport.presentation.feature.client.contact.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.domain.model.client.ContactStatistics
import net.calvuz.qreport.presentation.core.components.ListStatItem

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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Contatti totali
            ListStatItem(
                icon = Icons.Default.Person,
                value = statistics.totalContacts.toString(),
                label = "Contatti"
            )

            // Contatti primari
            ListStatItem(
                icon = Icons.Default.Star,
                value = statistics.primaryContacts.toString(),
                label = "Primari",
                color = if (statistics.primaryContacts > 0)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Contatti raggiungibili (con telefono o email)
            ListStatItem(
                icon = Icons.Default.Phone,
                value = (statistics.totalContacts - statistics.contactsWithoutContact).toString(),
                label = "Raggiungibili",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}