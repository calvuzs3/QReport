package net.calvuz.qreport.client.contract.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.client.contract.domain.model.ContractStatistics
import net.calvuz.qreport.app.app.presentation.components.QrListStatItem

@Composable
fun ContractsStatisticsSummary(
    statistics: ContractStatistics,
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
            // ContractsError
            QrListStatItem(
                icon = Icons.Default.AssignmentTurnedIn,
                value = statistics.totalContracts.toString(),
                label = stringResource(R.string.contracts_list_title)
            )

            // Active
            QrListStatItem(
                icon = Icons.Default.Star,
                value = statistics.validContracts.toString(),
                label = stringResource(R.string.label_actives),
                color = if (statistics.validContracts > 0)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Inactive
            QrListStatItem(
                icon = Icons.Outlined.Star,
                value = statistics.outdatedContracts.toString(),
                label = stringResource(R.string.label_not_actives),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}