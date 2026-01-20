package net.calvuz.qreport.ti.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import timber.log.Timber

/**
 * Dialog for collecting digital signature with full-screen signature pad
 */
@Composable
fun SignatureCollectionDialog(
    isVisible: Boolean,
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    onDismiss: () -> Unit,
    onSignatureConfirm: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            var hasSignature by remember { mutableStateOf(false) }
            var signaturePaths by remember { mutableStateOf<List<Path>>(emptyList()) }
            var isProcessing by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    SignatureDialogHeader(
                        title = title,
                        subtitle = subtitle,
                        description = description,
                        icon = icon
                    )

                    // Signature pad area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Take available space
                            .heightIn(min = 300.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!isProcessing) {
                                SignaturePad(
                                    modifier = Modifier.fillMaxSize(),
                                    onSignatureChanged = { hasSignature = it },
                                    onPathsChanged = { signaturePaths = it },
                                    strokeColor = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.0f
                                )
                            } else {
                                ProcessingIndicator()
                            }
                        }
                    }

                    // Instructions
                    if (!hasSignature) {
                        InstructionCard()
                    }

                    // Action buttons
                    ActionButtons(
                        hasSignature = hasSignature,
                        isProcessing = isProcessing,
                        onClear = {
                            signaturePaths = emptyList()
                            hasSignature = false
                        },
                        onCancel = onDismiss,
                        onConfirm = {
                            if (hasSignature && signaturePaths.isNotEmpty()) {
                                isProcessing = true

                                try {
                                    // Convert paths to bitmap
                                    val bitmap = pathsToBitmap(
                                        paths = signaturePaths,
                                        width = 400,
                                        height = 200,
                                        strokeColor = Color.Black,
                                        strokeWidth = 3.0f
                                    )

                                    Timber.d("SignatureDialog: Signature bitmap created: ${bitmap.width}x${bitmap.height}")
                                    onSignatureConfirm(bitmap)

                                } catch (e: Exception) {
                                    Timber.e(e, "SignatureDialog: Error processing signature")
                                    isProcessing = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Dialog header with icon, title and description
 */
@Composable
private fun SignatureDialogHeader(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Processing indicator shown while bitmap is being created
 */
@Composable
private fun ProcessingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            Text(
                text = "Elaborazione firma...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Instructions card shown when no signature is present
 */
@Composable
private fun InstructionCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Disegna la firma nell'area sopra utilizzando il dito o uno stylus",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Action buttons for clear, cancel and confirm
 */
@Composable
private fun ActionButtons(
    hasSignature: Boolean,
    isProcessing: Boolean,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Clear button
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.weight(1f),
            enabled = hasSignature && !isProcessing
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancella")
        }

        // Cancel button
        TextButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            enabled = !isProcessing
        ) {
            Text("Annulla")
        }

        // Confirm button
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1.5f),
            enabled = hasSignature && !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Conferma Firma")
            }
        }
    }
}

/**
 * Convenience functions for specific signature types
 */
@Composable
fun TechnicianSignatureDialog(
    isVisible: Boolean,
    technicianName: String,
    onDismiss: () -> Unit,
    onSignatureConfirm: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    SignatureCollectionDialog(
        isVisible = isVisible,
        title = "Firma Tecnico",
        subtitle = technicianName.takeIf { it.isNotBlank() } ?: "Tecnico",
        description = "Apponi la tua firma per confermare l'intervento tecnico completato",
        icon = Icons.Default.Engineering,
        onDismiss = onDismiss,
        onSignatureConfirm = onSignatureConfirm,
        modifier = modifier
    )
}

@Composable
fun CustomerSignatureDialog(
    isVisible: Boolean,
    customerName: String,
    onDismiss: () -> Unit,
    onSignatureConfirm: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    SignatureCollectionDialog(
        isVisible = isVisible,
        title = "Firma Cliente",
        subtitle = customerName.takeIf { it.isNotBlank() } ?: "Cliente",
        description = "Il cliente firma per approvare l'intervento tecnico effettuato",
        icon = Icons.Default.Business,
        onDismiss = onDismiss,
        onSignatureConfirm = onSignatureConfirm,
        modifier = modifier
    )
}