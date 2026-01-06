package net.calvuz.qreport.export.data.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.photo.domain.model.ImageProcessor
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.graphics.scale

/**
 * Implementazione semplice di ImageProcessor
 *
 * Features:
 * - Ottimizzazione qualità e ridimensionamento foto
 * - Aggiunta watermark testuale
 * - Gestione errori robusta
 * - Performance ottimizzate per export
 */
@Singleton
class ImageProcessorImpl @Inject constructor() : ImageProcessor {

    override suspend fun optimizePhoto(
        sourceFile: File,
        targetFile: File,
        quality: Int,
        maxWidth: Int?,
        maxHeight: Int?
    ): Long = withContext(Dispatchers.IO) {

        try {
            if (!sourceFile.exists()) {
                throw IllegalArgumentException("Source file non esiste: ${sourceFile.absolutePath}")
            }

            // Decodifica immagine sorgente
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            // Calcola dimensioni target
            val (targetWidth, targetHeight) = calculateTargetDimensions(
                originalWidth, originalHeight, maxWidth, maxHeight
            )

            // Calcola sample size per ottimizzare memoria
            val sampleSize = calculateSampleSize(
                originalWidth, originalHeight, targetWidth, targetHeight
            )

            // Decodifica con sample size ottimizzato
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Usa meno memoria per export
            }

            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
                ?: throw IllegalStateException("Impossibile decodificare immagine")

            // Ridimensiona se necessario
            val finalBitmap = if (bitmap.width != targetWidth || bitmap.height != targetHeight) {
                val resized = bitmap.scale(targetWidth, targetHeight)
                if (resized != bitmap) bitmap.recycle()
                resized
            } else {
                bitmap
            }

            // Salva con qualità specificata
            targetFile.parentFile?.mkdirs()

            FileOutputStream(targetFile).use { out ->
                val success = finalBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality.coerceIn(1, 100),
                    out
                )
                if (!success) {
                    throw IllegalStateException("Fallito compressione JPEG")
                }
            }

            finalBitmap.recycle()

            val resultSize = targetFile.length()
            Timber.d("Foto ottimizzata: ${sourceFile.name} (${formatFileSize(sourceFile.length())}) → ${targetFile.name} (${formatFileSize(resultSize)})")

            resultSize

        } catch (e: Exception) {
            Timber.e(e, "Errore ottimizzazione foto: ${sourceFile.absolutePath}")

            // Fallback: copia file originale se ottimizzazione fallisce
            if (sourceFile != targetFile) {
                sourceFile.copyTo(targetFile, overwrite = true)
                targetFile.length()
            } else {
                sourceFile.length()
            }
        }
    }

    @Deprecated("Not used")
    override suspend fun addWatermark(
        sourceFile: File,
        targetFile: File,
        watermarkText: String
    ): Long = withContext(Dispatchers.IO) {

        try {
            if (!sourceFile.exists()) {
                throw IllegalArgumentException("Source file non esiste: ${sourceFile.absolutePath}")
            }

            // Decodifica immagine
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
                ?: throw IllegalStateException("Impossibile decodificare immagine")

            // Crea bitmap modificabile
            val watermarkedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()

            // Aggiungi watermark
            val canvas = Canvas(watermarkedBitmap)
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = calculateWatermarkTextSize(watermarkedBitmap.width)
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
                alpha = 200  // Semi-trasparente
                isAntiAlias = true
            }

            // Calcola posizione watermark (angolo in basso a destra)
            val textBounds = Rect()
            paint.getTextBounds(watermarkText, 0, watermarkText.length, textBounds)

            val x = watermarkedBitmap.width - textBounds.width() - 20f
            val y = watermarkedBitmap.height - 20f

            canvas.drawText(watermarkText, x, y, paint)

            // Salva risultato
            targetFile.parentFile?.mkdirs()

            FileOutputStream(targetFile).use { out ->
                val success = watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                if (!success) {
                    throw IllegalStateException("Fallito compressione JPEG con watermark")
                }
            }

            watermarkedBitmap.recycle()

            val resultSize = targetFile.length()
            Timber.d("Watermark aggiunto: ${sourceFile.name} → ${targetFile.name} (${formatFileSize(resultSize)})")

            resultSize

        } catch (e: Exception) {
            Timber.e(e, "Errore aggiunta watermark: ${sourceFile.absolutePath}")

            // Fallback: copia file originale
            sourceFile.copyTo(targetFile, overwrite = true)
            targetFile.length()
        }
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    private fun calculateTargetDimensions(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int?,
        maxHeight: Int?
    ): Pair<Int, Int> {

        if (maxWidth == null && maxHeight == null) {
            return originalWidth to originalHeight
        }

        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

        return when {
            maxWidth != null && maxHeight != null -> {
                // Rispetta entrambi i limiti mantenendo aspect ratio
                val widthRatio = maxWidth.toFloat() / originalWidth
                val heightRatio = maxHeight.toFloat() / originalHeight
                val ratio = minOf(widthRatio, heightRatio)

                ((originalWidth * ratio).toInt()) to ((originalHeight * ratio).toInt())
            }
            maxWidth != null -> {
                // Limita solo larghezza
                if (originalWidth <= maxWidth) {
                    originalWidth to originalHeight
                } else {
                    maxWidth to (maxWidth / aspectRatio).toInt()
                }
            }
            maxHeight != null -> {
                // Limita solo altezza
                if (originalHeight <= maxHeight) {
                    originalWidth to originalHeight
                } else {
                    (maxHeight * aspectRatio).toInt() to maxHeight
                }
            }
            else -> originalWidth to originalHeight
        }
    }

    private fun calculateSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var sampleSize = 1

        if (originalHeight > targetHeight || originalWidth > targetWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2

            // Calcola sample size che mantiene dimensioni >= target
            while ((halfHeight / sampleSize) >= targetHeight &&
                (halfWidth / sampleSize) >= targetWidth) {
                sampleSize *= 2
            }
        }

        return sampleSize
    }

    private fun calculateWatermarkTextSize(imageWidth: Int): Float {
        // Dimensione testo proporzionale alla larghezza immagine
        return when {
            imageWidth < 600 -> 24f
            imageWidth < 1200 -> 36f
            imageWidth < 2000 -> 48f
            else -> 64f
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)}KB"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))}MB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))}GB"
        }
    }
}