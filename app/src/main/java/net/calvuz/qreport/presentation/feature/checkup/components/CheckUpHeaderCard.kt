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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.calvuz.qreport.domain.model.checkup.CheckItemStatus
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpIslandAssociation
import net.calvuz.qreport.domain.model.checkup.CheckUpProgress
import net.calvuz.qreport.presentation.feature.checkup.StatusChipForCheckUp

/**
 * Header card con informazioni del check-up e pulsante edit
 */
//@Composable
//fun CheckUpHeaderCard(
//    checkUp: CheckUp,
//    progress: CheckUpProgress,
//    onEditHeader: () -> Unit = {},
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(20.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Header with status and edit button
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column {
//                    Text(
//                        text = checkUp.header.clientInfo.companyName,
//                        style = MaterialTheme.typography.titleLarge, //headlineSmall
//                        fontWeight = FontWeight.Bold
//                    )
//                    Text(
//                        text = "${checkUp.islandType.displayName} • S/N: ${checkUp.header.islandInfo.serialNumber}",
//                        style = MaterialTheme.typography.bodyLarge,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//
//                }
//                Column(
//                    horizontalAlignment = Alignment.End
//                ) {
//                    Row(
//                        horizontalArrangement = Arrangement.End
//
//                    ) {
//                        StatusChipForCheckUp(status = checkUp.status)
//                    }
//
//                    Row(
//                        horizontalArrangement = Arrangement.End,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
////                    StatusChipForCheckUp(status = checkUp.status)
//
//                        IconButton(
//                            onClick = onEditHeader,
//                            modifier = Modifier.size(40.dp)
//                        ) {
//                            Icon(
//                                Icons.Default.Edit,
//                                contentDescription = "Modifica informazioni",
//                                tint = MaterialTheme.colorScheme.primary
//                            )
//                        }
//                    }
//
//                }
//            }
//
//
//        }
//
//        HorizontalDivider()
//
//        // Client and Site info
//        if (checkUp.header.clientInfo.contactPerson.isNotBlank() ||
//            checkUp.header.clientInfo.site.isNotBlank()
//        ) {
//            InfoRow(
//                icon = Icons.Default.Person,
//                label = "Contatto",
//                value = buildString {
//                    if (checkUp.header.clientInfo.contactPerson.isNotBlank()) {
//                        append(checkUp.header.clientInfo.contactPerson)
//                    }
//                    if (checkUp.header.clientInfo.site.isNotBlank()) {
//                        if (isNotEmpty()) append(" • ")
//                        append(checkUp.header.clientInfo.site)
//                    }
//                }
//            )
//        }
//
//        // Island details
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            if (checkUp.header.islandInfo.model.isNotBlank()) {
//                InfoColumn(
//                    label = "Modello",
//                    value = checkUp.header.islandInfo.model,
//                    modifier = Modifier.weight(1f)
//                )
//            }
//
//            if (checkUp.header.islandInfo.operatingHours > 0) {
//                InfoColumn(
//                    label = "Ore Lavoro",
//                    value = "${checkUp.header.islandInfo.operatingHours}h",
//                    modifier = Modifier.weight(1f)
//                )
//            }
//
//            if (checkUp.header.islandInfo.cycleCount > 0) {
//                InfoColumn(
//                    label = "Cicli",
//                    value = checkUp.header.islandInfo.cycleCount.toString(),
//                    modifier = Modifier.weight(1f)
//                )
//            }
//        }
//
//        // Technician info
//        if (checkUp.header.technicianInfo.name.isNotBlank()) {
//            InfoRow(
//                icon = Icons.Default.Engineering, // .EngineeringOutlined,
//                label = "Tecnico",
//                value = buildString {
//                    append(checkUp.header.technicianInfo.name)
//                    if (checkUp.header.technicianInfo.company.isNotBlank()) {
//                        append(" (${checkUp.header.technicianInfo.company})")
//                    }
//                }
//            )
//        }
//
//        // Progress indicator
//        HorizontalDivider()
//
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column {
//                Text(
//                    text = "Progresso",
//                    style = MaterialTheme.typography.labelMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                // Calculate progress from checkItems
//                val completedCount =
//                    checkUp.checkItems.count { it.status != CheckItemStatus.PENDING }
//                val totalCount = checkUp.checkItems.size
//
//                Text(
//                    text = "$completedCount/$totalCount completati",
//                    style = MaterialTheme.typography.bodyLarge,
//                    fontWeight = FontWeight.SemiBold
//                )
//            }
//
//            CircularProgressIndicator(
//                progress = { progress.overallProgress },
//                modifier = Modifier.size(48.dp),
//                strokeWidth = 4.dp,
//            )
//        }
//
//        // Notes if present
//        if (checkUp.header.notes.isNotBlank()) {
//            Card(
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceVariant
//                )
//            ) {
//                Column(
//                    modifier = Modifier.padding(12.dp)
//                ) {
//                    Row(
//                        horizontalArrangement = Arrangement.spacedBy(8.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            Icons.Default.Notes,
//                            contentDescription = null,
//                            modifier = Modifier.size(16.dp),
//                            tint = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                        Text(
//                            text = "Note",
//                            style = MaterialTheme.typography.labelMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = checkUp.header.notes,
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                }
//            }
//        }
//    }
//}
//
//
//@Composable
//private fun InfoRow(
//    icon: ImageVector,
//    label: String,
//    value: String,
//    modifier: Modifier = Modifier
//) {
//    Row(
//        modifier = modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.spacedBy(12.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Icon(
//            imageVector = icon,
//            contentDescription = null,
//            modifier = Modifier.size(20.dp),
//            tint = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//
//        Column {
//            Text(
//                text = label,
//                style = MaterialTheme.typography.labelMedium,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Text(
//                text = value,
//                style = MaterialTheme.typography.bodyMedium
//            )
//        }
//    }
//}
//
//@Composable
//private fun InfoColumn(
//    label: String,
//    value: String,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier
//    ) {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.labelMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//        Text(
//            text = value,
//            style = MaterialTheme.typography.bodyMedium,
//            fontWeight = FontWeight.Medium
//        )
//    }
//}
/**
 * Header card con informazioni del check-up e pulsante edit
 * ✅ ENHANCED: Aggiunto supporto per associazioni CheckUp-Isole
 */
@Composable
fun CheckUpHeaderCard(
    modifier: Modifier = Modifier,
    checkUp: CheckUp,
    progress: CheckUpProgress,
    associations: List<CheckUpIslandAssociation> = emptyList(), // ✅ NUOVO PARAMETRO
    onEditHeader: () -> Unit = {},
    onManageAssociation: () -> Unit = {} // ✅ NUOVO PARAMETRO
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with status and edit button
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
                        text =
                            "S/N: ${checkUp.header.islandInfo.serialNumber}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        horizontalArrangement = Arrangement.End
                    ) {
                        StatusChipForCheckUp(status = checkUp.status)
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ✅ NUOVO: Association button
                        IconButton(
                            onClick = onManageAssociation,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (associations.isNotEmpty())
                                    Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = "Gestisci associazione",
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
                                contentDescription = "Modifica informazioni",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ✅ NUOVO: Association info section (se ci sono associazioni)
            if (associations.isNotEmpty()) {
                AssociationInfoSection(associations = associations)
            }

            HorizontalDivider()

            // ===== TUTTO IL RESTO RIMANE IDENTICO =====

            // Client and Site info
            if (checkUp.header.clientInfo.contactPerson.isNotBlank() ||
                checkUp.header.clientInfo.site.isNotBlank()
            ) {
                InfoRow(
                    icon = Icons.Default.Person,
                    label = "Contatto",
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

            // Island details
            Row(
//            modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (checkUp.header.islandInfo.model.isNotBlank()) {
                    InfoColumn(
                        label = "Modello",
                        value = checkUp.header.islandInfo.model,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (checkUp.header.islandInfo.operatingHours > 0) {
                    InfoColumn(
                        label = "Ore Lavoro",
                        value = "${checkUp.header.islandInfo.operatingHours}h",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (checkUp.header.islandInfo.cycleCount > 0) {
                    InfoColumn(
                        label = "Cicli",
                        value = checkUp.header.islandInfo.cycleCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Technician info
            if (checkUp.header.technicianInfo.name.isNotBlank()) {
                InfoRow(
                    icon = Icons.Default.Engineering,
                    label = "Tecnico",
                    value = buildString {
                        append(checkUp.header.technicianInfo.name)
                        if (checkUp.header.technicianInfo.company.isNotBlank()) {
                            append(" (${checkUp.header.technicianInfo.company})")
                        }
                    }
                )
            }

            // Progress indicator
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Progresso",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val completedCount =
                        checkUp.checkItems.count { it.status != CheckItemStatus.PENDING }
                    val totalCount = checkUp.checkItems.size

                    Text(
                        text = "$completedCount/$totalCount completati",
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

            // Notes if present
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
                                text = "Note",
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
private fun AssociationInfoSection(
    associations: List<CheckUpIslandAssociation>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = "CheckUp Associato",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            associations.forEach { association ->
                Row(
//                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Isola: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = association.islandId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = association.associationType.displayName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }

                // Mostra note se presenti
                if (!association.notes.isNullOrBlank()) {
                    Text(
                        text = association.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ===== COMPONENTI ESISTENTI INVARIATI =====

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
