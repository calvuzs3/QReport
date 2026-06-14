package net.calvuz.qreport.photo.presentation.ui

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import net.calvuz.qreport.R

/**
 * Schermata principale della camera per catturare foto.
 * ✅ CORRETTO: Ora usa CameraViewModel dedicato invece di PhotoViewModel
 */
@Suppress("ParamsComparedByRef")@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    checkItemId: String,
    onPhotoSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel() // ✅ CORREZIONE: CameraViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraUiState by viewModel.cameraUiState.collectAsStateWithLifecycle()

    // Gestione permessi camera
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Stato per tenere traccia del PreviewView
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // ✅ CORREZIONE: Inizializza camera quando permessi sono concessi e preview è ready
    LaunchedEffect(cameraPermissionState.status.isGranted, previewView) {
        if (cameraPermissionState.status.isGranted && previewView != null) {
            viewModel.initializeCamera(lifecycleOwner, previewView!!)
        }
    }

    // Mostra success e torna indietro
    LaunchedEffect(cameraUiState.captureSuccess) {
        if (cameraUiState.captureSuccess) {
            onPhotoSaved()
            viewModel.resetCaptureSuccess()
        }
    }

    when {
        cameraPermissionState.status.isGranted -> {
            CameraContent(
                cameraUiState = cameraUiState,
                onCapturePhoto = { viewModel.capturePhoto(checkItemId) },
                onSetFlashMode = viewModel::setFlashMode,
                onSetZoomRatio = viewModel::setZoomRatio,
                onNavigateBack = onNavigateBack,
                onClearError = viewModel::clearError,
                onPreviewReady = { previewView = it }, // ✅ NUOVO: Callback per PreviewView
                modifier = modifier
            )
        }
        cameraPermissionState.status.shouldShowRationale -> {
            CameraPermissionRationale(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onNavigateBack = onNavigateBack,
                modifier = modifier
            )
        }
        else -> {
            CameraPermissionRequest(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onNavigateBack = onNavigateBack,
                modifier = modifier
            )
        }
    }
}

/**
 * Contenuto principale della camera quando i permessi sono concessi.
 * ✅ CORRETTO: Aggiunto callback per PreviewView ready
 */
@Composable
private fun CameraContent(
    cameraUiState: CameraUiState,
    onCapturePhoto: () -> Unit,
    onSetFlashMode: (Int) -> Unit,
    onSetZoomRatio: (Float) -> Unit,
    onNavigateBack: () -> Unit,
    onClearError: () -> Unit,
    onPreviewReady: (PreviewView) -> Unit, // ✅ NUOVO: Callback per PreviewView
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ✅ CORREZIONE: Camera Preview con callback
        AndroidView(
            factory = { context ->
                PreviewView(context).also { previewView ->
                    onPreviewReady(previewView) // ✅ Notifica quando preview è pronto
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Controls
        CameraTopControls(
            flashMode = cameraUiState.flashMode,
            hasFlash = cameraUiState.hasFlash,
            onFlashModeChanged = onSetFlashMode,
            onNavigateBack = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

        // Zoom Controls
        if (cameraUiState.showZoomControls) {
            ZoomControls(
                currentZoom = cameraUiState.zoomRatio,
                minZoom = cameraUiState.minZoomRatio,
                maxZoom = cameraUiState.maxZoomRatio,
                onZoomChanged = onSetZoomRatio,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
            )
        }

        // Bottom Controls
        CameraBottomControls(
            isCapturing = cameraUiState.isCapturing,
            canCapture = cameraUiState.canCapture,
            onCapturePhoto = onCapturePhoto,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // Loading Overlay
        if (cameraUiState.isInitializing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.photo_camera_initializing),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
        }

        // Error Snackbar
        cameraUiState.error?.let { error ->
            LaunchedEffect(error) {
                delay(3000)
                onClearError()
            }

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error.asString(),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Controlli superiori della camera (flash, back button).
 */
@Composable
private fun CameraTopControls(
    flashMode: Int,
    hasFlash: Boolean,
    onFlashModeChanged: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = stringResource(R.string.photo_camera_cd_back),
                tint = Color.White
            )
        }

        // Flash Control
        if (hasFlash) {
            IconButton(
                onClick = {
                    val nextMode = when (flashMode) {
                        FlashMode.OFF -> FlashMode.AUTO
                        FlashMode.AUTO -> FlashMode.ON
                        else -> FlashMode.OFF
                    }
                    onFlashModeChanged(nextMode)
                },
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                val icon = when (flashMode) {
                    FlashMode.OFF -> Icons.Default.FlashOff
                    FlashMode.ON -> Icons.Default.FlashOn
                    else -> Icons.Default.FlashAuto
                }
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.photo_camera_cd_flash),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Controlli zoom laterali.
 */
@Composable
private fun ZoomControls(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = {
                val newZoom = (currentZoom + 0.5f).coerceAtMost(maxZoom)
                onZoomChanged(newZoom)
            }
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = stringResource(R.string.photo_camera_cd_zoom_in),
                tint = Color.White
            )
        }

        Text(
            text = "${(currentZoom * 10).toInt() / 10f}x",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        IconButton(
            onClick = {
                val newZoom = (currentZoom - 0.5f).coerceAtLeast(minZoom)
                onZoomChanged(newZoom)
            }
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOut,
                contentDescription = stringResource(R.string.photo_camera_cd_zoom_out),
                tint = Color.White
            )
        }
    }
}

/**
 * Controlli inferiori della camera (pulsante scatto).
 */
@Composable
private fun CameraBottomControls(
    isCapturing: Boolean,
    canCapture: Boolean,
    onCapturePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Capture Button
        FloatingActionButton(
            onClick = onCapturePhoto,
            modifier = Modifier.size(80.dp),
            containerColor = if (canCapture) MaterialTheme.colorScheme.primary else Color.Gray,
            contentColor = Color.White
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.photo_camera_cd_capture),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Schermata di richiesta permessi camera.
 */
@Composable
private fun CameraPermissionRequest(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.photo_camera_permission_required_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.photo_camera_permission_required_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.photo_camera_permission_grant))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

/**
 * Schermata rationale per i permessi camera.
 */
@Composable
private fun CameraPermissionRationale(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.photo_camera_permission_rationale_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.photo_camera_permission_rationale_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.photo_camera_permission_retry))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.photo_camera_permission_back))
        }
    }
}