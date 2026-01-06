package net.calvuz.qreport.photo.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import net.calvuz.qreport.photo.data.local.dao.PhotoDao
import net.calvuz.qreport.photo.data.local.mapper.toDomain
import net.calvuz.qreport.photo.data.local.mapper.toEntity
import net.calvuz.qreport.photo.domain.model.Photo
import net.calvuz.qreport.photo.domain.model.PhotoFilter
import net.calvuz.qreport.photo.domain.model.PhotoPerspective
import net.calvuz.qreport.photo.domain.model.PhotoResolution
import net.calvuz.qreport.photo.domain.model.PhotoSummary
import net.calvuz.qreport.photo.domain.repository.PhotoRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementazione concreta del PhotoRepository utilizzando Room Database.
 * AGGIORNATA per usare la PhotoEntity esistente con TypeConverters eleganti.
 *
 * Vantaggi della vostra implementazione:
 * - TypeConverters automatici per PhotoLocation, PhotoPerspective, PhotoResolution
 * - Niente JSON serialization manuale
 * - Type safety completa
 * - CheckUp integration già presente
 */
@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao
) : PhotoRepository {

    // ===== CREATE =====

    override suspend fun insertPhoto(photo: Photo): String {
        photoDao.insertPhoto(photo.toEntity())
        return photo.id
    }

    override suspend fun insertPhotos(photos: List<Photo>): List<String> {
        val entities = photos.toEntity()
        photoDao.insertPhotos(entities)
        return photos.map { it.id }
    }

    // ===== READ =====

    override suspend fun getPhotoById(photoId: String): Photo? {
        return photoDao.getPhotoById(photoId)?.toDomain()
    }

    override fun getPhotosByCheckItemId(checkItemId: String): Flow<List<Photo>> {
        return photoDao.getPhotosByCheckItemIdFlow(checkItemId)
            .map { entities -> entities.toDomain() }
    }

    override fun getAllPhotos(): Flow<List<Photo>> {
        return photoDao.getAllPhotosFlow()
            .map { entities -> entities.toDomain() }
    }

    override fun getFilteredPhotos(filter: PhotoFilter): Flow<List<Photo>> {
        // Utilizziamo il loro DAO per filtri base, poi applichiamo filtri aggiuntivi
        return if (filter.checkItemId != null) {
            getPhotosByCheckItemId(filter.checkItemId)
        } else {
            getAllPhotos()
        }.map { photos ->
            var filteredPhotos = photos

            // Applica filtri aggiuntivi
            filter.dateRange?.let { (startDate, endDate) ->
                filteredPhotos = filteredPhotos.filter { photo ->
                    photo.takenAt >= startDate && photo.takenAt <= endDate
                }
            }

            filter.minFileSize?.let { minSize ->
                filteredPhotos = filteredPhotos.filter { it.fileSize >= minSize }
            }

            filter.maxFileSize?.let { maxSize ->
                filteredPhotos = filteredPhotos.filter { it.fileSize <= maxSize }
            }

            filter.hasCaption?.let { hasCaption ->
                filteredPhotos = if (hasCaption) {
                    filteredPhotos.filter { it.caption.isNotBlank() }
                } else {
                    filteredPhotos.filter { it.caption.isBlank() }
                }
            }

            filter.perspective?.let { perspective ->
                filteredPhotos = filteredPhotos.filter {
                    it.metadata.perspective == perspective
                }
            }

            filter.resolution?.let { resolution ->
                filteredPhotos = filteredPhotos.filter {
                    it.metadata.resolution == resolution
                }
            }

            // ✅ NUOVO: Filtri dimensioni
            filter.minWidth?.let { minWidth ->
                filteredPhotos = filteredPhotos.filter {
                    it.metadata.width >= minWidth
                }
            }

            filter.maxWidth?.let { maxWidth ->
                filteredPhotos = filteredPhotos.filter {
                    it.metadata.width <= maxWidth
                }
            }

            filter.minHeight?.let { minHeight ->
                filteredPhotos = filteredPhotos.filter {
                    it.metadata.height >= minHeight
                }
            }

            filter.maxHeight?.let { maxHeight ->
                filteredPhotos = filteredPhotos.filter {
                    it.metadata.height <= maxHeight
                }
            }

            filter.aspectRatioCategory?.let { category ->
                filteredPhotos = filteredPhotos.filter { photo ->
                    val width = photo.metadata.width.toFloat()
                    val height = photo.metadata.height.toFloat()
                    if (width == 0f || height == 0f) return@filter false

                    val aspectRatio = width / height
                    when (category) {
                        "landscape" -> aspectRatio > 1.1f
                        "portrait" -> aspectRatio < 0.9f
                        "square" -> aspectRatio in 0.9f..1.1f
                        else -> true
                    }
                }
            }



            filteredPhotos
        }
    }

    override suspend fun getPhotosByPerspective(perspective: PhotoPerspective): List<Photo> {
        return photoDao.getPhotosByPerspective(perspective.name).toDomain()
    }

    override suspend fun getPhotosByResolution(resolution: PhotoResolution): List<Photo> {
        return photoDao.getPhotosByResolution(resolution.name).toDomain()
    }


    // ===============================
    // ✅ NUOVO: METODI DIMENSIONI
    // ===============================

    /**
     * Recupera foto per dimensioni specifiche.
     */
    suspend fun getPhotosByDimensions(width: Int, height: Int): List<Photo> {
        return photoDao.getPhotosByDimensions(width, height).toDomain()
    }

    /**
     * Recupera foto più grandi di certe dimensioni.
     */
    suspend fun getPhotosLargerThan(minWidth: Int, minHeight: Int): List<Photo> {
        return photoDao.getPhotosLargerThan(minWidth, minHeight).toDomain()
    }

    /**
     * Recupera foto più piccole di certe dimensioni.
     */
    suspend fun getPhotosSmallerThan(maxWidth: Int, maxHeight: Int): List<Photo> {
        return photoDao.getPhotosSmallerThan(maxWidth, maxHeight).toDomain()
    }

    /**
     * Recupera foto per range di dimensioni.
     */
    suspend fun getPhotosByDimensionRange(
        minWidth: Int, maxWidth: Int,
        minHeight: Int, maxHeight: Int
    ): List<Photo> {
        return photoDao.getPhotosByDimensionRange(minWidth, maxWidth, minHeight, maxHeight).toDomain()
    }

    /**
     * Recupera foto per aspect ratio.
     */
    suspend fun getPhotosByAspectRatio(aspectRatioMin: Float, aspectRatioMax: Float): List<Photo> {
        return photoDao.getPhotosByAspectRatio(aspectRatioMin, aspectRatioMax).toDomain()
    }

    /**
     * Recupera foto landscape.
     */
    suspend fun getLandscapePhotos(): List<Photo> {
        return photoDao.getLandscapePhotos().toDomain()
    }

    /**
     * Recupera foto portrait.
     */
    suspend fun getPortraitPhotos(): List<Photo> {
        return photoDao.getPortraitPhotos().toDomain()
    }

    /**
     * Recupera foto square.
     */
    suspend fun getSquarePhotos(tolerance: Float = 5.0f): List<Photo> {
        return photoDao.getSquarePhotos(tolerance).toDomain()
    }

    /**
     * Recupera foto con dimensioni non valide.
     */
    suspend fun getPhotosWithInvalidDimensions(): List<Photo> {
        return photoDao.getPhotosWithInvalidDimensions().toDomain()
    }

    /**
     * Recupera la risoluzione media delle foto.
     */
    suspend fun getAverageResolution(): Float {
        return photoDao.getAverageResolution() ?: 0f
    }

    /**
     * Recupera le dimensioni più comuni.
     */
    suspend fun getMostCommonDimensions(limit: Int = 10): List<DimensionStats> {
        return photoDao.getMostCommonDimensions(limit).map { stats ->
            DimensionStats(
                width = stats.width,
                height = stats.height,
                count = stats.count,
                aspectRatio = if (stats.height > 0) stats.width.toFloat() / stats.height.toFloat() else 0f,
                category = when {
                    stats.width == stats.height -> "square"
                    stats.width > stats.height -> "landscape"
                    else -> "portrait"
                }
            )
        }
    }


    /**
     * NUOVO: Metodo che sfrutta la vostra CheckUp integration
     */
    override suspend fun getPhotosByCheckUp(checkUpId: String): List<Photo> {
        return photoDao.getPhotosByCheckUp(checkUpId).toDomain()
    }

    /**
     * NUOVO: Flow per CheckUp photos
     */
    fun getPhotosByCheckUpFlow(checkUpId: String): Flow<List<Photo>> {
        return photoDao.getPhotosByCheckUpFlow(checkUpId)
            .map { entities -> entities.toDomain() }
    }

    override suspend fun getPhotosCountByCheckItemId(checkItemId: String): Int {
        return photoDao.getPhotosCountByCheckItemId(checkItemId)
    }

    /**
     * NUOVO: Count per CheckUp
     */
    override suspend fun getPhotosCountByCheckUp(checkUpId: String): Int {
        return photoDao.getPhotosCountByCheckUp(checkUpId)
    }

    override suspend fun getPhotosSummary(): PhotoSummary {
        val statistics = photoDao.getPhotoStatistics()
        val perspectiveDistribution = getPerspectiveDistribution()
        val resolutionDistribution = getResolutionDistribution()

        return if (statistics != null) {
            PhotoSummary(
                totalPhotos = statistics.totalCount,
                totalSize = statistics.totalSize,
                photosWithCaption = statistics.photosWithCaption,
                averageFileSize = statistics.averageSize,
                oldestPhoto = Instant.Companion.fromEpochMilliseconds(statistics.oldestTimestamp),
                newestPhoto = Instant.Companion.fromEpochMilliseconds(statistics.newestTimestamp),
                perspectiveDistribution = perspectiveDistribution,
                resolutionDistribution = resolutionDistribution,
                // ✅ NUOVO: Statistiche dimensioni
                averageResolution = statistics.averageResolution ?: 0f,
                photosWithValidDimensions = statistics.photosWithValidDimensions ?: 0,
                landscapePhotos = statistics.landscapePhotos ?: 0,
                portraitPhotos = statistics.portraitPhotos ?: 0,
                squarePhotos = statistics.squarePhotos ?: 0
            )
        } else {
            PhotoSummary(
                totalPhotos = 0,
                totalSize = 0L,
                photosWithCaption = 0,
                averageFileSize = 0L,
                oldestPhoto = null,
                newestPhoto = null,
                averageResolution = 0f,
                photosWithValidDimensions = 0,
                landscapePhotos = 0,
                portraitPhotos = 0,
                squarePhotos = 0,
            )
        }
    }

    override suspend fun getPhotosByDateRange(
        startDate: Instant,
        endDate: Instant
    ): List<Photo> {
        val startTimestamp = startDate.toEpochMilliseconds()
        val endTimestamp = endDate.toEpochMilliseconds()

        return photoDao.getPhotosByDateRange(startTimestamp, endTimestamp)
            .toDomain()
    }

    override suspend fun getNextOrderIndex(checkItemId: String): Int {
        return photoDao.getNextOrderIndex(checkItemId)
    }

    // ===== UPDATE =====

    override suspend fun updatePhoto(photo: Photo) {
        photoDao.updatePhoto(photo.toEntity())
    }

    override suspend fun updatePhotoCaption(photoId: String, caption: String) {
        photoDao.updatePhotoCaption(photoId, caption)
    }

    override suspend fun updatePhotoFilePath(photoId: String, newFilePath: String) {
        photoDao.updatePhotoFilePath(photoId, newFilePath)
    }

    override suspend fun updatePhotoOrderIndex(photoId: String, newOrderIndex: Int) {
        photoDao.updatePhotoOrderIndex(photoId, newOrderIndex)
    }

    override suspend fun updatePhotoPerspective(photoId: String, perspective: PhotoPerspective?) {
        photoDao.updatePhotoPerspective(photoId, perspective?.name)
    }

    override suspend fun reorderPhotos(photoOrderUpdates: List<Pair<String, Int>>) {
        photoDao.reorderPhotos(photoOrderUpdates)
    }

    // ===== DELETE =====

    override suspend fun deletePhoto(photoId: String) {
        photoDao.deletePhotoById(photoId)
    }

    override suspend fun deletePhotosByCheckItemId(checkItemId: String): Int {
        return photoDao.deletePhotosByCheckItemId(checkItemId)
    }

    override suspend fun deletePhotos(photoIds: List<String>): Int {
        return photoDao.deletePhotosByIds(photoIds)
    }

    override suspend fun deletePhotosOlderThan(beforeDate: Instant): Int {
        val beforeTimestamp = beforeDate.toEpochMilliseconds()
        return photoDao.deletePhotosOlderThan(beforeTimestamp)
    }

    /**
     * Cleanup foto orfane.
     * @return Numero di foto eliminate
     */
    override suspend fun deleteOrphanedPhotos() {
        photoDao.deleteOrphanedPhotos()
    }

    // ===== UTILITY =====

    override suspend fun photoExists(photoId: String): Boolean {
        return photoDao.photoExists(photoId)
    }

    override suspend fun getTotalPhotosSize(): Long {
        return photoDao.getTotalPhotosSize() ?: 0L
    }

    override suspend fun getPhotosSize(checkItemId: String): Long {
        return photoDao.getPhotosSizeByCheckItemId(checkItemId) ?: 0L
    }

    /**
     * NUOVO: Size per CheckUp
     */
    suspend fun getPhotosSizeByCheckUp(checkUpId: String): Long {
        return photoDao.getTotalFileSizeByCheckUp(checkUpId) ?: 0L
    }

    override suspend fun getCheckItemIdsWithPhotos(): List<String> {
        return photoDao.getCheckItemIdsWithPhotos()
    }

    override suspend fun getOrphanedPhotoPaths(): List<String> {
        return photoDao.getOrphanedPhotoPaths()
    }

    override suspend fun getPerspectiveDistribution(): Map<PhotoPerspective, Int> {
        val rawDistribution = photoDao.getPerspectiveDistributionRaw()
        return rawDistribution.mapNotNull { (perspectiveString, count) ->
            try {
                PhotoPerspective.valueOf(perspectiveString) to count
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    override suspend fun getResolutionDistribution(): Map<PhotoResolution, Int> {
        val rawDistribution = photoDao.getResolutionDistributionRaw()
        return rawDistribution.mapNotNull { (resolutionString, count) ->
            try {
                PhotoResolution.valueOf(resolutionString) to count
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    override suspend fun searchPhotosByCaption(query: String): List<Photo> {
        return photoDao.searchPhotosByCaption(query).toDomain()
    }



    // ===============================
// ✅ NUOVO: DATA CLASSES PER DIMENSIONI
// ===============================

    /**
     * Statistiche dimensioni foto
     */
    data class DimensionStats(
        val width: Int,
        val height: Int,
        val count: Int,
        val aspectRatio: Float,
        val category: String
    )

}