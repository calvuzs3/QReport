package net.calvuz.qreport.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.data.local.entity.PhotoEntity
import net.calvuz.qreport.domain.model.photo.PhotoStatistics

@Dao
interface PhotoDao {

    data class PerspectiveCount(
        val perspective: String,
        val count: Int
    )

    data class ResolutionCount(
        val resolution: String,
        val count: Int
    )

    // ✅ NUOVO: Data class per statistiche dimensioni
    data class DimensionStats(
        val width: Int,
        val height: Int,
        val count: Int
    )

    data class AspectRatioStats(
        val aspectRatio: Float,
        val count: Int,
        val category: String // "landscape", "portrait", "square"
    )

    // ===== CREATE =====

    /**
     * Inserisce una nuova foto nel database.
     * @param photo Entità foto da inserire
     * @return Row ID della foto inserita
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    /**
     * Inserisce multiple foto in una singola transazione.
     * @param photos Lista delle foto da inserire
     * @return Lista dei row ID inseriti
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>): List<Long>

    // ===== READ =====

    /**
     * Recupera una foto per ID.
     * @param photoId ID della foto
     * @return Entità foto o null se non trovata
     */
    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: String): PhotoEntity?

    /**
     * Recupera tutte le foto associate a un check item con Flow reattivo.
     * Ordinate per orderIndex e poi per data di scatto.
     * @param checkItemId ID del check item
     * @return Flow con lista delle foto ordinate
     */
    @Query("""
        SELECT * FROM photos 
        WHERE check_item_id = :checkItemId 
        ORDER BY order_index ASC, taken_at DESC
    """)
    fun getPhotosByCheckItemIdFlow(checkItemId: String): Flow<List<PhotoEntity>>

    /**
     * Recupera tutte le foto associate a un check item (versione suspend).
     * @param checkItemId ID del check item
     * @return Lista delle foto ordinate
     */
    @Query("""
        SELECT * FROM photos 
        WHERE check_item_id = :checkItemId 
        ORDER BY order_index ASC, taken_at DESC
    """)
    suspend fun getPhotosByCheckItemId(checkItemId: String): List<PhotoEntity>

    /**
     * Recupera tutte le foto nel database con Flow reattivo.
     * @return Flow con tutte le foto ordinate per data (più recenti prima)
     */
    @Query("SELECT * FROM photos ORDER BY taken_at DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoEntity>>

    /**
     * Recupera tutte le foto nel database (versione suspend).
     * @return Lista di tutte le foto
     */
    @Query("SELECT * FROM photos ORDER BY taken_at DESC")
    suspend fun getAllPhotos(): List<PhotoEntity>

    /**
     * Recupera foto per intervallo di date.
     * @param startTime Timestamp di inizio
     * @param endTime Timestamp di fine
     * @return Lista delle foto nell'intervallo
     */
    @Query("""
        SELECT * FROM photos 
        WHERE taken_at BETWEEN :startTime AND :endTime 
        ORDER BY taken_at DESC
    """)
    suspend fun getPhotosByDateRange(startTime: Long, endTime: Long): List<PhotoEntity>

    /**
     * Recupera foto per perspective.
     * @param perspective Perspective da cercare
     * @return Lista delle foto con quella perspective
     */
    @Query("""
        SELECT * FROM photos 
        WHERE perspective = :perspective 
        ORDER BY taken_at DESC
    """)
    suspend fun getPhotosByPerspective(perspective: String): List<PhotoEntity>

    /**
     * Recupera foto per resolution.
     * @param resolution Resolution da cercare
     * @return Lista delle foto con quella resolution
     */
    @Query("""
        SELECT * FROM photos 
        WHERE resolution = :resolution 
        ORDER BY taken_at DESC
    """)
    suspend fun getPhotosByResolution(resolution: String): List<PhotoEntity>

    // ===============================
    // ✅ NUOVO: QUERY PER DIMENSIONI
    // ===============================

    /**
     * Recupera foto per dimensioni specifiche.
     * @param width Larghezza
     * @param height Altezza
     * @return Lista delle foto con quelle dimensioni
     */
    @Query("""
        SELECT * FROM photos 
        WHERE width = :width AND height = :height 
        ORDER BY taken_at DESC
    """)
    suspend fun getPhotosByDimensions(width: Int, height: Int): List<PhotoEntity>

    /**
     * Recupera foto più grandi di certe dimensioni.
     * @param minWidth Larghezza minima
     * @param minHeight Altezza minima
     * @return Lista delle foto
     */
    @Query("""
        SELECT * FROM photos 
        WHERE width >= :minWidth AND height >= :minHeight 
        ORDER BY (width * height) DESC
    """)
    suspend fun getPhotosLargerThan(minWidth: Int, minHeight: Int): List<PhotoEntity>

    /**
     * Recupera foto più piccole di certe dimensioni.
     * @param maxWidth Larghezza massima
     * @param maxHeight Altezza massima
     * @return Lista delle foto
     */
    @Query("""
        SELECT * FROM photos 
        WHERE width <= :maxWidth AND height <= :maxHeight 
        ORDER BY (width * height) ASC
    """)
    suspend fun getPhotosSmallerThan(maxWidth: Int, maxHeight: Int): List<PhotoEntity>

    /**
     * Recupera foto per range di dimensioni.
     * @param minWidth Larghezza minima
     * @param maxWidth Larghezza massima
     * @param minHeight Altezza minima
     * @param maxHeight Altezza massima
     * @return Lista delle foto nel range
     */
    @Query("""
        SELECT * FROM photos 
        WHERE width BETWEEN :minWidth AND :maxWidth 
        AND height BETWEEN :minHeight AND :maxHeight 
        ORDER BY taken_at DESC
    """)
    suspend fun getPhotosByDimensionRange(
        minWidth: Int, maxWidth: Int,
        minHeight: Int, maxHeight: Int
    ): List<PhotoEntity>

    /**
     * Recupera foto per aspect ratio (landscape/portrait/square).
     * @param aspectRatioMin Aspect ratio minimo (es. 0.9 per quasi square)
     * @param aspectRatioMax Aspect ratio massimo (es. 1.1 per quasi square)
     * @return Lista delle foto nel range aspect ratio
     */
    @Query("""
        SELECT * FROM photos 
        WHERE (CAST(width AS REAL) / CAST(height AS REAL)) BETWEEN :aspectRatioMin AND :aspectRatioMax
        ORDER BY taken_at DESC
    """)
    suspend fun getPhotosByAspectRatio(aspectRatioMin: Float, aspectRatioMax: Float): List<PhotoEntity>

    /**
     * Recupera foto landscape (width > height).
     * @return Lista delle foto landscape
     */
    @Query("""
        SELECT * FROM photos 
        WHERE width > height 
        ORDER BY taken_at DESC
    """)
    suspend fun getLandscapePhotos(): List<PhotoEntity>

    /**
     * Recupera foto portrait (height > width).
     * @return Lista delle foto portrait
     */
    @Query("""
        SELECT * FROM photos 
        WHERE height > width 
        ORDER BY taken_at DESC
    """)
    suspend fun getPortraitPhotos(): List<PhotoEntity>

    /**
     * Recupera foto square (width ≈ height).
     * @param tolerance Tolleranza per considerare una foto quadrata (default 5%)
     * @return Lista delle foto square
     */
    @Query("""
        SELECT * FROM photos 
        WHERE ABS(width - height) <= (CASE WHEN width > height THEN width ELSE height END) * :tolerance / 100.0
        ORDER BY taken_at DESC
    """)
    suspend fun getSquarePhotos(tolerance: Float = 5.0f): List<PhotoEntity>

    /**
     * Recupera la risoluzione media delle foto.
     * @return Media dei pixel (width * height)
     */
    @Query("SELECT AVG(width * height) FROM photos WHERE width > 0 AND height > 0")
    suspend fun getAverageResolution(): Float?

    /**
     * Recupera le dimensioni più comuni.
     * @param limit Numero massimo di dimensioni da restituire
     * @return Lista delle dimensioni più comuni
     */
    @Query("""
        SELECT width, height, COUNT(*) as count 
        FROM photos 
        WHERE width > 0 AND height > 0 
        GROUP BY width, height 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getMostCommonDimensions(limit: Int = 10): List<DimensionStats>

    /**
     * Recupera distribuzione aspect ratio.
     * @return Lista delle categorie aspect ratio con conteggi
     */
    @Query("""
        SELECT 
            CASE 
                WHEN width = height THEN 'square'
                WHEN width > height THEN 'landscape' 
                ELSE 'portrait' 
            END as category,
            ROUND(CAST(width AS REAL) / CAST(height AS REAL), 2) as aspectRatio,
            COUNT(*) as count
        FROM photos 
        WHERE width > 0 AND height > 0
        GROUP BY category, aspectRatio
        ORDER BY count DESC
    """)
    suspend fun getAspectRatioDistribution(): List<AspectRatioStats>

    /**
     * Recupera foto con dimensioni non valide (0x0).
     * @return Lista delle foto con dimensioni mancanti
     */
    @Query("""
        SELECT * FROM photos 
        WHERE width = 0 OR height = 0 
        ORDER BY taken_at DESC
    """)
    suspend fun getPhotosWithInvalidDimensions(): List<PhotoEntity>

    /**
     * Recupera il numero di foto per un check item.
     * @param checkItemId ID del check item
     * @return Numero di foto
     */
    @Query("SELECT COUNT(*) FROM photos WHERE check_item_id = :checkItemId")
    suspend fun getPhotosCountByCheckItemId(checkItemId: String): Int

    /**
     * Recupera il numero totale di foto nel database.
     * @return Numero totale di foto
     */
    @Query("SELECT COUNT(*) FROM photos")
    suspend fun getTotalPhotosCount(): Int

    /**
     * Recupera la dimensione totale di tutte le foto.
     * @return Dimensione totale in bytes
     */
    @Query("SELECT SUM(file_size) FROM photos")
    suspend fun getTotalPhotosSize(): Long?

    /**
     * Recupera la dimensione delle foto per un check item.
     * @param checkItemId ID del check item
     * @return Dimensione totale in bytes
     */
    @Query("SELECT SUM(file_size) FROM photos WHERE check_item_id = :checkItemId")
    suspend fun getPhotosSizeByCheckItemId(checkItemId: String): Long?

    /**
     * Recupera tutti gli ID dei check item che hanno foto.
     * @return Lista degli ID dei check item
     */
    @Query("SELECT DISTINCT check_item_id FROM photos")
    suspend fun getCheckItemIdsWithPhotos(): List<String>

    /**
     * Recupera foto che corrispondono a una caption (ricerca parziale).
     * @param searchQuery Query di ricerca
     * @return Lista delle foto che corrispondono
     */
    @Query("""
        SELECT * FROM photos 
        WHERE caption LIKE '%' || :searchQuery || '%' 
        ORDER BY taken_at DESC
    """)
    suspend fun searchPhotosByCaption(searchQuery: String): List<PhotoEntity>

    /**
     * Recupera foto con dimensione file maggiore di un valore.
     * @param minSize Dimensione minima in bytes
     * @return Lista delle foto
     */
    @Query("""
        SELECT * FROM photos 
        WHERE file_size > :minSize 
        ORDER BY file_size DESC
    """)
    suspend fun getPhotosLargerThan(minSize: Long): List<PhotoEntity>

    /**
     * Recupera le foto più recenti (limitato a un numero).
     * @param limit Numero massimo di foto da recuperare
     * @return Lista delle foto più recenti
     */
    @Query("""
        SELECT * FROM photos 
        ORDER BY taken_at DESC 
        LIMIT :limit
    """)
    suspend fun getRecentPhotos(limit: Int): List<PhotoEntity>

    /**
     * Recupera il prossimo orderIndex per un check item.
     * @param checkItemId ID del check item
     * @return Prossimo orderIndex disponibile
     */
    @Query("""
        SELECT COALESCE(MAX(order_index), -1) + 1 
        FROM photos 
        WHERE check_item_id = :checkItemId
    """)
    suspend fun getNextOrderIndex(checkItemId: String): Int

    // ===== UPDATE =====

    /**
     * Aggiorna una foto esistente.
     * @param photo Entità foto con dati aggiornati
     */
    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    /**
     * Aggiorna solo la caption di una foto.
     * @param photoId ID della foto
     * @param caption Nuova caption
     */
    @Query("""
        UPDATE photos 
        SET caption = :caption  
        WHERE id = :photoId
    """)
    suspend fun updatePhotoCaption(photoId: String, caption: String)

    /**
     * Aggiorna il percorso file di una foto.
     * @param photoId ID della foto
     * @param newFilePath Nuovo percorso del file
     */
    @Query("""
        UPDATE photos 
        SET file_path = :newFilePath 
        WHERE id = :photoId
    """)
    suspend fun updatePhotoFilePath(photoId: String, newFilePath: String)

    /**
     * Aggiorna l'orderIndex di una foto.
     * @param photoId ID della foto
     * @param newOrderIndex Nuovo orderIndex
     */
    @Query("""
        UPDATE photos 
        SET order_index = :newOrderIndex  
        WHERE id = :photoId
    """)
    suspend fun updatePhotoOrderIndex(photoId: String, newOrderIndex: Int)

    /**
     * Aggiorna la perspective di una foto.
     * @param photoId ID della foto
     * @param perspective Nuova perspective
     */
    @Query("""
        UPDATE photos 
        SET perspective = :perspective  
        WHERE id = :photoId
    """)
    suspend fun updatePhotoPerspective(photoId: String, perspective: String?)

    // ===============================
    // ✅ NUOVO: UPDATE DIMENSIONI
    // ===============================

    /**
     * Aggiorna le dimensioni di una foto.
     * @param photoId ID della foto
     * @param width Nuova larghezza
     * @param height Nuova altezza
     */
    @Query("""
        UPDATE photos 
        SET width = :width, height = :height  
        WHERE id = :photoId
    """)
    suspend fun updatePhotoDimensions(photoId: String, width: Int, height: Int)

    /**
     * Aggiorna solo la larghezza di una foto.
     * @param photoId ID della foto
     * @param width Nuova larghezza
     */
    @Query("""
        UPDATE photos 
        SET width = :width  
        WHERE id = :photoId
    """)
    suspend fun updatePhotoWidth(photoId: String, width: Int)

    /**
     * Aggiorna solo l'altezza di una foto.
     * @param photoId ID della foto
     * @param height Nuova altezza
     */
    @Query("""
        UPDATE photos 
        SET height = :height  
        WHERE id = :photoId
    """)
    suspend fun updatePhotoHeight(photoId: String, height: Int)

    /**
     * Riordina le foto di un check item.
     * @param photoOrderUpdates Lista di coppie (photoId, newOrderIndex)
     */
    @Transaction
    suspend fun reorderPhotos(photoOrderUpdates: List<Pair<String, Int>>) {
        photoOrderUpdates.forEach { (photoId, newOrderIndex) ->
            updatePhotoOrderIndex(photoId, newOrderIndex)
        }
    }

    // ===== DELETE =====

    /**
     * Elimina una foto per ID.
     * @param photoId ID della foto da eliminare
     */
    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: String)

    /**
     * Elimina tutte le foto di un check item.
     * @param checkItemId ID del check item
     * @return Numero di righe eliminate
     */
    @Query("DELETE FROM photos WHERE check_item_id = :checkItemId")
    suspend fun deletePhotosByCheckItemId(checkItemId: String): Int

    /**
     * Elimina foto per lista di ID.
     * @param photoIds Lista degli ID da eliminare
     * @return Numero di righe eliminate
     */
    @Query("DELETE FROM photos WHERE id IN (:photoIds)")
    suspend fun deletePhotosByIds(photoIds: List<String>): Int

    /**
     * Elimina foto più vecchie di un timestamp.
     * @param beforeTimestamp Timestamp limite
     * @return Numero di righe eliminate
     */
    @Query("DELETE FROM photos WHERE taken_at < :beforeTimestamp")
    suspend fun deletePhotosOlderThan(beforeTimestamp: Long): Int

    /**
     * Elimina tutte le foto (per operazioni di reset/pulizia).
     * @return Numero di righe eliminate
     */
    @Query("DELETE FROM photos")
    suspend fun deleteAllPhotos(): Int

    // ===== UTILITY =====

    /**
     * Verifica se una foto esiste.
     * @param photoId ID della foto
     * @return True se esiste
     */
    @Query("SELECT EXISTS(SELECT 1 FROM photos WHERE id = :photoId)")
    suspend fun photoExists(photoId: String): Boolean

    /**
     * Verifica se esiste una foto con un determinato percorso file.
     * @param filePath Percorso del file
     * @return True se esiste
     */
    @Query("SELECT EXISTS(SELECT 1 FROM photos WHERE file_path = :filePath)")
    suspend fun photoExistsByPath(filePath: String): Boolean

    /**
     * ✅ AGGIORNATO: Recupera statistiche generali delle foto (con dimensioni).
     * @return Oggetto con statistiche aggregate
     */
    @Query("""
        SELECT 
            COUNT(*) as totalCount,
            SUM(file_size) as totalSize,
            AVG(file_size) as averageSize,
            MIN(taken_at) as oldestTimestamp,
            MAX(taken_at) as newestTimestamp,
            COUNT(CASE WHEN caption != '' THEN 1 END) as photosWithCaption,
            AVG(width * height) as averageResolution,
            COUNT(CASE WHEN width > 0 AND height > 0 THEN 1 END) as photosWithValidDimensions,
            COUNT(CASE WHEN width > height THEN 1 END) as landscapePhotos,
            COUNT(CASE WHEN height > width THEN 1 END) as portraitPhotos,
            COUNT(CASE WHEN width = height THEN 1 END) as squarePhotos
        FROM photos
    """)
    suspend fun getPhotoStatistics(): PhotoStatistics?

    /**
     * Recupera distribuzione per perspective.
     * @return Mappa perspective -> count
     */
    @Query("""
        SELECT perspective, COUNT(*) as count 
        FROM photos 
        WHERE perspective IS NOT NULL 
        GROUP BY perspective
    """)
    suspend fun getPerspectiveDistributionRaw(): List<PerspectiveCount>

    /**
     * Recupera distribuzione per resolution.
     * @return Mappa resolution -> count
     */
    @Query("""
        SELECT resolution, COUNT(*) as count 
        FROM photos 
        WHERE resolution IS NOT NULL 
        GROUP BY resolution
    """)
    suspend fun getResolutionDistributionRaw(): List<ResolutionCount>

    /**
     * Recupera percorsi di foto orfane (check_item_id non esiste nella tabella check_items).
     * @return Lista dei percorsi delle foto orfane
     */
    @Query("""
        SELECT file_path FROM photos 
        WHERE check_item_id NOT IN (SELECT id FROM check_items)
    """)
    suspend fun getOrphanedPhotoPaths(): List<String>

    // DEFAULT -- //

    @Query("SELECT * FROM photos WHERE check_item_id = :checkItemId ORDER BY taken_at DESC")
    fun getPhotosByCheckItemFlow(checkItemId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE check_item_id = :checkItemId ORDER BY taken_at DESC")
    suspend fun getPhotosByCheckItem(checkItemId: String): List<PhotoEntity>

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    // ============================================================
    // METODI PER STATISTICHE (richiesti dal Repository)
    // ============================================================

    @Query("""
        SELECT COUNT(*) FROM photos 
        INNER JOIN check_items ON photos.check_item_id = check_items.id 
        WHERE check_items.checkup_id = :checkUpId
    """)
    suspend fun getPhotosCountByCheckUp(checkUpId: String): Int

    @Query("SELECT COUNT(*) FROM photos WHERE check_item_id = :checkItemId")
    suspend fun getPhotosCountByCheckItem(checkItemId: String): Int

    // ============================================================
    // RICERCHE E UTILITY
    // ============================================================

    @Query("""
        SELECT photos.* FROM photos 
        INNER JOIN check_items ON photos.check_item_id = check_items.id 
        WHERE check_items.checkup_id = :checkUpId 
        ORDER BY photos.taken_at DESC
    """)
    fun getPhotosByCheckUpFlow(checkUpId: String): Flow<List<PhotoEntity>>

    @Query("""
        SELECT photos.* FROM photos 
        INNER JOIN check_items ON photos.check_item_id = check_items.id 
        WHERE check_items.checkup_id = :checkUpId 
        ORDER BY photos.taken_at DESC
    """)
    suspend fun getPhotosByCheckUp(checkUpId: String): List<PhotoEntity>

    @Query("SELECT SUM(file_size) FROM photos WHERE check_item_id = :checkItemId")
    suspend fun getTotalFileSizeByCheckItem(checkItemId: String): Long?

    @Query("""
        SELECT SUM(photos.file_size) FROM photos 
        INNER JOIN check_items ON photos.check_item_id = check_items.id 
        WHERE check_items.checkup_id = :checkUpId
    """)
    suspend fun getTotalFileSizeByCheckUp(checkUpId: String): Long?

    // Cleanup per foto orfane
    @Query("""
        DELETE FROM photos WHERE check_item_id NOT IN (
            SELECT id FROM check_items
        )
    """)
    suspend fun deleteOrphanedPhotos()
}