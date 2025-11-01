package net.calvuz.qreport.domain.model.photo

/**
 * Stati possibili per le operazioni sulle foto
 */
sealed class PhotoResult<out T> {
    data class Success<T>(val data: T) : PhotoResult<T>()
    data class Error(val exception: Throwable, val errorType: PhotoErrorType) : PhotoResult<Nothing>()
    data object Loading : PhotoResult<Nothing>()
}