package net.calvuz.qreport.photo.data.local.repository

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.file.domain.model.DirectorySpec
import net.calvuz.qreport.app.file.domain.repository.*
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.photo.domain.repository.PhotoFileInfo
import net.calvuz.qreport.photo.domain.repository.PhotoFileRepository
import net.calvuz.qreport.photo.domain.repository.StorageInfo
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PhotoFileRepositoryImpl - File operations specifiche per foto
 *
 * SI APPOGGIA a CoreFileRepository per operazioni base
 */
@Singleton
class PhotoFileRepositoryImpl @Inject constructor(
    private val coreFileRepo: CoreFileRepository
) : PhotoFileRepository {

    companion object {
        private const val THUMBNAILS_SUBDIR = "thumbnails"
    }

    // ===== DIRECTORY MANAGEMENT =====

    override suspend fun getPhotosDirectory(): QrResult<String, QrError> {
        return coreFileRepo.getOrCreateDirectory(DirectorySpec.Core.PHOTOS)
    }

    override suspend fun getThumbnailsDirectory(): QrResult<String, QrError> {
        return try {
            when (val photosResult = getPhotosDirectory()) {
                is QrResult.Error -> photosResult
                is QrResult.Success -> {
                    coreFileRepo.createSubDirectory(DirectorySpec.Core.PHOTOS, THUMBNAILS_SUBDIR)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create thumbnails directory")
            QrResult.Error(QrError.PhotoError.DIRECTORY_CREATE)
        }
    }

    override suspend fun createCheckItemPhotoDirectory(checkItemId: String): QrResult<String, QrError> {
        return try {
            coreFileRepo.createSubDirectory(DirectorySpec.Core.PHOTOS, checkItemId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create checkItem photo directory: $checkItemId")
            QrResult.Error(QrError.PhotoError.DIRECTORY_CREATE)
        }
    }

    // ===== PHOTO FILE OPERATIONS =====

    override suspend fun createPhotoFilePath(checkItemId: String): QrResult<String, QrError> {
        return try {
            // Create checkItem directory if needed
            when (val dirResult = createCheckItemPhotoDirectory(checkItemId)) {
                is QrResult.Error -> dirResult
                is QrResult.Success -> {
                    val timestamp = System.currentTimeMillis()
                    val uniqueId = UUID.randomUUID().toString().take(8)
                    val fileName = "photo_${checkItemId}_${timestamp}_${uniqueId}.jpg"
                    val photoPath = File(dirResult.data, fileName).absolutePath

                    Timber.d("Generated photo path: $photoPath")
                    QrResult.Success(photoPath)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create photo file path")
            QrResult.Error(QrError.PhotoError.FILE_CREATE)
        }
    }

    override suspend fun createThumbnailFilePath(photoFileName: String): QrResult<String, QrError> {
        return try {
            when (val thumbDirResult = getThumbnailsDirectory()) {
                is QrResult.Error -> thumbDirResult
                is QrResult.Success -> {
                    val thumbnailFileName = "thumb_$photoFileName"
                    val thumbnailPath = File(thumbDirResult.data, thumbnailFileName).absolutePath

                    QrResult.Success(thumbnailPath)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create thumbnail path")
            QrResult.Error(QrError.PhotoError.THUMBNAIL_CREATE)
        }
    }

    override suspend fun getPhotoFileSize(photoFilePath: String): QrResult<Long, QrError> {
        return try {
            when (val sizeResult = coreFileRepo.getFileSize(photoFilePath)) {
                is QrResult.Success -> QrResult.Success(sizeResult.data)
                is QrResult.Error -> QrResult.Error(QrError.PhotoError.FILE_ACCESS)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get photo file size")
            QrResult.Error(QrError.PhotoError.FILE_ACCESS)
        }
    }

    // ===== PHOTO VALIDATION =====

    override suspend fun validatePhotoFile(photoFilePath: String): QrResult<Boolean, QrError> {
        return try {
            val exists = coreFileRepo.fileExists(photoFilePath)
            QrResult.Success(exists)
        } catch (e: Exception) {
            Timber.e(e, "Failed to validate photo file")
            QrResult.Error(QrError.PhotoError.VALIDATION)
        }
    }

    override suspend fun getAvailablePhotoStorage(): QrResult<StorageInfo, QrError> {
        return try {
            when (val photosDirResult = getPhotosDirectory()) {
                is QrResult.Error -> QrResult.Error(photosDirResult.error)
                is QrResult.Success -> {
                    val photosDirSize = coreFileRepo.getDirectorySize(photosDirResult.data)
                    val thumbnailsDirResult = getThumbnailsDirectory()

                    when (photosDirSize) {
                        is QrResult.Success -> {
                            val thumbnailsSize = if (thumbnailsDirResult is QrResult.Success) {
                                when (val result = coreFileRepo.getDirectorySize(thumbnailsDirResult.data)) {
                                    is QrResult.Success -> result.data
                                    is QrResult.Error -> 0L
                                }
                            } else 0L

                            val listResult = coreFileRepo.listFiles(photosDirResult.data)
                            val photoCount = when (listResult) {
                                is QrResult.Success -> listResult.data.count { !it.isDirectory }
                                is QrResult.Error -> 0
                            }

                            val storageInfo = StorageInfo(
                                totalSize = photosDirSize.data + thumbnailsSize,
                                photosSize = photosDirSize.data,
                                thumbnailsSize = thumbnailsSize,
                                photoCount = photoCount,
                                availableSpace = getAvailableSpace() // File system available space
                            )

                            QrResult.Success(storageInfo)
                        }
                        is QrResult.Error -> QrResult.Error(QrError.PhotoError.STORAGE_ACCESS)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get storage info")
            QrResult.Error(QrError.PhotoError.STORAGE_ACCESS)
        }
    }

    // ===== PHOTO CLEANUP =====

    override suspend fun deletePhotoFiles(photoFilePath: String, thumbnailPath: String?): QrResult<Unit, QrError> {
        return try {
            var hasErrors = false

            // Delete main photo
            when (coreFileRepo.deleteFile(photoFilePath)) {
                is QrResult.Error -> {
                    Timber.e("Failed to delete photo: $photoFilePath")
                    hasErrors = true
                }
                is QrResult.Success -> Timber.d("Deleted photo: $photoFilePath")
            }

            // Delete thumbnail if exists
            thumbnailPath?.let { thumbPath ->
                when (coreFileRepo.deleteFile(thumbPath)) {
                    is QrResult.Error -> {
                        Timber.w("Failed to delete thumbnail: $thumbPath")
                        // Thumbnail deletion failure is not critical
                    }
                    is QrResult.Success -> Timber.d("Deleted thumbnail: $thumbPath")
                }
            }

            if (hasErrors) {
                QrResult.Error(QrError.PhotoError.DELETE)
            } else {
                QrResult.Success(Unit)
            }

        } catch (e: Exception) {
            Timber.e(e, "Error deleting photo files")
            QrResult.Error(QrError.PhotoError.DELETE)
        }
    }

    override suspend fun cleanupOrphanedPhotos(): QrResult<Int, QrError> {
        return try {
            // TODO: Implement orphaned photos cleanup
            // Would need database repository to check which photos are referenced
            Timber.d("Orphaned photos cleanup not implemented yet")
            QrResult.Success(0)
        } catch (e: Exception) {
            QrResult.Error(QrError.PhotoError.CLEANUP)
        }
    }

    override suspend fun listCheckItemPhotos(checkItemId: String): QrResult<List<PhotoFileInfo>, QrError> {
        return try {
            when (val photosDirResult = getPhotosDirectory()) {
                is QrResult.Error -> QrResult.Error(photosDirResult.error)
                is QrResult.Success -> {
                    val checkItemDir = File(photosDirResult.data, checkItemId)

                    if (!checkItemDir.exists()) {
                        return QrResult.Success(emptyList())
                    }

                    when (val listResult = coreFileRepo.listFiles(checkItemDir.absolutePath)) {
                        is QrResult.Error ->  QrResult.Error(listResult.error)
                        is QrResult.Success -> {
                            val photoFiles = listResult.data
                                .filter { !it.isDirectory && it.extension in listOf("jpg", "jpeg", "png") }
                                .map { fileInfo ->
                                    PhotoFileInfo(
                                        fileName = fileInfo.name,
                                        filePath = fileInfo.path,
                                        fileSize = fileInfo.size,
                                        lastModified = fileInfo.lastModified,
                                        hasThumbnail = hasThumbnailFile(fileInfo.name)
                                    )
                                }

                            QrResult.Success(photoFiles)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to list checkItem photos")
            QrResult.Error(QrError.PhotoError.LIST)
        }
    }

    // ===== HELPER METHODS =====

    private fun getAvailableSpace(): Long {
        return try {
            // Get available space on internal storage
            val internalDir = File("/data/data")
            internalDir.usableSpace
        } catch (e: Exception) {
            Timber.w(e, "Failed to get available space")
            0L
        }
    }

    private suspend fun hasThumbnailFile(photoFileName: String): Boolean {
        return try {
            when (val thumbDirResult = getThumbnailsDirectory()) {
                is QrResult.Success -> {
                    val thumbnailPath = File(thumbDirResult.data, "thumb_$photoFileName").absolutePath
                    coreFileRepo.fileExists(thumbnailPath)
                }
                is QrResult.Error -> false
            }
        } catch (e: Exception) {
            false
        }
    }
}