package net.calvuz.qreport.backup.data.model

/**
 * Eccezione custom per errori di serializzazione
 */
class BackupSerializationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)