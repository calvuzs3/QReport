package net.calvuz.qreport.presentation.feature.client.facility.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.domain.model.client.FacilityWithIslands
import net.calvuz.qreport.presentation.core.components.ListStatItem
import kotlin.collections.sumOf

/**
 * Componente per mostrare statistiche riassuntive degli Stabilimenti
 * Design semplificato uguale alla scheda Facilities
 */
@Composable
fun FacilityStatisticsSummary(
    statistics: List<FacilityWithIslands>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val totalIslands = statistics.sumOf { it.islands.size }
        val activeIslands = statistics.sumOf { facility ->
            facility.islands.count { it.isActive }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ListStatItem(
                icon = Icons.Default.Business,
                value = statistics.size.toString(),
                label = "Stabilimenti"
            )
            ListStatItem(
                icon = Icons.Default.PrecisionManufacturing,
                value = totalIslands.toString(),
                label = "Isole Totali"
            )
            ListStatItem(
                icon = Icons.Default.CheckCircle,
                value = activeIslands.toString(),
                label = "Isole Attive",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}