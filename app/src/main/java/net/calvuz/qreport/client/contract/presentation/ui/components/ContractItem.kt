package net.calvuz.qreport.client.contract.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.client.contract.data.local.isValid
import net.calvuz.qreport.client.contract.domain.model.Contract


@Composable
fun ContractListItem(
    contract: Contract,
    onContractClick: (String) -> Unit = {},
    onEditContract: (String) -> Unit = {},
) {
    Card(
        onClick = { onContractClick(contract.id) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (contract.isValid()) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = stringResource(R.string.label_valid),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "${contract.startDate.toItalianDate()} - ${contract.endDate.toItalianDate()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                contract.name?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                contract.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (contract.hasMaintenance) {
                        Text(
                            text = "${stringResource(R.string.contracts_screen_form_field_maintenance)} ✔",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (contract.hasPriority) {
                        Text(
                            text = "${stringResource(R.string.contracts_screen_form_field_priority)} ✔",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (contract.hasRemoteAssistance) {
                        Text(
                            text = "${stringResource(R.string.contracts_screen_form_field_remote_assistance)} ✔",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onEditContract(contract.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
