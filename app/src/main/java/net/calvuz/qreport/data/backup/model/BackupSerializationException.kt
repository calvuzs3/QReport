package net.calvuz.qreport.data.backup.model

/**
 * Eccezione custom per errori di serializzazione
 */
class BackupSerializationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)