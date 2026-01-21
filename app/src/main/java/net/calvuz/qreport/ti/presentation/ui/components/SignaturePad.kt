package net.calvuz.qreport.ti.presentation.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import timber.log.Timber
import java.io.File

/**
 * Global state holder for signature capture
 * Stores the current signature strokes for bitmap generation
 */
private object SignatureCaptureState {
    var currentStrokes: List<List<Offset>> = emptyList()
    var canvasSize: IntSize = IntSize(400, 200)

    fun clear() {
        currentStrokes = emptyList()
    }
}

/**
 * Signature pad component for collecting digital signatures
 *
 * FIXED VERSION:
 * - Properly tracks canvas size for coordinate scaling
 * - Uses point lists instead of mutable Path objects
 * - Correctly scales coordinates when converting to bitmap
 */
@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureChanged: (Boolean) -> Unit = {},
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 4.0f,
    backgroundColor: Color = Color.White
) {
    // Track canvas size for proper scaling during bitmap generation
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Store strokes as lists of points for clean state management
    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Update global state for bitmap capture
    LaunchedEffect(strokes, canvasSize) {
        SignatureCaptureState.currentStrokes = strokes
        SignatureCaptureState.canvasSize = canvasSize

        val hasSignature = strokes.isNotEmpty()
        onSignatureChanged(hasSignature)

        if (hasSignature) {
            Timber.d("SignaturePad: ${strokes.size} strokes, canvas: ${canvasSize.width}x${canvasSize.height}")
        }
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .onSizeChanged { size ->
                canvasSize = size
                Timber.v("SignaturePad: Canvas size: ${size.width}x${size.height}")
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStroke = listOf(offset)
                            Timber.v("SignaturePad: Stroke started at (${offset.x.toInt()}, ${offset.y.toInt()})")
                        },
                        onDrag = { change, _ ->
                            currentStroke = currentStroke + change.position
                        },
                        onDragEnd = {
                            if (currentStroke.size >= 2) {
                                strokes = strokes + listOf(currentStroke)
                                Timber.d("SignaturePad: Stroke completed with ${currentStroke.size} points, total: ${strokes.size} strokes")
                            }
                            currentStroke = emptyList()
                        }
                    )
                }
        ) {
            // Draw background
            drawRect(
                color = backgroundColor,
                topLeft = Offset.Zero,
                size = size
            )

            // Draw completed strokes
            strokes.forEach { points ->
                if (points.size >= 2) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { point ->
                            lineTo(point.x, point.y)
                        }
                    }
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

            // Draw current stroke being drawn
            if (currentStroke.size >= 2) {
                val path = Path().apply {
                    moveTo(currentStroke.first().x, currentStroke.first().y)
                    currentStroke.drop(1).forEach { point ->
                        lineTo(point.x, point.y)
                    }
                }
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
        if (strokes.isEmpty() && currentStroke.isEmpty()) {
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
 * Capture current signature as ImageBitmap
 *
 * This function reads from the global SignatureCaptureState and properly
 * scales the coordinates from canvas size to target bitmap size.
 *
 * @param width Target bitmap width
 * @param height Target bitmap height
 * @param strokeColor Color for the signature strokes
 * @param strokeWidth Base stroke width (will be scaled)
 * @return ImageBitmap with the signature, or null if no signature
 */
fun captureSignatureBitmap(
    width: Int = 600,
    height: Int = 300,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 4.0f
): ImageBitmap? {
    val strokes = SignatureCaptureState.currentStrokes
    val sourceSize = SignatureCaptureState.canvasSize

    if (strokes.isEmpty()) {
        Timber.w("captureSignatureBitmap: No strokes to capture")
        return null
    }

    if (sourceSize.width <= 0 || sourceSize.height <= 0) {
        Timber.e("captureSignatureBitmap: Invalid source size: $sourceSize")
        return null
    }

    return try {
        // Calculate scale factors
        val scaleX = width.toFloat() / sourceSize.width.toFloat()
        val scaleY = height.toFloat() / sourceSize.height.toFloat()

        Timber.d("captureSignatureBitmap: Converting ${strokes.size} strokes, " +
                "source: ${sourceSize.width}x${sourceSize.height}, " +
                "target: ${width}x${height}, " +
                "scale: %.2fx%.2f".format(scaleX, scaleY))

        // Create Android Bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Fill background with white
        val backgroundPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Create stroke paint with scaled width
        val scaledStrokeWidth = strokeWidth * ((scaleX + scaleY) / 2f)
        val strokePaint = android.graphics.Paint().apply {
            color = strokeColor.toArgb()
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = scaledStrokeWidth
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }

        // Draw each stroke with scaled coordinates
        strokes.forEach { points ->
            if (points.size >= 2) {
                val path = android.graphics.Path()

                // Scale first point
                val firstPoint = points.first()
                path.moveTo(firstPoint.x * scaleX, firstPoint.y * scaleY)

                // Scale remaining points
                points.drop(1).forEach { point ->
                    path.lineTo(point.x * scaleX, point.y * scaleY)
                }

                canvas.drawPath(path, strokePaint)
            }
        }

        val imageBitmap = bitmap.asImageBitmap()
        Timber.d("captureSignatureBitmap: Successfully created ${width}x${height} bitmap")

        // Clear state after successful capture
        SignatureCaptureState.clear()

        imageBitmap

    } catch (e: Exception) {
        Timber.e(e, "captureSignatureBitmap: Error creating bitmap")
        null
    }
}

/**
 * Composable to display a saved signature from file path
 */
@Composable
fun SignaturePreview(
    signaturePath: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "Firma digitale"
) {
    var bitmap by remember(signaturePath) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(signaturePath) {
        isLoading = true
        hasError = false

        try {
            if (signaturePath.isNotEmpty()) {
                val file = File(signaturePath)
                if (file.exists()) {
                    val androidBitmap = BitmapFactory.decodeFile(signaturePath)
                    if (androidBitmap != null) {
                        bitmap = androidBitmap.asImageBitmap()
                        Timber.d("SignaturePreview: Loaded signature from $signaturePath")
                    } else {
                        hasError = true
                        Timber.w("SignaturePreview: Failed to decode bitmap from $signaturePath")
                    }
                } else {
                    hasError = true
                    Timber.w("SignaturePreview: File not found: $signaturePath")
                }
            } else {
                hasError = true
            }
        } catch (e: Exception) {
            Timber.e(e, "SignaturePreview: Error loading signature")
            hasError = true
        } finally {
            isLoading = false
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                hasError -> {
                    Text(
                        text = "Firma non disponibile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                bitmap != null -> {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Card showing signature status with optional preview
 */
@Composable
fun SignatureStatusCard(
    title: String,
    hasSignature: Boolean,
    signaturePath: String,
    onCollectSignature: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (hasSignature)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasSignature) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Show signature preview if available
            if (hasSignature && signaturePath.isNotEmpty()) {
                SignaturePreview(
                    signaturePath = signaturePath,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Button to collect/recollect signature
            OutlinedButton(
                onClick = onCollectSignature,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (hasSignature) "Nuova firma" else "Raccogli firma"
                )
            }
        }
    }
}