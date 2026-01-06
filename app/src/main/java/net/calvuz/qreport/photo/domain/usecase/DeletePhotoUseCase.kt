package net.calvuz.qreport.photo.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import net.calvuz.qreport.photo.domain.model.PhotoErrorType
import net.calvuz.qreport.photo.domain.model.PhotoResult
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.domain.repository.PhotoRepository
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case per eliminare foto dal sistema.
 * Gestisce l'eliminazione sia dal database che dal filesystem.
 */
@Singleton
class DeletePhotoUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {

    /**
     * Elimina una singola foto per ID.
     *
     * @param photoId ID della foto da eliminare
     * @param deleteFile Se true, elimina anche il file dal filesystem
     * @return Risultato dell'operazione
     */
    suspend operator fun invoke(
        photoId: String,
        deleteFile: Boolean = true
    ): PhotoResult<Unit> = withContext(Dispatchers.IO) {

        try {
            // 1. Recupera i dettagli della foto prima di eliminarla
            val photo = photoRepository.getPhotoById(photoId)
                ?: return@withContext PhotoResult.Error(
                    Exception("Foto non trovata"),
                    PhotoErrorType.FILE_NOT_FOUND
                )

            // 2. Elimina dal database
            photoRepository.deletePhoto(photoId)

            // 3. Elimina i file se richiesto
            if (deleteFile) {
                deletePhotoFiles(photo.filePath, photo.thumbnailPath)
            }

            PhotoResult.Success(Unit)

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Elimina tutte le foto di un check item.
     *
     * @param checkItemId ID del check item
     * @param deleteFiles Se true, elimina anche i file dal filesystem
     * @return Risultato con il numero di foto eliminate
     */
    suspend fun deleteCheckItemPhotos(
        checkItemId: String,
        deleteFiles: Boolean = true
    ): PhotoResult<Int> = withContext(Dispatchers.IO) {

        try {
            // 1. Recupera tutte le foto del check item se dobbiamo eliminare i file
            val photoFiles = if (deleteFiles) {
                photoRepository.getPhotosByCheckItemId(checkItemId)
                    .let { flow ->
                        // Converte il Flow in List per questa operazione
                        val photos = mutableListOf<Photo>()
                        flow.collect { photos.addAll(it) }
                        photos
                    }
            } else emptyList()

            // 2. Elimina dal database
            val deletedCount = photoRepository.deletePhotosByCheckItemId(checkItemId)

            // 3. Elimina i file se richiesto
            if (deleteFiles) {
                photoFiles.forEach { photo ->
                    deletePhotoFiles(photo.filePath, photo.thumbnailPath)
                }
            }

            PhotoResult.Success(deletedCount)

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Elimina multiple foto per ID.
     *
     * @param photoIds Lista degli ID delle foto da eliminare
     * @param deleteFiles Se true, elimina anche i file dal filesystem
     * @return Risultato con il numero di foto eliminate
     */
    suspend fun deleteMultiplePhotos(
        photoIds: List<String>,
        deleteFiles: Boolean = true
    ): PhotoResult<Int> = withContext(Dispatchers.IO) {

        try {
            var deletedCount = 0
            val errors = mutableListOf<Throwable>()

            for (photoId in photoIds) {
                val result = invoke(photoId, deleteFiles)
                when (result) {
                    is PhotoResult.Success -> deletedCount++
                    is PhotoResult.Error -> errors.add(result.exception)
                    is PhotoResult.Loading -> { /* Non dovrebbe accadere */ }
                }
            }

            if (errors.isNotEmpty() && deletedCount == 0) {
                // Tutti i tentativi sono falliti
                PhotoResult.Error(
                    Exception("Eliminazione fallita per tutte le foto: ${errors.first().message}"),
                    PhotoErrorType.PROCESSING_ERROR
                )
            } else {
                // Almeno alcune eliminazioni sono riuscite
                PhotoResult.Success(deletedCount)
            }

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Elimina foto più vecchie di una data specificata.
     *
     * @param beforeDate Data limite (Instant)
     * @param deleteFiles Se true, elimina anche i file dal filesystem
     * @return Risultato con il numero di foto eliminate
     */
    suspend fun deleteOldPhotos(
        beforeDate: Instant,
        deleteFiles: Boolean = true
    ): PhotoResult<Int> = withContext(Dispatchers.IO) {

        try {
            // 1. Se dobbiamo eliminare i file, recupera prima le foto da eliminare
            val photoFiles = if (deleteFiles) {
                photoRepository.getPhotosByDateRange(
                    Instant.DISTANT_PAST,
                    beforeDate
                )
            } else emptyList()

            // 2. Elimina dal database
            val deletedCount = photoRepository.deletePhotosOlderThan(beforeDate)

            // 3. Elimina i file se richiesto
            if (deleteFiles) {
                photoFiles.forEach { photo ->
                    deletePhotoFiles(photo.filePath, photo.thumbnailPath)
                }
            }

            PhotoResult.Success(deletedCount)

        } catch (e: Exception) {
            PhotoResult.Error(e, mapExceptionToErrorType(e))
        }
    }

    /**
     * Elimina i file fisici della foto (immagine principale + thumbnail).
     */
    private fun deletePhotoFiles(filePath: String, thumbnailPath: String?) {
        try {
            // Elimina file principale
            val mainFile = File(filePath)
            if (mainFile.exists()) {
                mainFile.delete()
            }

            // Elimina thumbnail se esiste
            thumbnailPath?.let { path ->
                val thumbFile = File(path)
                if (thumbFile.exists()) {
                    thumbFile.delete()
                }
            }
        } catch (e: Exception) {
            // Log dell'errore ma non fermare l'operazione
            // L'eliminazione dal database è più importante
        }
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
            exception is IOException ->
                PhotoErrorType.FILE_NOT_FOUND
            else -> PhotoErrorType.PROCESSING_ERROR
        }
    }
}