package net.calvuz.qreport.client.unit.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.calvuz.qreport.app.app.presentation.components.ConfirmDeleteDialog
import net.calvuz.qreport.app.app.presentation.components.EmptyState
import net.calvuz.qreport.client.unit.domain.model.MechanicalUnit
import net.calvuz.qreport.client.unit.presentation.ui.components.MechanicalUnitCard

/**
 * Screen that lists all [MechanicalUnit]s for a given island.
 *
 * @param islandName      Display name of the parent island (shown in title).
 * @param onNavigateBack  Back navigation callback.
 * @param onNavigateToAdd Navigate to the add form for a new unit.
 * @param onNavigateToEdit Navigate to the edit form for an existing unit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicalUnitListScreen(
    islandName: String,
    onNavigateBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (unitId: String) -> Unit,
    viewModel: MechanicalUnitListViewModel = hiltViewModel()
) {
    val units by viewModel.units.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    // Unit pending deletion — drives the confirm dialog
    var pendingDelete by remember { mutableStateOf<MechanicalUnit?>(null) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = islandName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Indietro")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi unità")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->

        if (units.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(innerPadding),
                textTitle = "Nessuna unità meccanica",
                textMessage = "Aggiungi la prima unità con il pulsante +",
                iconImageVector = Icons.Default.Settings,
                iconContentDescription = "Nessuna unità",
                iconActionImageVector = Icons.Default.Add,
                iconActionContentDescription = "Aggiungi unità",
                textAction = "Aggiungi unità",
                onAction = onNavigateToAdd
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(units, key = { it.id }) { unit ->
                    MechanicalUnitCard(
                        unit = unit,
                        onEditClick = { onNavigateToEdit(unit.id) },
                        onDeleteClick = { pendingDelete = unit }
                    )
                }
            }
        }
    }

    // Confirm before deleting
    pendingDelete?.let { unit ->
        ConfirmDeleteDialog(
            objectName = "unità meccanica",
            objectDesc = unit.name,
            onConfirm = {
                viewModel.delete(unit)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

