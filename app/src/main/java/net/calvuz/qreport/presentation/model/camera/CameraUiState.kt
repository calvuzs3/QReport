package net.calvuz.qreport.presentation.model.camera

import net.calvuz.qreport.domain.model.photo.PhotoPerspective
import net.calvuz.qreport.domain.model.photo.PhotoResolution

/**
 * Stato UI per la camera screen.
 */
data class CameraUiState(
    val isInitialized: Boolean = false,
    val isInitializing: Boolean = false,
    val isCapturing: Boolean = false,
    val hasFlash: Boolean = false,
    val flashMode: Int = 0,
    val zoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val error: String? = null,
    val captureSuccess: Boolean = false,
    val permissionGranted: Boolean = false,
    val showPermissionRationale: Boolean = false,
    val selectedPerspective: PhotoPerspective = PhotoPerspective.OVERVIEW,
    val selectedResolution: PhotoResolution = PhotoResolution.HIGH
) {
    val canCapture: Boolean get() = isInitialized && !isCapturing
    val showZoomControls: Boolean get() = maxZoomRatio > 1f
}