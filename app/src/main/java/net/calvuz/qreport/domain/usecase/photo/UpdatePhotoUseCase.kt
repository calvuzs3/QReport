package net.calvuz.qreport.domain.usecase.photo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.photo.PhotoConstants
import net.calvuz.qreport.domain.model.photo.PhotoErrorType
import net.calvuz.qreport.domain.model.photo.PhotoPerspective
import net.calvuz.qreport.domain.model.photo.PhotoResult
import net.calvuz.qreport.domain.repository.PhotoRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case per aggiornare le informazioni delle foto.
 * Aggiornato per supportare perspective, orderIndex e altre nuove funzionalità.
 */
@Singleton
class UpdatePhotoUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {

    /**
     * Aggiorna la caption di una foto.
     *
     * @param photoId ID della foto da aggiornare
     * @param newCaption Nuova caption
     * @return Risultato dell'operazione con la foto aggiornata
     */
    suspend fun updateCaption(
        photoId: String,
        newCaption: String
    ): PhotoResult<Photo> = withContext(Dispatchers.IO) {

        try {
            // 1. Verifica che la foto esista
            val existingPhoto = photoRepository.getPhotoById(photoId)
                ?: return@withContext PhotoResult.Error(
                    Exception("Foto non trovata"),
                    PhotoErrorType.FILE_NOT_FOUND
                )

            // 2. Aggiorna la caption
            photoRepository.updatePhotoCaption(photoId, newCaption)

            // 3. Recupera la foto aggiornata
            val updatedPhoto = photoRepository.getPhotoById(photoId)
                ?: return@withContext PhotoResult.Error(
                    Exception("Errore nel recupero della foto aggiornata"),
                    PhotoErrorType.PROCESSING_ERROR
                )

            PhotoResult.Success(updatedPhoto)

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Aggiorna la perspective di una foto.
     *
     * @param photoId ID della foto
     * @param newPerspective Nuova perspective
     * @return Risultato dell'operazione
     */
    suspend fun updatePerspective(
        photoId: String,
        newPerspective: PhotoPerspective?
    ): PhotoResult<Photo> = withContext(Dispatchers.IO) {

        try {
            // 1. Verifica che la foto esista
            val existingPhoto = photoRepository.getPhotoById(photoId)
                ?: return@withContext PhotoResult.Error(
                    Exception("Foto non trovata"),
                    PhotoErrorType.FILE_NOT_FOUND
                )

            // 2. Aggiorna la perspective
            photoRepository.updatePhotoPerspective(photoId, newPerspective)

            // 3. Recupera la foto aggiornata
            val updatedPhoto = photoRepository.getPhotoById(photoId)
                ?: return@withContext PhotoResult.Error(
                    Exception("Errore nel recupero della foto aggiornata"),
                    PhotoErrorType.PROCESSING_ERROR
                )

            PhotoResult.Success(updatedPhoto)

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Aggiorna l'orderIndex di una foto.
     *
     * @param photoId ID della foto
     * @param newOrderIndex Nuovo orderIndex
     * @return Risultato dell'operazione
     */
    suspend fun updateOrderIndex(
        photoId: String,
        newOrderIndex: Int
    ): PhotoResult<Photo> = withContext(Dispatchers.IO) {

        try {
            // 1. Verifica che la foto esista
            val existingPhoto = photoRepository.getPhotoById(photoId)
                ?: return@withContext PhotoResult.Error(
                    Exception("Foto non trovata"),
                    PhotoErrorType.FILE_NOT_FOUND
                )

            // 2. Aggiorna l'orderIndex
            photoRepository.updatePhotoOrderIndex(photoId, newOrderIndex)

            // 3. Recupera la foto aggiornata
            val updatedPhoto = photoRepository.getPhotoById(photoId)
                ?: return@withContext PhotoResult.Error(
                    Exception("Errore nel recupero della foto aggiornata"),
                    PhotoErrorType.PROCESSING_ERROR
                )

            PhotoResult.Success(updatedPhoto)

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Riordina le foto di un check item.
     *
     * @param photoOrderUpdates Lista di coppie (photoId, newOrderIndex)
     * @return Risultato con il numero di foto riordinate
     */
    suspend fun reorderPhotos(
        photoOrderUpdates: List<Pair<String, Int>>
    ): PhotoResult<Int> = withContext(Dispatchers.IO) {

        try {
            photoRepository.reorderPhotos(photoOrderUpdates)
            PhotoResult.Success(photoOrderUpdates.size)

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Aggiorna completamente una foto.
     *
     * @param photo Foto con i dati aggiornati
     * @return Risultato dell'operazione
     */
    suspend fun updatePhoto(photo: Photo): PhotoResult<Photo> = withContext(Dispatchers.IO) {

        try {
            // 1. Verifica che la foto esista
            val existingPhoto = photoRepository.getPhotoById(photo.id)
                ?: return@withContext PhotoResult.Error(
                    Exception("Foto non trovata"),
                    PhotoErrorType.FILE_NOT_FOUND
                )

            // 2. Aggiorna la foto
            photoRepository.updatePhoto(photo)

            // 3. Recupera la foto aggiornata
            val updatedPhoto = photoRepository.getPhotoById(photo.id)
                ?: return@withContext PhotoResult.Error(
                    Exception("Errore nel recupero della foto aggiornata"),
                    PhotoErrorType.PROCESSING_ERROR
                )

            PhotoResult.Success(updatedPhoto)

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Aggiorna il percorso file di una foto (utile per operazioni di spostamento/riorganizzazione).
     *
     * @param photoId ID della foto
     * @param newFilePath Nuovo percorso del file
     * @return Risultato dell'operazione
     */
    suspend fun updateFilePath(
        photoId: String,
        newFilePath: String
    ): PhotoResult<Photo> = withContext(Dispatchers.IO) {

        try {
            // 1. Verifica che la foto esista
            val existingPhoto = photoRepository.getPhotoById(photoId)
                ?: return@withContext PhotoResult.Error(
                    Exception("Foto non trovata"),
                    PhotoErrorType.FILE_NOT_FOUND
                )

            // 2. Verifica che il nuovo file esista
            val newFile = java.io.File(newFilePath)
            if (!newFile.exists()) {
                return@withContext PhotoResult.Error(
                    Exception("Il nuovo file non esiste: $newFilePath"),
                    PhotoErrorType.FILE_NOT_FOUND
                )
            }

            // 3. Aggiorna il percorso
            photoRepository.updatePhotoFilePath(photoId, newFilePath)

            // 4. Recupera la foto aggiornata
            val updatedPhoto = photoRepository.getPhotoById(photoId)
                ?: return@withContext PhotoResult.Error(
                    Exception("Errore nel recupero della foto aggiornata"),
                    PhotoErrorType.PROCESSING_ERROR
                )

            PhotoResult.Success(updatedPhoto)

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Aggiorna le caption di multiple foto in batch.
     *
     * @param updates Mappa di photoId -> nuova caption
     * @return Risultato con il numero di foto aggiornate con successo
     */
    suspend fun updateMultipleCaptions(
        updates: Map<String, String>
    ): PhotoResult<Int> = withContext(Dispatchers.IO) {

        try {
            var successCount = 0
            val errors = mutableListOf<Throwable>()

            for ((photoId, newCaption) in updates) {
                val result = updateCaption(photoId, newCaption)
                when (result) {
                    is PhotoResult.Success -> successCount++
                    is PhotoResult.Error -> errors.add(result.exception)
                    is PhotoResult.Loading -> { /* Non dovrebbe accadere */ }
                }
            }

            if (errors.isNotEmpty() && successCount == 0) {
                // Tutti gli aggiornamenti sono falliti
                PhotoResult.Error(
                    Exception("Aggiornamento fallito per tutte le foto: ${errors.first().message}"),
                    PhotoErrorType.PROCESSING_ERROR
                )
            } else {
                // Almeno alcuni aggiornamenti sono riusciti
                PhotoResult.Success(successCount)
            }

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Valida una caption prima dell'aggiornamento.
     *
     * @param caption Caption da validare
     * @return True se la caption è valida
     */
    fun validateCaption(caption: String): Boolean {
        return caption.length <= MAX_CAPTION_LENGTH &&
                !caption.contains(FORBIDDEN_CHARACTERS)
    }

    /**
     * Valida un orderIndex.
     *
     * @param orderIndex OrderIndex da validare
     * @return True se è valido
     */
    fun validateOrderIndex(orderIndex: Int): Boolean {
        return orderIndex >= 0
    }

    /**
     * Mappa le eccezioni ai tipi di errore specifici.
     */
    private fun mapExceptionToErrorType(exception: Exception): PhotoErrorType {
        return when {
            exception.message?.contains("not found", ignoreCase = true) == true ->
                PhotoErrorType.FILE_NOT_FOUND
            exception.message?.contains("permission", ignoreCase = true) == true ->
                PhotoErrorType.PERMISSION_DENIED
            exception is java.io.IOException ->
                PhotoErrorType.FILE_NOT_FOUND
            else -> PhotoErrorType.PROCESSING_ERROR
        }
    }

    companion object {
        private const val MAX_CAPTION_LENGTH = PhotoConstants.MAX_CAPTION_LENGTH
        private val FORBIDDEN_CHARACTERS = PhotoConstants.FORBIDDEN_CHARACTERS
    }
}