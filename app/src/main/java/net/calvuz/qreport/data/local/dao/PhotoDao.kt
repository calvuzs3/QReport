package net.calvuz.qreport.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.data.local.entity.PhotoEntity

@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos WHERE check_item_id = :checkItemId ORDER BY taken_at DESC")
    fun getPhotosByCheckItemFlow(checkItemId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE check_item_id = :checkItemId ORDER BY taken_at DESC")
    suspend fun getPhotosByCheckItem(checkItemId: String): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getPhotoById(id: String): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhotoById(id: String)

    @Query("DELETE FROM photos WHERE check_item_id = :checkItemId")
    suspend fun deletePhotosByCheckItemId(checkItemId: String)

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