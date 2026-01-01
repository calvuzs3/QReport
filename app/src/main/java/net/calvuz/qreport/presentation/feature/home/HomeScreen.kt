@file:OptIn(ExperimentalMaterial3Api::class)

package net.calvuz.qreport.presentation.feature.home

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.calvuz.qreport.domain.model.checkup.CheckUp
import net.calvuz.qreport.domain.model.island.IslandType
import net.calvuz.qreport.presentation.core.components.LoadingState
import net.calvuz.qreport.presentation.feature.checkup.components.CheckupStatusChip

/**
 * Home Screen - QReport Dashboard
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToClients: () -> Unit,
    onNavigateToCheckUps: () -> Unit,
    onNavigateToNewCheckUp: () -> Unit,
    onNavigateToCheckUpDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle navigation effects
    LaunchedEffect(uiState.selectedCheckUpId) {
        uiState.selectedCheckUpId?.let { checkUpId ->
            onNavigateToCheckUpDetail(checkUpId)
            viewModel.clearSelectedCheckUp()
        }
    }

    // Handle quick create success
    LaunchedEffect(uiState.quickCreatedCheckUpId) {
        uiState.quickCreatedCheckUpId?.let { checkUpId ->
            onNavigateToCheckUpDetail(checkUpId)
            viewModel.clearSelectedCheckUp() // Reset after navigation
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error message
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
            LoadingState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // stats Cards
                item {
                    DashboardStatsSection(
                        stats = uiState.dashboardStats,
                        onNavigateToCheckUps = onNavigateToCheckUps
                    )
                }

                // Navigation Actions
                item {
                    NavigationActionsSection(
                        onNavigateToClients = onNavigateToClients,
                        onNavigateToCheckUps = onNavigateToCheckUps,
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
 * HEADER
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
 * Dashboard stats
 */
@Composable
private fun DashboardStatsSection(
    stats: DashboardStatistics?,
    onNavigateToCheckUps: () -> Unit
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
            onClick = onNavigateToCheckUps
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        onClick = onClick
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
 * Quick Action Card
 */
@Composable
private fun QuickActionCard(
    islandType: IslandType,
    onClick: () -> Unit,
    isLoading: Boolean
) {
    // use islandType.icon and displayName
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
                imageVector = islandType.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = islandType.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Quick Creation Dialog
 */
@Composable
private fun QuickCreateDialog(
    onDismiss: () -> Unit,
    onConfirm: (IslandType, String) -> Unit
) {
    // Default POLY_MOVE invece di vecchi tipi
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

                // Dropdown per selezione famiglia POLY
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
 * Recent Check-up
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
 * In progress Check-up
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
 * Single Check-up Card
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

                CheckupStatusChip(status = checkUp.status)
            }
        }
    }
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
 * ✅ NEW: Sezione azioni di navigazione principale
 */
@Composable
private fun NavigationActionsSection(
    onNavigateToClients: () -> Unit,
    onNavigateToCheckUps: () -> Unit
) {
    Text(
        text = "Gestione",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Client Management - Featured
        NavigationActionCard(
            modifier = Modifier.weight(1f),
            title = "Clienti",
            description = "Gestisci aziende",
            icon = Icons.Default.Business,
            onClick = onNavigateToClients,
            isHighlighted = true
        )

        // Check-ups Management
        NavigationActionCard(
            modifier = Modifier.weight(1f),
            title = "Check-up",
            description = "Controlli attivi",
            icon = Icons.AutoMirrored.Filled.Assignment,
            onClick = onNavigateToCheckUps,
            isHighlighted = false
        )
    }
}

/**
 * ✅ NEW: Card per azioni di navigazione
 */
@Composable
private fun NavigationActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val backgroundColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = contentColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}