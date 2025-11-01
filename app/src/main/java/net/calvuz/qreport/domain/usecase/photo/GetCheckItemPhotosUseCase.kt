package net.calvuz.qreport.domain.usecase.photo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.calvuz.qreport.domain.model.photo.Photo
import net.calvuz.qreport.domain.model.photo.PhotoResult
import net.calvuz.qreport.domain.model.photo.PhotoErrorType
import net.calvuz.qreport.domain.repository.PhotoRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case per recuperare le foto associate a un check item.
 * Aggiornato per i nuovi domain model - le foto sono ora ordinate per orderIndex.
 */
@Singleton
class GetCheckItemPhotosUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {

    /**
     * Recupera tutte le foto associate a un check item specifico.
     * Le foto sono ordinate per orderIndex e poi per data di scatto.
     *
     * @param checkItemId ID del check item
     * @return Flow che emette PhotoResult con la lista delle foto
     */
    operator fun invoke(checkItemId: String): Flow<PhotoResult<List<Photo>>> {
        return photoRepository.getPhotosByCheckItemId(checkItemId)
            .map { photos ->
                try {
                    // Le foto arrivano già ordinate dal repository (orderIndex ASC, takenAt DESC)
                    PhotoResult.Success(photos)
                } catch (e: Exception) {
                    PhotoResult.Error(e, PhotoErrorType.PROCESSING_ERROR)
                }
            }
    }

    /**
     * Recupera solo il numero di foto per un check item (utile per badge/counter).
     *
     * @param checkItemId ID del check item
     * @return Numero di foto associate
     */
    suspend fun getPhotosCount(checkItemId: String): Int {
        return try {
            photoRepository.getPhotosCountByCheckItemId(checkItemId)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Verifica se un check item ha foto associate.
     *
     * @param checkItemId ID del check item
     * @return True se ha almeno una foto
     */
    suspend fun hasPhotos(checkItemId: String): Boolean {
        return getPhotosCount(checkItemId) > 0
    }

    /**
     * Recupera le foto raggruppate per perspective.
     *
     * @param checkItemId ID del check item
     * @return Mappa perspective -> lista foto
     */
    suspend fun getPhotosGroupedByPerspective(checkItemId: String): Map<String, List<Photo>> {
        return try {
            val photos = photoRepository.getPhotosByCheckItemId(checkItemId)
            var photosList: List<Photo> = emptyList()

            // Raccoglie le foto dal Flow (per questa operazione sincrona)
            photos.collect { photosList = it }

            photosList.groupBy { photo ->
                photo.metadata.perspective?.displayName ?: "Senza Categoria"
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Recupera le foto ordinate per data di scatto (utile per timeline).
     *
     * @param checkItemId ID del check item
     * @return Lista delle foto ordinate per data (più recenti prima)
     */
    suspend fun getPhotosByDate(checkItemId: String): List<Photo> {
        return try {
            val photos = photoRepository.getPhotosByCheckItemId(checkItemId)
            var photosList: List<Photo> = emptyList()

            photos.collect { photosList = it }

            // Riordina per data di scatto (più recenti prima)
            photosList.sortedByDescending { it.takenAt }
        } catch (e: Exception) {
            emptyList()
        }
    }
}