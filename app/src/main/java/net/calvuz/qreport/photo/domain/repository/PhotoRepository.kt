package net.calvuz.qreport.photo.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.domain.model.PhotoFilter
import net.calvuz.qreport.photo.domain.model.PhotoPerspective
import net.calvuz.qreport.photo.domain.model.PhotoResolution
import net.calvuz.qreport.photo.domain.model.PhotoSummary

/**
 * Repository interface per gestire le operazioni CRUD sulle foto.
 * Aggiornato per utilizzare i domain model esistenti del progetto.
 */
interface PhotoRepository {

    // ===== CREATE =====

    /**
     * Inserisce una nuova foto nel database.
     * @param photo La foto da salvare
     * @return L'ID della foto inserita
     */
    suspend fun insertPhoto(photo: Photo): String

    /**
     * Inserisce multiple foto in una singola transazione.
     * @param photos Lista delle foto da salvare
     * @return Lista degli ID delle foto inserite
     */
    suspend fun insertPhotos(photos: List<Photo>): List<String>

    // ===== READ =====

    /**
     * Recupera una foto per ID.
     * @param photoId ID della foto
     * @return La foto se trovata, null altrimenti
     */
    suspend fun getPhotoById(photoId: String): Photo?

    /**
     * Recupera tutte le foto associate a un check item.
     * @param checkItemId ID del check item
     * @return Flow con la lista delle foto ordinate per orderIndex
     */
    fun getPhotosByCheckItemId(checkItemId: String): Flow<List<Photo>>

    /**
     * Recupera tutte le foto per un checkup.
     * @param checkUpId ID del checkup
     * @return Flow con le foto del checkup
     */
    suspend fun getPhotosByCheckUp(checkUpId: String): List<Photo>

    /**
     * Recupera tutte le foto nel database.
     * @return Flow con tutte le foto ordinate per data (più recenti prima)
     */
    fun getAllPhotos(): Flow<List<Photo>>

    /**
     * Recupera foto filtrate secondo i criteri specificati.
     * @param filter Filtri da applicare
     * @return Flow con le foto filtrate
     */
    fun getFilteredPhotos(filter: PhotoFilter): Flow<List<Photo>>

    /**
     * Recupera foto per perspective.
     * @param perspective Perspective da cercare
     * @return Lista delle foto con quella perspective
     */
    suspend fun getPhotosByPerspective(perspective: PhotoPerspective): List<Photo>

    /**
     * Recupera foto per resolution.
     * @param resolution Resolution da cercare
     * @return Lista delle foto con quella resolution
     */
    suspend fun getPhotosByResolution(resolution: PhotoResolution): List<Photo>

    /**
     * Recupera il numero totale di foto per un check item.
     * @param checkItemId ID del check item
     * @return Numero di foto
     */
    suspend fun getPhotosCountByCheckItemId(checkItemId: String): Int

    /**
     * Recupera il numero di foto per un checkup.
     * @param checkUpId ID del checkup
     * @return Numero di foto
     */
    suspend fun getPhotosCountByCheckUp(checkUpId: String): Int

    /**
     * Recupera un riassunto delle foto nel sistema.
     * @return Statistiche generali delle foto
     */
    suspend fun getPhotosSummary(): PhotoSummary

    /**
     * Recupera foto per intervallo di date.
     * @param startDate Data di inizio
     * @param endDate Data di fine
     * @return Lista delle foto nell'intervallo
     */
    suspend fun getPhotosByDateRange(
        startDate: Instant,
        endDate: Instant
    ): List<Photo>

    /**
     * Recupera il prossimo orderIndex per un check item.
     * @param checkItemId ID del check item
     * @return Prossimo orderIndex disponibile
     */
    suspend fun getNextOrderIndex(checkItemId: String): Int

    // ===== UPDATE =====

    /**
     * Aggiorna una foto esistente.
     * @param photo La foto con i dati aggiornati
     */
    suspend fun updatePhoto(photo: Photo)

    /**
     * Aggiorna solo la caption di una foto.
     * @param photoId ID della foto
     * @param caption Nuova caption
     */
    suspend fun updatePhotoCaption(photoId: String, caption: String)

    /**
     * Aggiorna il percorso del file della foto.
     * @param photoId ID della foto
     * @param newFilePath Nuovo percorso file
     */
    suspend fun updatePhotoFilePath(photoId: String, newFilePath: String)

    /**
     * Aggiorna l'orderIndex di una foto.
     * @param photoId ID della foto
     * @param newOrderIndex Nuovo orderIndex
     */
    suspend fun updatePhotoOrderIndex(photoId: String, newOrderIndex: Int)

    /**
     * Aggiorna la perspective di una foto.
     * @param photoId ID della foto
     * @param perspective Nuova perspective
     */
    suspend fun updatePhotoPerspective(photoId: String, perspective: PhotoPerspective?)

    /**
     * Riordina le foto di un check item.
     * @param photoOrderUpdates Lista di coppie (photoId, newOrderIndex)
     */
    suspend fun reorderPhotos(photoOrderUpdates: List<Pair<String, Int>>)

    // ===== DELETE =====

    /**
     * Elimina una foto per ID.
     * @param photoId ID della foto da eliminare
     */
    suspend fun deletePhoto(photoId: String)

    /**
     * Elimina tutte le foto associate a un check item.
     * @param checkItemId ID del check item
     * @return Numero di foto eliminate
     */
    suspend fun deletePhotosByCheckItemId(checkItemId: String): Int

    /**
     * Elimina foto multiple per ID.
     * @param photoIds Lista degli ID delle foto da eliminare
     * @return Numero di foto eliminate
     */
    suspend fun deletePhotos(photoIds: List<String>): Int

    /**
     * Elimina tutte le foto più vecchie di una data specificata.
     * @param beforeDate Data limite
     * @return Numero di foto eliminate
     */
    suspend fun deletePhotosOlderThan(beforeDate: Instant): Int

    /**
     * Cleanup foto orfane.
     * @return Numero di foto eliminate
     */
    suspend fun deleteOrphanedPhotos()

    // ===== UTILITY =====

    /**
     * Verifica se una foto esiste nel database.
     * @param photoId ID della foto
     * @return True se la foto esiste
     */
    suspend fun photoExists(photoId: String): Boolean

    /**
     * Recupera la dimensione totale occupata dalle foto.
     * @return Dimensione totale in bytes
     */
    suspend fun getTotalPhotosSize(): Long

    /**
     * Recupera la dimensione occupata dalle foto di un check item.
     * @param checkItemId ID del check item
     * @return Dimensione totale in bytes
     */
    suspend fun getPhotosSize(checkItemId: String): Long

    /**
     * Recupera tutti gli ID dei check item che hanno foto associate.
     * @return Lista degli ID dei check item con foto
     */
    suspend fun getCheckItemIdsWithPhotos(): List<String>

    /**
     * Recupera il percorso delle foto orfane (non associate a check item esistenti).
     * @return Lista dei percorsi delle foto orfane
     */
    suspend fun getOrphanedPhotoPaths(): List<String>

    /**
     * Recupera distribuzione delle foto per perspective.
     * @return Mappa perspective -> count
     */
    suspend fun getPerspectiveDistribution(): Map<PhotoPerspective, Int>

    /**
     * Recupera distribuzione delle foto per resolution.
     * @return Mappa resolution -> count
     */
    suspend fun getResolutionDistribution(): Map<PhotoResolution, Int>

    /**
     * Cerca foto per caption.
     * @param query Query di ricerca
     * @return Lista delle foto che corrispondono
     */
    suspend fun searchPhotosByCaption(query: String): List<Photo>
}