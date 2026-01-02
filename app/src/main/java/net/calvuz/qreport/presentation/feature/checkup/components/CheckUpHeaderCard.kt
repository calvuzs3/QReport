package net.calvuz.qreport.presentation.feature.checkup.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.R
import net.calvuz.qreport.domain.model.checkup.CheckItemStatus
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpIslandAssociation
import net.calvuz.qreport.domain.model.checkup.CheckUpProgress
import net.calvuz.qreport.presentation.feature.checkup.model.AssociationTypeExt.asString
import net.calvuz.qreport.presentation.feature.checkup.model.AssociationTypeExt.getDescription

/**
 * Header card with CheckUp info and Edit Button
 */
@Composable
fun CheckUpHeaderCard(
    modifier: Modifier = Modifier,
    checkUp: CheckUp,
    progress: CheckUpProgress,
    associations: List<CheckUpIslandAssociation> = emptyList(),
    onEditHeader: () -> Unit = {},
    onManageAssociation: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = checkUp.header.clientInfo.companyName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = checkUp.islandType.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.checkup_component_header_serial_number,
                            checkUp.header.islandInfo.serialNumber
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(horizontalArrangement = Arrangement.End) {
                        CheckupStatusChip(status = checkUp.status)
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onManageAssociation,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (associations.isNotEmpty())
                                    Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = stringResource(
                                    R.string.checkup_component_header_action_manage_association
                                ),
                                tint = if (associations.isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onEditHeader,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(
                                    R.string.checkup_component_header_action_edit
                                ),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Island details
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                if (checkUp.header.islandInfo.model.isNotBlank()) {
                    InfoColumn(
                        label = stringResource(R.string.checkup_component_header_label_model),
                        value = checkUp.header.islandInfo.model,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (checkUp.header.islandInfo.operatingHours > 0) {
                    InfoColumn(
                        label = stringResource(R.string.checkup_component_header_label_operating_hours),
                        value = stringResource(
                            R.string.checkup_component_header_label_operating_hours_value,
                            checkUp.header.islandInfo.operatingHours
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (checkUp.header.islandInfo.cycleCount > 0) {
                    InfoColumn(
                        label = stringResource(R.string.checkup_component_header_label_cycle_count),
                        value = checkUp.header.islandInfo.cycleCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()

            // Association
            if (associations.isNotEmpty()) {
                InfoRow(
                    icon = Icons.Default.Link,
                    label = stringResource(R.string.checkup_component_header_association_title),
                    value =stringResource(
                        R.string.checkup_component_header_association_island,
                        associations[0].islandId
                    ),
                )
                InfoRow(
                    icon = Icons.Default.Link,
                    label = stringResource(R.string.checkup_component_header_association_subtitle),
                    value = "${associations[0].associationType.asString()} - ${associations[0].associationType.getDescription()}"
                )
            }

            HorizontalDivider()

            // Contact info
            if (checkUp.header.clientInfo.contactPerson.isNotBlank() ||
                checkUp.header.clientInfo.site.isNotBlank()
            ) {
                InfoRow(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.checkup_component_header_label_contact),
                    value = buildString {
                        if (checkUp.header.clientInfo.contactPerson.isNotBlank()) {
                            append(checkUp.header.clientInfo.contactPerson)
                        }
                        if (checkUp.header.clientInfo.site.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(checkUp.header.clientInfo.site)
                        }
                    }
                )
            }

            // Technician
            if (checkUp.header.technicianInfo.name.isNotBlank()) {
                InfoRow(
                    icon = Icons.Default.Engineering,
                    label = stringResource(R.string.checkup_component_header_label_technician),
                    value = buildString {
                        append(checkUp.header.technicianInfo.name)
                        if (checkUp.header.technicianInfo.company.isNotBlank()) {
                            append(" (${checkUp.header.technicianInfo.company})")
                        }
                    }
                )
            }

            // Progress
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.checkup_component_header_progress),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val completedCount =
                        checkUp.checkItems.count { it.status != CheckItemStatus.PENDING }
                    val totalCount = checkUp.checkItems.size

                    Text(
                        text = stringResource(
                            R.string.checkup_component_header_progress_completed,
                            completedCount,
                            totalCount
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                CircularProgressIndicator(
                    progress = { progress.overallProgress },
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp,
                )
            }

            // Notes
            if (checkUp.header.notes.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.Notes,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.checkup_component_header_notes),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = checkUp.header.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: String,
    value: String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if( icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InfoColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
