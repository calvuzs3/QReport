package net.calvuz.qreport.client.contact.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.client.contact.domain.model.ContactStatistics
import net.calvuz.qreport.app.app.presentation.components.ListStatItem
import net.calvuz.qreport.R

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
                label = stringResource(R.string.contact_title)
            )

            // Contatti primari
            ListStatItem(
                icon = Icons.Default.Star,
                value = statistics.primaryContacts.toString(),
                label = stringResource(R.string.contact_primary_title),
                color = if (statistics.primaryContacts > 0)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Contatti raggiungibili (con telefono o email)
            ListStatItem(
                icon = Icons.Default.Phone,
                value = (statistics.totalContacts - statistics.contactsWithoutContact).toString(),
                label = stringResource(R.string.contact_reachable_title),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}