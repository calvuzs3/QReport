package net.calvuz.qreport.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.calvuz.qreport.domain.model.file.FileManager
import net.calvuz.qreport.domain.model.camera.CameraSettings
import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.photo.PhotoMetadata
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import net.calvuz.qreport.domain.model.photo.PhotoLocation
import net.calvuz.qreport.presentation.screen.photo.ImageInfo
import java.io.InputStream

/**
 * ✅ COMPATIBILE: PhotoStorageManager per PhotoMetadata ATTUALE (senza width/height)
 *
 * STRATEGIA: Salva width/height negli EXIF data come fallback
 * Le dimensioni sono accessibili via extractImageMetadata() quando necessario
 */
@Singleton
class PhotoStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: FileManager
) {

    companion object {
        private const val THUMBNAILS_SUBDIR = "thumbnails"
        private const val THUMBNAIL_SIZE = 150
        private const val THUMBNAIL_QUALITY = 80

        // Chiavi EXIF custom per dimensioni
        private const val EXIF_WIDTH_KEY = "ImageWidth"
        private const val EXIF_HEIGHT_KEY = "ImageHeight"
    }

    private val photosDir: String by lazy { fileManager.getPhotosDirectory() }

    private val thumbnailsDir: File by lazy {
        File(photosDir, THUMBNAILS_SUBDIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * ✅ COMPATIBILE con PhotoMetadata ATTUALE (senza width/height diretti)
     */
    suspend fun savePhoto(
        checkItemId: String,
        imageUri: Uri,
        caption: String = "",
        cameraSettings: CameraSettings = CameraSettings.default(),
        orderIndex: Int = 0
    ): PhotoSaveResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔄 Salvando foto per checkItem: $checkItemId")

            // 1. Crea file path usando FileManager
            val photoFilePath = fileManager.createPhotoFile(checkItemId)
            val photoFile = File(photoFilePath)

            // 2. Salva foto con correzione orientamento
            val photoSaved = savePhotoWithOrientationCorrection(
                imageUri,
                photoFile,
                cameraSettings
            )

            if (!photoSaved) {
                return@withContext PhotoSaveResult.Error("Impossibile salvare la foto originale")
            }

            // 3. Genera thumbnail
            val thumbnailFileName = "thumb_${photoFile.name}"
            val thumbnailFile = File(thumbnailsDir, thumbnailFileName)
            val thumbnailSaved = generateThumbnailWithCorrectOrientation(photoFile, thumbnailFile, cameraSettings)

            // 4. ✅ Estrai metadati immagine (ora UTILIZZATI negli EXIF)
            val imageMetadata = extractImageMetadata(photoFile)
            Timber.d("📏 Dimensioni foto: ${imageMetadata.width}x${imageMetadata.height}")

            // 5. ✅ COMPATIBILE: Crea PhotoMetadata usando la struttura ATTUALE
            val photoMetadata = PhotoMetadata(
                // EXIF data includono anche le dimensioni per reference
                exifData = buildExifDataMap(photoFile, imageMetadata),

                // Prospettiva foto
                perspective = cameraSettings.perspective,

                // GPS se abilitato
                gpsLocation = if (cameraSettings.enableGpsTagging) {
                    extractGpsLocation(photoFile)
                } else null,

                // Timestamp
                timestamp = Clock.System.now(),

                // File size
                fileSize = fileManager.getFileSize(photoFilePath),

                // Risoluzione target
                resolution = cameraSettings.resolution,

                // Impostazioni camera
                cameraSettings = cameraSettings
            )

            // 6. ✅ Crea Photo usando il modello reale
            val photo = Photo(
                id = generatePhotoId(),
                checkItemId = checkItemId,
                fileName = photoFile.name,
                filePath = photoFilePath,
                thumbnailPath = if (thumbnailSaved) thumbnailFile.absolutePath else null,
                caption = caption,
                takenAt = Clock.System.now(),
                fileSize = fileManager.getFileSize(photoFilePath),
                orderIndex = orderIndex,
                metadata = photoMetadata
            )

            Timber.d("✅ Foto salvata: ${photo.filePath}")
            Timber.d("✅ Dimensioni (in EXIF): ${imageMetadata.width}x${imageMetadata.height}")
            Timber.d("✅ Thumbnail: ${photo.thumbnailPath}")

            PhotoSaveResult.Success(photo)

        } catch (e: Exception) {
            Timber.e(e, "Errore durante il salvataggio foto")
            PhotoSaveResult.Error("Errore salvataggio: ${e.message}")
        }
    }

    /**
     * ✅ NUOVO: Costruisce mappa EXIF includendo dimensioni immagine
     */
    private fun buildExifDataMap(photoFile: File, imageMetadata: ImageMetadata): Map<String, String> {
        val exifMap = mutableMapOf<String, String>()

        try {
            val exif = ExifInterface(photoFile.absolutePath)

            // Aggiungi EXIF standard
            exif.getAttribute(ExifInterface.TAG_MAKE)?.let {
                exifMap[ExifInterface.TAG_MAKE] = it
            }
            exif.getAttribute(ExifInterface.TAG_MODEL)?.let {
                exifMap[ExifInterface.TAG_MODEL] = it
            }
            exif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
                exifMap[ExifInterface.TAG_DATETIME] = it
            }
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1).let {
                exifMap[ExifInterface.TAG_ORIENTATION] = it.toString()
            }

            // ✅ IMPORTANTE: Aggiungi dimensioni dell'immagine
            exifMap[EXIF_WIDTH_KEY] = imageMetadata.width.toString()
            exifMap[EXIF_HEIGHT_KEY] = imageMetadata.height.toString()

        } catch (e: Exception) {
            Timber.w(e, "Errore lettura EXIF, usando solo dimensioni")
            // Fallback: almeno le dimensioni
            exifMap[EXIF_WIDTH_KEY] = imageMetadata.width.toString()
            exifMap[EXIF_HEIGHT_KEY] = imageMetadata.height.toString()
        }

        return exifMap.toMap()
    }

    /**
     * ✅ UTILITY: Estrai dimensioni da PhotoMetadata (via EXIF)
     */
    fun getPhotoDimensions(photo: Photo): Pair<Int, Int> {
        val width = photo.metadata.exifData[EXIF_WIDTH_KEY]?.toIntOrNull() ?: 0
        val height = photo.metadata.exifData[EXIF_HEIGHT_KEY]?.toIntOrNull() ?: 0
        return Pair(width, height)
    }

    /**
     * ✅ UTILITY: Estrai dimensioni da file (runtime)
     */
    fun extractImageMetadata(photoFile: File): ImageMetadata {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(photoFile.absolutePath, options)

        return ImageMetadata(
            width = options.outWidth,
            height = options.outHeight
        )
    }

    // ... Resto dei metodi helper rimane uguale ...

    private suspend fun savePhotoWithOrientationCorrection(
        imageUri: Uri,
        destinationFile: File,
        cameraSettings: CameraSettings
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile("temp_photo", ".jpg")

            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext false

            val originalOrientation = getExifOrientation(tempFile.absolutePath)
            val rotationDegrees = exifOrientationToRotation(originalOrientation)

            Timber.d("📐 EXIF Orientation: $originalOrientation -> Rotazione: ${rotationDegrees}°")

            if (cameraSettings.autoCorrectOrientation && rotationDegrees != 0f) {
                Timber.d("🔄 Applicando correzione orientamento...")

                val originalBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                    ?: return@withContext false

                val rotatedBitmap = rotateBitmap(originalBitmap, rotationDegrees)

                val config = cameraSettings.cameraConfig
                val finalBitmap = if (rotatedBitmap.width > config.maxImageDimension ||
                    rotatedBitmap.height > config.maxImageDimension) {
                    resizeBitmap(rotatedBitmap, config.maxImageDimension, config.maxImageDimension)
                } else {
                    rotatedBitmap
                }

                FileOutputStream(destinationFile).use { outputStream ->
                    finalBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        config.effectiveJpegQuality,
                        outputStream
                    )
                }

                writeCorrectExif(tempFile.absolutePath, destinationFile.absolutePath, cameraSettings)

                originalBitmap.recycle()
                if (rotatedBitmap != finalBitmap) rotatedBitmap.recycle()
                finalBitmap.recycle()

                Timber.d("✅ Orientamento corretto applicato")

            } else {
                tempFile.copyTo(destinationFile, overwrite = true)

                if (originalOrientation != ExifInterface.ORIENTATION_NORMAL) {
                    Timber.w("⚠️ Orientamento non corretto, ma correzione disabilitata")
                }
            }

            tempFile.delete()

            val success = destinationFile.exists() && destinationFile.length() > 0
            Timber.d("📦 Foto salvata: $success (${destinationFile.length()} bytes)")

            return@withContext success

        } catch (e: Exception) {
            Timber.e(e, "❌ Errore correzione orientamento")
            return@withContext false
        }
    }

    private suspend fun generateThumbnailWithCorrectOrientation(
        originalFile: File,
        thumbnailFile: File,
        cameraSettings: CameraSettings
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext generateThumbnail(originalFile, thumbnailFile)
        } catch (e: Exception) {
            Timber.e(e, "❌ Errore generazione thumbnail")
            return@withContext false
        }
    }

    private fun getExifOrientation(filePath: String): Int {
        return try {
            val exif = ExifInterface(filePath)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            Timber.w(e, "Impossibile leggere orientamento EXIF")
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun exifOrientationToRotation(orientation: Int): Float {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        return bitmap.scale(newWidth, newHeight)
    }

    private fun writeCorrectExif(
        originalPath: String,
        destinationPath: String,
        cameraSettings: CameraSettings
    ) {
        try {
            val originalExif = ExifInterface(originalPath)
            val destinationExif = ExifInterface(destinationPath)

            destinationExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())

            originalExif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
                destinationExif.setAttribute(ExifInterface.TAG_DATETIME, it)
            }
            originalExif.getAttribute(ExifInterface.TAG_MAKE)?.let {
                destinationExif.setAttribute(ExifInterface.TAG_MAKE, it)
            }
            originalExif.getAttribute(ExifInterface.TAG_MODEL)?.let {
                destinationExif.setAttribute(ExifInterface.TAG_MODEL, it)
            }

            destinationExif.saveAttributes()

        } catch (e: Exception) {
            Timber.w(e, "Impossibile scrivere EXIF corretti")
        }
    }

    private fun extractGpsLocation(photoFile: File): PhotoLocation? {
        val exif = ExifInterface(photoFile.absolutePath)
        val latLong = FloatArray(2)
        val hasGps = exif.getLatLong(latLong)

        if (hasGps) {
            return PhotoLocation(
                latitude = latLong[0].toDouble(),
                longitude = latLong[1].toDouble(),
                altitude = exif.getAltitude(Double.NaN).takeIf { !it.isNaN() },
                accuracy = exif.getAttribute("GPSHPositioningError")?.toFloatOrNull()
            )
        }
        return null
    }

    private fun generateThumbnail(originalFile: File, thumbnailFile: File): Boolean {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(originalFile.absolutePath, options)

            val sampleSize = calculateInSampleSize(options, THUMBNAIL_SIZE, THUMBNAIL_SIZE)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }

            val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath, decodeOptions)
                ?: return false

            val thumbnailBitmap = createSquareThumbnail(originalBitmap, THUMBNAIL_SIZE)

            FileOutputStream(thumbnailFile).use { outputStream ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outputStream)
            }

            originalBitmap.recycle()
            thumbnailBitmap.recycle()

            val success = thumbnailFile.exists() && thumbnailFile.length() > 0
            Timber.d("Thumbnail generata: $success")
            success

        } catch (e: Exception) {
            Timber.e(e, "Errore generazione thumbnail")
            false
        }
    }

    private fun createSquareThumbnail(bitmap: Bitmap, size: Int): Bitmap {
        val minDimension = minOf(bitmap.width, bitmap.height)
        val scale = size.toFloat() / minDimension

        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()

        val scaledBitmap = bitmap.scale(scaledWidth, scaledHeight)

        val startX = (scaledWidth - size) / 2
        val startY = (scaledHeight - size) / 2

        return Bitmap.createBitmap(scaledBitmap, startX, startY, size, size)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun generatePhotoId(): String {
        return "photo_${Clock.System.now().epochSeconds}_${java.util.UUID.randomUUID().toString().take(8)}"
    }

    // Public utility methods
    suspend fun deletePhoto(photo: Photo): Boolean = withContext(Dispatchers.IO) {
        try {
            var deleted = true

            deleted = fileManager.deletePhotoFile(photo.filePath) && deleted

            photo.thumbnailPath?.let { thumbnailPath ->
                val thumbnailFile = File(thumbnailPath)
                if (thumbnailFile.exists()) {
                    deleted = thumbnailFile.delete() && deleted
                }
            }

            Timber.d("Foto eliminata: $deleted")
            deleted

        } catch (e: Exception) {
            Timber.e(e, "Errore eliminazione foto")
            false
        }
    }

    fun photoExists(photo: Photo): Boolean {
        val photoExists = File(photo.filePath).exists()
        val thumbnailExists = photo.thumbnailPath?.let { File(it).exists() } ?: true

        return photoExists && thumbnailExists
    }

    fun getStorageInfo(): StorageInfo {
        val photosDir = File(fileManager.getPhotosDirectory())
        val photosSize = photosDir.walkTopDown()
            .filter { it.isFile && !it.path.contains(THUMBNAILS_SUBDIR) }
            .map { it.length() }.sum()
        val thumbnailsSize = thumbnailsDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }.sum()
        val photoCount = photosDir.listFiles()
            ?.filter { it.isFile && !it.path.contains(THUMBNAILS_SUBDIR) }?.size ?: 0

        return StorageInfo(
            totalSize = photosSize + thumbnailsSize,
            photosSize = photosSize,
            thumbnailsSize = thumbnailsSize,
            photoCount = photoCount
        )
    }


    // ===== IMPORT METHODS =====

    /**
     * Importa una foto dalla galleria nel sistema di storage dell'app.
     * Segue lo stesso workflow di savePhoto() ma partendo da un URI esterno.
     *
     * @param checkItemId ID del check item di destinazione
     * @param sourceImageUri URI della foto dalla galleria
     * @param caption Descrizione della foto
     * @param cameraSettings Impostazioni che includono perspective e resolution
     * @param orderIndex Indice di ordinamento
     * @return PhotoSaveResult con la foto importata o errore
     */
    suspend fun importPhoto(
        checkItemId: String,
        sourceImageUri: Uri,
        caption: String = "",
        cameraSettings: CameraSettings = CameraSettings.default(),
        orderIndex: Int = 0
    ): PhotoSaveResult {
        return try {
            Timber.d("📥 IMPORT PHOTO START: $sourceImageUri")

            // 1. Valida l'URI sorgente
            if (!validateImageUri(sourceImageUri)) {
                return PhotoSaveResult.Error("URI immagine non valida o non accessibile")
            }

            // 2. Crea i path di destinazione
            val photoFilePath = fileManager.createPhotoFile(checkItemId)
            val photoFile = File(photoFilePath)

            // 3. Copia il file dalla galleria alla nostra directory
            val copyResult = copyImageFromUri(sourceImageUri, photoFile)
            if (!copyResult) {
                return PhotoSaveResult.Error("Errore durante la copia del file")
            }

            // 4. ✅ CORRETTO: Usa il metodo esistente per correzione orientamento
            // Crea un Uri temporaneo dal file copiato per usare il workflow esistente
            val tempUri = Uri.fromFile(photoFile)
            val tempCorrectedFile = File.createTempFile("corrected_", ".jpg", photoFile.parentFile)

            val correctionSuccess = savePhotoWithOrientationCorrection(
                tempUri,
                tempCorrectedFile,
                cameraSettings
            )

            if (correctionSuccess) {
                // Sostituisci il file originale con quello corretto
                tempCorrectedFile.copyTo(photoFile, overwrite = true)
                tempCorrectedFile.delete()
            }

            // 5. ✅ CORRETTO: Genera thumbnail con i parametri giusti
            val thumbnailFileName = "thumb_${photoFile.name}"
            val thumbnailFile = File(thumbnailsDir, thumbnailFileName)
            val thumbnailSaved = generateThumbnailWithCorrectOrientation(photoFile, thumbnailFile, cameraSettings)

            // 6. ✅ CORRETTO: Usa i metodi esistenti per metadati
            val imageMetadata = extractImageMetadata(photoFile)
            val photoMetadata = PhotoMetadata(
                exifData = buildExifDataMap(photoFile, imageMetadata),
                perspective = cameraSettings.perspective,
                gpsLocation = if (cameraSettings.enableGpsTagging) {
                    extractGpsLocation(photoFile)
                } else null,
                timestamp = Clock.System.now(),
                fileSize = photoFile.length(),
                resolution = cameraSettings.resolution,
                cameraSettings = cameraSettings
            )

            // 7. ✅ CORRETTO: Usa metodo esistente per creare Photo
            val photo = Photo(
                id = generatePhotoId(),
                checkItemId = checkItemId,
                fileName = photoFile.name,
                filePath = photoFile.absolutePath,
                thumbnailPath = if (thumbnailSaved) thumbnailFile.absolutePath else null,
                caption = caption,
                takenAt = Clock.System.now(),
                fileSize = photoFile.length(),
                orderIndex = orderIndex,
                metadata = photoMetadata
            )

            Timber.d("✅ IMPORT PHOTO SUCCESS: ${photo.fileName}")
            PhotoSaveResult.Success(photo)

        } catch (e: Exception) {
            Timber.e("❌ IMPORT PHOTO ERROR: ${e.message}")
            PhotoSaveResult.Error("Errore import foto: ${e.message}")
        }
    }

    /**
     * Ottieni prossimo orderIndex per un check item
     */
    suspend fun getNextOrderIndex(checkItemId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                // Conta le foto esistenti nella directory del check item
                val photosDir = File(fileManager.getPhotosDirectory())
                val checkItemFiles = photosDir.listFiles { file ->
                    file.name.contains(checkItemId) && file.extension in listOf("jpg", "jpeg", "png")
                }

                checkItemFiles?.size ?: 0
            } catch (e: Exception) {
                Timber.e("Error getting next order index: ${e.message}")
                0
            }
        }
    }

    /**
     * Copia un file da URI esterno al file system dell'app.
     */
    private suspend fun copyImageFromUri(sourceUri: Uri, destinationFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri)
                    ?: return@withContext false

                val outputStream = FileOutputStream(destinationFile)

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                destinationFile.exists() && destinationFile.length() > 0
            } catch (e: Exception) {
                Timber.e("❌ Error copying file from URI: ${e.message}")
                false
            }
        }
    }

    /**
     * Valida se un URI di immagine è accessibile e valido.
     */
    fun validateImageUri(imageUri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val isAccessible = inputStream != null
            inputStream?.close()

            // Verifica anche il MIME type
            val mimeType = context.contentResolver.getType(imageUri)
            val isValidImage = mimeType?.startsWith("image/") == true

            isAccessible && isValidImage
        } catch (e: Exception) {
            Timber.e("❌ Error validating URI: ${e.message}")
            false
        }
    }

    /**
     * Estrae informazioni preliminari da un URI di immagine.
     */
    suspend fun extractImageInfo(imageUri: Uri): ImageInfo {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: throw Exception("Impossibile accedere all'immagine")

                // Usa BitmapFactory.Options per ottenere dimensioni senza caricare la bitmap
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                inputStream.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

                val mimeType = context.contentResolver.getType(imageUri) ?: "image/unknown"
                val fileName = getFileNameFromUri(imageUri)
                val fileSize = getFileSizeFromUri(imageUri)

                ImageInfo(
                    width = options.outWidth,
                    height = options.outHeight,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    fileName = fileName
                )

            } catch (e: Exception) {
                Timber.e("❌ Error extracting image info: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Ottiene il nome del file dall'URI.
     */
    private fun getFileNameFromUri(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                it.getString(nameIndex) ?: "imported_image.jpg"
            } else {
                "imported_image.jpg"
            }
        } ?: "imported_image.jpg"
    }

    /**
     * Ottiene la dimensione del file dall'URI.
     */
    private fun getFileSizeFromUri(uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && it.moveToFirst()) {
                it.getLong(sizeIndex)
            } else {
                0L
            }
        } ?: 0L
    }

    /**
     * Crea un oggetto Photo da una foto importata.
     */
    private fun createPhotoFromImport(
        checkItemId: String,
        photoFile: File,
        thumbnailFile: File?,
        caption: String,
        metadata: PhotoMetadata,
        orderIndex: Int
    ): Photo {
        return Photo(
            id = "", // Verrà impostato dal repository
            checkItemId = checkItemId,
            fileName = photoFile.name,
            filePath = photoFile.absolutePath,
            thumbnailPath = thumbnailFile?.absolutePath,
            caption = caption,
            takenAt = Clock.System.now(), // ✅ Timestamp import
            fileSize = photoFile.length(),
            orderIndex = orderIndex,
            metadata = metadata
        )
    }


}

/**
 * Risultato del salvataggio foto.
 */
sealed class PhotoSaveResult {
    data class Success(val photo: Photo) : PhotoSaveResult()
    data class Error(val message: String) : PhotoSaveResult()
}

/**
 * Metadati immagine base (dimensioni reali).
 */
data class ImageMetadata(
    val width: Int,
    val height: Int
)

/**
 * Info storage utilizzato.
 */
data class StorageInfo(
    val totalSize: Long,
    val photosSize: Long,
    val thumbnailsSize: Long,
    val photoCount: Int
)