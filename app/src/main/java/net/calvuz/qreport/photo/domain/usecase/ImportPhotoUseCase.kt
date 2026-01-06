package net.calvuz.qreport.photo.domain.usecase

import android.net.Uri
import net.calvuz.qreport.photo.data.local.repository.PhotoSaveResult
import net.calvuz.qreport.photo.data.local.repository.PhotoStorageManager
import net.calvuz.qreport.photo.domain.model.CameraSettings
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.domain.model.PhotoErrorType
import net.calvuz.qreport.photo.domain.model.PhotoResult
import net.calvuz.qreport.photo.domain.repository.PhotoRepository
import net.calvuz.qreport.photo.presentation.ui.ImageInfo
import javax.inject.Inject


/**
 * Use case per importare foto dalla galleria del telefono.
 * Segue lo stesso workflow di CapturePhotoUseCase ma per foto esistenti.
 */
class ImportPhotoUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val photoStorageManager: PhotoStorageManager
) {
    /**
     * Importa una foto dalla galleria nel check item specificato.
     *
     * @param checkItemId ID del check item di destinazione
     * @param imageUri URI della foto selezionata dalla galleria
     * @param caption Descrizione della foto (opzionale)
     * @param cameraSettings Impostazioni che includono perspective e resolution
     * @return PhotoResult con la foto importata o errore
     */
    suspend operator fun invoke(
        checkItemId: String,
        imageUri: Uri,
        caption: String = "",
        cameraSettings: CameraSettings = CameraSettings.default()
    ): PhotoResult<Photo> {
        return try {
            // 1. Ottieni prossimo orderIndex per il check item
            val orderIndex = photoStorageManager.getNextOrderIndex(checkItemId)

            // 2. Importa nel file system con stesso processing delle foto catturate
            when (val importResult = photoStorageManager.importPhotoFromUri(
                checkItemId = checkItemId,
                imageUri = imageUri,
                caption = caption,
                orderIndex = orderIndex
            )) {
                is PhotoSaveResult.Success -> {
                    // 3. Salva nel database
                    val photoId = photoRepository.insertPhoto(importResult.photo)
                    PhotoResult.Success(importResult.photo.copy(id = photoId))
                }
                is PhotoSaveResult.Error -> {
                    PhotoResult.Error(
                        exception = Exception(importResult.message),
                        errorType = PhotoErrorType.STORAGE_ERROR
                    )
                }
            }
        } catch (e: Exception) {
            PhotoResult.Error(e, PhotoErrorType.IMPORT_ERROR)
        }
    }

    /**
     * Valida se l'URI della foto è accessibile e valida.
     */
    fun validateImageUri(imageUri: Uri): PhotoResult<Boolean> {
        return try {
            val isValid = photoStorageManager.validateImageUri(imageUri)
            if (isValid) {
                PhotoResult.Success(true)
            } else {
                PhotoResult.Error(
                    exception = Exception("URI immagine non valida o non accessibile"),
                    errorType = PhotoErrorType.VALIDATION_ERROR
                )
            }
        } catch (e: Exception) {
            PhotoResult.Error(e, PhotoErrorType.VALIDATION_ERROR)
        }
    }

    /**
     * Ottiene informazioni preliminari sulla foto senza importarla.
     * Utile per preview e validazione.
     */
    suspend fun getImageInfo(imageUri: Uri): PhotoResult<ImageInfo> {
        return try {
            val info = photoStorageManager.extractImageInfo(imageUri)
            PhotoResult.Success(info)
        } catch (e: Exception) {
            PhotoResult.Error(e, PhotoErrorType.PROCESSING_ERROR)
        }
    }
}

