package net.calvuz.qreport.ti.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import timber.log.Timber
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Signature pad component for collecting digital signatures
 * Compatible with SignatureCollectionDialog.kt
 */
@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureChanged: (Boolean) -> Unit = {},
    onPathsChanged: (List<Path>) -> Unit = {},
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 3.0f,
    backgroundColor: Color = Color.White
) {
    var paths by remember { mutableStateOf<List<Path>>(emptyList()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    // Notify parent about signature changes
    LaunchedEffect(paths) {
        val hasSignature = paths.isNotEmpty()
        onSignatureChanged(hasSignature)
        onPathsChanged(paths)

        if (hasSignature) {
            Timber.d("SignaturePad: Signature updated - ${paths.size} paths")
        }
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newPath = Path()
                            newPath.moveTo(offset.x, offset.y)
                            currentPath = newPath

                            Timber.v("SignaturePad: Started new path at (${offset.x.toInt()}, ${offset.y.toInt()})")
                        },
                        onDrag = { change, _ ->
                            currentPath?.let { path ->
                                path.lineTo(change.position.x, change.position.y)
                                // Trigger recomposition by updating paths
                                paths = paths.toMutableList().apply {
                                    if (isNotEmpty()) {
                                        removeLastOrNull()
                                    }
                                    add(path)
                                }
                            }
                        },
                        onDragEnd = {
                            currentPath?.let { completedPath ->
                                paths = paths + completedPath
                                currentPath = null

                                Timber.d("SignaturePad: Completed path - total paths: ${paths.size}")
                            }
                        }
                    )
                }
        ) {
            // Clear background
            drawRect(
                color = backgroundColor,
                topLeft = Offset.Zero,
                size = size
            )

            // Draw all completed paths
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Draw current path being drawn
            currentPath?.let { path ->
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Placeholder text when empty
        if (paths.isEmpty() && currentPath == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Disegna qui la tua firma",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Convert paths to ImageBitmap for signature storage
 * Required by SignatureCollectionDialog.kt
 */
fun pathsToBitmap(
    paths: List<Path>,
    width: Int,
    height: Int,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 3.0f
): ImageBitmap {
    return try {
        // Create Android Bitmap
        val androidBitmap = android.graphics.Bitmap.createBitmap(
            width,
            height,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(androidBitmap)

        // Create Paint for background
        val backgroundPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }

        // Create Paint for strokes
        val strokePaint = android.graphics.Paint().apply {
            color = strokeColor.toArgb()
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }

        // Clear background to white
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw all paths
        paths.forEach { path ->
            canvas.drawPath(path.asAndroidPath(), strokePaint)
        }

        // Convert Android Bitmap to ImageBitmap
        val imageBitmap = androidBitmap.asImageBitmap()

        Timber.d("SignaturePad: Created signature bitmap ${width}x${height} with ${paths.size} paths")
        imageBitmap

    } catch (e: Exception) {
        Timber.e(e, "SignaturePad: Error creating bitmap from paths")
        // Return empty white bitmap as fallback
        try {
            val fallbackAndroidBitmap = android.graphics.Bitmap.createBitmap(
                width,
                height,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val fallbackCanvas = android.graphics.Canvas(fallbackAndroidBitmap)
            val whitePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
            }
            fallbackCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), whitePaint)
            fallbackAndroidBitmap.asImageBitmap()
        } catch (fallbackError: Exception) {
            Timber.e(fallbackError, "SignaturePad: Error creating fallback bitmap")
            // Last resort: empty ImageBitmap
            ImageBitmap(width, height)
        }
    }
}