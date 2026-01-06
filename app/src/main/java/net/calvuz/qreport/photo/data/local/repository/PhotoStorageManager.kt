package net.calvuz.qreport.photo.data.local.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.photo.domain.model.CameraSettings
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.domain.model.PhotoMetadata
import net.calvuz.qreport.photo.domain.model.PhotoLocation
import net.calvuz.qreport.photo.presentation.ui.ImageInfo
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import net.calvuz.qreport.photo.domain.repository.PhotoFileRepository
import net.calvuz.qreport.photo.domain.repository.StorageInfo

/**
 * PhotoStorageManager - COMPLETE & CLEAN VERSION
 *
 * ✅ USES: PhotoFileRepository for all file operations
 * ✅ MAINTAINS: All business logic (EXIF, thumbnails, orientation)
 * ✅ NO LEGACY: Zero references to old FileManager
 */
@Singleton
class PhotoStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoFileRepo: PhotoFileRepository
) {

    companion object {
        private const val THUMBNAIL_SIZE = 150
        private const val THUMBNAIL_QUALITY = 80
        private const val EXIF_WIDTH_KEY = "ImageWidth"
        private const val EXIF_HEIGHT_KEY = "ImageHeight"
    }

    // ===== LAZY INITIALIZATION =====

    private val photosDir: String by lazy {
        runBlocking {
            when (val result = photoFileRepo.getPhotosDirectory()) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    Timber.e("Failed to get photos directory")
                    context.filesDir.resolve("photos").absolutePath
                }
            }
        }
    }

    private val thumbnailsDir: File by lazy {
        runBlocking {
            when (val result = photoFileRepo.getThumbnailsDirectory()) {
                is QrResult.Success -> File(result.data)
                is QrResult.Error -> {
                    Timber.e("Failed to get thumbnails directory")
                    File(photosDir, "thumbnails").apply { mkdirs() }
                }
            }
        }
    }

    // ===== MAIN PHOTO OPERATIONS =====

    /**
     * Save photo with all metadata, thumbnails, and orientation correction
     */
    suspend fun savePhoto(
        checkItemId: String,
        imageUri: Uri,
        caption: String = "",
        cameraSettings: CameraSettings = CameraSettings.default(),
        orderIndex: Int = 0
    ): PhotoSaveResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔄 Saving photo for checkItem: $checkItemId")

            // 1. Create photo file path
            val photoFilePathResult = photoFileRepo.createPhotoFilePath(checkItemId)
            val photoFilePath = when (photoFilePathResult) {
                is QrResult.Success -> photoFilePathResult.data
                is QrResult.Error -> return@withContext PhotoSaveResult.Error("Cannot create photo path")
            }
            val photoFile = File(photoFilePath)

            // 2. Save photo with orientation correction
            val photoSaved = savePhotoWithOrientationCorrection(imageUri, photoFile, cameraSettings)
            if (!photoSaved) {
                return@withContext PhotoSaveResult.Error("Failed to save original photo")
            }

            // 3. Create thumbnail
            val thumbnailPathResult = photoFileRepo.createThumbnailFilePath(photoFile.name)
            val thumbnailFile = when (thumbnailPathResult) {
                is QrResult.Success -> File(thumbnailPathResult.data)
                is QrResult.Error -> null
            }

            val thumbnailSaved = thumbnailFile?.let {
                generateThumbnailWithCorrectOrientation(photoFile, it, cameraSettings)
            } == true

            // 4. Extract image metadata
            val imageMetadata = extractImageMetadata(photoFile)

            // 5. Get file size
            val fileSizeResult = photoFileRepo.getPhotoFileSize(photoFilePath)
            val fileSize = when (fileSizeResult) {
                is QrResult.Success -> fileSizeResult.data
                is QrResult.Error -> photoFile.length()
            }

            // 6. Create PhotoMetadata
            val photoMetadata = PhotoMetadata(
                exifData = buildExifDataMap(photoFile, imageMetadata),
                perspective = cameraSettings.perspective,
                gpsLocation = if (cameraSettings.enableGpsTagging) {
                    extractGpsLocation(photoFile)
                } else null,
                timestamp = Clock.System.now(),
                fileSize = fileSize,
                resolution = cameraSettings.resolution,
                cameraSettings = cameraSettings
            )

            // 7. Create Photo object
            val photo = Photo(
                id = generatePhotoId(),
                checkItemId = checkItemId,
                fileName = photoFile.name,
                filePath = photoFilePath,
                thumbnailPath = thumbnailFile?.absolutePath,
                caption = caption,
                takenAt = Clock.System.now(),
                fileSize = fileSize,
                orderIndex = orderIndex,
                metadata = photoMetadata
            )

            Timber.d("✅ Photo saved: ${photo.filePath}")
            PhotoSaveResult.Success(photo)

        } catch (e: Exception) {
            Timber.e(e, "Photo save failed")
            PhotoSaveResult.Error("Save error: ${e.message}")
        }
    }

    /**
     * Import photo from external URI
     */
    suspend fun importPhotoFromUri(
        checkItemId: String,
        imageUri: Uri,
        caption: String = "",
        orderIndex: Int? = null
    ): PhotoSaveResult {
        return try {
            val actualOrderIndex = orderIndex ?: getNextOrderIndex(checkItemId)
            savePhoto(checkItemId, imageUri, caption, CameraSettings.default(), actualOrderIndex)
        } catch (e: Exception) {
            Timber.e("Photo import failed: ${e.message}")
            PhotoSaveResult.Error("Import error: ${e.message}")
        }
    }

    /**
     * Get next order index for checkItem photos
     */
    suspend fun getNextOrderIndex(checkItemId: String): Int {
        return try {
            when (val result = photoFileRepo.listCheckItemPhotos(checkItemId)) {
                is QrResult.Success -> result.data.size
                is QrResult.Error -> 0
            }
        } catch (e: Exception) {
            Timber.e("Error getting next order index: ${e.message}")
            0
        }
    }

    /**
     * Delete photo and its thumbnail
     */
    suspend fun deletePhoto(photoFilePath: String, thumbnailPath: String? = null): Boolean {
        return try {
            when (photoFileRepo.deletePhotoFiles(photoFilePath, thumbnailPath)) {
                is QrResult.Success -> {
                    Timber.d("Photo deleted: $photoFilePath")
                    true
                }
                is QrResult.Error -> {
                    Timber.e("Failed to delete photo: $photoFilePath")
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Delete photo error")
            false
        }
    }

    // ===== PHOTO UTILITIES =====

    /**
     * Extract photo dimensions from saved photo
     */
    fun getPhotoDimensions(photo: Photo): Pair<Int, Int> {
        val width = photo.metadata.exifData[EXIF_WIDTH_KEY]?.toIntOrNull() ?: 0
        val height = photo.metadata.exifData[EXIF_HEIGHT_KEY]?.toIntOrNull() ?: 0
        return Pair(width, height)
    }

    /**
     * Extract image metadata (dimensions) from file
     */
    fun extractImageMetadata(photoFile: File): ImageMetadata {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoFile.absolutePath, options)
        return ImageMetadata(width = options.outWidth, height = options.outHeight)
    }

    /**
     * Validate image URI accessibility and format
     */
    fun validateImageUri(imageUri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val isAccessible = inputStream != null
            inputStream?.close()

            val mimeType = context.contentResolver.getType(imageUri)
            val isValidImage = mimeType?.startsWith("image/") == true

            isAccessible && isValidImage
        } catch (e: Exception) {
            Timber.e("Error validating URI: ${e.message}")
            false
        }
    }

    /**
     * Extract image info from URI without saving
     */
    suspend fun extractImageInfo(imageUri: Uri): ImageInfo = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw Exception("Cannot access image")

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            inputStream.use { BitmapFactory.decodeStream(it, null, options) }

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
            Timber.e("Error extracting image info: ${e.message}")
            throw e
        }
    }

    // ===== BUSINESS LOGIC HELPERS =====

    private fun buildExifDataMap(photoFile: File, imageMetadata: ImageMetadata): Map<String, String> {
        val exifMap = mutableMapOf<String, String>()

        try {
            val exif = ExifInterface(photoFile.absolutePath)

            exif.getAttribute(ExifInterface.TAG_MAKE)?.let { exifMap[ExifInterface.TAG_MAKE] = it }
            exif.getAttribute(ExifInterface.TAG_MODEL)?.let { exifMap[ExifInterface.TAG_MODEL] = it }
            exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { exifMap[ExifInterface.TAG_DATETIME] = it }
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1).let {
                exifMap[ExifInterface.TAG_ORIENTATION] = it.toString()
            }

            // Add image dimensions
            exifMap[EXIF_WIDTH_KEY] = imageMetadata.width.toString()
            exifMap[EXIF_HEIGHT_KEY] = imageMetadata.height.toString()

        } catch (e: Exception) {
            Timber.w(e, "EXIF read error, using dimensions only")
            exifMap[EXIF_WIDTH_KEY] = imageMetadata.width.toString()
            exifMap[EXIF_HEIGHT_KEY] = imageMetadata.height.toString()
        }

        return exifMap.toMap()
    }

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

            if (rotationDegrees == 0) {
                tempFile.copyTo(destinationFile, overwrite = true)
            } else {
                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees.toFloat())

                FileOutputStream(destinationFile).use { out ->
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, cameraSettings.compressionQuality, out)
                }

                bitmap.recycle()
                rotatedBitmap.recycle()
            }

            tempFile.delete()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error saving photo with orientation correction")
            false
        }
    }

    private fun generateThumbnailWithCorrectOrientation(
        originalFile: File,
        thumbnailFile: File,
        cameraSettings: CameraSettings
    ): Boolean {
        return try {
            val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath) ?: return false

            val orientation = getExifOrientation(originalFile.absolutePath)
            val rotationDegrees = exifOrientationToRotation(orientation)

            val rotatedBitmap = if (rotationDegrees != 0) {
                rotateBitmap(originalBitmap, rotationDegrees.toFloat())
            } else {
                originalBitmap
            }

            val thumbnailBitmap = rotatedBitmap.scale(THUMBNAIL_SIZE, THUMBNAIL_SIZE, true)

            FileOutputStream(thumbnailFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
            }

            if (rotatedBitmap != originalBitmap) rotatedBitmap.recycle()
            originalBitmap.recycle()
            thumbnailBitmap.recycle()

            true
        } catch (e: Exception) {
            Timber.e(e, "Error generating thumbnail")
            false
        }
    }

    private fun getExifOrientation(filePath: String): Int {
        return try {
            val exif = ExifInterface(filePath)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun exifOrientationToRotation(orientation: Int): Int {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun extractGpsLocation(photoFile: File): PhotoLocation? {
        return try {
            val exif = ExifInterface(photoFile.absolutePath)
            val latLong = FloatArray(2)

            if (exif.getLatLong(latLong)) {
                PhotoLocation(
                    latitude = latLong[0].toDouble(),
                    longitude = latLong[1].toDouble()
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun generatePhotoId(): String = UUID.randomUUID().toString()

    // ===== URI HELPER METHODS =====

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

    // ===== STORAGE MANAGEMENT =====

    /**
     * Get photo storage information
     */
    suspend fun getStorageInfo(): StorageInfo? {
        return try {
            when (val result = photoFileRepo.getAvailablePhotoStorage()) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    Timber.e("Failed to get storage info")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Storage info error")
            null
        }
    }

    /**
     * Cleanup orphaned photos and temporary files
     */
    suspend fun cleanupPhotos(): Int {
        return try {
            when (val result = photoFileRepo.cleanupOrphanedPhotos()) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    Timber.e("Photo cleanup failed")
                    0
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Cleanup error")
            0
        }
    }
}

// ===== DATA CLASSES =====

/**
 * Photo save operation result
 */
sealed class PhotoSaveResult {
    data class Success(val photo: Photo) : PhotoSaveResult()
    data class Error(val message: String) : PhotoSaveResult()
}

/**
 * Image metadata (dimensions)
 */
data class ImageMetadata(
    val width: Int,
    val height: Int
)