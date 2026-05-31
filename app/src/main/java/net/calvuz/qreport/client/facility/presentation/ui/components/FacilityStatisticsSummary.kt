package net.calvuz.qreport.client.facility.presentation.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.ListStatItem
import net.calvuz.qreport.client.facility.domain.model.FacilityWithIslands

/**
 * Summary statistics card for a list of [FacilityWithIslands].
 * Shown above the facility list in the client detail screen.
 */
@Composable
fun FacilityStatisticsSummary(
    statistics: List<FacilityWithIslands>,
    modifier: Modifier = Modifier
) {
    val totalIslands = statistics.sumOf { it.islands.size }
    val activeIslands = statistics.sumOf { it.islands.count { island -> island.isActive } }

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
            ListStatItem(
                icon = Icons.Default.Business,
                value = statistics.size.toString(),
                label = stringResource(R.string.facility_stat_facilities)
            )
            ListStatItem(
                icon = Icons.Default.PrecisionManufacturing,
                value = totalIslands.toString(),
                label = stringResource(R.string.facility_stat_islands_total)
            )
            ListStatItem(
                icon = Icons.Default.CheckCircle,
                value = activeIslands.toString(),
                label = stringResource(R.string.facility_stat_islands_active),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}