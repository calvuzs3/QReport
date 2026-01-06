package net.calvuz.qreport.photo.domain.usecase

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.calvuz.qreport.photo.data.local.repository.PhotoSaveResult
import net.calvuz.qreport.photo.data.local.repository.PhotoStorageManager
import net.calvuz.qreport.photo.domain.model.CameraSettings
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.domain.model.PhotoErrorType
import net.calvuz.qreport.photo.domain.model.PhotoResult
import net.calvuz.qreport.photo.domain.repository.PhotoRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * ✅ FINALE: CapturePhotoUseCase compatibile con PhotoResult.kt reale
 * Include gestione errorType per classificazione errori
 */
class CapturePhotoUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val photoStorageManager: PhotoStorageManager
) {

    /**
     * ✅ CORRETTO: Compatibile con PhotoResult reale con errorType
     */
    suspend operator fun invoke(
        checkItemId: String,
        imageUri: Uri,
        caption: String = "",
        cameraSettings: CameraSettings = CameraSettings.default()
    ): PhotoResult<Photo> {

        return try {
            Timber.d("Cattura foto per checkItemId: $checkItemId")

            // 1. ✅ Ottieni prossimo orderIndex per questo checkItem
            val orderIndex = photoStorageManager.getNextOrderIndex(checkItemId)

            // 2. ✅ Usa PhotoStorageManager per salvare e generare thumbnail
            when (val saveResult = photoStorageManager.savePhoto(
                checkItemId = checkItemId,
                imageUri = imageUri,
                caption = caption,
                cameraSettings = cameraSettings,
                orderIndex = orderIndex
            )) {
                is PhotoSaveResult.Success -> {
                    val photo = saveResult.photo

                    // 3. ✅ CORRETTO: Salva nel repository usando insertPhoto
                    try {
                        val photoId = photoRepository.insertPhoto(photo)

                        Timber.d("Foto salvata con successo nel DB: $photoId")
                        Timber.d("Thumbnail: ${photo.thumbnailPath}")
                        Timber.d("Metadata: ${photo.metadata}")

                        PhotoResult.Success(photo)

                    } catch (e: Exception) {
                        // Se fallisce il salvataggio nel DB, elimina i file
                        photoStorageManager.deletePhoto(photo.filePath)
                        Timber.e(e, "Errore salvataggio nel repository")

                        PhotoResult.Error(
                            exception = e,
                            errorType = PhotoErrorType.REPOSITORY_ERROR
                        )
                    }
                }
                is PhotoSaveResult.Error -> {
                    Timber.e("Errore salvataggio foto: ${saveResult.message}")

                    // ✅ CORRETTO: Classifica errore come STORAGE_ERROR
                    PhotoResult.Error(
                        exception = Exception(saveResult.message),
                        errorType = PhotoErrorType.STORAGE_ERROR
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Errore durante la cattura foto")

            // ✅ CORRETTO: Classifica errore come CAPTURE_ERROR
            PhotoResult.Error(
                exception = e,
                errorType = PhotoErrorType.CAPTURE_ERROR
            )
        }
    }

    /**
     * Versione con flusso reattivo per UI.
     */
    fun capturePhotoFlow(
        checkItemId: String,
        imageUri: Uri,
        caption: String = "",
        cameraSettings: CameraSettings = CameraSettings.default()
    ): Flow<PhotoResult<Photo>> = flow {

        emit(PhotoResult.Loading)

        try {
            val result = invoke(checkItemId, imageUri, caption, cameraSettings)
            emit(result)

        } catch (e: Exception) {
            emit(
                PhotoResult.Error(
                    exception = e,
                    errorType = PhotoErrorType.CAPTURE_ERROR
                )
            )
        }
    }

    /**
     * Verifica che una foto esista nel filesystem.
     */
//    suspend fun verifyPhotoExists(photo: Photo): PhotoResult<Boolean> {
//        return try {
//            val exists = photoStorageManager.photoExist(photo)
//            PhotoResult.Success(exists)
//        } catch (e: Exception) {
//            PhotoResult.Error(
//                exception = e,
//                errorType = PhotoErrorType.VALIDATION_ERROR
//            )
//        }
//    }

    /**
     * Ottieni info storage utilizzato.
     */
//    suspend fun getStorageInfo(): PhotoResult<StorageInfo> {
//        return try {
//            val storageInfo = photoStorageManager.getStorageInfo()
//            PhotoResult.Success(storageInfo)
//        } catch (e: Exception) {
//            PhotoResult.Error(
//                exception = e,
//                errorType = PhotoErrorType.STORAGE_ERROR
//            )
//        }
//    }
}

/**
 * ✅ Extension per facilità d'uso dal ViewModel.
 */
suspend fun CapturePhotoUseCase.capturePhoto(
    checkItemId: String,
    imageUri: Uri,
    caption: String = ""
): PhotoResult<Photo> {
    return invoke(checkItemId, imageUri, caption)
}