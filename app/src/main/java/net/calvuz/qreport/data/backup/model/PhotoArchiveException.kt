package net.calvuz.qreport.data.backup.model

/**
 * Eccezione custom per errori archivi foto
 */
class PhotoArchiveException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)