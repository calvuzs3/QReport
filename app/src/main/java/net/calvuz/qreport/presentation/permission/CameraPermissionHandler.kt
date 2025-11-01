package net.calvuz.qreport.presentation.permission

import android.Manifest
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*

/**
 * Gestore unificato per i permessi camera.
 * ✅ CORRETTO: Separato callback logico da rendering UI
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionHandler(
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: () -> Unit = {}, // ✅ CORREZIONE: Non più @Composable
    showRationaleContent: @Composable () -> Unit = { CameraPermissionPlaceholder() }, // ✅ NUOVO: Contenuto da mostrare
    content: @Composable (PermissionState) -> Unit = { }
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var showRationale by remember { mutableStateOf(false) }
    var showPermissionRequest by remember { mutableStateOf(false) }

    when (cameraPermissionState.status) {
        is PermissionStatus.Granted -> {
            onPermissionGranted()
        }
        is PermissionStatus.Denied -> {
            if (cameraPermissionState.status.shouldShowRationale) {
                LaunchedEffect(cameraPermissionState.status) {
                    showRationale = true
                }
            } else {
                LaunchedEffect(cameraPermissionState.status) {
                    showPermissionRequest = true
                }
            }

            // Mostra contenuto per permesso negato
            showRationaleContent()
        }
    }

    // Dialog per rationale
    if (showRationale) {
        CameraPermissionRationale(
            onRequestPermission = {
                showRationale = false
                cameraPermissionState.launchPermissionRequest()
            },
            onDismiss = {
                showRationale = false
                onPermissionDenied() // ✅ Ora è callback normale
            }
        )
    }

    // Dialog per richiesta iniziale
    if (showPermissionRequest) {
        CameraPermissionRequest(
            onRequestPermission = {
                showPermissionRequest = false
                cameraPermissionState.launchPermissionRequest()
            },
            onDismiss = {
                showPermissionRequest = false
                onPermissionDenied() // ✅ Ora è callback normale
            }
        )
    }

    content(cameraPermissionState)
}

/**
 * Dialog per la richiesta iniziale del permesso camera.
 */
@Composable
private fun CameraPermissionRequest(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit = {} // ✅ CORREZIONE: Non più nullable
) {
    AlertDialog(
        onDismissRequest = onDismiss, // ✅ Ora funziona
        icon = {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null
            )
        },
        title = {
            Text("Permesso Camera")
        },
        text = {
            Text(
                "QReport ha bisogno dell'accesso alla camera per scattare foto durante i check-up. " +
                        "Le foto aiutano a documentare lo stato dei componenti ispezionati."
            )
        },
        confirmButton = {
            Button(
                onClick = onRequestPermission
            ) {
                Text("Concedi Permesso")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

/**
 * Dialog per spiegare perché il permesso è necessario (rationale).
 */
@Composable
private fun CameraPermissionRationale(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit = {} // ✅ CORREZIONE: Non più nullable
) {
    AlertDialog(
        onDismissRequest = onDismiss, // ✅ Ora funziona
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error // ✅ CORREZIONE: warning non esiste
            )
        },
        title = {
            Text("Permesso Camera Necessario")
        },
        text = {
            Text(
                "Senza l'accesso alla camera, non sarà possibile documentare fotograficamente " +
                        "i check-up. Le foto sono fondamentali per:\n\n" +
                        "• Documentare anomalie e problemi\n" +
                        "• Creare report completi per i clienti\n" +
                        "• Mantenere uno storico visuale\n" +
                        "• Facilitare analisi future\n\n" +
                        "Ti preghiamo di concedere il permesso per utilizzare questa funzionalità."
            )
        },
        confirmButton = {
            Button(
                onClick = onRequestPermission
            ) {
                Text("Concedi Permesso")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Non Ora")
            }
        }
    )
}

/**
 * Hook per gestire lo stato del permesso camera in modo reattivo.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberCameraPermissionState(): CameraPermissionStatus {
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)

    return when (permissionState.status) {
        is PermissionStatus.Granted -> CameraPermissionStatus.Granted
        is PermissionStatus.Denied -> {
            if (permissionState.status.shouldShowRationale) {
                CameraPermissionStatus.DeniedWithRationale(
                    requestPermission = { permissionState.launchPermissionRequest() }
                )
            } else {
                CameraPermissionStatus.Denied(
                    requestPermission = { permissionState.launchPermissionRequest() }
                )
            }
        }
    }
}

/**
 * Stati possibili per il permesso camera.
 */
sealed class CameraPermissionStatus {
    data object Granted : CameraPermissionStatus()

    data class Denied(
        val requestPermission: () -> Unit
    ) : CameraPermissionStatus()

    data class DeniedWithRationale(
        val requestPermission: () -> Unit
    ) : CameraPermissionStatus()
}

/**
 * Composable helper per verificare rapidamente se il permesso è concesso.
 * ✅ CORRETTO: Usa nuovo pattern
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequireCameraPermission(
    onPermissionDenied: () -> Unit = {}, // ✅ Callback normale
    content: @Composable () -> Unit
) {
    CameraPermissionHandler(
        onPermissionGranted = content,
        onPermissionDenied = onPermissionDenied,
        showRationaleContent = { CameraPermissionPlaceholder() }
    )
}

/**
 * Placeholder da mostrare quando il permesso non è disponibile.
 */
@Composable
private fun CameraPermissionPlaceholder() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = "Permesso camera non concesso",
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

// ✅ BONUS: Pattern per usage semplificato
@Composable
fun CameraPermissionWrapper(
    onPermissionDenied: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val cameraPermissionState = rememberCameraPermissionState()

    when (cameraPermissionState) {
        is CameraPermissionStatus.Granted -> {
            content()
        }
        is CameraPermissionStatus.Denied -> {
            // Auto-richiedi permesso
            LaunchedEffect(Unit) {
                cameraPermissionState.requestPermission()
            }
            CameraPermissionPlaceholder()
        }
        is CameraPermissionStatus.DeniedWithRationale -> {
            // Mostra rationale automaticamente
            LaunchedEffect(Unit) {
                cameraPermissionState.requestPermission()
            }
            CameraPermissionPlaceholder()
        }
    }
}