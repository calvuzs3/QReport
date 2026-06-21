package net.calvuz.qreport.client.document.presentation.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.calvuz.qreport.R
import net.calvuz.qreport.client.document.domain.model.DocumentCategory
import net.calvuz.qreport.client.document.domain.model.DocumentMimeTypes
import net.calvuz.qreport.client.document.domain.model.DocumentScope
import net.calvuz.qreport.client.document.presentation.model.displayLabel
import net.calvuz.qreport.client.document.presentation.ui.DocumentViewModel

/**
 * Reusable Documents tab composable.
 *
 * Works for any [DocumentScope] — the caller provides [scope] and
 * [scopeEntityId], then calls the appropriate ViewModel load function
 * inside a [LaunchedEffect].
 *
 * Usage in IslandDetailScreen:
 * ```
 * DocumentsTab(
 *     scope         = DocumentScope.ISLAND,
 *     scopeEntityId = islandId
 * )
 * ```
 */
@Composable
fun DocumentsTab(
    scope: DocumentScope,
    scopeEntityId: String?,
    modifier: Modifier = Modifier,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    // Load documents when the tab first appears
    LaunchedEffect(scope, scopeEntityId) {
        when (scope) {
            DocumentScope.ISLAND   -> viewModel.loadForIsland(scopeEntityId ?: return@LaunchedEffect)
            DocumentScope.FACILITY -> viewModel.loadForFacility(scopeEntityId ?: return@LaunchedEffect)
            DocumentScope.CLIENT   -> viewModel.loadForClient(scopeEntityId ?: return@LaunchedEffect)
            DocumentScope.GLOBAL   -> viewModel.loadGlobal()
        }
    }

    // Show snackbar on one-shot messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.onSnackbarShown()
        }
    }

    // File picker launcher — registered before any conditional UI
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onDocumentPicked(
                scope         = scope,
                scopeEntityId = scopeEntityId,
                uri           = it,
                category      = uiState.categoryFilter ?: DocumentCategory.OTHER
            )
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = { pickerLauncher.launch(DocumentMimeTypes.PICKER_ALL) }
                ) {
                    Icon(Icons.Default.Add, contentDescription =
                        stringResource(R.string.action_add_document))
                }
            }
        },
        topBar = {
            // Selection mode top bar
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    count     = uiState.selectedCount,
                    onClear   = viewModel::onClearSelection,
                    onDelete  = viewModel::onDeleteSelected
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Category filter chips
            CategoryFilterRow(
                selected  = uiState.categoryFilter,
                onSelect  = viewModel::onCategoryFilterSelected
            )

            // Content
            when {
                uiState.isLoading || uiState.isImporting -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text  = uiState.error!!.asString(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = viewModel::onErrorDismissed) {
                                Text(stringResource(R.string.action_close))
                            }
                        }
                    }
                }

                uiState.filteredDocuments.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text  = stringResource((R.string.label_no_documents)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        // Extra bottom padding so the FAB doesn't cover the last item
                        contentPadding     = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.filteredDocuments,
                            key   = { it.id }
                        ) { document ->
                            DocumentCard(
                                document    = document,
                                variant     = DocumentCardVariant.COMPACT,
                                isSelected  = document.id in uiState.selectedIds,
                                onOpen      = viewModel::onOpenDocument,
                                onEdit      = { viewModel.onUpdateDocument(it) },
                                onDelete    = { viewModel.onRequestDelete(it.id) },
                                onLongPress = { viewModel.onToggleSelection(it.id) }
                            )
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (uiState.pendingDelete != null) {
            AlertDialog(
                onDismissRequest = viewModel::onCancelDelete,
                title = { Text(stringResource(R.string.action_delete_document)) },
                text  = { Text(stringResource(R.string.action_delete_document_confirmation)) },
                confirmButton = {
                    TextButton(onClick = viewModel::onConfirmDelete) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme
                            .colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onCancelDelete) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

// ── Category filter row ───────────────────────────────────────────────────────

@Composable
private fun CategoryFilterRow(
    selected: DocumentCategory?,
    onSelect: (DocumentCategory?) -> Unit
) {
    LazyRow(
        contentPadding     = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected  = selected == null,
                onClick   = { onSelect(null) },
                label     = { Text(stringResource(R.string.label_all)) }
            )
        }
        items(DocumentCategory.entries) { category ->
            FilterChip(
                selected  = selected == category,
                onClick   = { onSelect(if (selected == category) null else category) },
                label     = { Text(category.displayLabel().asString()) }
            )
        }
    }
}

// ── Selection top bar ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.material3.TopAppBar(
        title = { Text(stringResource(R.string.label_count_selected, count)) },
        navigationIcon = {
            TextButton(onClick = onClear) { Text(stringResource(R.string.action_cancel)) }
        },
        actions = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}