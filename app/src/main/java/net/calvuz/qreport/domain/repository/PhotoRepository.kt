package net.calvuz.qreport.domain.repository

import kotlinx.coroutines.flow.Flow
import net.calvuz.qreport.domain.model.Photo

interface PhotoRepository {

    fun getPhotosByCheckUpId(checkUpId: String): Flow<List<Photo>>

    fun getPhotosByCheckItemId(checkItemId: String): Flow<List<Photo>>

    suspend fun getPhotoById(id: String): Photo?

    suspend fun addPhoto(photo: Photo): String

    suspend fun updatePhoto(photo: Photo)

    suspend fun updatePhotoCaption(id: String, caption: String)

    suspend fun deletePhoto(id: String)

    suspend fun deletePhotosByCheckUpId(checkUpId: String)

    suspend fun getPhotoCount(checkUpId: String): Int

    suspend fun getTotalFileSize(checkUpId: String): Long
}