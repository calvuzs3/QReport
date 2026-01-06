package net.calvuz.qreport.backup.data.model

/**
 * Eccezione custom per errori archivi foto
 */
class PhotoArchiveException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)