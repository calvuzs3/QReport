package net.calvuz.qreport.client.contract.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.list.*
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianLastModified
import net.calvuz.qreport.client.contract.data.local.isValid
import net.calvuz.qreport.client.contract.domain.model.Contract
import net.calvuz.qreport.client.contract.presentation.ui.ContractsStatistics

/**
 * TechnicalIntervention implementation using GenericCardSystem
 *
 * Shows how to implement the generic card pattern for a specific domain entity
 */

/**
 * Data class that combines intervention with its statistics for card display
 */
data class ContractCardData(
    val contract: Contract,
    val stats: ContractsStatistics
)

/**
 * Content provider implementation for TechnicalIntervention
 */
class ContractContentProvider : BaseCardContentProvider<ContractCardData>() {

    @Composable
    override fun HeaderSection(item: ContractCardData) {
        CardComponents.HeaderRow(
            title = item.contract.name ?: "GENERICO" ,

            )
    }

    @Composable
    override fun MainSection(item: ContractCardData) {
        Text(
            text = "${item.contract.startDate.toItalianDate()} - ${item.contract.endDate.toItalianDate()}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = item.contract.description ?: "",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    @Composable
    override fun DetailsSection(item: ContractCardData) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // MAINTENANCE
            if (item.contract.hasMaintenance) {
                ContractItem(
                    icon = Icons.Default.Build,
                    label = stringResource(R.string.label_maintenance),
                    value = true.toString(),
                    onClick = { },
                    isValid = true
                )
            }

            // ASSISTANCE
            if (item.contract.hasRemoteAssistance) {
                ContractItem(
                    icon = Icons.Default.Assistant,
                    label = stringResource(R.string.label_assistance),
                    value = true.toString(),
                    onClick = {},
                    isValid = true
                )
            }

            // MOBILE
            if (item.contract.hasPriority) {
                ContractItem(
                    icon = Icons.Default.PriorityHigh,
                    label = stringResource(R.string.label_priority),
                    value = true.toString(),
                    onClick = {},
                    isValid = true
                )
            }
        }
    }

    @Composable
    override fun FooterSection(item: ContractCardData) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            ContractStatusChip(valid = item.contract.isValid(), active = item.contract.isActive)

            Text(
                text =
                    item.contract.updatedAt.toItalianLastModified()
                ,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Override compact to remove details section
    @Composable
    override fun CompactContent(item: ContractCardData, isSelected: Boolean, modifier: Modifier) {
        Column(modifier = modifier) {
            HeaderSection(item)
            Spacer(modifier = Modifier.height(8.dp))
            MainSection(item)
            Spacer(modifier = Modifier.height(8.dp))

            // Compact footer with just essential info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ROLE
                if (item.contract.hasMaintenance) {
                    Row {
                        Text(
                            text = "${stringResource(R.string.label_maintenance)} ✔️",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Override minimal to show just intervention number and customer
    @Composable
    override fun MinimalContent(item: ContractCardData, modifier: Modifier) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.contract.startDate.toItalianDate()} - ${item.contract.endDate.toItalianDate()}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.contract.hasMaintenance) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.label_maintenance),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Convenience composable for TechnicalIntervention cards
 */
@Composable
fun ContractCard(
    contract: Contract,
    stats: ContractsStatistics,
    variant: CardVariant = CardVariant.FULL,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val data = ContractCardData(contract, stats)
    val contentProvider = ContractContentProvider()

    GenericCard(
        item = data,
        variant = variant,
        contentProvider = contentProvider,
        isSelected = isSelected,
        isLoading = isLoading,
        modifier = modifier
    )
}

/**
 * Extension function for even easier usage
 */
@Composable
fun Contract.asCard(
    stats: ContractsStatistics,
    variant: CardVariant = CardVariant.FULL,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    ContractCard(
        contract = this,
        stats = stats,
        variant = variant,
        isSelected = isSelected,
        isLoading = isLoading,
        modifier = modifier
    )
}

/**
 * Builder pattern usage example
 */
@Composable
fun ContractCardBuilder(
    contract: Contract,
    stats: ContractsStatistics,
    variant: CardVariant = CardVariant.FULL,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val data = ContractCardData(contract, stats)

    data.cardBuilder()
        .variant(variant)
        .selected(isSelected)
        .loading(isLoading)
        .modifier(modifier)
        .build(ContractContentProvider())
}

/**
 * Status chip component (reused from original)
 */
@Composable
private fun ContractStatusChip(
    active: Boolean,
    valid: Boolean,
    modifier: Modifier = Modifier
) {
    val (text, containerColor, contentColor) = when (active) {
        true -> {
            when (valid) {

                true -> Triple(
                    stringResource(R.string.contracts_status_active),
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary
                )

                false -> Triple(
                    stringResource(R.string.contracts_status_active),
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            }
        }

        else -> Triple(
            stringResource(R.string.contracts_status_expired),
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )

    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ContractItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    isValid: Boolean
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

        }
    }
}