package net.calvuz.qreport.di

/**
 * Interface per gestione file e storage
 */
interface FileManager {
    fun getPhotosDirectory(): String
    fun getExportsDirectory(): String
    fun createPhotoFile(checkItemId: String): String
    fun deletePhotoFile(filePath: String): Boolean
    fun getFileSize(filePath: String): Long
}