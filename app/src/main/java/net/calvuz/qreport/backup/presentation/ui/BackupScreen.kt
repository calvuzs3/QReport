package net.calvuz.qreport.backup.presentation.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import net.calvuz.qreport.backup.domain.model.BackupInfo
import net.calvuz.qreport.backup.presentation.ui.model.BackupProgress
import net.calvuz.qreport.backup.presentation.ui.model.RestoreProgress
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.backup.presentation.ui.components.BackupActionCard
import net.calvuz.qreport.backup.presentation.ui.components.BackupConfirmationDialog
import net.calvuz.qreport.backup.presentation.ui.components.BackupHeaderCard
import net.calvuz.qreport.backup.presentation.ui.components.BackupItemCard
import net.calvuz.qreport.backup.presentation.ui.components.BackupOptions
import net.calvuz.qreport.backup.presentation.ui.components.BackupOptionsCard
import net.calvuz.qreport.backup.presentation.ui.components.BackupProgressDialog
import net.calvuz.qreport.backup.presentation.ui.components.DeleteBackupDialog
import net.calvuz.qreport.backup.presentation.ui.components.RestoreBackupConfirmationDialog
import net.calvuz.qreport.backup.presentation.ui.components.RestoreBackupProgressDialog
import net.calvuz.qreport.share.presentation.ui.ShareBackupDialog
import net.calvuz.qreport.share.presentation.ui.ShareBackupViewModel

/**
 * BACKUP SCREEN - FASE 5.4
 *
 * Screen principale per gestione backup QReport con:
 * - Overview sistema backup
 * - Configurazione opzioni backup
 * - Progress tracking creazione/ripristino
 * - Lista backup disponibili con azioni
 * - Dialogs conferma e gestione errori
 */

@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    backupViewModel: BackupViewModel = hiltViewModel(),
    shareViewModel: ShareBackupViewModel = hiltViewModel(),
    @ApplicationContext context: Context = LocalContext.current
) {
    // Collect UI state
    val backupUiState by backupViewModel.uiState.collectAsStateWithLifecycle()
    val shareUiState by shareViewModel.uiState.collectAsStateWithLifecycle()
    val backupProgress by backupViewModel.backupProgress.collectAsStateWithLifecycle()
    val restoreProgress by backupViewModel.restoreProgress.collectAsStateWithLifecycle()

    // Dialog states
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedBackup by remember { mutableStateOf<BackupInfo?>(null) }

    // Dialogs visibility
    var showRestoreProgressDialog by remember { mutableStateOf(false) }
    var showBackupProgressDialog by remember { mutableStateOf(false) }

    // SnackBar
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(restoreProgress) {
        when (restoreProgress) {
            is RestoreProgress.InProgress -> {
                // Dialog già aperto, non fare nulla
            }
            is RestoreProgress.Completed,
            is RestoreProgress.Error -> {
                // Mantieni dialog aperto per permettere all'utente di vedere il risultato
                // Il dialog si chiuderà solo con onDismiss
            }
            is RestoreProgress.Idle -> {
                showRestoreProgressDialog = false
            }
        }
    }

    LaunchedEffect(backupProgress) {
        when (backupProgress) {
            is BackupProgress.InProgress -> {
                // If Dialog is not opened, let's open it
                if (!showBackupProgressDialog) {
                    showBackupProgressDialog = true  // open dialog when backup starts
                }
            }
            is BackupProgress.Completed,
            is BackupProgress.Error -> {
                // Keep dialog opened
                // onDismiss will close it
            }
            is BackupProgress.Idle -> {
                showBackupProgressDialog = false
            }
        }
    }


    // ===== MAIN LAYOUT =====

    Scaffold(
        topBar = {
            BackupTopBar(
                onNavigateBack = onNavigateBack,
                onRefresh = backupViewModel::refreshData,
                isRefreshing = backupUiState.isLoading
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState )
        },
        modifier = modifier
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues((16.dp)),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ===== BACKUP HEADER =====
                item {
                    BackupHeaderCard(
                        totalBackups = backupUiState.availableBackups.size,
                        lastBackupDate = backupUiState.lastBackupDate,
                        estimatedSize = backupUiState.estimatedBackupSize
                    )
                }

                // ===== BACKUP OPTIONS =====
                item {
                    BackupOptionsCard(
                        includePhotos = backupUiState.includePhotos,
//                        includeThumbnails = uiState.includeThumbnails,
                        backupMode = backupUiState.backupMode,
                        onTogglePhotos = backupViewModel::toggleIncludePhotos,
//                        onToggleThumbnails = viewModel::toggleIncludeThumbnails,
                        onModeChange = backupViewModel::updateBackupMode,
                        estimatedSize = backupUiState.estimatedBackupSize
                    )
                }

                // ===== BACKUP ACTION =====
                item {
                    BackupActionCard(
                        isBackupInProgress = backupProgress is BackupProgress.InProgress,
                        backupProgress = backupProgress,
                        onShowBackupConfirmation = { showBackupDialog = true },
                        onCancelBackup = backupViewModel::cancelBackup
                    )
                }

                // ===== BACKUP LIST HEADER =====
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Backup Disponibili",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (backupUiState.availableBackups.isNotEmpty()) {
                            Text(
                                text = "${backupUiState.availableBackups.size} backup",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // ===== BACKUP LIST OR EMPTY STATE =====
                if (backupUiState.availableBackups.isEmpty()) {
                    item {
                        EmptyState(
                            textTitle = "Nessun backup trovato",
                            textMessage = "Crea un nuovo backup per iniziare",
                            iconImageVector = Icons.Default.Refresh,
                            iconContentDescription = "Nessun Backup Trovato"
                        )
                    }
                } else {
                    items(
                        items = backupUiState.availableBackups,
                        key = { backup -> backup.id }
                    ) { backup ->
                        BackupItemCard(
                            backup = backup,
                            onRestore = {
                                selectedBackup = backup
                                showRestoreDialog = true
                            },
                            onDelete = {
                                selectedBackup = backup
                                showDeleteDialog = true
                            },
                            onShare = {
                                backupViewModel.showShareDialog(backup.id)
                            },
                            isRestoreInProgress = restoreProgress is RestoreProgress.InProgress
                        )
                    }
                }

                // Bottom spacing for list
                item {
                    Spacer(
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            // ===== LOADING OVERLAY =====
            if (backupUiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // ===== DIALOGS =====

    // Backup confirmation Dialog
    if (showBackupDialog) {
        BackupConfirmationDialog(
            backupOptions = BackupOptions(
                includePhotos = backupUiState.includePhotos,
                includeThumbnails = backupUiState.includeThumbnails,
                backupMode = backupUiState.backupMode,
                description = backupUiState.backupDescription,
                estimatedSize = backupUiState.estimatedBackupSize
            ),
            onConfirm = {
                backupViewModel.createBackup()  // ✅ Ora chiamato dal dialog
                showBackupDialog = false
                showBackupProgressDialog = true  // ✅ Apri subito progress dialog
            },
            onDismiss = {
                showBackupDialog = false
            }
        )
    }

    // Restore confirmation Dialog
    selectedBackup?.let { backup ->
        if (showRestoreDialog) {
            RestoreBackupConfirmationDialog(
                backup = backup,
                onConfirm = { strategy ->
                    backupViewModel.restoreBackup(backup.id, strategy)
                    showRestoreDialog = false
                    showRestoreProgressDialog = true
                    // do not set selectedBackup to null here
                },
                onDismiss = {
                    showRestoreDialog = false
                    selectedBackup = null
                }
            )
        }

        // Delete confirmation Dialog
        if (showDeleteDialog) {
            DeleteBackupDialog(
                backup = backup,
                onConfirm = {
                    backupViewModel.deleteBackup(backup.id)
                    showDeleteDialog = false
                    selectedBackup = null
                },
                onDismiss = {
                    showDeleteDialog = false
                    selectedBackup = null
                }
            )
        }
    }

    // Show ProgressDialog
    if (showRestoreProgressDialog) {
        RestoreBackupProgressDialog(
            progress = restoreProgress,
            onCancel = {
                backupViewModel.cancelRestore()
                showRestoreProgressDialog = false
                selectedBackup = null
            },
            onDismiss = {
                showRestoreProgressDialog = false
                selectedBackup = null
            }
        )
    }

    // Show ProgressDialog
    if (showBackupProgressDialog) {
        BackupProgressDialog(
            progress = backupProgress,
            onCancel = {
                backupViewModel.cancelBackup()
                showBackupProgressDialog = false
            },
            onDismiss = {
                showBackupProgressDialog = false
            }
        )
    }

    // SHARE DIALOG
    if (backupUiState.showShareDialog && backupUiState.shareBackupPath != null) {
        val backupPath = backupUiState.shareBackupPath!!
        val backupName = backupUiState.shareBackupName ?: "Backup"

        LaunchedEffect(backupPath) {
            shareViewModel.loadShareOptions(backupPath)
        }

        ShareBackupDialog(
            backupPath = backupPath,
            backupName = backupName,
            shareOptionOldVersions = shareUiState.shareOptionOldVersions,
            isLoading = shareUiState.isLoading,
            onShareSelected = { option ->
                shareViewModel.shareBackup(
                    backupPath = backupPath,
                    option = option,
                    onIntentReady = { intent ->
                        // ✅ CORRECTED: Start share intent
                        context.startActivity(intent)
                        backupViewModel.hideShareDialog()
                    }
                )
            },
            onDismiss = backupViewModel::hideShareDialog
        )
    }

    // ✅ CORRECTED: Handle sharing errors from ShareViewModel
    LaunchedEffect(shareUiState.error) {
        shareUiState.error?.let { error ->
            snackbarHostState.showSnackbar("Errore condivisione: $error")
            shareViewModel.clearError()
        }
    }

    // ===== UI MESSAGES =====

    // Handle UI messages (success, error, info)
    LaunchedEffect(backupUiState.successMessage) {
        backupUiState.successMessage?.let { message ->
            // Show snackbar for success
            snackbarHostState.showSnackbar(message)
            backupViewModel.dismissMessage()
        }
    }

    LaunchedEffect(backupUiState.errorMessage) {
        backupUiState.errorMessage?.let { message ->
            // Show snackbar for error
            snackbarHostState.showSnackbar(message)
            backupViewModel.dismissMessage()
        }
    }

    LaunchedEffect(backupUiState.infoMessage) {
        backupUiState.infoMessage?.let { message ->
            // Show snackbar for info
            snackbarHostState.showSnackbar(message)
            backupViewModel.dismissMessage()
        }
    }
}

// ===== TOP BAR =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupTopBar(
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = "Sistema Backup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Indietro"
                )
            }
        },
        actions = {
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Aggiorna"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

// ===== PREVIEW =====

@Preview(showBackground = true)
@Composable
private fun BackupScreenPreview() {
    MaterialTheme {
        // Preview with mock data - would need to be implemented
         BackupScreen(onNavigateBack = {})
    }
}