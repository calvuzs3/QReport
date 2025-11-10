package net.calvuz.qreport.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Utility per gestione date e formattazione
 */
object DateTimeUtils {

    /**
     * Converte kotlinx.datetime.Instant in stringa sicura per nomi file
     * Formato: YYYYMMDD (solo data, locale)
     *
     * @return Stringa formato "20241110" per utilizzo nei nomi file
     */
    fun Instant.toFilenameSafeDate(): String {
        val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())

        return buildString {
            append(localDateTime.year.toString().padStart(4, '0'))
            append(localDateTime.monthNumber.toString().padStart(2, '0'))
            append(localDateTime.dayOfMonth.toString().padStart(2, '0'))
        }
    }
}