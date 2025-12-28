package net.calvuz.qreport.presentation.feature.photo

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.calvuz.qreport.domain.model.photo.PhotoPerspective

/**
 * Screen per preview e configurazione di una foto importata dalla galleria.
 * Permette di impostare perspective, caption e confermare l'import.
 */
@Composable
fun PhotoImportPreviewScreen(
    checkItemId: String,
    photoUri: Uri,
    onNavigateBack: () -> Unit,
    onImportSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoViewModel = hiltViewModel()
) {
    val importState by viewModel.importUiState.collectAsStateWithLifecycle()

    // Stati locali per la configurazione
    var selectedPerspective by remember { mutableStateOf<PhotoPerspective?>(null) }
    var caption by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        PhotoImportTopBar(
            onNavigateBack = onNavigateBack,
            isImporting = importState.isImporting,
            canConfirm = selectedPerspective != null,
            onConfirmImport = {
                viewModel.importPhoto(
                    checkItemId = checkItemId,
                    photoUri = photoUri,
                    perspective = selectedPerspective!!,
                    caption = caption
                )
            }
        )

        when {
            importState.isLoading -> {
                PhotoImportLoading()
            }
            importState.error != null -> {
                PhotoImportError(
                    error = importState.error!!,
                    onRetry = {
                        viewModel.clearImportError()
                    },
                    onDismiss = onNavigateBack
                )
            }
            else -> {
                PhotoImportContent(
                    photoUri = photoUri,
                    selectedPerspective = selectedPerspective,
                    caption = caption,
                    onPerspectiveChanged = { selectedPerspective = it },
                    onCaptionChanged = { caption = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Gestisci successo import
    LaunchedEffect(importState.isImportSuccess) {
        if (importState.isImportSuccess) {
            onImportSuccess()
        }
    }
}

/**
 * Top Bar con azioni di conferma e annullamento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoImportTopBar(
    onNavigateBack: () -> Unit,
    isImporting: Boolean,
    canConfirm: Boolean,
    onConfirmImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text("Importa Foto")
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Torna indietro"
                )
            }
        },
        actions = {
            Button(
                onClick = onConfirmImport,
                enabled = canConfirm && !isImporting,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                if (isImporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("Importando...")
                    }
                } else {
                    Text("Conferma")
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Contenuto principale con preview e configurazione.
 */
@Composable
private fun PhotoImportContent(
    photoUri: Uri,
    selectedPerspective: PhotoPerspective?,
    caption: String,
    onPerspectiveChanged: (PhotoPerspective?) -> Unit,
    onCaptionChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preview della foto
        PhotoPreviewCard(
            photoUri = photoUri,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        // ✅ NUOVO: Selezione Perspective con Dropdown
        PerspectiveDropdownCard(
            selectedPerspective = selectedPerspective,
            onPerspectiveChanged = onPerspectiveChanged
        )

        // Input Caption
        CaptionInputCard(
            caption = caption,
            onCaptionChanged = onCaptionChanged
        )

        Spacer(modifier = Modifier.weight(1f))

        // Info card
        InfoCard()
    }
}

/**
 * Card per preview della foto importata.
 */
@Composable
private fun PhotoPreviewCard(
    photoUri: Uri,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Anteprima foto da importare",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * ✅ NUOVO: Card per selezione della perspective con dropdown a tutta larghezza.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerspectiveDropdownCard(
    selectedPerspective: PhotoPerspective?,
    onPerspectiveChanged: (PhotoPerspective?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con icona e titolo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Prospettiva *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Seleziona la prospettiva che meglio descrive questa foto:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ✅ DROPDOWN SELECTOR a tutta larghezza
            PerspectiveDropdownSelector(
                selectedPerspective = selectedPerspective,
                onPerspectiveChanged = onPerspectiveChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * ✅ NUOVO: Dropdown selector per le perspective usando ExposedDropdownMenuBox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerspectiveDropdownSelector(
    selectedPerspective: PhotoPerspective?,
    onPerspectiveChanged: (PhotoPerspective?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedPerspective?.displayName ?: "",
            onValueChange = { },
            readOnly = true,
            label = {
                Text("Scegli prospettiva")
            },
            placeholder = {
                Text("Seleziona una prospettiva...")
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (selectedPerspective != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            ),
            supportingText = {
                if (selectedPerspective == null) {
                    Text(
                        text = "Campo obbligatorio",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            isError = selectedPerspective == null,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            PhotoPerspective.entries.forEach { perspective ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                                        Column {
                                Text(
                                    text = perspective.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    onClick = {
                        onPerspectiveChanged(perspective)
                        expanded = false
                    },
                    leadingIcon = if (selectedPerspective == perspective) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selezionata",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Card per input della caption.
 */
@Composable
private fun CaptionInputCard(
    caption: String,
    onCaptionChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Descrizione",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = caption,
                onValueChange = onCaptionChanged,
                modifier = Modifier.fillMaxWidth(),

                placeholder = {
                    Text("Aggiungi una descrizione...")
                },
                maxLines = 3,
                supportingText = {
                    Text("${caption.length}/200 caratteri")
                }
            )
        }
    }
}

/**
 * Card informativa.
 */
@Composable
private fun InfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "La foto verrà processata e salvata con gli stessi metadati delle foto catturate dalla camera.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Loading state durante l'import.
 */
@Composable
private fun PhotoImportLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Caricamento anteprima...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Error state con opzioni di retry.
 */
@Composable
private fun PhotoImportError(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Errore import foto",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Annulla")
                    }
                    Button(onClick = onRetry) {
                        Text("Riprova")
                    }
                }
            }
        }
    }
}