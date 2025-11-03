@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.presentation.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.presentation.ui.theme.QReportTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.model.island.IslandType

/**
 * Home Screen - Dashboard principale QReport
 *
 * Features:
 * - Statistiche dashboard in cards
 * - Check-up recenti e in corso
 * - Quick actions per nuovi check-up
 * - Navigazione rapida alle sezioni principali
 */
@Composable
fun HomeScreen(
    onNavigateToCheckUps: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToNewCheckUp: () -> Unit,           // ✅ AGGIUNTO
    onNavigateToCheckUpDetail: (String) -> Unit,  // ✅ AGGIUNTO
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle navigation effects - CORRETTO
    LaunchedEffect(uiState.selectedCheckUpId) {
        uiState.selectedCheckUpId?.let { checkUpId ->
            onNavigateToCheckUpDetail(checkUpId)  // ✅ USA il parametro corretto
            viewModel.clearSelectedCheckUp()  // ✅ Reset dopo navigazione
        }
    }

    // Handle quick create success
    LaunchedEffect(uiState.quickCreatedCheckUpId) {
        uiState.quickCreatedCheckUpId?.let { checkUpId ->
            onNavigateToCheckUpDetail(checkUpId)  // ✅ AGGIUNTO
            viewModel.clearSelectedCheckUp()  // ✅ Reset dopo navigazione
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error snackbar or handle error
            viewModel.dismissError()
        }
    }

    // Success handling
    if (uiState.showQuickCreateSuccess) {
        LaunchedEffect(Unit) {
            // Show success message
            viewModel.dismissQuickCreateSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        HomeHeader(
            onRefresh = { viewModel.refresh() },
            isLoading = uiState.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Dashboard content
        if (uiState.isLoading) {
            LoadingContent()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Statistics Cards
                item {
                    DashboardStatsSection(
                        stats = uiState.dashboardStats,
                        onNavigateToCheckUps = onNavigateToCheckUps,
                        onNavigateToArchive = onNavigateToArchive
                    )
                }

                // Quick Actions
                item {
                    QuickActionsSection(
                        onCreateCheckUp = { islandType, clientName ->
                            viewModel.createQuickCheckUp(islandType, clientName)
                        },
                        isCreating = uiState.isCreatingCheckUp
                    )
                }

                // Recent Check-ups
                item {
                    RecentCheckUpsSection(
                        checkUps = uiState.recentCheckUps,
                        onCheckUpClick = { checkUpId ->
                            viewModel.navigateToCheckUp(checkUpId)
                        }
                    )
                }

                // In Progress Check-ups
                item {
                    InProgressCheckUpsSection(
                        checkUps = uiState.inProgressCheckUps,
                        onCheckUpClick = { checkUpId ->
                            viewModel.navigateToCheckUp(checkUpId)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Header della home con titolo e azioni
 */
@Composable
private fun HomeHeader(
    onRefresh: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "QReport",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Check-up Isole Robotizzate",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onRefresh,
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Aggiorna"
            )
        }
    }
}

/**
 * Sezione statistiche dashboard
 */
@Composable
private fun DashboardStatsSection(
    stats: DashboardStatistics?,
    onNavigateToCheckUps: () -> Unit,
    onNavigateToArchive: () -> Unit
) {
    Text(
        text = "Panoramica",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total Check-ups
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Totali",
            value = stats?.totalCheckUps?.toString() ?: "0",
            icon = Icons.AutoMirrored.Filled.Assignment,
            onClick = onNavigateToCheckUps
        )

        // Active Check-ups
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Attivi",
            value = stats?.activeCheckUps?.toString() ?: "0",
            icon = Icons.Default.PlayArrow,
            onClick = onNavigateToCheckUps
        )

        // Completed This Week
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Settimana",
            value = stats?.completedThisWeek?.toString() ?: "0",
            icon = Icons.Default.CheckCircle,
            onClick = onNavigateToArchive
        )
    }
}

/**
 * Card per statistiche
 */
@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Sezione quick actions
 */
@Composable
private fun QuickActionsSection(
    onCreateCheckUp: (IslandType, String) -> Unit,
    isCreating: Boolean
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Text(
        text = "Azioni Rapide",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(IslandType.entries) { islandType ->
            QuickActionCard(
                islandType = islandType,
                onClick = { showCreateDialog = true },
                isLoading = isCreating
            )
        }
    }

    // Create Dialog
    if (showCreateDialog) {
        QuickCreateDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { islandType, clientName ->
                onCreateCheckUp(islandType, clientName)
                showCreateDialog = false
            }
        )
    }
}

/**
 * Card per quick action
 */
// CORRETTA - Righe 310-352
@Composable
private fun QuickActionCard(
    islandType: IslandType,
    onClick: () -> Unit,
    isLoading: Boolean
) {
    // ✅ USA islandType.icon e displayName direttamente
    Card(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = islandType.icon,  // ✅ CORRETTO
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = islandType.displayName,  // ✅ CORRETTO
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dialog per creazione rapida check-up
 */
// CORRETTA - Righe 357-406
@Composable
private fun QuickCreateDialog(
    onDismiss: () -> Unit,
    onConfirm: (IslandType, String) -> Unit
) {
    // ✅ Default POLY_MOVE invece di vecchi tipi
    var selectedIslandType by remember { mutableStateOf(IslandType.POLY_MOVE) }
    var clientName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Check-up") },
        text = {
            Column {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nome Cliente") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Tipo Isola",
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ✅ Dropdown per selezione famiglia POLY
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedIslandType.displayName,
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        IslandType.entries.forEach { islandType ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = islandType.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(islandType.displayName)
                                    }
                                },
                                onClick = {
                                    selectedIslandType = islandType
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (clientName.isNotBlank()) {
                        onConfirm(selectedIslandType, clientName)
                    }
                },
                enabled = clientName.isNotBlank()
            ) {
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

/**
 * Sezione check-up recenti
 */
@Composable
private fun RecentCheckUpsSection(
    checkUps: List<CheckUp>,
    onCheckUpClick: (String) -> Unit
) {
    Text(
        text = "Recenti",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (checkUps.isEmpty()) {
        EmptyStateCard("Nessun check-up recente")
    } else {
        checkUps.take(3).forEach { checkUp ->
            CheckUpCard(
                checkUp = checkUp,
                onClick = { onCheckUpClick(checkUp.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Sezione check-up in corso
 */
@Composable
private fun InProgressCheckUpsSection(
    checkUps: List<CheckUp>,
    onCheckUpClick: (String) -> Unit
) {
    if (checkUps.isNotEmpty()) {
        Text(
            text = "In Corso",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        checkUps.forEach { checkUp ->
            CheckUpCard(
                checkUp = checkUp,
                onClick = { onCheckUpClick(checkUp.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Card per singolo check-up
 */
@Composable
private fun CheckUpCard(
    checkUp: CheckUp,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = checkUp.header.clientInfo.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = checkUp.islandType.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = checkUp.updatedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusChip(status = checkUp.status)
            }
        }
    }
}

/**
 * Chip per status check-up
 */
@Composable
private fun StatusChip(status: CheckUpStatus) {
    val (color, text) = when (status) {
        CheckUpStatus.DRAFT -> MaterialTheme.colorScheme.outline to "Bozza"
        CheckUpStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary to "In Corso"
        CheckUpStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to "Completato"
        CheckUpStatus.EXPORTED -> MaterialTheme.colorScheme.secondary to "Esportato"
        CheckUpStatus.ARCHIVED -> MaterialTheme.colorScheme.surfaceVariant to "Archiviato"
    }

    AssistChip(
        onClick = { },
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color
        )
    )
}

/**
 * Empty state card
 */
@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Loading content
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// ============================================================
// PREVIEWS
// ============================================================

// CORRETTO - Righe 575-584
@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    QReportTheme {
        HomeScreen(
            onNavigateToCheckUps = {},
            onNavigateToArchive = {},
            onNavigateToNewCheckUp = {},           // ✅ AGGIUNTO
            onNavigateToCheckUpDetail = {}         // ✅ AGGIUNTO
        )
    }
}