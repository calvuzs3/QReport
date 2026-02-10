package net.calvuz.qreport.ti.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.app.app.presentation.components.list.*
import net.calvuz.qreport.app.util.DateTimeUtils.toItalianDate
import net.calvuz.qreport.settings.domain.model.ListViewMode
import net.calvuz.qreport.ti.domain.model.InterventionStatus
import net.calvuz.qreport.ti.domain.model.TechnicalIntervention
import net.calvuz.qreport.ti.domain.model.WorkLocationType
import net.calvuz.qreport.ti.presentation.ui.InterventionStatistics

/**
 * TechnicalIntervention implementation using GenericCardSystem
 *
 * Shows how to implement the generic card pattern for a specific domain entity
 */

/**
 * Data class that combines intervention with its statistics for card display
 */
data class TechnicalInterventionCardData(
    val intervention: TechnicalIntervention,
    val stats: InterventionStatistics
)

/**
 * Content provider implementation for TechnicalIntervention
 */
class TechnicalInterventionContentProvider : BaseCardContentProvider<TechnicalInterventionCardData>() {

    @Composable
    override fun HeaderSection(item: TechnicalInterventionCardData) {
        CardComponents.HeaderRow(
            title = item.intervention.interventionNumber,
        )
    }

    @Composable
    override fun MainSection(item: TechnicalInterventionCardData) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Customer information
            Text(
                text = item.intervention.customerData.customerName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (item.intervention.customerData.customerContact.isNotBlank()) {
                Text(
                    text = item.intervention.customerData.customerContact,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    override fun DetailsSection(item: TechnicalInterventionCardData) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Robot information row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CardComponents.InfoRow(
                    label = stringResource(R.string.robot_serial_number),
                    value = item.intervention.robotData.serialNumber,
                    modifier = Modifier.weight(1f)
                )

                CardComponents.InfoRow(
                    label = stringResource(R.string.operating_hours),
                    value = "${item.intervention.robotData.hoursOfDuty} h"
                )
            }

            // Work location
            Text(
                text = when (item.intervention.workLocation.type) {
                    WorkLocationType.CLIENT_SITE ->
                        stringResource(R.string.work_location_client_site)
                    WorkLocationType.OUR_SITE ->
                        stringResource(R.string.work_location_our_site)
                    WorkLocationType.OTHER ->
                        item.intervention.workLocation.customLocation.ifBlank {
                            stringResource(R.string.work_location_other)
                        }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status already shown in header, so show technicians count or other info
            if (item.intervention.technicians.isNotEmpty()) {
                Text(
                    text = "${item.intervention.technicians.size} technicians: ${item.intervention.technicians.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.width(0.dp))
            }

        }
    }

    @Composable
    override fun FooterSection(item: TechnicalInterventionCardData) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

                InterventionStatusChip(status = item.intervention.status)

            Text(
                text = stringResource(
                    R.string.updated_days_ago,
                    item.stats.daysSinceLastUpdate
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Override compact to remove details section
    @Composable
    override fun CompactContent(item: TechnicalInterventionCardData, isSelected: Boolean, modifier: Modifier) {
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
                Text(
                    text = item.intervention.robotData.serialNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.intervention.robotData.hoursOfDuty}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Override minimal to show just intervention number and customer
    @Composable
    override fun MinimalContent(item: TechnicalInterventionCardData, modifier: Modifier) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.intervention.interventionNumber,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = item.intervention.customerData.customerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = item.intervention.createdAt.toItalianDate(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Convenience composable for TechnicalIntervention cards
 */
@Composable
fun TechnicalInterventionCard(
    intervention: TechnicalIntervention,
    stats: InterventionStatistics,
    variant: ListViewMode = ListViewMode.FULL,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val data = TechnicalInterventionCardData(intervention, stats)
    val contentProvider = TechnicalInterventionContentProvider()

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
fun TechnicalIntervention.asCard(
    stats: InterventionStatistics,
    variant: ListViewMode = ListViewMode.FULL,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    TechnicalInterventionCard(
        intervention = this,
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
fun TechnicalInterventionCardBuilder(
    intervention: TechnicalIntervention,
    stats: InterventionStatistics,
    variant: ListViewMode = ListViewMode.FULL,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val data = TechnicalInterventionCardData(intervention, stats)

    data.cardBuilder()
        .variant(variant)
        .selected(isSelected)
        .loading(isLoading)
        .modifier(modifier)
        .build(TechnicalInterventionContentProvider())
}

/**
 * Status chip component (reused from original)
 */
@Composable
private fun InterventionStatusChip(
    status: InterventionStatus,
    modifier: Modifier = Modifier
) {
    val (text, containerColor, contentColor) = when (status) {
        InterventionStatus.DRAFT -> Triple(
            stringResource(R.string.status_draft),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )

        InterventionStatus.IN_PROGRESS -> Triple(
            stringResource(R.string.status_in_progress),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )

        InterventionStatus.PENDING_REVIEW -> Triple(
            stringResource(R.string.status_pending_review),
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary
        )

        InterventionStatus.COMPLETED -> Triple(
            stringResource(R.string.status_completed),
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )

        InterventionStatus.ARCHIVED -> Triple(
            stringResource(R.string.status_archived),
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.onSurface
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

///**
// * Usage examples in different scenarios
// */
//object TechnicalInterventionCardExamples {
//
//    @Composable
//    fun FullCardExample(intervention: TechnicalIntervention, stats: InterventionStatistics) {
//        // Standard usage
//        TechnicalInterventionCard(
//            intervention = intervention,
//            stats = stats,
//            variant = CardVariant.FULL
//        )
//    }
//
//    @Composable
//    fun CompactListExample(intervention: TechnicalIntervention, stats: InterventionStatistics) {
//        // Using extension function
//        intervention.asCard(
//            stats = stats,
//            variant = CardVariant.COMPACT,
//            modifier = Modifier.padding(horizontal = 8.dp)
//        )
//    }
//
//    @Composable
//    fun MinimalSearchResultExample(intervention: TechnicalIntervention, stats: InterventionStatistics) {
//        // Using builder pattern
//        TechnicalInterventionCardBuilder(
//            intervention = intervention,
//            stats = stats,
//            variant = CardVariant.MINIMAL,
//            modifier = Modifier.fillMaxWidth()
//        )
//    }
//
//    @Composable
//    fun SelectedCardExample(intervention: TechnicalIntervention, stats: InterventionStatistics) {
//        // Card with selection state
//        intervention.asCard(
//            stats = stats,
//            variant = CardVariant.FULL,
//            isSelected = true
//        )
//    }
//}